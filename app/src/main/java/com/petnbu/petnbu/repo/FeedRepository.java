package com.petnbu.petnbu.repo;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Trigger;
import com.google.gson.Gson;
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
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.User;
import com.petnbu.petnbu.util.IdUtil;
import com.petnbu.petnbu.util.RateLimiter;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FeedRepository {

    public static final int FEEDS_PER_PAGE = 10;

    private final PetDb mPetDb;

    private final FeedDao mFeedDao;

    private final UserDao mUserDao;

    private final AppExecutors mAppExecutors;

    private final WebService mWebService;

    private final Application mApplication;

    private final RateLimiter<String> mRateLimiter = new RateLimiter<>(10, TimeUnit.SECONDS);

    @Inject
    public FeedRepository(PetDb petDb, FeedDao feedDao, UserDao userDao, AppExecutors appExecutors, WebService webService, Application application) {
        mPetDb = petDb;
        mFeedDao = feedDao;
        mUserDao = userDao;
        mAppExecutors = appExecutors;
        mWebService = webService;
        mApplication = application;
    }

    public LiveData<Resource<List<Feed>>> loadFeeds() {
        return new NetworkBoundResource<List<Feed>, List<Feed>>(mAppExecutors) {
            @Override
            protected void saveCallResult(@NonNull List<Feed> item) {
                mFeedDao.insert(item);
            }

            @Override
            protected boolean shouldFetch(@Nullable List<Feed> data) {
                return data == null || data.isEmpty() || mRateLimiter.shouldFetch("feeds");
            }

            @NonNull
            @Override
            protected LiveData<List<Feed>> loadFromDb() {
                return mFeedDao.loadFeeds();
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<List<Feed>>> createCall() {
                return mWebService.getFeeds(System.currentTimeMillis(), FEEDS_PER_PAGE);
            }

            @Override
            protected void deleteDataFromDb() {
                mFeedDao.deleteAllExcludeStatus(Feed.STATUS_UPLOADING);
            }
        }.asLiveData();
    }

    public LiveData<Resource<Boolean>> fetchNextPage(Feed lastFeed) {
        FetchNextPageFeed fetchNextPageTask = new FetchNextPageFeed(lastFeed, mWebService, mPetDb, mAppExecutors);
        mAppExecutors.networkIO().execute(fetchNextPageTask);
        return fetchNextPageTask.getLiveData();
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
            mFeedDao.insert(feed);
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

}
