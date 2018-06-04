package com.petnbu.petnbu.feed;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;
import android.databinding.ObservableBoolean;
import android.text.TextUtils;

import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.SharedPrefUtil;
import com.petnbu.petnbu.SingleLiveEvent;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.Status;
import com.petnbu.petnbu.model.UserEntity;
import com.petnbu.petnbu.repo.FeedRepository;
import com.petnbu.petnbu.repo.UserRepository;

import java.util.ArrayList;

import javax.inject.Inject;

import timber.log.Timber;

public class CreateEditFeedViewModel extends ViewModel {

    @Inject
    UserRepository mUserRepository;

    @Inject
    FeedRepository mFeedRepository;

    @Inject
    Application mApplication;

    public final ObservableBoolean showLoading = new ObservableBoolean();

    private final MutableLiveData<Feed> feedLiveData = new MutableLiveData<>();

    private boolean mIsNewFeed;

    private String mFeedId;

    private ArrayList<Photo> mSelectedPhotos = new ArrayList<>();

    public CreateEditFeedViewModel() {
        Timber.i("create CreateEditFeedViewModel");
        PetApplication.getAppComponent().inject(this);
    }

    public LiveData<UserEntity> loadUserInfo() {
        return Transformations.switchMap(mUserRepository.getUserById(SharedPrefUtil.INSTANCE.getUserId()), userResource -> {
            MutableLiveData<UserEntity> userLiveData = new MutableLiveData<>();
            if (userResource != null && userResource.data != null) {
                userLiveData.setValue(userResource.data);
            } else {
                userLiveData.setValue(null);
            }
            return userLiveData;
        });
    }

    public ArrayList<Photo> getSelectedPhotos() {
        return mSelectedPhotos;
    }

    public LiveData<Feed> getFeed(String feedId) {
        if(!TextUtils.isEmpty(feedId)) {
            if(feedLiveData.getValue() == null){
                return Transformations.switchMap(mFeedRepository.getFeed(feedId), input -> {
                    feedLiveData.setValue(input.data);
                    if(input.data != null) {
                        mFeedId = feedId;
                    }
                    showLoading.set(Status.LOADING.equals(input.status));
                    return feedLiveData;
                });
            }
            return feedLiveData;
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
            Feed feed = new Feed();
            feed.setFeedId(mFeedId);
            feed.setContent(content);
            feed.setPhotos(photos);
            mFeedRepository.updateFeed(feed);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Timber.i("onCleared CreateEditFeedViewModel");
    }
}
