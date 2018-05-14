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
import com.petnbu.petnbu.jobs.CreateEditFeedJob;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.Paging;
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

    public LiveData<Resource<List<Feed>>> loadFeeds(String pagingId) {
        return new NetworkBoundResource<List<Feed>, List<Feed>>(mAppExecutors) {
            @Override
            protected void saveCallResult(@NonNull List<Feed> items) {
                List<String> listId = new ArrayList<>(items.size());
                Paging paging;
                if(items.isEmpty()){
                    paging = new Paging(pagingId, listId, true, null);
                }else{
                    for (Feed item : items) {
                        listId.add(item.getFeedId());
                    }
                    paging = new Paging(pagingId,
                            listId, false,
                            listId.get(listId.size() - 1));
                }
                mPetDb.runInTransaction(() -> {
                    mFeedDao.insertFromFeedList(items);
                    for (Feed item : items) {
                        mUserDao.insert(item.getFeedUser());
                    }
                    mFeedDao.insert(paging);
                });
            }

            @Override
            protected boolean shouldFetch(@Nullable List<Feed> data) {
                return true;
            }

            @NonNull
            @Override
            protected LiveData<List<Feed>> loadFromDb() {
                return Transformations.switchMap(mFeedDao.loadFeedPaging(Paging.GLOBAL_FEEDS_PAGING_ID), input -> {
                    if (input == null) {
                        MutableLiveData<List<Feed>> data = new MutableLiveData<>();
                        data.postValue(null);
                        return data;
                    } else {
                        Timber.i("loadFeedsFromDb paging: %s", input.toString());
                        return mFeedDao.loadFeedsIncludeUploadingPost(input.getIds());
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
                mFeedDao.deleteFeedPaging(pagingId);
            }
        }.asLiveData();
    }

    public LiveData<Resource<Feed>> getFeed(String feedId){
        return new NetworkBoundResource<Feed, Feed>(mAppExecutors){
            @Override
            protected void saveCallResult(@NonNull Feed item) {
                mFeedDao.insertFromFeed(item);
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
                Paging paging = new Paging(userId,
                        listId, false,
                        listId.get(listId.size() - 1));

                mPetDb.runInTransaction(() -> {
                    mFeedDao.insertFromFeedList(items);
                    for (Feed item : items) {
                        mUserDao.insert(item.getFeedUser());
                    }
                    mFeedDao.insert(paging);
                });
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
                        return mFeedDao.loadFeedsIncludeUploadingPost(input.getIds());
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
                mFeedDao.deleteFeedPaging(userId);
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
            mPetDb.runInTransaction(() -> {
                UserEntity userEntity = mUserDao.findUserById(SharedPrefUtil.getUserId(mApplication));
                FeedUser feedUser = new FeedUser(userEntity.getUserId(), userEntity.getAvatar(), userEntity.getName());
                feed.setStatus(FeedEntity.STATUS_UPLOADING);
                feed.setFeedUser(feedUser);
                feed.setTimeCreated(new Date());
                feed.setTimeUpdated(new Date());
                feed.setFeedId(IdUtil.generateID("feed"));
                mFeedDao.insertFromFeed(feed);
            });
            scheduleSaveFeedJob(feed, false);
        });
    }

    public void updateFeed(Feed feed) {
        mAppExecutors.diskIO().execute(() -> {
            mPetDb.runInTransaction(() -> {
                FeedEntity feedEntity = mFeedDao.findFeedEntityById(feed.getFeedId());
                feedEntity.setStatus(FeedEntity.STATUS_UPLOADING);
                feedEntity.setTimeUpdated(new Date());
                feedEntity.setContent(feed.getContent());
                feedEntity.setPhotos(feed.getPhotos());
                mFeedDao.update(feedEntity);
            });
            scheduleSaveFeedJob(feed, true);
        });
    }

    private void scheduleSaveFeedJob(Feed feed, boolean isUpdating) {
        FirebaseJobDispatcher jobDispatcher = PetApplication.getAppComponent().getJobDispatcher();
        Job job = jobDispatcher.newJobBuilder()
                .setService(CreateEditFeedJob.class)
                .setExtras(CreateEditFeedJob.extras(feed.getFeedId(), isUpdating))
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
                Paging paging = new Paging(Paging.GLOBAL_FEEDS_PAGING_ID,
                        listId, false,
                        listId.get(listId.size() - 1));
                mPetDb.runInTransaction(() -> {
                    mFeedDao.insertFromFeedList(items);
                    for (Feed feedItem : items) {
                        mUserDao.insert(feedItem.getFeedUser());
                    }
                    mFeedDao.insert(paging);
                });
            }

            @Override
            protected boolean shouldFetch(@Nullable List<Feed> data) {
                return true;
            }

            @NonNull
            @Override
            protected LiveData<List<Feed>> loadFromDb() {
                return Transformations.switchMap(mFeedDao.loadFeedPaging(Paging.GLOBAL_FEEDS_PAGING_ID), input -> {
                    if (input == null) {
                        MutableLiveData<List<Feed>> data = new MutableLiveData<>();
                        data.postValue(null);
                        return data;
                    } else {
                        return mFeedDao.loadFeeds(input.getIds());
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
                mFeedDao.deleteFeedPaging(Paging.GLOBAL_FEEDS_PAGING_ID);
            }
        }.asLiveData();
    }
}
