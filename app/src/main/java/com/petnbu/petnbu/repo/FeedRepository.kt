package com.petnbu.petnbu.repo

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.Transformations
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import android.support.annotation.MainThread
import androidx.core.net.toUri
import androidx.work.*
import com.petnbu.petnbu.AppExecutors
import com.petnbu.petnbu.api.ApiResponse
import com.petnbu.petnbu.api.WebService
import com.petnbu.petnbu.call
import com.petnbu.petnbu.db.PetDb
import com.petnbu.petnbu.db.runInTransaction
import com.petnbu.petnbu.extensions.beginSysTrace
import com.petnbu.petnbu.jobs.CompressPhotoWorker
import com.petnbu.petnbu.jobs.CreateEditFeedWorker
import com.petnbu.petnbu.jobs.UploadPhotoWorker
import com.petnbu.petnbu.model.*
import com.petnbu.petnbu.model.LocalStatus.STATUS_UPLOADING
import com.petnbu.petnbu.observe
import com.petnbu.petnbu.util.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject
constructor(private val mPetDb: PetDb,
            private val mAppExecutors: AppExecutors,
            private val mWebService: WebService) {

    private val mRateLimiter = RateLimiter<String>(10, TimeUnit.SECONDS)

    fun loadFeedsPaging(pagingId: String, userId: String): Listing<FeedUI> {
        val helper = PagingRequestHelper(mAppExecutors.diskIO())
        val initLoad = {
            helper.runIfNotRunning(PagingRequestHelper.RequestType.INITIAL, object : PagingRequestHelper.Request {
                override fun run(callback: PagingRequestHelper.Request.Callback) {
                    loadFeeds(pagingId, success = {
                        callback.recordSuccess()
                    }, failed = { error ->
                        callback.recordFailure(error)
                    })
                }
            })
        }

        val feedsBoundaryCallback = object : PagedList.BoundaryCallback<FeedUI>() {
            val initNetworkState = helper.createStatusLiveData(PagingRequestHelper.RequestType.INITIAL)
            val afterNetworkState = helper.createStatusLiveData(PagingRequestHelper.RequestType.AFTER)
            var isEnded = false

            override fun onZeroItemsLoaded() {
                initLoad()
            }

            override fun onItemAtEndLoaded(itemAtEnd: FeedUI) {
                if (isEnded) {
                    return
                }
                helper.runIfNotRunning(PagingRequestHelper.RequestType.AFTER, object : PagingRequestHelper.Request {
                    override fun run(callback: PagingRequestHelper.Request.Callback) {
                        loadFeedsAfter(itemAtEnd.feedId, pagingId, success = { feeds ->
                            isEnded = feeds.isEmpty()
                            callback.recordSuccess()
                        }, failed = { error ->
                            callback.recordFailure(error)
                        })
                    }
                })
            }
        }

        val pagedListConfig = PagedList.Config.Builder()
                .setPageSize(FEEDS_PER_PAGE)
                .setInitialLoadSizeHint(FEEDS_PER_PAGE)
                .build()
        // create a data source factory from Room
        val dataSourceFactory = mPetDb.feedDao().loadFeedsIds(pagingId, userId)
        val builder = LivePagedListBuilder(dataSourceFactory, pagedListConfig)
                .setBoundaryCallback(feedsBoundaryCallback)
        // we are using a mutable live data to trigger refresh requests which eventually calls
        // refresh method and gets a new live data. Each refresh request by the user becomes a newly
        // dispatched data in refreshTrigger
        val refreshTrigger = MutableLiveData<Unit>()
        val refreshState = Transformations.switchMap(refreshTrigger) {
            refresh(pagingId)
        }
        // init 1st load to get up-to-date data
        initLoad()

        return Listing(pagedList = builder.build(),
                networkState = feedsBoundaryCallback.initNetworkState,
                loadMoreNetworkState = feedsBoundaryCallback.afterNetworkState,
                refreshState = refreshState,
                refresh = {
                    refreshTrigger.call()
                },
                retry = {
                    helper.retryAllFailed()
                })
    }

    private fun loadFeeds(pagingId: String,
                          success: (() -> Unit)? = null,
                          failed: ((Throwable) -> Unit)? = null) {
        mWebService.getGlobalFeeds(System.currentTimeMillis(), FEEDS_PER_PAGE).observe { apiResponse ->
            if (apiResponse != null && apiResponse.isSuccessful && apiResponse.body != null) {
                saveFeeds(pagingId, apiResponse.body)
                success?.invoke()
            } else {
                failed?.invoke(apiResponse?.error ?: Throwable())
            }
        }
    }

    private fun loadFeedsAfter(afterFeedId: String, pagingId: String,
                               success: ((List<Feed>) -> Unit)? = null,
                               failed: ((Throwable) -> Unit)? = null) {
        mWebService.getGlobalFeeds(afterFeedId, FEEDS_PER_PAGE).observe { apiResponse ->
            if (apiResponse != null && apiResponse.isSuccessful) {
                val feeds = apiResponse.body
                if (feeds != null && feeds.isNotEmpty()) {
                    saveFeeds(pagingId, feeds)
                    success?.invoke(apiResponse.body)
                } else {
                    success?.invoke(emptyList())
                }
            } else {
                failed?.invoke(apiResponse?.error ?: Throwable())
            }
        }
    }

    private fun saveFeeds(pagingId: String, feeds: List<Feed>) {
        mPetDb.runInTransaction(mAppExecutors.diskIO()) {
            feeds.forEach {
                it.pagingIds.add(pagingId)
            }

            mPetDb.feedDao().insertFromFeedList(feeds)
            feeds.forEach {
                mPetDb.userDao().insert(it.feedUser)
                it.latestComment?.apply {
                    //the latestComment return from server does not have latestSubComment
                    mPetDb.commentDao().insertIfNotExists(this.toEntity())
                    mPetDb.userDao().insert(this.feedUser)
                }
            }
        }
    }

    fun loadFeeds(pagingId: String, userId: String): LiveData<Resource<List<FeedUI>>> {
        return object : NetworkBoundResource<List<FeedUI>, List<Feed>>(mAppExecutors) {
            override fun saveCallResult(item: List<Feed>) {
                val listId = ArrayList<String>(item.size)
                val paging: Paging
                item.forEach {
                    it.pagingIds.add(pagingId)
                    listId.add(it.feedId)
                }
                paging = Paging(pagingId,
                        listId, listId.isEmpty(),
                        if (listId.isEmpty()) null else listId[listId.size - 1])
                mPetDb.runInTransaction {
                    mPetDb.feedDao().insertFromFeedList(item)
                    item.forEach {
                        mPetDb.userDao().insert(it.feedUser)
                        it.latestComment?.apply {
                            //the latestComment return from server does not have latestSubComment
                            mPetDb.commentDao().insertIfNotExists(this.toEntity())
                            mPetDb.userDao().insert(this.feedUser)
                        }
                    }
                    mPetDb.pagingDao().insert(paging)
                }
            }

            override fun shouldFetch(data: List<FeedUI>?): Boolean {
                return data == null || data.isEmpty() || mRateLimiter.shouldFetch(pagingId)
            }

            override fun loadFromDb(): LiveData<List<FeedUI>> {
                return Transformations.switchMap(mPetDb.pagingDao().loadFeedPaging(pagingId)) { input ->
                    if (input == null) {
                        val data = MutableLiveData<List<FeedUI>>()
                        data.postValue(null)
                        return@switchMap data
                    } else {
                        Timber.i("loadFeedsFromDb paging: %s", input.toString())
                        return@switchMap mPetDb.feedDao().loadFeedsIds(input.getIds()!!, userId)
                    }
                }
            }

            override fun createCall(): LiveData<ApiResponse<List<Feed>>> {
                return mWebService.getGlobalFeeds(System.currentTimeMillis(), FEEDS_PER_PAGE)
            }

            override fun shouldDeleteOldData(body: List<Feed>?): Boolean {
                return true
            }

            override fun deleteDataFromDb(body: List<Feed>?) {
                mPetDb.pagingDao().deleteFeedPaging(pagingId)
            }
        }.asLiveData()
    }

    /**
     * When refresh is called, we simply run a fresh network request and when it arrives, clear
     * the database table and insert all new items in a transaction.
     * <p>
     * Since the PagedList already uses a database bound data source, it will automatically be
     * updated after the database transaction is finished.
     */
    @MainThread
    private fun refresh(pagingId: String): LiveData<NetworkState> {
        val networkState = MutableLiveData<NetworkState>()
        if (mRateLimiter.shouldFetch(pagingId)) {
            networkState.value = NetworkState.LOADING
            mWebService.getGlobalFeeds(System.currentTimeMillis(), FEEDS_PER_PAGE).observe { apiResponse ->
                if (apiResponse != null && apiResponse.isSuccessful && apiResponse.body != null) {
                    saveFeeds(pagingId, apiResponse.body)
                    networkState.value = NetworkState.LOADED
                } else {
                    networkState.value = NetworkState.error(apiResponse?.errorMessage)
                }
            }
        } else {
            networkState.value = NetworkState.LOADED
        }
        return networkState
    }

    fun getFeed(feedId: String): LiveData<Resource<FeedUI>> {
        return object : NetworkBoundResource<FeedUI, Feed>(mAppExecutors) {
            override fun saveCallResult(item: Feed) {
                mPetDb.feedDao().insertFromFeed(item)
                mPetDb.userDao().insert(item.feedUser)
                item.latestComment?.apply {
                    //the latestComment return from server does not have latestSubComment
                    mPetDb.commentDao().insertIfNotExists(toEntity())
                    mPetDb.userDao().insert(feedUser)
                }
            }

            override fun shouldFetch(data: FeedUI?): Boolean {
                return data == null
            }

            override fun deleteDataFromDb(body: Feed?) {
                mPetDb.feedDao().deleteFeedById(feedId)
            }

            override fun loadFromDb(): LiveData<FeedUI> {
                return mPetDb.feedDao().loadFeedById(feedId)
            }

            override fun createCall(): LiveData<ApiResponse<Feed>> {
                return mWebService.getFeed(feedId)
            }
        }.asLiveData()
    }

    fun loadUserFeeds(userId: String, pagingId: String): LiveData<Resource<List<FeedUI>>> {
        return object : NetworkBoundResource<List<FeedUI>, List<Feed>>(mAppExecutors) {
            override fun saveCallResult(item: List<Feed>) {
                val listId = ArrayList<String>(item.size)
                item.forEach { listId.add(it.feedId) }
                val paging = Paging(pagingId,
                        listId, listId.isEmpty(),
                        if (listId.isEmpty()) null else listId[listId.size - 1])

                mPetDb.runInTransaction {
                    mPetDb.feedDao().insertFromFeedList(item)
                    item.forEach {
                        mPetDb.userDao().insert(it.feedUser)
                        it.latestComment?.apply {
                            mPetDb.commentDao().insertIfNotExists(this.toEntity())
                            mPetDb.userDao().insert(this.feedUser)
                        }
                    }
                    mPetDb.pagingDao().insert(paging)
                }
            }

            override fun shouldFetch(data: List<FeedUI>?): Boolean =
                    data == null || data.isEmpty() || mRateLimiter.shouldFetch(pagingId)


            override fun loadFromDb(): LiveData<List<FeedUI>> {
                return Transformations.switchMap(mPetDb.pagingDao().loadFeedPaging(pagingId)) { input ->
                    if (input == null) {
                        val data = MutableLiveData<List<FeedUI>>()
                        data.postValue(null)
                        return@switchMap data
                    } else {
                        Timber.i("loadFeedsFromDb paging: %s", input.toString())
                        return@switchMap mPetDb.feedDao().loadFeedsIds(input.getIds()!!, userId)
                    }
                }
            }

            override fun createCall(): LiveData<ApiResponse<List<Feed>>> = mWebService.getUserFeed(userId, Date().time, FEEDS_PER_PAGE)


            override fun shouldDeleteOldData(body: List<Feed>?): Boolean {
                val shouldDelete = body?.isEmpty() ?: true || mRateLimiter.shouldFetch(pagingId)
                Timber.i("loadUserProfile: should delete = %s", shouldDelete)
                return shouldDelete
            }

            override fun deleteDataFromDb(body: List<Feed>?) {
                Timber.i("deleting old query for userId: %s", pagingId)
                mPetDb.pagingDao().deleteFeedPaging(pagingId)
            }
        }.asLiveData()
    }

    fun fetchNextPage(pagingId: String): LiveData<Resource<Boolean>> {
        val fetchNextPageTask = FetchNextPageGlobalFeed(pagingId, mWebService, mPetDb, mAppExecutors)
        mAppExecutors.networkIO().execute(fetchNextPageTask)
        return fetchNextPageTask.liveData
    }

    fun fetchNextUserFeedPage(userId: String, pagingId: String): LiveData<Resource<Boolean>> {
        val fetchNextPageUserFeed = FetchNextPageUserFeed(userId, pagingId, mWebService, mPetDb, mAppExecutors)
        mAppExecutors.networkIO().execute(fetchNextPageUserFeed)
        return fetchNextPageUserFeed.liveData
    }

    fun createNewFeed(content: String, photos: ArrayList<Photo>) {
        mAppExecutors.diskIO().execute {
            mPetDb.runInTransaction {
                mPetDb.userDao().findUserById(SharedPrefUtil.userId)?.let { localUser ->
                    val localFeed = FeedEntity().also { feed ->
                        feed.feedId = IdUtil.generateID("feed")
                        feed.fromUserId = localUser.userId
                        feed.content = content
                        feed.photos = photos
                        feed.timeCreated = Date()
                        feed.timeUpdated = Date()
                        feed.status = STATUS_UPLOADING
                    }
                    mPetDb.feedDao().insert(localFeed)

                    val feedToProcess = Feed().also { feed ->
                        feed.feedId = localFeed.feedId
                        feed.feedUser = FeedUser(localUser.userId, localUser.avatar, localUser.name)
                        feed.content = localFeed.content
                        feed.photos = localFeed.photos
                        feed.timeCreated = localFeed.timeCreated
                        feed.timeUpdated = localFeed.timeUpdated
                        feed.status = localFeed.status
                    }
                    beginSysTrace("scheduleSaveFeedWorker") {
                        scheduleSaveFeedWorker(feedToProcess, false)
                    }
                }
            }
        }
    }

    fun updateFeed(feed: Feed) {
        mAppExecutors.diskIO().execute {
            mPetDb.runInTransaction {
                mPetDb.feedDao().findFeedEntityById(feed.feedId)?.let { localFeed ->
                    localFeed.status = STATUS_UPLOADING
                    localFeed.timeUpdated = Date()
                    localFeed.content = feed.content
                    localFeed.photos = feed.photos
                    mPetDb.feedDao().update(localFeed)
                }
            }
            beginSysTrace("scheduleSaveFeedWorker") {
                scheduleSaveFeedWorker(feed, true)
            }
        }
    }

    private fun scheduleSaveFeedWorker(feed: Feed, isUpdating: Boolean) {
        val uploadWorks = ArrayList<OneTimeWorkRequest>(feed.photos.size)
        val uploadConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val compressionWork = OneTimeWorkRequest.Builder(CompressPhotoWorker::class.java)
                .setInputData(CompressPhotoWorker.data(feed.photos))
                .build()

        for (photo in feed.photos) {
            val key = photo.originUrl.toUri().lastPathSegment
            val uploadWork = OneTimeWorkRequest.Builder(UploadPhotoWorker::class.java)
                    .setConstraints(uploadConstraints)
                    .setInputData(Data.Builder().putString(UploadPhotoWorker.KEY_PHOTO, key).build())
                    .build()
            uploadWorks.add(uploadWork)
        }

        if (!uploadWorks.isEmpty()) {
            val createFeedWork = OneTimeWorkRequest.Builder(CreateEditFeedWorker::class.java)
                    .setConstraints(uploadConstraints)
                    .setInputData(CreateEditFeedWorker.data(feed, isUpdating))
                    .build()

            WorkManager.getInstance()
                    .beginWith(compressionWork)
                    .then(uploadWorks)
                    .then(createFeedWork)
                    .enqueue()
        }
    }

    fun refresh(): LiveData<Resource<List<Feed>>> {
        return object : NetworkBoundResource<List<Feed>, List<Feed>>(mAppExecutors) {
            override fun saveCallResult(item: List<Feed>) {
                val listId = ArrayList<String>(item.size)
                item.forEach { listId.add(it.feedId) }
                val paging = Paging(Paging.GLOBAL_FEEDS_PAGING_ID,
                        listId, listId.isEmpty(),
                        if (listId.isEmpty()) null else listId[listId.size - 1])
                mPetDb.runInTransaction {
                    mPetDb.feedDao().insertFromFeedList(item)
                    item.forEach {
                        mPetDb.userDao().insert(it.feedUser)
                        it.latestComment?.apply {
                            //the latestComment return from server does not have latestSubComment
                            mPetDb.commentDao().insertIfNotExists(this.toEntity())
                            mPetDb.userDao().insert(this.feedUser)
                        }
                    }
                    mPetDb.pagingDao().insert(paging)
                }
            }

            override fun shouldFetch(data: List<Feed>?): Boolean {
                return true
            }

            override fun loadFromDb(): LiveData<List<Feed>> {
                val data = MutableLiveData<List<Feed>>()
                data.postValue(null)
                return data
            }

            override fun createCall(): LiveData<ApiResponse<List<Feed>>> {
                return mWebService.getGlobalFeeds(System.currentTimeMillis(), FEEDS_PER_PAGE)
            }

            override fun shouldDeleteOldData(body: List<Feed>?): Boolean {
                return true
            }

            override fun deleteDataFromDb(body: List<Feed>?) {
                mPetDb.pagingDao().deleteFeedPaging(Paging.GLOBAL_FEEDS_PAGING_ID)
            }
        }.asLiveData()
    }

    fun likeFeedHandler(userId: String, feedId: String) {
        mAppExecutors.diskIO().execute {
            mPetDb.feedDao().findFeedEntityById(feedId)?.let { localFeed ->
                if (!localFeed.likeInProgress) {
                    localFeed.likeInProgress = true
                    mPetDb.feedDao().update(localFeed)

                    mAppExecutors.networkIO().execute {
                        if (localFeed.isLiked) {
                            unLikeFeed(feedId, userId)
                        } else {
                            likeFeed(feedId, userId)
                        }
                    }
                }
            }
        }
    }

    private fun likeFeed(feedId: String, userId: String) {
        val result = mWebService.likeFeed(userId, feedId)
        result.observeForever(object : Observer<ApiResponse<Feed>> {
            override fun onChanged(feedApiResponse: ApiResponse<Feed>?) {
                if (feedApiResponse != null) {
                    result.removeObserver(this)
                    mAppExecutors.diskIO().execute {
                        mPetDb.runInTransaction {
                            if (feedApiResponse.isSuccessful && feedApiResponse.body != null) {
                                mPetDb.feedDao().findFeedEntityById(feedId)?.let { localFeed ->
                                    localFeed.isLiked = true
                                    localFeed.likeInProgress = false
                                    mPetDb.feedDao().update(localFeed)
                                }
                            } else {
                                mPetDb.feedDao().findFeedEntityById(feedId)?.let { localFeed ->
                                    localFeed.likeInProgress = false
                                    mPetDb.feedDao().update(localFeed)
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    private fun unLikeFeed(feedId: String, userId: String) {
        val result = mWebService.unLikeFeed(userId, feedId)
        result.observeForever(object : Observer<ApiResponse<Feed>> {
            override fun onChanged(feedApiResponse: ApiResponse<Feed>?) {
                if (feedApiResponse != null) {
                    result.removeObserver(this)
                    mAppExecutors.diskIO().execute {
                        mPetDb.runInTransaction {
                            if (feedApiResponse.isSuccessful && feedApiResponse.body != null) {
                                mPetDb.feedDao().findFeedEntityById(feedId)?.let { localFeed ->
                                    localFeed.isLiked = false
                                    localFeed.likeInProgress = false
                                    mPetDb.feedDao().update(localFeed)
                                }
                            } else {
                                mPetDb.feedDao().findFeedEntityById(feedId)?.let { localFeed ->
                                    localFeed.likeInProgress = false
                                    mPetDb.feedDao().update(localFeed)
                                }
                            }
                        }
                    }

                }
            }
        })
    }

    companion object {

        const val FEEDS_PER_PAGE = 10
    }
}
