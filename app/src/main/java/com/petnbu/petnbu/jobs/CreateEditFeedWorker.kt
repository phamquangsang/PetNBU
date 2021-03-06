package com.petnbu.petnbu.jobs

import android.arch.lifecycle.Observer
import androidx.core.net.toUri
import androidx.work.Data
import androidx.work.Worker
import com.google.gson.Gson
import com.petnbu.petnbu.AppExecutors
import com.petnbu.petnbu.PetApplication
import com.petnbu.petnbu.api.ApiResponse
import com.petnbu.petnbu.api.WebService
import com.petnbu.petnbu.db.FeedDao
import com.petnbu.petnbu.db.PetDb
import com.petnbu.petnbu.db.UserDao
import com.petnbu.petnbu.model.Feed
import com.petnbu.petnbu.model.FeedUser
import com.petnbu.petnbu.model.LocalStatus.*
import com.petnbu.petnbu.model.Paging
import com.petnbu.petnbu.model.Photo
import timber.log.Timber
import java.util.*
import java.util.concurrent.CountDownLatch
import javax.inject.Inject

class CreateEditFeedWorker : Worker() {

    @Inject
    lateinit var webService: WebService

    @Inject
    lateinit var feedDao: FeedDao

    @Inject
    lateinit var userDao: UserDao

    @Inject
    lateinit var petDb: PetDb

    @Inject
    lateinit var appExecutors: AppExecutors

