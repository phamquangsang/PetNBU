package com.petnbu.petnbu.repo

import android.arch.lifecycle.*
import android.arch.lifecycle.Observer
import androidx.core.net.toUri
import androidx.work.*
import com.petnbu.petnbu.AppExecutors
import com.petnbu.petnbu.api.ApiResponse
import com.petnbu.petnbu.api.WebService
import com.petnbu.petnbu.db.PetDb
import com.petnbu.petnbu.extensions.beginSysTrace
import com.petnbu.petnbu.jobs.CompressPhotoWorker
import com.petnbu.petnbu.jobs.CreateCommentWorker
import com.petnbu.petnbu.jobs.UploadPhotoWorker
import com.petnbu.petnbu.model.*
import com.petnbu.petnbu.util.IdUtil
import com.petnbu.petnbu.util.RateLimiter
import com.petnbu.petnbu.util.SharedPrefUtil
import com.petnbu.petnbu.util.Toaster
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepository @Inject
constructor(private val mPetDb: PetDb, private val mAppExecutors: AppExecutors,
            private val mWebService: WebService, private val mToaster: Toaster) {

    private val mRateLimiter = RateLimiter<String>(10, TimeUnit.MINUTES)

    fun createComment(toFeedId: String, content :String, photo :Photo?) {
        mAppExecutors.diskIO().execute {
            mPetDb.runInTransaction {
                mPetDb.userDao().findUserById(SharedPrefUtil.userId)?.run {
                    val feedUser = FeedUser(this.userId, this.avatar, this.name)
                    val comment = Comment(IdUtil.generateID("comment"), feedUser, content, photo, localStatus =  LocalStatus.STATUS_UPLOADING,
                            parentCommentId = "", parentFeedId =  toFeedId, timeCreated = Date(), timeUpdated = Date())
                    mPetDb.commentDao().insertFromComment(comment)

                    beginSysTrace("scheduleSaveComment") {
                        scheduleSaveCommentWorker(comment)
                    }
                }
            }
        }
    }

    fun createSubComment(toCommentId: String, content :String, photo :Photo?) {
        mAppExecutors.diskIO().execute {
            mPetDb.runInTransaction {
                mPetDb.userDao().findUserById(SharedPrefUtil.userId)?.run {
                    val feedUser = FeedUser(this.userId, this.avatar, this.name)
                    val comment = Comment(IdUtil.generateID("comment"), feedUser, content, photo, localStatus =  LocalStatus.STATUS_UPLOADING,
                            parentCommentId = toCommentId, parentFeedId =  "", timeCreated = Date(), timeUpdated = Date())
                    mPetDb.commentDao().insertFromComment(comment)

                    beginSysTrace("scheduleSaveComment") {
                        scheduleSaveCommentWorker(comment)
                    }
                }
            }
        }
    }

    private fun loadFeedById(feedId: String): LiveData<Resource<Feed>> {
        return object : NetworkBoundResource<Feed, Feed>(mAppExecutors) {
            override fun saveCallResult(item: Feed) {
                mPetDb.feedDao().insertFromFeed(item)
                mPetDb.userDao().insert(item.feedUser)
                if (item.latestComment != null) {
                    mPetDb.commentDao().insertFromComment(item.latestComment)
                    mPetDb.userDao().insert(item.latestComment!!.feedUser)
                }
            }

            override fun shouldFetch(data: Feed?) = data == null

            override fun deleteDataFromDb(body: Feed?) = mPetDb.feedDao().deleteFeedById(feedId)

            override fun loadFromDb(): LiveData<Feed> = mPetDb.feedDao().loadFeedById(feedId)

            override fun createCall(): LiveData<ApiResponse<Feed>> = mWebService.getFeed(feedId)
        }.asLiveData()
    }

    fun getFeedCommentsIncludeFeedContentHeader(feedId: String, after: Long, limit: Int): LiveData<Resource<List<CommentUI>>> {
        val feedSource = loadFeedById(feedId)
        val mediatorLiveData = MediatorLiveData<Resource<List<CommentUI>>>()
        mediatorLiveData.addSource(feedSource) { feedResource ->
            if (feedResource != null) {
                if (feedResource.status == Status.SUCCESS && feedResource.data != null) {
                    mediatorLiveData.removeSource(feedSource)
                    val feedComment = createCommentUIFromFeed(feedResource.data)
                    val commentsLiveData = getFeedComments(feedId, after, limit)
                    mediatorLiveData.addSource(commentsLiveData) { resourceComments ->
                        if (resourceComments != null) {
                            if (resourceComments.data != null){
                                val resourceCommentsMutable = arrayListOf(feedComment)
                                resourceComments.data.forEach({
                                    resourceCommentsMutable.add(it)
                                })
                                mediatorLiveData.setValue(Resource.success(resourceCommentsMutable))
                            }
                        }
                    }
                }
            }
        }
        return mediatorLiveData
    }

    private fun createCommentUIFromFeed(feed: Feed): CommentUI {
        return CommentUI(feed.feedId, feed.feedUser, feed.content, timeCreated = feed.timeCreated!!)
    }

    private fun getFeedComments(feedId: String, after: Long, limit: Int): LiveData<Resource<List<CommentUI>>> {

        return object : NetworkBoundResource<List<CommentUI>, List<Comment>>(mAppExecutors) {
            override fun saveCallResult(item: List<Comment>) {

                val listId = ArrayList<String>(item.size)
                val pagingId = Paging.feedCommentsPagingId(feedId)
                for (comment in item) {
                    listId.add(comment.id)
                }
                val paging = Paging(pagingId,
                        listId, false,
                        if (listId.isEmpty()) null else listId[listId.size - 1])
                mPetDb.runInTransaction {
                    mPetDb.commentDao().insertListComment(item)
                    for (comment in item) {
                        mPetDb.userDao().insert(comment.feedUser)
                        comment.latestComment?.run {
                            mPetDb.commentDao().insertFromComment(this)
                            mPetDb.userDao().insert(this.feedUser)
                        }
                    }
                    mPetDb.pagingDao().insert(paging)
                }
            }

            override fun shouldFetch(data: List<CommentUI>?): Boolean {
                return data == null || data.isEmpty() || mRateLimiter.shouldFetch(Paging.feedCommentsPagingId(feedId))
            }

            override fun deleteDataFromDb(body: List<Comment>?) {
                mPetDb.pagingDao().deleteFeedPaging(Paging.feedCommentsPagingId(feedId))
            }

            override fun shouldDeleteOldData(body: List<Comment>?): Boolean {
                return false
            }

            override fun loadFromDb(): LiveData<List<CommentUI>> {
                return Transformations.switchMap(mPetDb.pagingDao().loadFeedPaging(Paging.feedCommentsPagingId(feedId))) { input ->
                    if (input == null) {
                        val data = MutableLiveData<List<CommentUI>>()
                        data.postValue(null)
                        data
                    } else {
                        Timber.i("loadFeedsFromDb paging: %s", input.toString())
                        mPetDb . commentDao ().loadFeedComments(input.getIds()!!, feedId)
                    }
                }
            }

            override fun createCall(): LiveData<ApiResponse<List<Comment>>> {
                return mWebService.getFeedComments(feedId, after, limit)
            }
        }.asLiveData()
    }

    fun getSubComments(parentCommentId: String, after: Long, limit: Int): LiveData<Resource<List<CommentUI>>> {

        return object : NetworkBoundResource<List<CommentUI>, List<Comment>>(mAppExecutors) {
            override fun saveCallResult(item: List<Comment>) {
                val listId = ArrayList<String>(item.size)
                val pagingId = Paging.subCommentsPagingId(parentCommentId)
                for (comment in item) {
                    listId.add(comment.id)
                }
                val paging = Paging(pagingId,
                        listId, false,
                        if (listId.isEmpty()) null else listId[listId.size - 1])
                mPetDb.runInTransaction {
                    mPetDb.commentDao().insertListComment(item)
                    for (comment in item) {
                        mPetDb.userDao().insert(comment.feedUser)
                        comment.latestComment?.run {
                            mPetDb.commentDao().insertFromComment(this.latestComment)
                            mPetDb.userDao().insert(this.feedUser)
                        }

                    }
                    mPetDb.pagingDao().insert(paging)
                }
            }

            override fun shouldFetch(data: List<CommentUI>?): Boolean {
                return data == null || data.isEmpty() || mRateLimiter.shouldFetch(Paging.subCommentsPagingId(parentCommentId))
            }

            override fun deleteDataFromDb(body: List<Comment>?) {
                mPetDb.pagingDao().deleteFeedPaging(Paging.subCommentsPagingId(parentCommentId))
            }

            override fun shouldDeleteOldData(body: List<Comment>?): Boolean {
                return false
            }

            override fun loadFromDb(): LiveData<List<CommentUI>> {
                return Transformations.switchMap(mPetDb.pagingDao()
                        .loadFeedPaging(Paging.subCommentsPagingId(parentCommentId))) { input ->
                    if (input == null) {
                        val data = MutableLiveData<List<CommentUI>>()
                        data.postValue(null)
                        return@switchMap data
                    } else {
                        Timber.i("loadSubCommentsFromDb paging: %s", input.toString())
                        return@switchMap mPetDb . commentDao ().loadSubComments(input.getIds()!!, parentCommentId)
                    }
                }
            }

            override fun createCall(): LiveData<ApiResponse<List<Comment>>> {
                return mWebService.getSubComments(parentCommentId, after, limit)
            }
        }.asLiveData()
    }

    fun fetchCommentsNextPage(feedId: String, pagingId: String): LiveData<Resource<Boolean>> {
        val fetchNextPageTask = FetchNextPageFeedComment(feedId, pagingId, mWebService, mPetDb, mAppExecutors)
        mAppExecutors.networkIO().execute(fetchNextPageTask)
        return fetchNextPageTask.liveData
    }

    fun fetchSubCommentsNextPage(commentid: String, pagingId: String): LiveData<Resource<Boolean>> {
        val fetchNextPageTask = FetchNextPageSubComment(commentid, pagingId, mWebService, mPetDb, mAppExecutors)
        mAppExecutors.networkIO().execute(fetchNextPageTask)
        return fetchNextPageTask.liveData
    }

    private fun scheduleSaveCommentWorker(comment: Comment) {
        val uploadConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        val createCommentWork = OneTimeWorkRequest.Builder(CreateCommentWorker::class.java)
                .setInputData(CreateCommentWorker.data(comment))
                .setConstraints(uploadConstraints)
                .build()

        comment.photo?.run {
            val compressionWork = OneTimeWorkRequest.Builder(CompressPhotoWorker::class.java)
                    .setInputData(CompressPhotoWorker.data(this))
                    .build()

            val key = originUrl.toUri().lastPathSegment
            val uploadWork = OneTimeWorkRequest.Builder(UploadPhotoWorker::class.java)
                    .setConstraints(uploadConstraints)
                    .setInputData(Data.Builder().putString(UploadPhotoWorker.KEY_PHOTO, key).build())
                    .build()
            WorkManager.getInstance()
                    ?.beginWith(compressionWork)
                    ?.then(uploadWork)
                    ?.then(createCommentWork)
                    ?.enqueue()
        } ?: run {
            WorkManager.getInstance()?.enqueue(createCommentWork)
        }


    }

    fun likeCommentHandler(userId: String, commentId: String) {
        mAppExecutors.diskIO().execute {
            val comment = mPetDb.commentDao().getCommentById(commentId)
            if (comment == null || comment.likeInProgress) {
                return@execute
            }
            comment.likeInProgress = true
            mPetDb.commentDao().update(comment)
            mAppExecutors.networkIO().execute {
                if (comment.isLiked) {
                    unLikeComment(comment, userId)
                } else {
                    likeComment(comment, userId)
                }
            }
        }
    }

    private fun likeComment(comment: CommentEntity, userId: String) {
        val result = mWebService.likeComment(userId, comment.id)
        result.observeForever(object : Observer<ApiResponse<Comment>> {
            override fun onChanged(feedApiResponse: ApiResponse<Comment>?) {
                if (feedApiResponse != null) {
                    result.removeObserver(this)
                    mAppExecutors.diskIO().execute {
                        mPetDb.runInTransaction {
                            if (feedApiResponse.isSuccessful && feedApiResponse.body != null) {
                                val commentResult = feedApiResponse.body.toEntity()
                                commentResult.likeInProgress = false
                                mPetDb.commentDao().update(commentResult)
                            } else {
                                mAppExecutors.mainThread().execute { mToaster.makeText(feedApiResponse.errorMessage) }
                                comment.likeInProgress = false
                                mPetDb.commentDao().update(comment)
                            }
                        }
                    }
                }
            }
        })
    }

    private fun unLikeComment(comment: CommentEntity, userId: String) {
        val result = mWebService.unLikeComment(userId, comment.id)
        result.observeForever(object : Observer<ApiResponse<Comment>> {
            override fun onChanged(feedApiResponse: ApiResponse<Comment>?) {
                if (feedApiResponse != null) {
                    result.removeObserver(this)
                    mAppExecutors.diskIO().execute {
                        mPetDb.runInTransaction {
                            if (feedApiResponse.isSuccessful && feedApiResponse.body != null) {
                                val feedResult = feedApiResponse.body.toEntity()
                                feedResult.likeInProgress = false
                                mPetDb.commentDao().update(feedResult)
                            } else {
                                mAppExecutors.mainThread().execute { mToaster.makeText(feedApiResponse.errorMessage) }
                                comment.likeInProgress = false
                                mPetDb.commentDao().update(comment)
                            }
                        }
                    }

                }
            }
        })
    }

    fun likeSubCommentHandler(userId: String, subCommentId: String) {
        mAppExecutors.diskIO().execute {
            val subComment = mPetDb.commentDao().getCommentById(subCommentId)
            if (subComment == null || subComment.likeInProgress) {
                return@execute
            }
            subComment.likeInProgress = true
            mPetDb.commentDao().update(subComment)
            mAppExecutors.networkIO().execute {
                if (subComment.isLiked) {
                    unLikeSubComment(subComment, userId)
                } else {
                    likeSubComment(subComment, userId)
                }
            }
        }
    }

    private fun likeSubComment(subComment: CommentEntity, userId: String) {
        val result = mWebService.likeSubComment(userId, subComment.id)
        result.observeForever(object : Observer<ApiResponse<Comment>> {
            override fun onChanged(feedApiResponse: ApiResponse<Comment>?) {
                if (feedApiResponse != null) {
                    result.removeObserver(this)
                    mAppExecutors.diskIO().execute {
                        mPetDb.runInTransaction {
                            if (feedApiResponse.isSuccessful && feedApiResponse.body != null) {
                                val commentResult = feedApiResponse.body.toEntity()
                                commentResult.likeInProgress = false
                                mPetDb.commentDao().update(commentResult)
                            } else {
                                mAppExecutors.mainThread().execute { mToaster.makeText(feedApiResponse.errorMessage) }
                                subComment.likeInProgress = false
                                mPetDb.commentDao().update(subComment)
                            }
                        }
                    }
                }
            }
        })
    }

    private fun unLikeSubComment(subComment: CommentEntity, userId: String) {
        val result = mWebService.unLikeSubComment(userId, subComment.id)
        result.observeForever(object : Observer<ApiResponse<Comment>> {
            override fun onChanged(feedApiResponse: ApiResponse<Comment>?) {
                if (feedApiResponse != null) {
                    result.removeObserver(this)
                    mAppExecutors.diskIO().execute {
                        mPetDb.runInTransaction {
                            if (feedApiResponse.isSuccessful && feedApiResponse.body != null) {
                                val feedResult = feedApiResponse.body.toEntity()
                                feedResult.likeInProgress = false
                                mPetDb.commentDao().update(feedResult)
                            } else {
                                mAppExecutors.mainThread().execute { mToaster.makeText(feedApiResponse.errorMessage) }
                                subComment.likeInProgress = false
                                mPetDb.commentDao().update(subComment)
                            }
                        }
                    }

                }
            }
        })
    }

    companion object {
        const val COMMENT_PER_PAGE = 30
    }
}
