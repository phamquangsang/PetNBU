package com.petnbu.petnbu.feed;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import java.util.ArrayList;

public class FeedsViewModel extends AndroidViewModel {

    private final FeedsRepository mFeedsRepository;
//    private final MutableLiveData<ArrayList<Feed>> mFeeds;
//    private final Observer<ArrayList<Feed>> feedsObserver = new Observer<ArrayList<Feed>>() {
//        @Override
//        public void onChanged(@Nullable ArrayList<Feed> feeds) {
//            mFeeds.setValue(feeds);
//        }
//    };

    public FeedsViewModel(@NonNull Application application, FeedsRepository feedsRepository) {
        super(application);
        mFeedsRepository = feedsRepository;
//        mFeeds = new MutableLiveData<>();
//        mFeedsRepository.getFeeds().observeForever(feedsObserver);
    }

    public LiveData<ArrayList<Feed>> getFeeds() {
        return mFeedsRepository.getFeeds();
    }

    @Override
    protected void onCleared() {
//        mFeedsRepository.getFeeds().removeObserver(feedsObserver);
        super.onCleared();
    }
}
