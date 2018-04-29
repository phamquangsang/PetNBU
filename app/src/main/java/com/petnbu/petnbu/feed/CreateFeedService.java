package com.petnbu.petnbu.feed;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

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
import com.petnbu.petnbu.util.IdUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

//switch to CreateFeedJob
@Deprecated
public class CreateFeedService extends JobIntentService{

    private static int JOB_ID = 1000;
    @Inject
    WebService mWebService;

    @Inject
    FeedDao mFeedDao;

    @Inject
    UserDao mUserDao;

    @Inject
    AppExecutors mAppExecutors;

    private static final String FEED_EXTRA = "feed_extra";
    private Feed mFeed;

    public CreateFeedService() {
        PetApplication.getAppComponent().inject(this);
    }

    public static void enqueueWork(Context context, Feed feed){
        Intent intent = new Intent(context, CreateFeedService.class);
        intent.putExtra(FEED_EXTRA, feed);
        enqueueWork(context, CreateFeedService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        mFeed = intent.getParcelableExtra(FEED_EXTRA);
        if(mFeed.getStatus() == Feed.STATUS_DONE){
            return;
        }

        String feedId = IdUtil.generateID("feed");
        if(mFeed.getFeedId() == null) {
            mFeed.setFeedId(feedId);
        }else{
            feedId = mFeed.getFeedId();
        }
        Timber.i("received mFeed %s", mFeed);
        User user = mUserDao.findUserById(SharedPrefUtil.getUserId(getApplication()));
        FeedUser feedUser = new FeedUser(user.getUserId(), user.getAvatar().getOriginUrl(), user.getName());
        mFeed.setFeedUser(feedUser);
        if(mFeed.getTimeCreated() == null){
            mFeed.setTimeCreated(new Date());
        }
        mFeed.setTimeUpdated(new Date());
        mFeed.setStatus(Feed.STATUS_UPLOADING);
        mFeedDao.insert(mFeed);
        List<String> photoUrls = new ArrayList<>(mFeed.getPhotos().size());
        for (Photo photo : mFeed.getPhotos()) {
            photoUrls.add(photo.getOriginUrl());
        }

        final String localFeedId = feedId;

        if(photoUrls.isEmpty()){
            uploadFeed(mFeed, localFeedId);
        }else{
            new StorageApi.OnUploadingImage(photoUrls){
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
    }

    private void uploadFeed(Feed feed, String temporaryFeedId) {
        mWebService.createFeed(feed, new SuccessCallback<Feed>() {
            @Override
            public void onSuccess(Feed newFeed) {
                Timber.i("upload mFeed succeed %s", newFeed.toString());
                mAppExecutors.diskIO().execute(() -> {
                    mFeedDao.deleteFeedById(temporaryFeedId);
                    newFeed.setStatus(Feed.STATUS_DONE);
                    mFeedDao.insert(newFeed);
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
        mAppExecutors.diskIO().execute(() -> mFeedDao.update(feed));
    }
}
