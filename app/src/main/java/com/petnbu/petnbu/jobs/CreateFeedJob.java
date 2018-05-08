package com.petnbu.petnbu.jobs;


import android.annotation.SuppressLint;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.gson.Gson;
import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.StorageApi;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.FeedDao;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.db.UserDao;
import com.petnbu.petnbu.model.FeedResponse;
import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.FeedPaging;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.UserEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class CreateFeedJob extends JobService {
    private static String FEED_ID_EXTRA = "feed-id-extra";

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

    public CreateFeedJob() {
        super();
        PetApplication.getAppComponent().inject(this);
    }

    public static Bundle putExtras(String localFeedId) {
        Bundle bundle = new Bundle();
        Gson gson = new Gson();
        bundle.putString(FEED_ID_EXTRA, localFeedId);
        return bundle;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public boolean onStartJob(JobParameters params) {
        //kick of the service
        Bundle bundle = params.getExtras();
        if (bundle == null) {
            return false;
        }

        final String feedId = bundle.getString(FEED_ID_EXTRA);
        mAppExecutors.diskIO().execute(() -> {
            FeedEntity feedEntity = mFeedDao.findFeedEntityById(feedId);
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
            List<String> photoUrls = new ArrayList<>(mFeedResponse.getPhotos().size());
            for (Photo photo : mFeedResponse.getPhotos()) {
                photoUrls.add(photo.getOriginUrl());
            }

            final String localFeedId = mFeedResponse.getFeedId();

            if (photoUrls.isEmpty()) {
                uploadFeed(mFeedResponse, localFeedId);
            } else {
                new StorageApi.OnUploadingImage(photoUrls) {
                    @Override
                    public void onCompleted(List<String> result) {
                        Timber.i("update %d photos complete", result.size());
                        for (int i = 0; i < result.size(); i++) {
                            mFeedResponse.getPhotos().get(i).setOriginUrl(result.get(i));
                        }
                        uploadFeed(mFeedResponse, localFeedId);
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Timber.e("upload photos failed with exception %s", e.toString());
                        updateLocalFeedError(mFeedResponse);
                    }
                }.start();
            }

        });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Timber.i("onJobStop");
        return true;
    }

    private void uploadFeed(FeedResponse feedResponse, String temporaryFeedId) {
        LiveData<ApiResponse<FeedResponse>> apiResponse = mWebService.createFeed(feedResponse);
        apiResponse.observeForever(new Observer<ApiResponse<FeedResponse>>() {
            @Override
            public void onChanged(@Nullable ApiResponse<FeedResponse> feedApiResponse) {
                if (feedApiResponse != null) {
                    if(feedApiResponse.isSucceed && feedApiResponse.body != null){
                        FeedResponse newFeedResponse = feedApiResponse.body;
                        Timber.i("upload mFeedResponse succeed %s", newFeedResponse.toString());
                        mAppExecutors.diskIO().execute(() -> {
                            Timber.i("update feedId from %s to %s", temporaryFeedId, newFeedResponse.getFeedId());

                            FeedPaging currentPaging = mFeedDao.findFeedPaging(FeedPaging.GLOBAL_FEEDS_PAGING_ID);
                            if(currentPaging != null){
                                currentPaging.getFeedIds().add(0, newFeedResponse.getFeedId());
                            }

                            FeedPaging userFeedPaging = mFeedDao.findFeedPaging(newFeedResponse.getFeedUser().getUserId());
                            if(userFeedPaging != null){
                                userFeedPaging.getFeedIds().add(0, newFeedResponse.getFeedId());
                            }

                            mPetDb.beginTransaction();
                            try{
                                mFeedDao.updateFeedId(temporaryFeedId, newFeedResponse.getFeedId());
                                newFeedResponse.setStatus(FeedEntity.STATUS_DONE);

                                if(currentPaging != null){
                                    mFeedDao.update(currentPaging);
                                }
                                if(userFeedPaging != null){
                                    mFeedDao.update(userFeedPaging);
                                }
                                mFeedDao.update(newFeedResponse.toEntity());
                                mPetDb.setTransactionSuccessful();
                            } finally {
                                mPetDb.endTransaction();
                            }

                            jobFinished(mParams, false);
                        });
                    }else{
                        Timber.e("uploadFeed error %s", feedApiResponse.errorMessage);
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