    override fun doWork(): Result {
        PetApplication.appComponent.inject(this)
        var workerResult = Result.FAILURE

        val feedId = inputData.getString(KEY_FEED_ID, "")
        val isUpdating = inputData.getBoolean(KEY_FLAG_UPDATING, false)

        if (!feedId.isNullOrEmpty()) {
            val feedEntity = feedDao.findFeedEntityById(feedId!!)
            val userEntity = userDao.findUserById(feedEntity?.fromUserId)
            petDb.commentDao().getCommentById(feedEntity?.latestCommentId)
            if (feedEntity != null && userEntity != null) {
                val feedUser = FeedUser(userEntity.userId, userEntity.avatar, userEntity.name)
                val feed = Feed(feedEntity.feedId, feedUser, feedEntity.photos,
                        feedEntity.commentCount, feedEntity.likeCount,
                        feedEntity.isLiked, feedEntity.content, null ,
                        feedEntity.timeCreated, feedEntity.timeUpdated, feedEntity.status, feedEntity.likeInProgress)

                if (inputData.getBoolean("result", false) && feed.isUploading()) {
                    feed.timeUpdated = Date()

                    val gson = Gson()
                    var uploadedPhotosFailed = false
                    for (photo in feed.photos) {
                        val key = photo.originUrl.toUri().lastPathSegment
                        val jsonPhotoArray = inputData.getStringArray(key)
                        var uploadedPhoto: Photo? = null

                        if (jsonPhotoArray != null && jsonPhotoArray.isNotEmpty() && !jsonPhotoArray[0].isNullOrEmpty()) {
                            uploadedPhoto = gson.fromJson(jsonPhotoArray[0], Photo::class.java)
                        } else {
                            val jsonPhoto = inputData.getString(key, "")
                            if (!jsonPhoto.isNullOrEmpty()) {
                                uploadedPhoto = gson.fromJson(jsonPhoto, Photo::class.java)
                            }
                        }

                        uploadedPhotosFailed = uploadedPhoto?.let {
                            photo.originUrl = it.originUrl
                            photo.largeUrl = it.largeUrl
                            photo.mediumUrl = it.mediumUrl
                            photo.smallUrl = it.smallUrl
                            photo.thumbnailUrl = it.thumbnailUrl
                            false
                        } ?: true
                    }
                    if (!uploadedPhotosFailed) {
                        try {
                            feed.save(isUpdating)
                            workerResult = Result.SUCCESS
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    } else {
                        updateLocalFeedError(feed)
                    }
                } else updateLocalFeedError(feed)

            }
        }
        return workerResult
    }

    private fun Feed.isUploading() = status == STATUS_UPLOADING

    private fun Feed.save(isUpdating: Boolean) {
        if (isUpdating)
            updateFeed(this)
        else createFeed(this, feedId)
    }

    @Throws(InterruptedException::class)
    private fun createFeed(feed: Feed, temporaryFeedId: String) {
        val countDownLatch = CountDownLatch(1)

        val apiResponse = webService.createFeed(feed)
        apiResponse.observeForever(object : Observer<ApiResponse<Feed>> {
            override fun onChanged(feedApiResponse: ApiResponse<Feed>?) {
                appExecutors.mainThread().execute { apiResponse.removeObserver(this) }

                if (feedApiResponse != null && feedApiResponse.isSuccessful && feedApiResponse.body != null) {
                    val newFeed = feedApiResponse.body

                    Timber.i("create feed succeed %s", newFeed.toString())
                    appExecutors.diskIO().execute {
                        Timber.i("update feedId from %s to %s", temporaryFeedId, newFeed.feedId)

                        petDb.runInTransaction {
                            feedDao.updateFeedId(temporaryFeedId, newFeed.feedId)
                            newFeed.status = STATUS_DONE

                            petDb.pagingDao().findFeedPaging(Paging.GLOBAL_FEEDS_PAGING_ID)?.apply {
                                getIds()!!.add(0, newFeed.feedId)
                                petDb.pagingDao().update(this)
                            }
                            petDb.pagingDao().findFeedPaging(newFeed.feedUser.userId)?.apply {
                                getIds()!!.add(0, newFeed.feedId)
                                petDb.pagingDao().update(this)
                            }
                            feedDao.update(newFeed.toEntity())
                        }
                        countDownLatch.countDown()
                    }
                } else {
                    Timber.e("create feed error %s", feedApiResponse?.errorMessage ?: "")
                    updateLocalFeedError(feed)
                    countDownLatch.countDown()
                }
            }
        })
        countDownLatch.await()
    }

    private fun updateFeed(feed: Feed) {
        val countDownLatch = CountDownLatch(1)
        val apiResponse = webService.updateFeed(feed.feedId, feed.content, feed.photos)

        apiResponse.observeForever(object : Observer<ApiResponse<Feed>> {
            override fun onChanged(feedApiResponse: ApiResponse<Feed>?) {
                if (feedApiResponse != null && feedApiResponse.isSuccessful && feedApiResponse.body != null) {
                    Timber.i("update feed succeed %s", feedApiResponse.body.toString())
                    val newFeed = feedApiResponse.body
                    newFeed.status = STATUS_DONE

                    appExecutors.diskIO().execute {
                        petDb.runInTransaction {
                            feedDao.updateContentPhotosFeed(newFeed.photos, newFeed.content, newFeed.feedId, newFeed.timeUpdated ?: Date())
                            feedDao.updateFeedLocalStatus(STATUS_DONE, newFeed.feedId)
                            countDownLatch.countDown()
                        }
                    }
                } else {
                    Timber.e("update feed error %s", feedApiResponse?.errorMessage ?: "")
                    updateLocalFeedError(feed)
                    countDownLatch.countDown()
                }
                apiResponse.removeObserver(this)
            }
        })
        countDownLatch.await()
    }

    private fun updateLocalFeedError(feed: Feed) {
        feed.apply {
            status = STATUS_ERROR
        }.let {
            appExecutors.diskIO().execute { feedDao.update(it.toEntity()) }
        }
    }

    companion object {
        private const val KEY_FEED_ID = "key-feed-id"
        private const val KEY_FLAG_UPDATING = "extra-updating"

        @JvmStatic
        fun data(feed: Feed, isUpdating: Boolean): Data = Data.Builder().apply {
            putString(KEY_FEED_ID, feed.feedId)
            putBoolean(KEY_FLAG_UPDATING, isUpdating)
        }.build()
    }
}
