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
import com.petnbu.petnbu.api.SuccessCallback;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.FeedDao;
import com.petnbu.petnbu.db.UserDao;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.Photo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class CreateFeedJob extends JobService {
    private static String FEED_ID_EXTRA = "feed-id-extra";

    private Feed mFeed;

    private JobParameters mParams;

    @Inject
    WebService mWebService;

    @Inject
    FeedDao mFeedDao;

    @Inject
    UserDao mUserDao;

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
            mFeed = mFeedDao.findFeedById(feedId);
            if (mFeed == null || mFeed.getStatus() != Feed.STATUS_UPLOADING) {
                jobFinished(params, false);
                return;
            }

            Timber.i("received mFeed %s", mFeed);

            mFeed.setTimeUpdated(new Date());
            List<String> photoUrls = new ArrayList<>(mFeed.getPhotos().size());
            for (Photo photo : mFeed.getPhotos()) {
                photoUrls.add(photo.getOriginUrl());
            }

            final String localFeedId = mFeed.getFeedId();

            if (photoUrls.isEmpty()) {
                uploadFeed(mFeed, localFeedId);
            } else {
                new StorageApi.OnUploadingImage(photoUrls) {
                    @Override
                    public void onCompleted(List<String> result) {
                        Timber.i("update %d photos complete", result.size());
                        for (int i = 0; i < result.size(); i++) {
                            mFeed.getPhotos().get(i).setOriginUrl(result.get(i));
                        }
                        uploadFeed(mFeed, localFeedId);
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Timber.e("upload photos failed with exception %s", e.toString());
                        updateLocalFeedError(mFeed);
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

    private void uploadFeed(Feed feed, String temporaryFeedId) {
        LiveData<ApiResponse<Feed>> apiResponse = mWebService.createFeed(feed);
        apiResponse.observeForever(new Observer<ApiResponse<Feed>>() {
            @Override
            public void onChanged(@Nullable ApiResponse<Feed> feedApiResponse) {
                if (feedApiResponse != null) {
                    if(feedApiResponse.isSucceed && feedApiResponse.body != null){
                        Feed newFeed = feedApiResponse.body;
                        Timber.i("upload mFeed succeed %s", newFeed.toString());
                        mAppExecutors.diskIO().execute(() -> {
                            Timber.i("update feedId from %s to %s", temporaryFeedId, newFeed.getFeedId());
                            mFeedDao.updateFeedId(temporaryFeedId, newFeed.getFeedId());
                            newFeed.setStatus(Feed.STATUS_DONE);
                            mFeedDao.update(newFeed);
                            jobFinished(mParams, false);
                        });
                    }else{
                        Timber.e("uploadFeed error %s", feedApiResponse.errorMessage);
                        updateLocalFeedError(feed);
                        jobFinished(mParams, true);
                    }
                    apiResponse.removeObserver(this);
                }
            }
        });
    }

    private void updateLocalFeedError(Feed feed) {
        feed.setStatus(Feed.STATUS_ERROR);
        mAppExecutors.diskIO().execute(() -> {
            mFeedDao.update(feed);
            jobFinished(mParams, true);
        });
    }
}
