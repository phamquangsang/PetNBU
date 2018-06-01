package com.petnbu.petnbu.repo

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.Transformations
import android.net.Uri

import com.petnbu.petnbu.AppExecutors
import com.petnbu.petnbu.SharedPrefUtil
import com.petnbu.petnbu.api.ApiResponse
import com.petnbu.petnbu.api.WebService
import com.petnbu.petnbu.db.PetDb
import com.petnbu.petnbu.jobs.CompressPhotoWorker
import com.petnbu.petnbu.jobs.CreateCommentWorker
import com.petnbu.petnbu.jobs.PhotoWorker
import com.petnbu.petnbu.jobs.UploadPhotoWorker
import com.petnbu.petnbu.model.Comment
import com.petnbu.petnbu.model.CommentEntity
import com.petnbu.petnbu.model.CommentUI
import com.petnbu.petnbu.model.Feed
import com.petnbu.petnbu.model.FeedUser
import com.petnbu.petnbu.model.LocalStatus
import com.petnbu.petnbu.model.Paging
import com.petnbu.petnbu.model.Resource
import com.petnbu.petnbu.model.Status
import com.petnbu.petnbu.model.UserEntity
import com.petnbu.petnbu.util.IdUtil
import com.petnbu.petnbu.util.RateLimiter
import com.petnbu.petnbu.util.Toaster
import com.petnbu.petnbu.util.TraceUtils

import java.util.ArrayList
import java.util.Date
import java.util.concurrent.TimeUnit

import javax.inject.Inject
import javax.inject.Singleton

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import timber.log.Timber

