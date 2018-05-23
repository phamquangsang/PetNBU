package com.petnbu.petnbu.repo;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.Transformations;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.common.api.Api;
import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.SharedPrefUtil;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.jobs.CompressPhotoWorker;
import com.petnbu.petnbu.jobs.CreateEditFeedWorker;
import com.petnbu.petnbu.jobs.PhotoWorker;
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
import com.petnbu.petnbu.util.Toaster;
import com.petnbu.petnbu.util.TraceUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import timber.log.Timber;

import static com.petnbu.petnbu.model.LocalStatus.STATUS_UPLOADING;

@Singleton
public class FeedRepository {

    public static final int FEEDS_PER_PAGE = 10;

    private final PetDb mPetDb;

    private final AppExecutors mAppExecutors;

    private final WebService mWebService;

    private final Application mApplication;

    private final RateLimiter<String> mRateLimiter = new RateLimiter<>(10, TimeUnit.MINUTES);

    private Toaster mToaster;

    @Inject
    public FeedRepository(PetDb petDb, AppExecutors appExecutors, WebService webService, Application application, Toaster toaster) {
        mPetDb = petDb;
        mAppExecutors = appExecutors;
        mWebService = webService;
        mApplication = application;
        mToaster = toaster;
    }

    public LiveData<Resource<List<FeedUI>>> loadFeeds(String pagingId, String userId) {
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
                    mPetDb.feedDao().insertFromFeedList(items);
                    for (Feed item : items) {
                        mPetDb.userDao().insert(item.getFeedUser());
                        if(item.getLatestComment()!= null){
                            mPetDb.commentDao().insertFromComment(item.getLatestComment());
                            mPetDb.userDao().insert(item.getLatestComment().getFeedUser());
                        }
                    }
                    mPetDb.pagingDao().insert(paging);
                });
            }

            @Override
            protected boolean shouldFetch(@Nullable List<FeedUI> data) {
                return data == null || data.isEmpty() || mRateLimiter.shouldFetch(pagingId);
            }

            @NonNull
            @Override
            protected LiveData<List<FeedUI>> loadFromDb() {
                return Transformations.switchMap(mPetDb.pagingDao().loadFeedPaging(pagingId), input -> {
                    if (input == null) {
                        MutableLiveData<List<FeedUI>> data = new MutableLiveData<>();
                        data.postValue(null);
                        return data;
                    } else {
                        Timber.i("loadFeedsFromDb paging: %s", input.toString());
                        return mPetDb.feedDao().loadFeedsIds(input.getIds(), userId);
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
                mPetDb.pagingDao().deleteFeedPaging(pagingId);
            }
        }.asLiveData();
    }

    public LiveData<Resource<Feed>> getFeed(String feedId) {
        return new NetworkBoundResource<Feed, Feed>(mAppExecutors) {
            @Override
            protected void saveCallResult(@NonNull Feed item) {
                mPetDb.feedDao().insertFromFeed(item);
                mPetDb.userDao().insert(item.getFeedUser());
                if(item.getLatestComment()!= null){
                    mPetDb.commentDao().insertFromComment(item.getLatestComment());
                    mPetDb.userDao().insert(item.getLatestComment().getFeedUser());
                }
            }

            @Override
            protected boolean shouldFetch(@Nullable Feed data) {
                return data == null;
            }

            @Override
            protected void deleteDataFromDb(Feed body) {
                mPetDb.feedDao().deleteFeedById(feedId);
            }

            @NonNull
            @Override
            protected LiveData<Feed> loadFromDb() {
                return mPetDb.feedDao().loadFeedById(feedId);
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
                    mPetDb.feedDao().insertFromFeedList(items);
                    for (Feed item : items) {
                        mPetDb.userDao().insert(item.getFeedUser());
                        if(item.getLatestComment()!= null){
                            mPetDb.commentDao().insertFromComment(item.getLatestComment());
                            mPetDb.userDao().insert(item.getLatestComment().getFeedUser());
                        }
                    }
                    mPetDb.pagingDao().insert(paging);
                });
            }

            @Override
            protected boolean shouldFetch(@Nullable List<FeedUI> data) {
                return data == null || data.isEmpty() || mRateLimiter.shouldFetch(pagingId);
            }

            @NonNull
            @Override
            protected LiveData<List<FeedUI>> loadFromDb() {
                return Transformations.switchMap(mPetDb.pagingDao().loadFeedPaging(pagingId), input -> {
                    if (input == null) {
                        MutableLiveData<List<FeedUI>> data = new MutableLiveData<>();
                        data.postValue(null);
                        return data;
                    } else {
                        Timber.i("loadFeedsFromDb paging: %s", input.toString());
                        return mPetDb.feedDao().loadFeedsIds(input.getIds(), userId);
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
                mPetDb.pagingDao().deleteFeedPaging(pagingId);
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
                UserEntity userEntity = mPetDb.userDao().findUserById(SharedPrefUtil.getUserId());
                FeedUser feedUser = new FeedUser(userEntity.getUserId(), userEntity.getAvatar(), userEntity.getName());
                feed.setStatus(STATUS_UPLOADING);
                feed.setFeedUser(feedUser);
                feed.setTimeCreated(new Date());
                feed.setTimeUpdated(new Date());
                feed.setFeedId(IdUtil.generateID("feed"));
                mPetDb.feedDao().insertFromFeed(feed);
            });
            TraceUtils.begin("scheduleSaveFeedWorker", () -> scheduleSaveFeedWorker(feed, false));
        });
    }

    public void updateFeed(Feed feed) {
        mAppExecutors.diskIO().execute(() -> {
            mPetDb.runInTransaction(() -> {
                FeedEntity feedEntity = mPetDb.feedDao().findFeedEntityById(feed.getFeedId());
                feedEntity.setStatus(STATUS_UPLOADING);
                feedEntity.setTimeUpdated(new Date());
                feedEntity.setContent(feed.getContent());
                feedEntity.setPhotos(feed.getPhotos());
                mPetDb.feedDao().update(feedEntity);
            });
            TraceUtils.begin("scheduleSaveFeedWorker", () -> scheduleSaveFeedWorker(feed, true));
        });
    }

    private void scheduleSaveFeedWorker(Feed feed, boolean isUpdating) {
        ArrayList<OneTimeWorkRequest> uploadWorks = new ArrayList<>(feed.getPhotos().size());
        // Constraints that defines when the task should run
        Constraints uploadConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest compressionWork =
                new OneTimeWorkRequest.Builder(CompressPhotoWorker.class)
                        .setInputData(CompressPhotoWorker.data(feed.getPhotos()))
                        .build();

        for (Photo photo : feed.getPhotos()) {
            String key = Uri.parse(photo.getOriginUrl()).getLastPathSegment();
            OneTimeWorkRequest uploadWork =
                    new OneTimeWorkRequest.Builder(UploadPhotoWorker.class)
                            .setConstraints(uploadConstraints)
                            .setInputData(new Data.Builder().putString(PhotoWorker.KEY_PHOTO, key).build())
                            .build();
            uploadWorks.add(uploadWork);
        }

        if (!uploadWorks.isEmpty()) {
            OneTimeWorkRequest createFeedWork =
                    new OneTimeWorkRequest.Builder(CreateEditFeedWorker.class)
                            .setConstraints(uploadConstraints)
                            .setInputData(CreateEditFeedWorker.data(feed, isUpdating))
                            .build();

            WorkManager.getInstance()
                    .beginWith(compressionWork)
                    .then(uploadWorks)
                    .then(createFeedWork)
                    .enqueue();
        }
    }

    public LiveData<Resource<List<Feed>>> refresh() {
        return new NetworkBoundResource<List<Feed>, List<Feed>>(mAppExecutors) {
            @Override
            protected void saveCallResult(@NonNull List<Feed> items) {
                Paging previous = mPetDb.pagingDao().findFeedPaging(Paging.GLOBAL_FEEDS_PAGING_ID);
                List<String> listId = new ArrayList<>(items.size());
                for (Feed item : items) {
                    listId.add(item.getFeedId());
                }
                Paging paging = new Paging(Paging.GLOBAL_FEEDS_PAGING_ID,
                        listId, listId.isEmpty(),
                        listId.isEmpty() ? null : listId.get(listId.size() - 1));
                mPetDb.runInTransaction(() -> {
                    mPetDb.feedDao().insertFromFeedList(items);
                    for (Feed feedItem : items) {
                        mPetDb.userDao().insert(feedItem.getFeedUser());
                        if(feedItem.getLatestComment()!= null){
                            mPetDb.commentDao().insertFromComment(feedItem.getLatestComment());
                            mPetDb.userDao().insert(feedItem.getLatestComment().getFeedUser());
                        }
                    }
                    mPetDb.pagingDao().insert(paging);
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
                mPetDb.pagingDao().deleteFeedPaging(Paging.GLOBAL_FEEDS_PAGING_ID);
            }
        }.asLiveData();
    }

    public void likeFeed(String userId, String feedId) {
        mAppExecutors.diskIO().execute(() -> {
            FeedEntity feed = mPetDb.feedDao().findFeedEntityById(feedId);
            if(feed.isLikeInProgress()){
                return;
            }
            feed.setLikeInProgress(true);
            mPetDb.feedDao().update(feed);
            mAppExecutors.networkIO().execute(() -> {
                if (feed.isLiked()) {
                    //todo unlike post
                    LiveData<ApiResponse<Feed>> result = mWebService.unLikeFeed(userId, feedId);
                    result.observeForever(new Observer<ApiResponse<Feed>>() {
                        @Override
                        public void onChanged(@Nullable ApiResponse<Feed> feedApiResponse) {
                            if (feedApiResponse != null) {
                                result.removeObserver(this);
                                if (feedApiResponse.isSucceed && feedApiResponse.body != null) {
                                    feed.setLikeCount(feedApiResponse.body.getLikeCount());
                                    feed.setLiked(false);
                                }else{
                                    mToaster.makeText(feedApiResponse.errorMessage);
                                }
                                feed.setLikeInProgress(false);
                                mAppExecutors.diskIO().execute(() -> mPetDb.feedDao().update(feed));
                            }
                        }
                    });
                } else {
                    //todo like post
                    LiveData<ApiResponse<Feed>> result = mWebService.likeFeed(userId, feedId);
                    result.observeForever(new Observer<ApiResponse<Feed>>() {
                        @Override
                        public void onChanged(@Nullable ApiResponse<Feed> feedApiResponse) {
                            if (feedApiResponse != null) {
                                result.removeObserver(this);
                                if (feedApiResponse.isSucceed && feedApiResponse.body != null) {
                                    feed.setLikeCount(feedApiResponse.body.getLikeCount());
                                    feed.setLiked(true);
                                }else{
                                    mToaster.makeText(feedApiResponse.errorMessage);
                                }
                                feed.setLikeInProgress(false);
                                mAppExecutors.diskIO().execute(() -> mPetDb.feedDao().update(feed));
                            }
                        }
                    });
                }
            });
        });
    }
}
