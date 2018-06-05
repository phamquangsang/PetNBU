package com.petnbu.petnbu.repo

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.Transformations
import android.net.Uri
import androidx.work.*
import com.petnbu.petnbu.AppExecutors
import com.petnbu.petnbu.SharedPrefUtil
import com.petnbu.petnbu.api.ApiResponse
import com.petnbu.petnbu.api.WebService
import com.petnbu.petnbu.db.PetDb
import com.petnbu.petnbu.jobs.CompressPhotoWorker
import com.petnbu.petnbu.jobs.CreateEditFeedWorker
import com.petnbu.petnbu.jobs.UploadPhotoWorker
import com.petnbu.petnbu.model.*
import com.petnbu.petnbu.model.LocalStatus.STATUS_UPLOADING
import com.petnbu.petnbu.util.IdUtil
import com.petnbu.petnbu.util.RateLimiter
import com.petnbu.petnbu.util.Toaster
import com.petnbu.petnbu.util.TraceUtils
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject
constructor(private val mPetDb: PetDb, private val mAppExecutors: AppExecutors, private val mWebService: WebService, private val mToaster: Toaster) {

    private val mRateLimiter = RateLimiter<String>(10, TimeUnit.MINUTES)

    fun loadFeeds(pagingId: String, userId: String): LiveData<Resource<List<FeedUI>>> {
        return object : NetworkBoundResource<List<FeedUI>, List<Feed>>(mAppExecutors) {
            override fun saveCallResult(item: List<Feed>) {
                val listId = ArrayList<String>(item.size)
                val paging: Paging
                item.forEach { listId.add(it.feedId) }
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
                        return@switchMap mPetDb.feedDao().loadFeedsIds(input.ids, userId)
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

    fun getFeed(feedId: String): LiveData<Resource<Feed>> {
        return object : NetworkBoundResource<Feed, Feed>(mAppExecutors) {
            override fun saveCallResult(item: Feed) {
                mPetDb.feedDao().insertFromFeed(item)
                mPetDb.userDao().insert(item.feedUser)
                item.latestComment?.apply {
                    //the latestComment return from server does not have latestSubComment
                    mPetDb.commentDao().insertIfNotExists(toEntity())
                    mPetDb.userDao().insert(feedUser)
                }
            }

            override fun shouldFetch(data: Feed?): Boolean {
                return data == null
            }

            override fun deleteDataFromDb(body: Feed?) {
                mPetDb.feedDao().deleteFeedById(feedId)
            }

            override fun loadFromDb(): LiveData<Feed> {
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
                        return@switchMap mPetDb.feedDao().loadFeedsIds(input.ids, userId)
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

    fun fetchNextUserFeedPage(pagingId: String): LiveData<Resource<Boolean>> {
        val fetchNextPageUserFeed = FetchNextPageUserFeed(pagingId, mWebService, mPetDb, mAppExecutors)
        mAppExecutors.networkIO().execute(fetchNextPageUserFeed)
        return fetchNextPageUserFeed.liveData
    }

    fun createNewFeed(feed: Feed) {
        mAppExecutors.diskIO().execute {
            mPetDb.runInTransaction {
                mPetDb.userDao().findUserById(SharedPrefUtil.userId)?.run {
                    val feedUser = FeedUser(this.userId, this.avatar, this.name)
                    feed.status = STATUS_UPLOADING
                    feed.feedUser = feedUser
                    feed.timeCreated = Date()
                    feed.timeUpdated = Date()
                    feed.feedId = IdUtil.generateID("feed")
                    mPetDb.feedDao().insertFromFeed(feed)
                }

            }
            TraceUtils.begin("scheduleSaveFeedWorker") { scheduleSaveFeedWorker(feed, false) }
        }
    }

    fun updateFeed(feed: Feed) {
        mAppExecutors.diskIO().execute {
            mPetDb.runInTransaction {
                mPetDb.feedDao().findFeedEntityById(feed.feedId)?.apply {
                    this.status = STATUS_UPLOADING
                    this.timeUpdated = Date()
                    this.content = feed.content
                    this.photos = feed.photos
                    mPetDb.feedDao().update(this)
                }
            }
            TraceUtils.begin("scheduleSaveFeedWorker") { scheduleSaveFeedWorker(feed, true) }
        }
    }

    private fun scheduleSaveFeedWorker(feed: Feed, isUpdating: Boolean) {
        val uploadWorks = ArrayList<OneTimeWorkRequest>(feed.photos.size)
        // Constraints that defines when the task should run
        val uploadConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val compressionWork = OneTimeWorkRequest.Builder(CompressPhotoWorker::class.java)
                .setInputData(CompressPhotoWorker.data(feed.photos))
                .build()

        for (photo in feed.photos) {
            val key = Uri.parse(photo.originUrl).lastPathSegment
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
            mPetDb.feedDao().findFeedEntityById(feedId)?.apply {
                if (this.likeInProgress) return@execute
                likeInProgress = true
                mPetDb.feedDao().update(this)
                mAppExecutors.networkIO().execute {
                    if (this.isLiked) {
                        unLikeFeed(this, userId, feedId)
                    } else {
                        likeFeed(this, userId, feedId)
                    }
                }
            }
        }
    }

    private fun likeFeed(feed: FeedEntity, userId: String, feedId: String) {
        val result = mWebService.likeFeed(userId, feedId)
        result.observeForever(object : Observer<ApiResponse<Feed>> {
            override fun onChanged(feedApiResponse: ApiResponse<Feed>?) {
                if (feedApiResponse != null) {
                    result.removeObserver(this)
                    mAppExecutors.diskIO().execute {
                        mPetDb.runInTransaction {
                            if (feedApiResponse.isSuccessful && feedApiResponse.body != null) {
                                val feedResult = feedApiResponse.body.toEntity()
                                feedResult.likeInProgress = false
                                mPetDb.feedDao().update(feedResult)
                            } else {
                                mAppExecutors.mainThread().execute { mToaster.makeText(feedApiResponse.errorMessage) }
                                feed.likeInProgress = false
                                mPetDb.feedDao().update(feed)
                            }
                        }
                    }
                }
            }
        })
    }

    private fun unLikeFeed(feed: FeedEntity, userId: String, feedId: String) {
        val result = mWebService.unLikeFeed(userId, feedId)
        result.observeForever(object : Observer<ApiResponse<Feed>> {
            override fun onChanged(feedApiResponse: ApiResponse<Feed>?) {
                if (feedApiResponse != null) {
                    result.removeObserver(this)
                    mAppExecutors.diskIO().execute {
                        mPetDb.runInTransaction {
                            if (feedApiResponse.isSuccessful && feedApiResponse.body != null) {
                                val feedResult = feedApiResponse.body.toEntity()
                                feedResult.likeInProgress = false
                                mPetDb.feedDao().update(feedResult)
                            } else {
                                mAppExecutors.mainThread().execute { mToaster.makeText(feedApiResponse.errorMessage) }
                                feed.likeInProgress = false
                                mPetDb.feedDao().update(feed)
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
