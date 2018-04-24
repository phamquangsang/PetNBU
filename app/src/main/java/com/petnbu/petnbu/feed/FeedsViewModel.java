package com.petnbu.petnbu.feed;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.model.Feed;

import java.util.List;

public class FeedsViewModel extends AndroidViewModel {

    private final int LIMIT_FEEDS = 10;
    private final WebService mWebService;
    private LiveData<List<Feed>> mFeedsLiveData;

    public FeedsViewModel(@NonNull Application application, WebService webService) {
        super(application);
        mWebService = webService;
    }

    public LiveData<List<Feed>> getFeeds() {
        mFeedsLiveData = mWebService.getFeeds(0, LIMIT_FEEDS);
        return mFeedsLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
