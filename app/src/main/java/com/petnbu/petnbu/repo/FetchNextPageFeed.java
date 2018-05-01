package com.petnbu.petnbu.repo;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;

import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.Status;

import java.util.List;

public class FetchNextPageFeed implements Runnable{

    //data boolean return if new feed has more item or not
    private final MutableLiveData<Resource<Boolean>> mLiveData = new MutableLiveData<>();
    private final PetDb mPetDb;
    private final WebService mWebService;
    private final AppExecutors mAppExecutors;
    private final Feed mFeed;

    public FetchNextPageFeed(Feed lastFeed, WebService webService, PetDb petDb, AppExecutors appExecutors) {
        mFeed = lastFeed;
        mWebService = webService;
        mPetDb = petDb;
        mAppExecutors = appExecutors;
    }

    @Override
    public void run() {
        mLiveData.postValue(new Resource<>(Status.LOADING, null, null));
        LiveData<ApiResponse<List<Feed>>> result = mWebService.getFeeds(mFeed.getTimeCreated().getTime(), FeedRepository.FEEDS_PER_PAGE);
        result.observeForever(new Observer<ApiResponse<List<Feed>>>() {
            @Override
            public void onChanged(@Nullable ApiResponse<List<Feed>> listApiResponse) {
                if(listApiResponse != null){
                    result.removeObserver(this);
                    if(listApiResponse.isSucceed){
                        if(listApiResponse.body != null && listApiResponse.body.size() > 0){
                            mAppExecutors.diskIO().execute(() -> mPetDb.feedDao().insert(listApiResponse.body));
                            mLiveData.postValue(new Resource<>(Status.SUCCESS, true, null));
                        } else{
                            mLiveData.postValue(new Resource<>(Status.SUCCESS, false, null));
                        }
                    }else{
                        mLiveData.postValue(new Resource<>(Status.ERROR, null, listApiResponse.errorMessage));
                    }
                }
            }
        });
    }

    public MutableLiveData<Resource<Boolean>> getLiveData() {
        return mLiveData;
    }
}
