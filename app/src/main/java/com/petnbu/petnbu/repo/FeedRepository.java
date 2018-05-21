package com.petnbu.petnbu.repo;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.SharedPrefUtil;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.CommentDao;
import com.petnbu.petnbu.db.FeedDao;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.db.UserDao;
import com.petnbu.petnbu.jobs.CompressPhotoWorker;
import com.petnbu.petnbu.jobs.CreateEditFeedWorker;
import com.petnbu.petnbu.jobs.UploadPhotoWorker;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.FeedUI;
import com.petnbu.petnbu.model.Paging;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.Photo;
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

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkManager;
import timber.log.Timber;

import static com.petnbu.petnbu.model.LocalStatus.STATUS_UPLOADING;

@Singleton
public class FeedRepository {

    public static final int FEEDS_PER_PAGE = 10;

    private final PetDb mPetDb;

    private final FeedDao mFeedDao;

    private final UserDao mUserDao;

    private final CommentDao mCommentDao;

    private final AppExecutors mAppExecutors;

    private final WebService mWebService;

    private final Application mApplication;

    private final RateLimiter<String> mRateLimiter = new RateLimiter<>(10, TimeUnit.MINUTES);

    @Inject
    public FeedRepository(PetDb petDb, FeedDao feedDao, UserDao userDao, CommentDao commentDao, AppExecutors appExecutors, WebService webService, Application application) {
        mPetDb = petDb;
        mFeedDao = feedDao;
        mUserDao = userDao;
        mCommentDao = commentDao;
        mAppExecutors = appExecutors;
        mWebService = webService;
        mApplication = application;
    }

