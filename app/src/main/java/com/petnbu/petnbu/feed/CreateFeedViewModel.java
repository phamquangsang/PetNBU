package com.petnbu.petnbu.feed;

import android.app.Application;
import android.arch.lifecycle.ViewModel;

import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.SharedPrefUtil;
import com.petnbu.petnbu.SingleLiveEvent;
import com.petnbu.petnbu.api.SuccessCallback;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.repo.UserRepository;

import java.util.ArrayList;

import javax.inject.Inject;

public class CreateFeedViewModel extends ViewModel {

    public final SingleLiveEvent<Boolean> showLoadingLiveData = new SingleLiveEvent<>();
    public final SingleLiveEvent<Boolean> createdFeedLiveData = new SingleLiveEvent<>();

    @Inject
    WebService mWebService;

    @Inject
    UserRepository mUserRepository;

    @Inject
    Application mApplication;

    public CreateFeedViewModel() {
        PetApplication.getAppComponent().inject(this);
    }

    public void createFeed(String content, ArrayList<Photo> photos) {
        showLoadingLiveData.setValue(true);


//        if(feed.getPhotos().isEmpty()) {
//            createFeed(feed);
//        } else {
//            ArrayList<String> urls = new ArrayList<>();
//            for (Photo photo : feed.getPhotos()) {
//                urls.add(photo.getOriginUrl());
//            }
//            showLoadingLiveData.setValue(true);
//
//            StorageApi.OnUploadingImage uploadingImage = new StorageApi.OnUploadingImage(urls) {
//                @Override
//                public void onCompleted(List<String> result) {
//                    for (int i = 0; i < feed.getPhotos().size(); i++) {
//                        feed.getPhotos().get(i).setOriginUrl(result.get(i));
//                    }
//                    createFeed(feed);
//                }
//
//                @Override
//                public void onFailed(Exception e) {
//                    showLoadingLiveData.setValue(false);
//                }
//            };
//            uploadingImage.start();
//        }
    }

    private void createFeed(Feed feed) {
        mWebService.createFeed(feed, new SuccessCallback<Feed>() {
            @Override
            public void onSuccess(Feed feed) {
                showLoadingLiveData.setValue(false);
                createdFeedLiveData.setValue(true);
            }

            @Override
            public void onFailed(Exception e) {
                showLoadingLiveData.setValue(false);
                createdFeedLiveData.setValue(false);
            }
        });
    }

    private Feed generateFeed(String content, ArrayList<Photo> photos) {
        Feed feed = new Feed();
        FeedUser feedUser = new FeedUser();
        feedUser.setUserId(SharedPrefUtil.getUserId(mApplication));
        feed.setFeedUser(feedUser);
        feed.setContent(content);
        feed.setPhotos(photos);
        return feed;
    }
}