@Singleton
class CommentRepository @Inject
constructor(private val mPetDb: PetDb, private val mAppExecutors: AppExecutors, private val mWebService: WebService, private val mApplication: Application, private val mToaster: Toaster) {

    private val mRateLimiter = RateLimiter<String>(10, TimeUnit.MINUTES)

    fun createComment(comment: Comment) {
        mAppExecutors.diskIO().execute {
            mPetDb.runInTransaction {
                val userEntity = mPetDb.userDao().findUserById(SharedPrefUtil.getUserId())
                val feedUser = FeedUser(userEntity.userId, userEntity.avatar, userEntity.name)
                comment.feedUser = feedUser
                comment.localStatus = LocalStatus.STATUS_UPLOADING
                comment.timeCreated = Date()
                comment.timeUpdated = Date()
                comment.id = IdUtil.generateID("comment")
                mPetDb.commentDao().insertFromComment(comment)
            }
            TraceUtils.begin("scheduleSaveComment") { scheduleSaveCommentWorker(comment) }
        }
    }

    fun loadFeedById(feedId: String): LiveData<Resource<Feed>> {
        return object : NetworkBoundResource<Feed, Feed>(mAppExecutors) {
            override fun saveCallResult(item: Feed) {
                mPetDb.feedDao().insertFromFeed(item)
                mPetDb.userDao().insert(item.feedUser)
                if (item.latestComment != null) {
                    mPetDb.commentDao().insertFromComment(item.latestComment)
                    mPetDb.userDao().insert(item.latestComment!!.feedUser)
                }
            }

            override fun shouldFetch(data: Feed?): Boolean {
                return data == null
            }

            override fun deleteDataFromDb(body: Feed) {
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
                            if (resourceComments.data != null)
                                resourceComments.data.add(0, feedComment)
                            mediatorLiveData.setValue(Resource.success(resourceComments.data))
                        }
                    }
                }
            }
        }
        return mediatorLiveData
    }

    private fun createCommentUIFromFeed(feed: Feed): CommentUI {
        val comment = CommentUI()
        comment.id = feed.feedId
        comment.setOwner(feed.feedUser)
        comment.setContent(feed.content)
        comment.setTimeCreated(feed.timeCreated)
        return comment
    }

    fun getFeedComments(feedId: String, after: Long, limit: Int): LiveData<Resource<List<CommentUI>>> {

        return object : NetworkBoundResource<List<CommentUI>, List<Comment>>(mAppExecutors) {
            override fun saveCallResult(items: List<Comment>) {

                val listId = ArrayList<String>(items.size)
                val pagingId = Paging.feedCommentsPagingId(feedId)
                for (item in items) {
                    listId.add(item.id)
                }
                val paging = Paging(pagingId,
                        listId, false,
                        if (listId.isEmpty()) null else listId[listId.size - 1])
                mPetDb.runInTransaction {
                    mPetDb.commentDao().insertListComment(items)
                    for (item in items) {
                        mPetDb.userDao().insert(item.feedUser)
                        if (item.latestComment != null) {
                            mPetDb.commentDao().insertFromComment(item.latestComment)
                            mPetDb.userDao().insert(item.latestComment.feedUser)
                        }
                    }
                    mPetDb.pagingDao().insert(paging)
                }
            }

            override fun shouldFetch(data: List<CommentUI>?): Boolean {
                return data == null || data.isEmpty() || mRateLimiter.shouldFetch(Paging.feedCommentsPagingId(feedId))
            }

            override fun deleteDataFromDb(body: List<Comment>) {
                mPetDb.pagingDao().deleteFeedPaging(Paging.feedCommentsPagingId(feedId))
            }

            override fun shouldDeleteOldData(body: List<Comment>): Boolean {
                return false
            }

            override fun loadFromDb(): LiveData<List<CommentUI>> {
                return Transformations.switchMap(mPetDb.pagingDao().loadFeedPaging(Paging.feedCommentsPagingId(feedId))) { input ->
                    if (input == null) {
                        val data = MutableLiveData<List<CommentUI>>()
                        data.postValue(null)
                        return@Transformations.switchMap data
                    } else {
                        Timber.i("loadFeedsFromDb paging: %s", input.toString())
                        return@Transformations.switchMap mPetDb . commentDao ().loadFeedComments(input.ids, feedId)
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
            override fun saveCallResult(items: List<Comment>) {
                val listId = ArrayList<String>(items.size)
                val pagingId = Paging.subCommentsPagingId(parentCommentId)
                for (item in items) {
                    listId.add(item.id)
                }
                val paging = Paging(pagingId,
                        listId, false,
                        if (listId.isEmpty()) null else listId[listId.size - 1])
                mPetDb.runInTransaction {
                    mPetDb.commentDao().insertListComment(items)
                    for (item in items) {
                        mPetDb.userDao().insert(item.feedUser)
                        if (item.latestComment != null) {
                            mPetDb.commentDao().insertFromComment(item.latestComment)
                            mPetDb.userDao().insert(item.latestComment.feedUser)
                        }

                    }
                    mPetDb.pagingDao().insert(paging)
                }
            }

            override fun shouldFetch(data: List<CommentUI>?): Boolean {
                return data == null || data.isEmpty() || mRateLimiter.shouldFetch(Paging.subCommentsPagingId(parentCommentId))
            }

            override fun deleteDataFromDb(body: List<Comment>) {
                mPetDb.pagingDao().deleteFeedPaging(Paging.subCommentsPagingId(parentCommentId))
            }

            override fun shouldDeleteOldData(body: List<Comment>): Boolean {
                return false
            }

            override fun loadFromDb(): LiveData<List<CommentUI>> {
                return Transformations.switchMap(mPetDb.pagingDao().loadFeedPaging(Paging.subCommentsPagingId(parentCommentId))) { input ->
                    if (input == null) {
                        val data = MutableLiveData<List<CommentUI>>()
                        data.postValue(null)
                        return@Transformations.switchMap data
                    } else {
                        Timber.i("loadSubCommentsFromDb paging: %s", input.toString())
                        return@Transformations.switchMap mPetDb . commentDao ().loadSubComments(input.ids, parentCommentId)
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

        if (comment.photo != null) {
            val compressionWork = OneTimeWorkRequest.Builder(CompressPhotoWorker::class.java)
                    .setInputData(CompressPhotoWorker.data(comment.photo))
                    .build()

            val key = Uri.parse(comment.photo.originUrl).lastPathSegment
            val uploadWork = OneTimeWorkRequest.Builder(UploadPhotoWorker::class.java)
                    .setConstraints(uploadConstraints)
                    .setInputData(Data.Builder().putString(PhotoWorker.KEY_PHOTO, key).build())
                    .build()
            WorkManager.getInstance()
                    .beginWith(compressionWork)
                    .then(uploadWork)
                    .then(createCommentWork)
                    .enqueue()
        } else {
            WorkManager.getInstance().enqueue(createCommentWork)
        }
    }

    fun likeCommentHandler(userId: String, commentId: String) {
        mAppExecutors.diskIO().execute {
            val comment = mPetDb.commentDao().getCommentById(commentId)
            if (comment.isLikeInProgress) {
                return@mAppExecutors.diskIO().execute
            }
            comment.isLikeInProgress = true
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
                            if (feedApiResponse.isSucceed && feedApiResponse.body != null) {
                                val commentResult = feedApiResponse.body.toEntity()
                                commentResult.isLikeInProgress = false
                                mPetDb.commentDao().update(commentResult)
                            } else {
                                mAppExecutors.mainThread().execute { mToaster.makeText(feedApiResponse.errorMessage) }
                                comment.isLikeInProgress = false
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
                            if (feedApiResponse.isSucceed && feedApiResponse.body != null) {
                                val feedResult = feedApiResponse.body.toEntity()
                                feedResult.isLikeInProgress = false
                                mPetDb.commentDao().update(feedResult)
                            } else {
                                mAppExecutors.mainThread().execute { mToaster.makeText(feedApiResponse.errorMessage) }
                                comment.isLikeInProgress = false
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
            if (subComment.isLikeInProgress) {
                return@mAppExecutors.diskIO().execute
            }
            subComment.isLikeInProgress = true
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
                            if (feedApiResponse.isSucceed && feedApiResponse.body != null) {
                                val commentResult = feedApiResponse.body.toEntity()
                                commentResult.isLikeInProgress = false
                                mPetDb.commentDao().update(commentResult)
                            } else {
                                mAppExecutors.mainThread().execute { mToaster.makeText(feedApiResponse.errorMessage) }
                                subComment.isLikeInProgress = false
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
                            if (feedApiResponse.isSucceed && feedApiResponse.body != null) {
                                val feedResult = feedApiResponse.body.toEntity()
                                feedResult.isLikeInProgress = false
                                mPetDb.commentDao().update(feedResult)
                            } else {
                                mAppExecutors.mainThread().execute { mToaster.makeText(feedApiResponse.errorMessage) }
                                subComment.isLikeInProgress = false
                                mPetDb.commentDao().update(subComment)
                            }
                        }
                    }

                }
            }
        })
    }

    companion object {

        val COMMENT_PER_PAGE = 10
    }
}
