package com.petnbu.petnbu.feed;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;

import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.SharedPrefUtil;
import com.petnbu.petnbu.SingleLiveEvent;
import com.petnbu.petnbu.model.FeedResponse;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.UserEntity;
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

    public LiveData<UserEntity> loadUserInfo() {
        return Transformations.switchMap(mUserRepository.getUserById(SharedPrefUtil.getUserId(mApplication)), userResource -> {
            MutableLiveData<UserEntity> userLiveData = new MutableLiveData<>();
            if (userResource != null && userResource.data != null) {
                userLiveData.setValue(userResource.data);
            } else {
                userLiveData.setValue(null);
            }
            return userLiveData;
        });
    }

    public void createFeed(String content, ArrayList<Photo> photos) {
        FeedResponse feedResponse = new FeedResponse();
        feedResponse.setContent(content);
        feedResponse.setPhotos(photos);
        mFeedRepository.createNewFeed(feedResponse);
    }
}
