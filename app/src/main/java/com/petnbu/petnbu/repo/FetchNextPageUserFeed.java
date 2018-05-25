package com.petnbu.petnbu.repo;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.Nullable;

import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.model.CommentEntity;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.Paging;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.Status;

import java.util.ArrayList;
import java.util.List;

public class FetchNextPageUserFeed implements Runnable{

    //data boolean return if new feed has more item or not
    private final MutableLiveData<Resource<Boolean>> mLiveData = new MutableLiveData<>();
    private final PetDb mPetDb;
    private final WebService mWebService;
    private final AppExecutors mAppExecutors;
    private final String mPagingId;

    public FetchNextPageUserFeed(String pagingId, WebService webService, PetDb petDb, AppExecutors appExecutors) {
        mPagingId = pagingId;
        mWebService = webService;
        mPetDb = petDb;
        mAppExecutors = appExecutors;
    }

    @Override
    public void run() {
        Paging currentPaging = mPetDb.pagingDao().findFeedPaging(mPagingId);

        if (currentPaging == null || currentPaging.isEnded() || currentPaging.getOldestId() == null) {
            mLiveData.postValue(new Resource<>(Status.SUCCESS, false, null));
            return;
        }

        mLiveData.postValue(new Resource<>(Status.LOADING, null, null));
        LiveData<ApiResponse<List<Feed>>> result = mWebService.getUserFeed(mPagingId, currentPaging.getOldestId(), FeedRepository.FEEDS_PER_PAGE);
        result.observeForever(new Observer<ApiResponse<List<Feed>>>() {
            @Override
            public void onChanged(@Nullable ApiResponse<List<Feed>> listApiResponse) {
                if (listApiResponse != null) {
                    result.removeObserver(this);
                    if (listApiResponse.isSucceed) {
                        if (listApiResponse.body != null && listApiResponse.body.size() > 0) {
                            List<String> ids = new ArrayList<>(currentPaging.getIds());
                            for (Feed item : listApiResponse.body) {
                                ids.add(item.getFeedId());
                            }
                            Paging newPaging = new Paging(mPagingId, ids, false, ids.get(ids.size() - 1));
                            mAppExecutors.diskIO().execute(() -> {
                                mPetDb.beginTransaction();
                                try{
                                    mPetDb.feedDao().insertFromFeedList(listApiResponse.body);
                                    for (Feed item : listApiResponse.body) {
                                        mPetDb.userDao().insert(item.getFeedUser());
                                        if (item.getLatestComment() != null) {
                                            //the latestComment return from server does not have latestSubComment
                                            mPetDb.commentDao().insertIfNotExists(item.getLatestComment().toEntity());
                                            mPetDb.userDao().insert(item.getLatestComment().getFeedUser());
                                        }
                                    }
                                    mPetDb.pagingDao().insert(newPaging);
                                    mPetDb.setTransactionSuccessful();
                                }finally {
                                    mPetDb.endTransaction();
                                }

                            });
                            mLiveData.postValue(new Resource<>(Status.SUCCESS, true, null));
                        } else {
                            currentPaging.setEnded(true);
                            mAppExecutors.diskIO().execute(() -> mPetDb.pagingDao().update(currentPaging));
                            mLiveData.postValue(new Resource<>(Status.SUCCESS, false, null));
                        }
                    } else {
                        mLiveData.postValue(new Resource<>(Status.ERROR, true, listApiResponse.errorMessage));
                    }
                }
            }
        });
    }

    public MutableLiveData<Resource<Boolean>> getLiveData() {
        return mLiveData;
    }
}
