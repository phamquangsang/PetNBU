package com.petnbu.petnbu.repo;

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.FeedDao;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.db.UserDao;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.Resource;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FeedRepository {

    private final PetDb mPetDb;

    private final FeedDao mFeedDao;

    private final UserDao mUserDao;

    private final AppExecutors mAppExecutors;

    private final WebService mWebService;

    @Inject
    public FeedRepository(PetDb petDb, FeedDao feedDao, UserDao userDao, AppExecutors appExecutors, WebService webService) {
        mPetDb = petDb;
        mFeedDao = feedDao;
        mUserDao = userDao;
        mAppExecutors = appExecutors;
        mWebService = webService;
    }

    public LiveData<Resource<List<Feed>>> loadFeeds(){
        return new NetworkBoundResource<List<Feed>, List<Feed>>(mAppExecutors){
            @Override
            protected void saveCallResult(@NonNull List<Feed> item) {
                mFeedDao.insert(item);
            }

            @Override
            protected boolean shouldFetch(@Nullable List<Feed> data) {
                return true;
//                return data == null || data.isEmpty();
            }

            @NonNull
            @Override
            protected LiveData<List<Feed>> loadFromDb() {
                return mFeedDao.loadFeeds();
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<List<Feed>>> createCall() {
                return mWebService.getFeeds(System.currentTimeMillis(), 50);
            }
        }.asLiveData();
    }

}
