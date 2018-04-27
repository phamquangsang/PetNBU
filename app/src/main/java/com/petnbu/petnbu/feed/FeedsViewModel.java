package com.petnbu.petnbu.feed;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;

import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.repo.FeedRepository;

import java.util.List;

import javax.inject.Inject;

public class FeedsViewModel extends ViewModel {

    @Inject
    FeedRepository mFeedRepository;

    private LiveData<Resource<List<Feed>>> mFeedsLiveData;

    public FeedsViewModel() {
        PetApplication.getAppComponent().inject(this);
    }

    public LiveData<Resource<List<Feed>>> getFeeds() {
        mFeedsLiveData = mFeedRepository.loadFeeds();
        return mFeedsLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