    public LiveData<Resource<List<FeedUI>>> loadFeeds(String pagingId) {
        return new NetworkBoundResource<List<FeedUI>, List<Feed>>(mAppExecutors) {
            @Override
            protected void saveCallResult(@NonNull List<Feed> items) {
                List<String> listId = new ArrayList<>(items.size());
                Paging paging;
                for (Feed item : items) {
                    listId.add(item.getFeedId());
                }
                paging = new Paging(pagingId,
                        listId, listId.isEmpty(),
                        listId.isEmpty() ? null : listId.get(listId.size() - 1));
                mPetDb.runInTransaction(() -> {
                    mFeedDao.insertFromFeedList(items);
                    for (Feed item : items) {
                        mUserDao.insert(item.getFeedUser());
                        mCommentDao.insertFromComment(item.getLatestComment());
                    }
                    mFeedDao.insert(paging);
                });
            }

            @Override
            protected boolean shouldFetch(@Nullable List<FeedUI> data) {
                return data == null || data.isEmpty() || mRateLimiter.shouldFetch(pagingId);
            }

            @NonNull
            @Override
            protected LiveData<List<FeedUI>> loadFromDb() {
                return Transformations.switchMap(mFeedDao.loadFeedPaging(pagingId), input -> {
                    if (input == null) {
                        MutableLiveData<List<FeedUI>> data = new MutableLiveData<>();
                        data.postValue(null);
                        return data;
                    } else {
                        Timber.i("loadFeedsFromDb paging: %s", input.toString());
                        return mFeedDao.loadFeedsIds(input.getIds());
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

    public LiveData<Resource<Feed>> getFeed(String feedId) {
        return new NetworkBoundResource<Feed, Feed>(mAppExecutors) {
            @Override
            protected void saveCallResult(@NonNull Feed item) {
                mFeedDao.insertFromFeed(item);
                mUserDao.insert(item.getFeedUser());
                mCommentDao.insertFromComment(item.getLatestComment());
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

    public LiveData<Resource<List<FeedUI>>> loadUserFeeds(String userId, String pagingId) {
        return new NetworkBoundResource<List<FeedUI>, List<Feed>>(mAppExecutors) {
            @Override
            protected void saveCallResult(@NonNull List<Feed> items) {
                List<String> listId = new ArrayList<>(items.size());
                for (Feed item : items) {
                    listId.add(item.getFeedId());
                }
                Paging paging = new Paging(pagingId,
                        listId, listId.isEmpty(),
                        listId.isEmpty() ? null : listId.get(listId.size() - 1));

                mPetDb.runInTransaction(() -> {
                    mFeedDao.insertFromFeedList(items);
                    for (Feed item : items) {
                        mUserDao.insert(item.getFeedUser());
                        mCommentDao.insertFromComment(item.getLatestComment());
                    }
                    mFeedDao.insert(paging);
                });
            }

            @Override
            protected boolean shouldFetch(@Nullable List<FeedUI> data) {
                return data == null || data.isEmpty() || mRateLimiter.shouldFetch(pagingId);
            }

            @NonNull
            @Override
            protected LiveData<List<FeedUI>> loadFromDb() {
                return Transformations.switchMap(mFeedDao.loadFeedPaging(pagingId), input -> {
                    if (input == null) {
                        MutableLiveData<List<FeedUI>> data = new MutableLiveData<>();
                        data.postValue(null);
                        return data;
                    } else {
                        Timber.i("loadFeedsFromDb paging: %s", input.toString());
                        return mFeedDao.loadFeedsIds(input.getIds());
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
                boolean shouldDelete = body.isEmpty() || mRateLimiter.shouldFetch(pagingId);
                Timber.i("loadUserProfile: should delete = %s", shouldDelete);
                return shouldDelete;
            }

            @Override
            protected void deleteDataFromDb(List<Feed> body) {
                Timber.i("deleting old query for userId: %s", pagingId);
                mFeedDao.deleteFeedPaging(pagingId);
            }
        }.asLiveData();
    }

    public LiveData<Resource<Boolean>> fetchNextPage(String pagingId) {
        FetchNextPageGlobalFeed fetchNextPageTask = new FetchNextPageGlobalFeed(pagingId, mWebService, mPetDb, mAppExecutors);
        mAppExecutors.networkIO().execute(fetchNextPageTask);
        return fetchNextPageTask.getLiveData();
    }

    public LiveData<Resource<Boolean>> fetchNextUserFeedPage(String pagingId) {
        FetchNextPageUserFeed fetchNextPageUserFeed = new FetchNextPageUserFeed(pagingId, mWebService, mPetDb, mAppExecutors);
        mAppExecutors.networkIO().execute(fetchNextPageUserFeed);
        return fetchNextPageUserFeed.getLiveData();
    }

    public void createNewFeed(Feed feed) {
        mAppExecutors.diskIO().execute(() -> {
            mPetDb.runInTransaction(() -> {
                UserEntity userEntity = mUserDao.findUserById(SharedPrefUtil.getUserId());
                FeedUser feedUser = new FeedUser(userEntity.getUserId(), userEntity.getAvatar(), userEntity.getName());
                feed.setStatus(STATUS_UPLOADING);
                feed.setFeedUser(feedUser);
                feed.setTimeCreated(new Date());
                feed.setTimeUpdated(new Date());
                feed.setFeedId(IdUtil.generateID("feed"));
                mPetDb.runInTransaction(new Runnable() {
                    @Override
                    public void run() {
                        mFeedDao.insertFromFeed(feed);
                        Paging feedsPaging = mFeedDao.findFeedPaging(Paging.GLOBAL_FEEDS_PAGING_ID);
                        Paging profilePaging = mFeedDao.findFeedPaging(Paging.userFeedsPagingId(feed.getFeedUser().getUserId()));
                        if (feedsPaging != null) {
                            feedsPaging.getIds().add(0, feed.getFeedId());
                            mFeedDao.update(feedsPaging);
                        }
                        if (profilePaging != null) {
                            profilePaging.getIds().add(0, feed.getFeedId());
                            mFeedDao.update(profilePaging);
                        }
                    }
                });
            });
            scheduleSaveFeedWorker(feed, false);
        });
    }

    public void updateFeed(Feed feed) {
        mAppExecutors.diskIO().execute(() -> {
            mPetDb.runInTransaction(() -> {
                FeedEntity feedEntity = mFeedDao.findFeedEntityById(feed.getFeedId());
                feedEntity.setStatus(STATUS_UPLOADING);
                feedEntity.setTimeUpdated(new Date());
                feedEntity.setContent(feed.getContent());
                feedEntity.setPhotos(feed.getPhotos());
                mFeedDao.update(feedEntity);
            });
            scheduleSaveFeedWorker(feed, true);
        });
    }

    private void scheduleSaveFeedWorker(Feed feed, boolean isUpdating) {
        ArrayList<WorkContinuation> workContinuations = new ArrayList<>(feed.getPhotos().size());
        // Constraints that defines when the task should run
        Constraints uploadConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        for (Photo photo : feed.getPhotos()) {
            OneTimeWorkRequest compressionWork =
                    new OneTimeWorkRequest.Builder(CompressPhotoWorker.class)
                            .setInputData(CompressPhotoWorker.data(photo))
                            .build();
            OneTimeWorkRequest uploadWork =
                    new OneTimeWorkRequest.Builder(UploadPhotoWorker.class)
                            .setConstraints(uploadConstraints)
                            .build();
            WorkContinuation continuation = WorkManager.getInstance()
                    .beginWith(compressionWork)
                    .then(uploadWork);
            workContinuations.add(continuation);
        }
        if (!workContinuations.isEmpty()) {
            OneTimeWorkRequest createFeedWork =
                    new OneTimeWorkRequest.Builder(CreateEditFeedWorker.class)
                            .setConstraints(uploadConstraints)
                            .setInputData(CreateEditFeedWorker.data(feed, isUpdating))
                            .build();

            if (workContinuations.size() > 1) {
                WorkContinuation
                        .combine(workContinuations)
                        .then(createFeedWork)
                        .enqueue();
            } else {
                workContinuations.get(0)
                        .then(createFeedWork)
                        .enqueue();
            }
        }
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
                        listId, listId.isEmpty(),
                        listId.isEmpty() ? null : listId.get(listId.size() - 1));
                mPetDb.runInTransaction(() -> {
                    mFeedDao.insertFromFeedList(items);
                    for (Feed feedItem : items) {
                        mUserDao.insert(feedItem.getFeedUser());
                        mCommentDao.insertFromComment(feedItem.getLatestComment());
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
                MutableLiveData<List<Feed>> data = new MutableLiveData<>();
                data.postValue(null);
                return data;
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
