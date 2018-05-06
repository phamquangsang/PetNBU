package com.petnbu.petnbu.feed;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;

import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.SharedPrefUtil;
import com.petnbu.petnbu.SingleLiveEvent;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.Status;
import com.petnbu.petnbu.model.User;
import com.petnbu.petnbu.repo.FeedRepository;
import com.petnbu.petnbu.repo.UserRepository;

import java.util.ArrayList;

import javax.inject.Inject;

public class CreateFeedViewModel extends ViewModel {

    public final SingleLiveEvent<Boolean> showLoadingLiveData = new SingleLiveEvent<>();
    public final SingleLiveEvent<Boolean> createdFeedLiveData = new SingleLiveEvent<>();

    @Inject
    UserRepository mUserRepository;

    @Inject
    FeedRepository mFeedRepository;

    @Inject
    Application mApplication;

    public CreateFeedViewModel() {
        PetApplication.getAppComponent().inject(this);
    }

    public LiveData<User> loadUserInfos() {
        LiveData<Resource<User>> userResourceLive = mUserRepository.getUserById(SharedPrefUtil.getUserId(mApplication));
        return Transformations.switchMap(userResourceLive, userResource -> {
            MutableLiveData<User> userLiveData = new MutableLiveData<>();
            if (userResource != null && userResource.data != null) {
                userLiveData.setValue(userResource.data);
            } else {
                userLiveData.setValue(null);
            }
            return userLiveData;
        });
    }

    public void createFeed(String content, ArrayList<Photo> photos) {
        Feed feed = new Feed();
        feed.setContent(content);
        feed.setPhotos(photos);
        mFeedRepository.createNewFeed(feed);
    }
}
