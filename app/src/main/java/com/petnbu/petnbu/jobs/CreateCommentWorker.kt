package com.petnbu.petnbu.jobs

import android.arch.lifecycle.Observer
import android.net.Uri
import androidx.work.Data
import androidx.work.Worker
import com.google.gson.Gson
import com.petnbu.petnbu.AppExecutors
import com.petnbu.petnbu.PetApplication
import com.petnbu.petnbu.api.ApiResponse
import com.petnbu.petnbu.api.WebService
import com.petnbu.petnbu.db.CommentDao
import com.petnbu.petnbu.db.PetDb
import com.petnbu.petnbu.db.UserDao
import com.petnbu.petnbu.model.Comment
import com.petnbu.petnbu.model.FeedUser
import com.petnbu.petnbu.model.LocalStatus.*
import com.petnbu.petnbu.model.Paging
import com.petnbu.petnbu.model.Photo
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import javax.inject.Inject

class CreateCommentWorker : Worker() {

    @Inject
    lateinit var mWebService: WebService

    @Inject
    lateinit var mCommentDao: CommentDao

    @Inject
    lateinit var mUserDao: UserDao

    @Inject
    lateinit var mPetDb: PetDb

    @Inject
    lateinit var mAppExecutors: AppExecutors

    override fun doWork(): WorkerResult {
        PetApplication.getAppComponent().inject(this)

        var workerResult: WorkerResult = WorkerResult.FAILURE
        val data = inputData
        val commentId = data.getString(KEY_COMMENT_ID, "")

        if (!commentId.isNullOrEmpty()) {
            val commentEntity = mCommentDao.getCommentById(commentId)
            if (commentEntity != null) {
                val userEntity = mUserDao.findUserById(commentEntity.ownerId)
                val feedUser = FeedUser(userEntity.userId, userEntity.avatar, userEntity.name)
                val comment = Comment(commentEntity.id, feedUser, commentEntity.content, commentEntity.photo,
                        commentEntity.likeCount, commentEntity.commentCount, null, commentEntity.parentCommentId,
                        commentEntity.parentFeedId, commentEntity.localStatus, commentEntity.timeCreated,
                        commentEntity.timeUpdated)

                if (comment.isUploading()) {
                    try {
                        if (comment.photo != null) {
                            val key = Uri.parse(comment.photo.originUrl).lastPathSegment
                            val jsonPhoto = data.getString(key, "")
                            if (!jsonPhoto.isNullOrEmpty()) {
                                val uploadedPhoto = Gson().fromJson(jsonPhoto, Photo::class.java)
                                uploadedPhoto?.apply {
                                    comment.photo.originUrl = originUrl
                                    comment.photo.largeUrl = largeUrl
                                    comment.photo.mediumUrl = mediumUrl
                                    comment.photo.smallUrl = smallUrl
                                    comment.photo.thumbnailUrl = thumbnailUrl

                                    comment.save()
                                    workerResult = WorkerResult.SUCCESS
                                }
                            }
                        } else {
                            comment.save()
                            workerResult = WorkerResult.SUCCESS
                        }
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return workerResult
    }

    private fun Comment.save() {
        if (!parentFeedId.isNullOrEmpty()) {
            createComment(this)
        } else if (!parentCommentId.isNullOrEmpty()) {
            createSubComment(this)
        }
    }

    private fun Comment.isUploading() = localStatus == STATUS_UPLOADING

    @Throws(InterruptedException::class)
    private fun createComment(comment: Comment) {
        val countDownLatch = CountDownLatch(1)
        val oldCommentId = comment.id

        val apiResponse = mWebService.createFeedComment(comment, comment.parentFeedId)
        apiResponse.observeForever(object : Observer<ApiResponse<Comment>> {
            override fun onChanged(commentApiResponse: ApiResponse<Comment>?) {
                apiResponse.removeObserver(this)

                if (commentApiResponse != null && commentApiResponse.isSucceed && commentApiResponse.body != null) {
                    Timber.d("create comment %s success", comment.id)
                    val newComment = commentApiResponse.body

                    mAppExecutors.diskIO().execute {
                        mPetDb.runInTransaction {
                            val feedCommentPaging = mPetDb.pagingDao().findFeedPaging(Paging.feedCommentsPagingId(comment.parentFeedId))
                            feedCommentPaging?.apply {
                                ids.add(0, newComment.id)
                                mPetDb.pagingDao().update(this)
                            }
                            mCommentDao.updateCommentId(oldCommentId, newComment.id)
                            newComment.localStatus = STATUS_DONE
                            mCommentDao.update(newComment.toEntity())
                            val parentFeed = mPetDb.feedDao().findFeedEntityById(newComment.parentFeedId)
                            mPetDb.feedDao().updateLatestCommentId(newComment.id, parentFeed?.commentCount ?: 0 + 1, newComment.parentFeedId)
                        }
                    }
                } else {
                    Timber.d("create comment %s error : %s", comment.id, commentApiResponse!!.errorMessage)
                    comment.localStatus = STATUS_ERROR
                    mAppExecutors.diskIO().execute { mCommentDao.update(comment.toEntity()) }
                }
                countDownLatch.countDown()
            }
        })
        countDownLatch.await()
    }

    @Throws(InterruptedException::class)
    private fun createSubComment(comment: Comment) {
        val countDownLatch = CountDownLatch(1)
        val oldCommentId = comment.id

        val apiResponse = mWebService.createReplyComment(comment, comment.parentCommentId)
        apiResponse.observeForever(object : Observer<ApiResponse<Comment>> {
            override fun onChanged(commentApiResponse: ApiResponse<Comment>?) {
                apiResponse.removeObserver(this)

                if (commentApiResponse != null && commentApiResponse.isSucceed && commentApiResponse.body != null) {
                    Timber.d("create comment %s success", comment.id)
                    val newComment = commentApiResponse.body

                    mAppExecutors.diskIO().execute {
                        mPetDb.runInTransaction {
                            val subCommentPaging = mPetDb.pagingDao().findFeedPaging(Paging.subCommentsPagingId(comment.parentCommentId))

                            subCommentPaging?.apply {
                                ids.add(0, newComment.id)
                                mPetDb.pagingDao().update(this)
                            }
                            mCommentDao.updateCommentId(oldCommentId, newComment.id)
                            newComment.localStatus = STATUS_DONE
                            mCommentDao.update(newComment.toEntity())

                            val parentComment = mCommentDao.getCommentById(comment.parentCommentId)
                            parentComment.latestCommentId = comment.id
                            parentComment.commentCount = parentComment.commentCount + 1
                            mCommentDao.update(parentComment)
                        }
                    }
                } else {
                    Timber.d("create comment %s error : %s", comment.id, commentApiResponse!!.errorMessage)
                    comment.localStatus = STATUS_ERROR
                    mAppExecutors.diskIO().execute { mCommentDao.update(comment.toEntity()) }
                }
                countDownLatch.countDown()
            }
        })
        countDownLatch.await()
    }

    companion object {

        private const val KEY_COMMENT_ID = "key-comment-id"

        fun data(comment: Comment): Data =Data.Builder()
                    .putString(KEY_COMMENT_ID, comment.id)
                    .build()
    }
}
