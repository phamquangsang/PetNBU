package com.petnbu.petnbu.feed;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;
import android.text.TextUtils;

import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.SharedPrefUtil;
import com.petnbu.petnbu.SingleLiveEvent;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.User;
import com.petnbu.petnbu.repo.FeedRepository;
import com.petnbu.petnbu.repo.UserRepository;

import java.util.ArrayList;

import javax.inject.Inject;

public class CreateEditFeedViewModel extends ViewModel {

    public final SingleLiveEvent<Boolean> showLoadingLiveData = new SingleLiveEvent<>();

    @Inject
    UserRepository mUserRepository;

    @Inject
    FeedRepository mFeedRepository;

    @Inject
    Application mApplication;

    private final MutableLiveData<Feed> feedLiveData = new MutableLiveData<>();

    private boolean mIsNewFeed;

    private String mFeedId;

    public CreateEditFeedViewModel() {
        PetApplication.getAppComponent().inject(this);
    }

    public LiveData<User> loadUserInfo() {
        return Transformations.switchMap(mUserRepository.getUserById(SharedPrefUtil.getUserId(mApplication)), userResource -> {
            MutableLiveData<User> userLiveData = new MutableLiveData<>();
            if (userResource != null && userResource.data != null) {
                userLiveData.setValue(userResource.data);
            } else {
                userLiveData.setValue(null);
            }
            return userLiveData;
        });
    }

    public LiveData<Feed> loadFeed(String feedId) {
        if(!TextUtils.isEmpty(feedId)) {
            return Transformations.switchMap(mFeedRepository.getFeed(feedId), input -> {
                feedLiveData.setValue(input.data);
                if(input.data != null) {
                    mFeedId = feedId;
                } else {
                    // load failed
                }
                return feedLiveData;
            });
        } else {
            mIsNewFeed = true;
            feedLiveData.setValue(null);
            return feedLiveData;
        }
    }

    public void saveFeed(String content, ArrayList<Photo> photos) {
        if(mIsNewFeed && TextUtils.isEmpty(mFeedId)) {
            Feed feed = new Feed();
            feed.setContent(content);
            feed.setPhotos(photos);
            mFeedRepository.createNewFeed(feed);
        } else {
            // Update feed
        }
    }
}
