package com.petnbu.petnbu.repo;

import android.app.MediaRouteActionProvider;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;

import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedPaging;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.Status;

import java.util.ArrayList;
import java.util.List;

public class FetchNextPageFeed implements Runnable {

    //data boolean return if new feed has more item or not
    private final MutableLiveData<Resource<Boolean>> mLiveData = new MutableLiveData<>();
    private final PetDb mPetDb;
    private final WebService mWebService;
    private final AppExecutors mAppExecutors;
    private final String mPagingId;

    public FetchNextPageFeed(String pagingId, WebService webService, PetDb petDb, AppExecutors appExecutors) {
        mPagingId = pagingId;
        mWebService = webService;
        mPetDb = petDb;
        mAppExecutors = appExecutors;
    }

    @Override
    public void run() {
        FeedPaging currentPaging = mPetDb.feedDao().findFeedPaging(mPagingId);
        if (currentPaging == null) {
            mLiveData.postValue(null);
            return;
        }
        Feed oldestFeed = mPetDb.feedDao().findFeedById(currentPaging.getOldestFeedId());

        if (currentPaging.isEnded()) {
            mLiveData.postValue(new Resource<>(Status.SUCCESS, true, null));
            return;
        }

        mLiveData.postValue(new Resource<>(Status.LOADING, null, null));
        LiveData<ApiResponse<List<Feed>>> result = mWebService.getFeeds(oldestFeed.getTimeCreated().getTime(), FeedRepository.FEEDS_PER_PAGE);
        result.observeForever(new Observer<ApiResponse<List<Feed>>>() {
            @Override
            public void onChanged(@Nullable ApiResponse<List<Feed>> listApiResponse) {
                if (listApiResponse != null) {
                    result.removeObserver(this);
                    if (listApiResponse.isSucceed) {
                        if (listApiResponse.body != null && listApiResponse.body.size() > 0) {
                            List<String> ids = new ArrayList<>(currentPaging.getFeedIds());
                            for (Feed item : listApiResponse.body) {
                                ids.add(item.getFeedId());
                            }
                            FeedPaging newPaging = new FeedPaging(FeedPaging.GLOBAL_FEEDS_PAGING_ID, ids, false, ids.get(ids.size() - 1));
                            mAppExecutors.diskIO().execute(() -> {
                                mPetDb.beginTransaction();
                                try{
                                    mPetDb.feedDao().insert(listApiResponse.body);
                                    mPetDb.feedDao().insert(newPaging);
                                    mPetDb.setTransactionSuccessful();
                                }finally {
                                    mPetDb.endTransaction();
                                }

                            });
                            mLiveData.postValue(new Resource<>(Status.SUCCESS, true, null));
                        } else {
                            currentPaging.setEnded(true);
                            mAppExecutors.diskIO().execute(() -> mPetDb.feedDao().update(currentPaging));
                            mLiveData.postValue(new Resource<>(Status.SUCCESS, false, null));
                        }
                    } else {
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
