package com.petnbu.petnbu.jobs;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.FeedDao;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.db.UserDao;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.Paging;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.UserEntity;

import java.util.Date;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.Worker;
import timber.log.Timber;

import static com.petnbu.petnbu.model.LocalStatus.STATUS_DONE;
import static com.petnbu.petnbu.model.LocalStatus.STATUS_ERROR;
import static com.petnbu.petnbu.model.LocalStatus.STATUS_UPLOADING;

public class CreateEditFeedWorker extends Worker {

    private static final String KEY_FEED_ID = "key-feed-id";
    private static final String KEY_FLAG_UPDATING = "extra-updating";

    public static Data data(Feed feed, boolean isUpdating) {
        Data data = new Data.Builder()
                .putString(KEY_FEED_ID, feed.getFeedId())
                .putBoolean(KEY_FLAG_UPDATING, isUpdating)
                .build();
        return data;
    }

    @Inject
    WebService mWebService;

    @Inject
    FeedDao mFeedDao;

    @Inject
    UserDao mUserDao;

    @Inject
    PetDb mPetDb;

    @Inject
    AppExecutors mAppExecutors;

    @NonNull
    @Override
    public WorkerResult doWork() {
        PetApplication.getAppComponent().inject(this);
        WorkerResult workerResult = WorkerResult.FAILURE;
        Data data = getInputData();

        String feedId = data.getString(KEY_FEED_ID, "");
        boolean isUpdating = data.getBoolean(KEY_FLAG_UPDATING, false);

        if(!TextUtils.isEmpty(feedId)) {
            FeedEntity feedEntity = mFeedDao.findFeedEntityById(feedId);
            if (feedEntity != null) {
                UserEntity userEntity = mUserDao.findUserById(feedEntity.getFromUserId());
                FeedUser feedUser = new FeedUser(userEntity.getUserId(), userEntity.getAvatar(), userEntity.getName());
                Feed feed = new Feed(feedEntity.getFeedId(), feedUser, feedEntity.getPhotos(), feedEntity.getCommentCount(), null
                        , feedEntity.getLikeCount(), feedEntity.getContent(), feedEntity.getTimeCreated()
                        , feedEntity.getTimeUpdated(), feedEntity.getStatus());

                if (feed.getStatus() == STATUS_UPLOADING) {
                    feed.setTimeUpdated(new Date());

                    Gson gson = new Gson();
                    boolean uploadedPhotosFailed = false;
                    for (Photo photo : feed.getPhotos()) {
                        String key = Uri.parse(photo.getOriginUrl()).getLastPathSegment();
                        String[] jsonPhotoArray = data.getStringArray(key);
                        Photo uploadedPhoto = null;

                        if (jsonPhotoArray != null && jsonPhotoArray.length > 0 && !TextUtils.isEmpty(jsonPhotoArray[0])) {
                            uploadedPhoto = gson.fromJson(jsonPhotoArray[0], Photo.class);
                        } else {
                            String jsonPhoto = data.getString(key, "");
                            if (!TextUtils.isEmpty(jsonPhoto)) {
                                uploadedPhoto = gson.fromJson(jsonPhoto, Photo.class);
                            }
                        }
                        if (uploadedPhoto != null) {
                            photo.setOriginUrl(uploadedPhoto.getOriginUrl());
                            photo.setLargeUrl(uploadedPhoto.getLargeUrl());
                            photo.setMediumUrl(uploadedPhoto.getMediumUrl());
                            photo.setSmallUrl(uploadedPhoto.getSmallUrl());
                            photo.setThumbnailUrl(uploadedPhoto.getThumbnailUrl());
                        } else {
                            uploadedPhotosFailed = true;
                        }
                    }
                    if(!uploadedPhotosFailed) {
                        try {
                            if (isUpdating)
                                updateFeed(feed);
                            else createFeed(feed, feed.getFeedId());

                            workerResult = WorkerResult.SUCCESS;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        updateLocalFeedError(feed);
                    }
                }
            }
        }
        return workerResult;
    }

    private void createFeed(Feed feed, String temporaryFeedId) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        LiveData<ApiResponse<Feed>> apiResponse = mWebService.createFeed(feed);
        apiResponse.observeForever(new Observer<ApiResponse<Feed>>() {
            @Override
            public void onChanged(@Nullable ApiResponse<Feed> feedApiResponse) {
                if (feedApiResponse != null && feedApiResponse.isSucceed && feedApiResponse.body != null) {
                    Feed newFeed = feedApiResponse.body;

                    Timber.i("create feed succeed %s", newFeed.toString());
                    mAppExecutors.diskIO().execute(() -> {
                        Timber.i("update feedId from %s to %s", temporaryFeedId, newFeed.getFeedId());

                        mPetDb.runInTransaction(() -> {
                            mFeedDao.updateFeedId(temporaryFeedId, newFeed.getFeedId());
                            newFeed.setStatus(STATUS_DONE);

                            Paging currentPaging = mPetDb.pagingDao().findFeedPaging(Paging.GLOBAL_FEEDS_PAGING_ID);
                            if (currentPaging != null) {
                                currentPaging.getIds().add(0, newFeed.getFeedId());
                                mPetDb.pagingDao().update(currentPaging);
                            }

                            Paging userPaging = mPetDb.pagingDao().findFeedPaging(newFeed.getFeedUser().getUserId());
                            if (userPaging != null) {
                                userPaging.getIds().add(0, newFeed.getFeedId());
                                mPetDb.pagingDao().update(userPaging);
                            }
                            mFeedDao.update(newFeed.toEntity());
                        });
                        countDownLatch.countDown();
                    });
                } else {
                    Timber.e("create feed error %s", feedApiResponse.errorMessage);
                    updateLocalFeedError(feed);
                    countDownLatch.countDown();
                }
                apiResponse.removeObserver(this);
            }
        });
        countDownLatch.await();
    }

    private void updateFeed(Feed feed) {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        LiveData<ApiResponse<Feed>> apiResponse = mWebService.updateFeed(feed);
        apiResponse.observeForever(new Observer<ApiResponse<Feed>>() {
            @Override
            public void onChanged(@Nullable ApiResponse<Feed> feedApiResponse) {
                if (feedApiResponse != null && feedApiResponse.isSucceed && feedApiResponse.body != null) {
                    Timber.i("update feed succeed %s", feedApiResponse.body.toString());
                    Feed newFeed = feedApiResponse.body;
                    newFeed.setStatus(STATUS_DONE);

                    mAppExecutors.diskIO().execute(() -> {
                        mFeedDao.update(newFeed.toEntity());
                        countDownLatch.countDown();
                    });
                    apiResponse.removeObserver(this);
                } else {
                    Timber.e("update feed error %s", feedApiResponse.errorMessage);
                    updateLocalFeedError(feed);
                    countDownLatch.countDown();
                }
            }
        });
    }

    private void updateLocalFeedError(Feed feed) {
        feed.setStatus(STATUS_ERROR);
        mAppExecutors.diskIO().execute(() -> mFeedDao.update(feed.toEntity()));
    }
}
