package com.petnbu.petnbu.repo;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Trigger;
import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.SharedPrefUtil;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.FeedDao;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.db.UserDao;
import com.petnbu.petnbu.jobs.CreateFeedJob;
import com.petnbu.petnbu.model.FeedResponse;
import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.FeedPaging;
import com.petnbu.petnbu.model.FeedUIModel;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.UserEntity;
import com.petnbu.petnbu.util.IdUtil;
import com.petnbu.petnbu.util.RateLimiter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class FeedRepository {

    public static final int FEEDS_PER_PAGE = 10;

    private final PetDb mPetDb;

    private final FeedDao mFeedDao;

    private final UserDao mUserDao;

    private final AppExecutors mAppExecutors;

    private final WebService mWebService;

    private final Application mApplication;

    private final RateLimiter<String> mRateLimiter = new RateLimiter<>(10, TimeUnit.MINUTES);

    @Inject
    public FeedRepository(PetDb petDb, FeedDao feedDao, UserDao userDao, AppExecutors appExecutors, WebService webService, Application application) {
        mPetDb = petDb;
        mFeedDao = feedDao;
        mUserDao = userDao;
        mAppExecutors = appExecutors;
        mWebService = webService;
        mApplication = application;
    }

    public LiveData<Resource<List<FeedUIModel>>> loadFeeds(String pagingId) {
        return new NetworkBoundResource<List<FeedUIModel>, List<FeedResponse>>(mAppExecutors) {
            @Override
            protected void saveCallResult(@NonNull List<FeedResponse> items) {
                List<String> listId = new ArrayList<>(items.size());
                for (FeedResponse item : items) {
                    listId.add(item.getFeedId());
                }
                FeedPaging paging = new FeedPaging(pagingId,
                        listId, false,
                        listId.get(listId.size() - 1));
                mPetDb.beginTransaction();
                try {
                    mFeedDao.insertFromFeedList(items);
                    for (FeedResponse item : items) {
                        mUserDao.insert(item.getFeedUser());
                    }
                    mFeedDao.insert(paging);
                    mPetDb.setTransactionSuccessful();
                } finally {
                    mPetDb.endTransaction();
                }

            }

            @Override
            protected boolean shouldFetch(@Nullable List<FeedUIModel> data) {
                return true;
            }

            @NonNull
            @Override
            protected LiveData<List<FeedUIModel>> loadFromDb() {
                return Transformations.switchMap(mFeedDao.loadFeedPaging(FeedPaging.GLOBAL_FEEDS_PAGING_ID), input -> {
                    if (input == null) {
                        MutableLiveData<List<FeedUIModel>> data = new MutableLiveData<>();
                        data.postValue(null);
                        return data;
                    } else {
                        Timber.i("loadFeedsFromDb paging: %s", input.toString());
                        return mFeedDao.loadFeedsIncludeUploadingPost(input.getFeedIds());
                    }
                });
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<List<FeedResponse>>> createCall() {
                return mWebService.getGlobalFeeds(System.currentTimeMillis(), FEEDS_PER_PAGE);
            }

            @Override
            protected boolean shouldDeleteOldData(List<FeedResponse> body) {
                return true;
            }

            @Override
            protected void deleteDataFromDb(List<FeedResponse> body) {
                mPetDb.beginTransaction();
                try {
                    mFeedDao.deleteFeedPaging(pagingId);
                    mPetDb.setTransactionSuccessful();
                } finally {
                    mPetDb.endTransaction();
                }
            }
        }.asLiveData();
    }

    public LiveData<Resource<FeedUIModel>> getFeed(String feedId){
        return new NetworkBoundResource<FeedUIModel, FeedResponse>(mAppExecutors){
            @Override
            protected void saveCallResult(@NonNull FeedResponse item) {
                mFeedDao.insertFromFeed(item);
            }

            @Override
            protected boolean shouldFetch(@Nullable FeedUIModel data) {
                return data == null;
            }

            @Override
            protected void deleteDataFromDb(FeedResponse body) {
                mFeedDao.deleteFeedById(feedId);
            }

            @NonNull
            @Override
            protected LiveData<FeedUIModel> loadFromDb() {
                return mFeedDao.loadFeedById(feedId);
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<FeedResponse>> createCall() {
                return mWebService.getFeed(feedId);
            }
        }.asLiveData();
    }

    public LiveData<Resource<List<FeedUIModel>>> loadUserFeeds(String userId) {
        return new NetworkBoundResource<List<FeedUIModel>, List<FeedResponse>>(mAppExecutors) {
            @Override
            protected void saveCallResult(@NonNull List<FeedResponse> items) {
                if(items.isEmpty()){
                    return;
                }
                List<String> listId = new ArrayList<>(items.size());
                for (FeedResponse item : items) {
                    listId.add(item.getFeedId());
                }
                FeedPaging paging = new FeedPaging(userId,
                        listId, false,
                        listId.get(listId.size() - 1));
                mPetDb.beginTransaction();
                try {
                    mFeedDao.insertFromFeedList(items);
                    for (FeedResponse item : items) {
                        mUserDao.insert(item.getFeedUser());
                    }
                    mFeedDao.insert(paging);
                    mPetDb.setTransactionSuccessful();
                } finally {
                    mPetDb.endTransaction();
                }

            }

            @Override
            protected boolean shouldFetch(@Nullable List<FeedUIModel> data) {
                return data == null || data.isEmpty() || mRateLimiter.shouldFetch(userId);
            }

            @NonNull
            @Override
            protected LiveData<List<FeedUIModel>> loadFromDb() {
                return Transformations.switchMap(mFeedDao.loadFeedPaging(userId), input -> {
                    if (input == null) {
                        MutableLiveData<List<FeedUIModel>> data = new MutableLiveData<>();
                        data.postValue(null);
                        return data;
                    } else {
                        Timber.i("loadFeedsFromDb paging: %s", input.toString());
                        return mFeedDao.loadFeedsIncludeUploadingPost(input.getFeedIds());
                    }
                });
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<List<FeedResponse>>> createCall() {
                Date now = new Date();
                return mWebService.getUserFeed(userId, now.getTime(), FEEDS_PER_PAGE);
            }

            @Override
            protected boolean shouldDeleteOldData(List<FeedResponse> body) {
                boolean shouldDelete = body.isEmpty() || mRateLimiter.shouldFetch(userId);
                Timber.i("loadUserProfile: should delete = %s", shouldDelete);
                return shouldDelete;
            }

            @Override
            protected void deleteDataFromDb(List<FeedResponse> body) {
                Timber.i("deleting old query for userId: %s", userId);
                mPetDb.beginTransaction();
                try {
                    mFeedDao.deleteFeedPaging(userId);
                    mPetDb.setTransactionSuccessful();
                } finally {
                    mPetDb.endTransaction();
                }
            }
        }.asLiveData();
    }

    public LiveData<Resource<Boolean>> fetchNextPage(String pagingId) {
        FetchNextPageGlobalFeed fetchNextPageTask = new FetchNextPageGlobalFeed(pagingId, mWebService, mPetDb, mAppExecutors);
        mAppExecutors.networkIO().execute(fetchNextPageTask);
        return fetchNextPageTask.getLiveData();
    }

    public LiveData<Resource<Boolean>> fetchNextUserFeedPage(String pagingId){
        FetchNextPageUserFeed fetchNextPageUserFeed = new FetchNextPageUserFeed(pagingId, mWebService, mPetDb, mAppExecutors);
        mAppExecutors.networkIO().execute(fetchNextPageUserFeed);
        return fetchNextPageUserFeed.getLiveData();
    }

    public void createNewFeed(FeedResponse feedResponse) {
        mAppExecutors.diskIO().execute(() -> {
            UserEntity userEntity = mUserDao.findUserById(SharedPrefUtil.getUserId(mApplication));
            FeedUser feedUser = new FeedUser(userEntity.getUserId(), userEntity.getAvatar().getOriginUrl(), userEntity.getName());
            feedResponse.setStatus(FeedEntity.STATUS_UPLOADING);
            feedResponse.setFeedUser(feedUser);
            feedResponse.setTimeCreated(new Date());
            feedResponse.setTimeUpdated(new Date());
            feedResponse.setFeedId(IdUtil.generateID("feed"));

            mFeedDao.insertFromFeed(feedResponse);

            scheduleCreateFeedJob(feedResponse);
        });
    }

    private void scheduleCreateFeedJob(FeedResponse feedResponse) {
        FirebaseJobDispatcher jobDispatcher = PetApplication.getAppComponent().getJobDispatcher();
        Job job = jobDispatcher.newJobBuilder()
                .setService(CreateFeedJob.class)
                .setExtras(CreateFeedJob.putExtras(feedResponse.getFeedId()))
                .setTag(feedResponse.getFeedId())
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .setTrigger(Trigger.executionWindow(0, 0))
                .build();
        jobDispatcher.mustSchedule(job);
    }

    public LiveData<Resource<List<FeedUIModel>>> refresh() {
        return new NetworkBoundResource<List<FeedUIModel>, List<FeedResponse>>(mAppExecutors) {
            @Override
            protected void saveCallResult(@NonNull List<FeedResponse> items) {
                List<String> listId = new ArrayList<>(items.size());
                for (FeedResponse item : items) {
                    listId.add(item.getFeedId());
                }
                FeedPaging paging = new FeedPaging(FeedPaging.GLOBAL_FEEDS_PAGING_ID,
                        listId, false,
                        listId.get(listId.size() - 1));
                mPetDb.beginTransaction();
                try {
                    mFeedDao.insertFromFeedList(items);
                    for (FeedResponse feedResponseItem : items) {
                        mUserDao.insert(feedResponseItem.getFeedUser());
                    }
                    mFeedDao.insert(paging);
                    mPetDb.setTransactionSuccessful();
                } finally {
                    mPetDb.endTransaction();
                }

            }

            @Override
            protected boolean shouldFetch(@Nullable List<FeedUIModel> data) {
                return true;
            }

            @NonNull
            @Override
            protected LiveData<List<FeedUIModel>> loadFromDb() {
                return Transformations.switchMap(mFeedDao.loadFeedPaging(FeedPaging.GLOBAL_FEEDS_PAGING_ID), input -> {
                    if (input == null) {
                        MutableLiveData<List<FeedUIModel>> data = new MutableLiveData<>();
                        data.postValue(null);
                        return data;
                    } else {
                        return mFeedDao.loadFeeds(input.getFeedIds());
                    }
                });
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<List<FeedResponse>>> createCall() {
                return mWebService.getGlobalFeeds(System.currentTimeMillis(), FEEDS_PER_PAGE);
            }

            @Override
            protected boolean shouldDeleteOldData(List<FeedResponse> body) {
                return true;
            }

            @Override
            protected void deleteDataFromDb(List<FeedResponse> body) {
                mPetDb.beginTransaction();
                FeedPaging paging = mFeedDao.findFeedPaging(FeedPaging.GLOBAL_FEEDS_PAGING_ID);
                try {
                    mFeedDao.deleteFeedPaging(FeedPaging.GLOBAL_FEEDS_PAGING_ID);
                    mPetDb.setTransactionSuccessful();
                } finally {
                    mPetDb.endTransaction();
                }
            }
        }.asLiveData();
    }
}
