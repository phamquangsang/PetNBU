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
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedPaging;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.User;
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

    public LiveData<Resource<List<Feed>>> loadFeeds(String pagingId) {
        return new NetworkBoundResource<List<Feed>, List<Feed>>(mAppExecutors) {
            @Override
            protected void saveCallResult(@NonNull List<Feed> items) {
                List<String> listId = new ArrayList<>(items.size());
                for (Feed item : items) {
                    listId.add(item.getFeedId());
                }
                FeedPaging paging = new FeedPaging(pagingId,
                        listId, false,
                        listId.get(listId.size() - 1));
                mPetDb.beginTransaction();
                try {
                    mFeedDao.insert(items);
                    mFeedDao.insert(paging);
                    mPetDb.setTransactionSuccessful();
                } finally {
                    mPetDb.endTransaction();
                }

            }

            @Override
            protected boolean shouldFetch(@Nullable List<Feed> data) {
                return true;
            }

            @NonNull
            @Override
            protected LiveData<List<Feed>> loadFromDb() {
                return Transformations.switchMap(mFeedDao.loadFeedPaging(FeedPaging.GLOBAL_FEEDS_PAGING_ID), input -> {
                    if (input == null) {
                        MutableLiveData<List<Feed>> data = new MutableLiveData<>();
                        data.postValue(null);
                        return data;
                    } else {
                        Timber.i("loadFeedsFromDb paging: %s", input.toString());
                        return mFeedDao.loadFeeds(input.getFeedIds());
                    }
                });
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<List<Feed>>> createCall() {
                return mWebService.getGlobalFeeds(System.currentTimeMillis(), FEEDS_PER_PAGE);
            }

            @Override
            protected boolean shouldDeleteOldData(List<Feed> body) {
                return true;
            }

            @Override
            protected void deleteDataFromDb(List<Feed> body) {
                mPetDb.beginTransaction();
                FeedPaging paging = mFeedDao.findFeedPaging(pagingId);
                try {
                    mFeedDao.deleteFeedPaging(pagingId);
                    mPetDb.setTransactionSuccessful();
                } finally {
                    mPetDb.endTransaction();
                }
            }
        }.asLiveData();
    }

    public LiveData<Resource<Feed>> getFeed(String feedId){
        return new NetworkBoundResource<Feed, Feed>(mAppExecutors){
            @Override
            protected void saveCallResult(@NonNull Feed item) {
                mFeedDao.insert(item);
            }

            @Override
            protected boolean shouldFetch(@Nullable Feed data) {
                return data == null;
            }

            @Override
            protected void deleteDataFromDb(Feed body) {
                mFeedDao.deleteFeedById(feedId);
            }

            @NonNull
            @Override
            protected LiveData<Feed> loadFromDb() {
                return mFeedDao.loadFeedById(feedId);
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<Feed>> createCall() {
                return mWebService.getFeed(feedId);
            }
        }.asLiveData();
    }

    public LiveData<Resource<List<Feed>>> loadUserFeeds(String userId) {
        return new NetworkBoundResource<List<Feed>, List<Feed>>(mAppExecutors) {
            @Override
            protected void saveCallResult(@NonNull List<Feed> items) {
                if(items.isEmpty()){
                    return;
                }
                List<String> listId = new ArrayList<>(items.size());
                for (Feed item : items) {
                    listId.add(item.getFeedId());
                }
                FeedPaging paging = new FeedPaging(userId,
                        listId, false,
                        listId.get(listId.size() - 1));
                mPetDb.beginTransaction();
                try {
                    mFeedDao.insert(items);
                    mFeedDao.insert(paging);
                    mPetDb.setTransactionSuccessful();
                } finally {
                    mPetDb.endTransaction();
                }

            }

            @Override
            protected boolean shouldFetch(@Nullable List<Feed> data) {
                return data == null || data.isEmpty() || mRateLimiter.shouldFetch(userId);
            }

            @NonNull
            @Override
            protected LiveData<List<Feed>> loadFromDb() {
                return Transformations.switchMap(mFeedDao.loadFeedPaging(userId), input -> {
                    if (input == null) {
                        MutableLiveData<List<Feed>> data = new MutableLiveData<>();
                        data.postValue(null);
                        return data;
                    } else {
                        Timber.i("loadFeedsFromDb paging: %s", input.toString());
                        return mFeedDao.loadFeeds(input.getFeedIds());
                    }
                });
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<List<Feed>>> createCall() {
                Date now = new Date();
                return mWebService.getUserFeed(userId, now.getTime(), FEEDS_PER_PAGE);
            }

            @Override
            protected boolean shouldDeleteOldData(List<Feed> body) {
                boolean shouldDelete = body.isEmpty() || mRateLimiter.shouldFetch(userId);
                Timber.i("loadUserProfile: should delete = %s", shouldDelete);
                return shouldDelete;
            }

            @Override
            protected void deleteDataFromDb(List<Feed> body) {
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

    public void createNewFeed(Feed feed) {
        mAppExecutors.diskIO().execute(() -> {
            User user = mUserDao.findUserById(SharedPrefUtil.getUserId(mApplication));
            FeedUser feedUser = new FeedUser(user.getUserId(), user.getAvatar().getOriginUrl(), user.getName());
            feed.setStatus(Feed.STATUS_UPLOADING);
            feed.setFeedUser(feedUser);
            feed.setTimeCreated(new Date());
            feed.setTimeUpdated(new Date());
            feed.setFeedId(IdUtil.generateID("feed"));

            FeedPaging currentFeedsPaging = mFeedDao.findFeedPaging(FeedPaging.GLOBAL_FEEDS_PAGING_ID);
            currentFeedsPaging.getFeedIds().add(0, feed.getFeedId());

            mPetDb.beginTransaction();
            try {
                mFeedDao.update(currentFeedsPaging);
                mFeedDao.insert(feed);
                mPetDb.setTransactionSuccessful();
            } finally {
                mPetDb.endTransaction();
            }

            scheduleCreateFeedJob(feed);
        });
    }

    private void scheduleCreateFeedJob(Feed feed) {
        FirebaseJobDispatcher jobDispatcher = PetApplication.getAppComponent().getJobDispatcher();
        Job job = jobDispatcher.newJobBuilder()
                .setService(CreateFeedJob.class)
                .setExtras(CreateFeedJob.putExtras(feed.getFeedId()))
                .setTag(feed.getFeedId())
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .setTrigger(Trigger.executionWindow(0, 0))
                .build();
        jobDispatcher.mustSchedule(job);
    }

    public LiveData<Resource<List<Feed>>> refresh() {
        return new NetworkBoundResource<List<Feed>, List<Feed>>(mAppExecutors) {
            @Override
            protected void saveCallResult(@NonNull List<Feed> items) {
                List<String> listId = new ArrayList<>(items.size());
                for (Feed item : items) {
                    listId.add(item.getFeedId());
                }
                FeedPaging paging = new FeedPaging(FeedPaging.GLOBAL_FEEDS_PAGING_ID,
                        listId, false,
                        listId.get(listId.size() - 1));
                mPetDb.beginTransaction();
                try {
                    mFeedDao.insert(items);
                    mFeedDao.insert(paging);
                    mPetDb.setTransactionSuccessful();
                } finally {
                    mPetDb.endTransaction();
                }

            }

            @Override
            protected boolean shouldFetch(@Nullable List<Feed> data) {
                return true;
            }

            @NonNull
            @Override
            protected LiveData<List<Feed>> loadFromDb() {
                return Transformations.switchMap(mFeedDao.loadFeedPaging(FeedPaging.GLOBAL_FEEDS_PAGING_ID), input -> {
                    if (input == null) {
                        MutableLiveData<List<Feed>> data = new MutableLiveData<>();
                        data.postValue(null);
                        return data;
                    } else {
                        return mFeedDao.loadFeeds(input.getFeedIds());
                    }
                });
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<List<Feed>>> createCall() {
                return mWebService.getGlobalFeeds(System.currentTimeMillis(), FEEDS_PER_PAGE);
            }

            @Override
            protected boolean shouldDeleteOldData(List<Feed> body) {
                return true;
            }

            @Override
            protected void deleteDataFromDb(List<Feed> body) {
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
