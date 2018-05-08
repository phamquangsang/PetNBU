package com.petnbu.petnbu.jobs;


import android.annotation.SuppressLint;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.webkit.URLUtil;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.StorageApi;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.FeedDao;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.db.UserDao;
import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.FeedResponse;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.Paging;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.UserEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class CreateEditFeedJob extends JobService {

    private static String EXTRA_FEED_ID = "extra-feed-id";
    private static String EXTRA_FLAG_UPDATING = "extra-updating";

    private FeedResponse mFeedResponse;

    private JobParameters mParams;

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

    public CreateEditFeedJob() {
        super();
        PetApplication.getAppComponent().inject(this);
    }

    public static Bundle extras(String localFeedId, boolean isUpdating) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_FEED_ID, localFeedId);
        bundle.putBoolean(EXTRA_FLAG_UPDATING, isUpdating);
        return bundle;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public boolean onStartJob(JobParameters params) {
        mParams = params;
        Bundle bundle = params.getExtras();
        if (bundle == null) {
            return false;
        }

        final String feedId = bundle.getString(EXTRA_FEED_ID);
        final boolean isUpdating = bundle.getBoolean(EXTRA_FLAG_UPDATING);

        mAppExecutors.diskIO().execute(() -> {
            FeedEntity feedEntity = mFeedDao.findFeedEntityById(feedId);
            if (feedEntity == null) {
                jobFinished(params, false);
                return;
            }
            UserEntity userEntity = mUserDao.findUserById(feedEntity.getFromUserId());
            FeedUser feedUser = new FeedUser(userEntity.getUserId(), userEntity.getAvatar().getOriginUrl(), userEntity.getName());
            mFeedResponse = new FeedResponse(feedEntity.getFeedId(), feedUser, feedEntity.getPhotos(), feedEntity.getCommentCount()
                    , feedEntity.getLikeCount(), feedEntity.getContent(), feedEntity.getTimeCreated()
                    , feedEntity.getTimeUpdated(), feedEntity.getStatus());

            if (mFeedResponse.getStatus() != FeedEntity.STATUS_UPLOADING) {
                jobFinished(params, false);
                Timber.i("status is not STATUS_UPLOADING");
                return;
            }

            Timber.i("received mFeedResponse %s", mFeedResponse);

            mFeedResponse.setTimeUpdated(new Date());

            final String localFeedId = mFeedResponse.getFeedId();
            List<String> photoUrls = new ArrayList<>(mFeedResponse.getPhotos().size());

            if (isUpdating) {
                for (Photo photo : mFeedResponse.getPhotos()) {
                    if (!URLUtil.isHttpUrl(photo.getOriginUrl()) && !URLUtil.isHttpsUrl(photo.getOriginUrl())) {
                        photoUrls.add(photo.getOriginUrl());
                    }
                }
                if (photoUrls.isEmpty()) {
                    updateFeed(mFeedResponse);
                } else {
                    new StorageApi.OnUploadingImage(photoUrls) {
                        @Override
                        public void onCompleted(List<String> result) {
                            Timber.i("update %d photos complete", result.size());
                            for (int i = 0; i < result.size(); i++) {
                                mFeedResponse.getPhotos().get(mFeedResponse.getPhotos().size() - result.size() + i)
                                        .setOriginUrl(result.get(i));
                            }
                            updateFeed(mFeedResponse);
                        }

                        @Override
                        public void onFailed(Exception e) {
                            Timber.e("upload photos failed with exception %s", e.toString());
                            updateLocalFeedError(mFeedResponse);
                        }
                    }.start();
                }
            } else {
                for (Photo photo : mFeedResponse.getPhotos()) {
                    photoUrls.add(photo.getOriginUrl());
                }

                if (photoUrls.isEmpty()) {
                    createFeed(mFeedResponse, localFeedId);
                } else {
                    new StorageApi.OnUploadingImage(photoUrls) {
                        @Override
                        public void onCompleted(List<String> result) {
                            Timber.i("update %d photos complete", result.size());
                            for (int i = 0; i < result.size(); i++) {
                                mFeedResponse.getPhotos().get(mFeedResponse.getPhotos().size() - result.size() + i)
                                        .setOriginUrl(result.get(i));
                            }
                            createFeed(mFeedResponse, localFeedId);
                        }

                        @Override
                        public void onFailed(Exception e) {
                            Timber.e("upload photos failed with exception %s", e.toString());
                            updateLocalFeedError(mFeedResponse);
                        }
                    }.start();
                }
            }
        });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Timber.i("onJobStop");
        return true;
    }

    private void createFeed(FeedResponse feedResponse, String temporaryFeedId) {
        LiveData<ApiResponse<FeedResponse>> apiResponse = mWebService.createFeed(feedResponse);
        apiResponse.observeForever(new Observer<ApiResponse<FeedResponse>>() {
            @Override
            public void onChanged(@Nullable ApiResponse<FeedResponse> feedApiResponse) {
                if (feedApiResponse != null) {
                    if (feedApiResponse.isSucceed && feedApiResponse.body != null) {
                        FeedResponse newFeedResponse = feedApiResponse.body;

                        Timber.i("upload mFeedResponse succeed %s", newFeedResponse.toString());
                        mAppExecutors.diskIO().execute(() -> {
                            Timber.i("update feedId from %s to %s", temporaryFeedId, newFeedResponse.getFeedId());

                            Paging currentPaging = mFeedDao.findFeedPaging(Paging.GLOBAL_FEEDS_PAGING_ID);
                            if (currentPaging != null) {
                                currentPaging.getIds().add(0, newFeedResponse.getFeedId());
                            }

                            Paging userPaging = mFeedDao.findFeedPaging(newFeedResponse.getFeedUser().getUserId());
                            if (userPaging != null) {
                                userPaging.getIds().add(0, newFeedResponse.getFeedId());
                            }

                            mPetDb.runInTransaction(() -> {
                                mFeedDao.updateFeedId(temporaryFeedId, newFeedResponse.getFeedId());
                                newFeedResponse.setStatus(FeedEntity.STATUS_DONE);

                                if (currentPaging != null) {
                                    mFeedDao.update(currentPaging);
                                }
                                if (userPaging != null) {
                                    mFeedDao.update(userPaging);
                                }
                                mFeedDao.update(newFeedResponse.toEntity());
                            });
                            jobFinished(mParams, false);
                        });
                    } else {
                        Timber.e("createFeed error %s", feedApiResponse.errorMessage);
                        updateLocalFeedError(feedResponse);
                        jobFinished(mParams, true);
                    }
                    apiResponse.removeObserver(this);
                }
            }
        });
    }

    private void updateFeed(FeedResponse feedResponse) {
        LiveData<ApiResponse<FeedResponse>> apiResponse = mWebService.updateFeed(feedResponse);
        apiResponse.observeForever(new Observer<ApiResponse<FeedResponse>>() {
            @Override
            public void onChanged(@Nullable ApiResponse<FeedResponse> feedApiResponse) {
                if (feedApiResponse != null) {
                    if (feedApiResponse.isSucceed && feedApiResponse.body != null) {
                        Timber.i("upload mFeedResponse succeed %s", feedApiResponse.body.toString());

                        FeedResponse newFeedResponse = feedApiResponse.body;
                        newFeedResponse.setStatus(FeedEntity.STATUS_DONE);

                        mAppExecutors.diskIO().execute(() -> {
                            mFeedDao.update(newFeedResponse.toEntity());
                            jobFinished(mParams, false);
                        });
                    } else {
                        Timber.e("createFeed error %s", feedApiResponse.errorMessage);
                        updateLocalFeedError(feedResponse);
                        jobFinished(mParams, true);
                    }
                    apiResponse.removeObserver(this);
                }
            }
        });
    }

    private void updateLocalFeedError(FeedResponse feedResponse) {
        feedResponse.setStatus(FeedEntity.STATUS_ERROR);
        mAppExecutors.diskIO().execute(() -> {
            mFeedDao.update(feedResponse.toEntity());
            jobFinished(mParams, true);
        });
    }
}
