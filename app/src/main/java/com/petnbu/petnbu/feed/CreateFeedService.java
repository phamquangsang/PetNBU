package com.petnbu.petnbu.feed;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.JobIntentService;

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
import com.petnbu.petnbu.model.Status;
import com.petnbu.petnbu.model.User;
import com.petnbu.petnbu.util.IdUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

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

    public CreateFeedService() {
        PetApplication.getAppComponent().inject(this);
    }

    public static void enqueueWork(Context c, Feed feed){
        Intent intent = new Intent(c, CreateFeedService.class);
        intent.putExtra(FEED_EXTRA, feed);
        enqueueWork(c, CreateFeedService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Feed feed = intent.getParcelableExtra(FEED_EXTRA);
        if(feed.getStatus() == Feed.STATUS_DONE){
            return;
        }
        String feedId = IdUtil.generateID("feed");
        if(feed.getFeedId() == null){
            feed.setFeedId(feedId);
        }else{
            feedId = feed.getFeedId();
        }
        Timber.i("received feed %s", feed);
        User user = mUserDao.findUserById(SharedPrefUtil.getUserId(getApplication()));
        FeedUser feedUser = new FeedUser(user.getUserId(), user.getAvatar().getOriginUrl(), user.getName());
        feed.setFeedUser(feedUser);
        if(feed.getTimeCreated() == null){
            feed.setTimeCreated(new Date());
        }
        feed.setTimeUpdated(new Date());
        feed.setStatus(Feed.STATUS_UPLOADING);
        mFeedDao.insert(feed);
        List<String> photoUrls = new ArrayList<>(feed.getPhotos().size());
        for (Photo photo : feed.getPhotos()) {
            photoUrls.add(photo.getOriginUrl());
        }

        final String localFeedId = feedId;

        if(photoUrls.isEmpty()){
            uploadFeed(feed, localFeedId);
        }else{
            new StorageApi.OnUploadingImage(photoUrls){
                @Override
                public void onCompleted(List<String> result) {
                    Timber.i("update %d photos complete", result.size());
                    for (int i = 0; i < result.size(); i++) {
                        feed.getPhotos().get(i).setOriginUrl(result.get(i));
                    }
                    uploadFeed(feed, localFeedId);
                }

                @Override
                public void onFailed(Exception e) {
                    Timber.e("upload photos failed with exception %s", e.toString());
                    updateLocalFeedError(feed);
                }
            }.start();
        }


        //

    }

    private void uploadFeed(Feed feed, String temporaryFeedId) {
        mWebService.createFeed(feed, new SuccessCallback<Feed>() {
            @Override
            public void onSuccess(Feed newFeed) {
                Timber.i("upload feed succeed %s", newFeed.toString());
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
