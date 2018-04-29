package com.petnbu.petnbu.jobs;


import android.annotation.SuppressLint;
import android.os.Bundle;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.gson.Gson;
import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.SharedPrefUtil;
import com.petnbu.petnbu.api.StorageApi;
import com.petnbu.petnbu.api.SuccessCallback;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.FeedDao;
import com.petnbu.petnbu.db.UserDao;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class CreateFeedJob extends JobService {
    private static String FEED_JSON_EXTRA = "feed-extra";

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

    public static Bundle putExtras(Feed feedAsJson) {
        Bundle bundle = new Bundle();
        Gson gson = new Gson();
        bundle.putString(FEED_JSON_EXTRA, gson.toJson(feedAsJson));
        return bundle;
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public boolean onStartJob(JobParameters params) {
        //kick of the service
        try{
            Bundle bundle = params.getExtras();
            if(bundle == null){
                return false;
            }

            Gson gson = new Gson();
            mFeed = gson.fromJson(bundle.getString(FEED_JSON_EXTRA), Feed.class);

            mParams = params;
            if (mFeed == null || mFeed.getFeedId() == null) {
                throw new IllegalStateException("Feed and its Id must be not null. " +
                        "You should generate temporary Id using IdUtil.generateID()");
            }

            PetApplication.getAppComponent().inject(this);
            Timber.i("received mFeed %s", mFeed);

            mAppExecutors.diskIO().execute(() -> {
                User user = mUserDao.findUserById(SharedPrefUtil.getUserId(getApplication()));
                FeedUser feedUser = new FeedUser(user.getUserId(), user.getAvatar().getOriginUrl(), user.getName());
                mFeed.setFeedUser(feedUser);
                if (mFeed.getTimeCreated() == null) {
                    mFeed.setTimeCreated(new Date());
                }
                mFeed.setTimeUpdated(new Date());
                mFeed.setStatus(Feed.STATUS_UPLOADING);
                mFeedDao.insert(mFeed);
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
        }catch (Exception e) {
            Timber.e(e);
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Timber.i("onJobStop");
        return true;
    }

    private void uploadFeed(Feed feed, String temporaryFeedId) {
        mWebService.createFeed(feed, new SuccessCallback<Feed>() {
            @Override
            public void onSuccess(Feed newFeed) {
                Timber.i("upload mFeed succeed %s", newFeed.toString());
                mAppExecutors.diskIO().execute(() -> {
                    Timber.i("update feedId from %s to %s", temporaryFeedId, newFeed.getFeedId());
                    mFeedDao.updateFeedId(temporaryFeedId, newFeed.getFeedId());
                    newFeed.setStatus(Feed.STATUS_DONE);
                    mFeedDao.update(newFeed);
                    jobFinished(mParams, false);
                });
            }

            @Override
            public void onFailed(Exception e) {
                Timber.e("uploadFeed error", e);
                updateLocalFeedError(feed);
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
