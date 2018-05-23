package com.petnbu.petnbu.repo;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
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
import com.petnbu.petnbu.jobs.CreateCommentWorker;
import com.petnbu.petnbu.jobs.UploadPhotoWorker;
import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.CommentUI;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.LocalStatus;
import com.petnbu.petnbu.model.Paging;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.Status;
import com.petnbu.petnbu.model.UserEntity;
import com.petnbu.petnbu.util.IdUtil;
import com.petnbu.petnbu.util.RateLimiter;
import com.petnbu.petnbu.util.TraceUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkManager;

@Singleton
public class CommentRepository {

    public static final int COMMENT_PER_PAGE = 10;

    private final PetDb mPetDb;

    private final FeedDao mFeedDao;

    private final UserDao mUserDao;

    private final CommentDao mCommentDao;

    private final AppExecutors mAppExecutors;

    private final WebService mWebService;

    private final Application mApplication;

    private final RateLimiter<String> mRateLimiter = new RateLimiter<>(10, TimeUnit.MINUTES);

    @Inject
    public CommentRepository(PetDb petDb, FeedDao feedDao, UserDao userDao, CommentDao commentDao, AppExecutors appExecutors, WebService webService, Application application) {
        mPetDb = petDb;
        mFeedDao = feedDao;
        mUserDao = userDao;
        mCommentDao = commentDao;
        mAppExecutors = appExecutors;
        mWebService = webService;
        mApplication = application;
    }

    public void createNewFeedComment(Comment comment) {
        mAppExecutors.diskIO().execute(() -> {
            mPetDb.runInTransaction(() -> {
                UserEntity userEntity = mUserDao.findUserById(SharedPrefUtil.getUserId());
                FeedUser feedUser = new FeedUser(userEntity.getUserId(), userEntity.getAvatar(), userEntity.getName());
                comment.setFeedUser(feedUser);
                comment.setLocalStatus(LocalStatus.STATUS_UPLOADING);
                comment.setTimeCreated(new Date());
                comment.setTimeUpdated(new Date());
                comment.setId(IdUtil.generateID("comment"));
                mCommentDao.insertFromComment(comment);
            });
            TraceUtils.begin("scheduleSaveComment", () -> scheduleSaveCommentWorker(comment));
        });
    }

    private CommentUI createCommentUIFromFeed(Feed feed) {
        CommentUI comment = new CommentUI();
        comment.setId(feed.getFeedId());
        comment.setOwner(feed.getFeedUser());
        comment.setContent(feed.getContent());
        comment.setTimeCreated(feed.getTimeCreated());
        return comment;
    }

    public LiveData<Resource<Feed>> loadFeedById(String feedId){
        return new NetworkBoundResource<Feed, Feed>(mAppExecutors){
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

    public LiveData<Resource<List<CommentUI>>> getFeedCommentsIncludeFeedContentHeader(String feedId, long after, int limit) {

        LiveData<Resource<Feed>> feedSource = loadFeedById(feedId);
        MediatorLiveData<Resource<List<CommentUI>>> mediatorLiveData = new MediatorLiveData<>();
        mediatorLiveData.addSource(feedSource, feedResource -> {
            if(feedResource != null){
                if(feedResource.status == Status.SUCCESS && feedResource.data != null){
                    mediatorLiveData.removeSource(feedSource);
                    CommentUI feedComment = createCommentUIFromFeed(feedResource.data);
                    LiveData<Resource<List<CommentUI>>> commentsLiveData = getFeedComments(feedId, after, limit);
                    mediatorLiveData.addSource(commentsLiveData, resourceComments -> {
                        if(resourceComments != null) {
                            if(resourceComments.data != null)
                                resourceComments.data.add(0, feedComment);
                            mediatorLiveData.setValue(Resource.success(resourceComments.data));
                        }
                    });
                }
            }
        });
        return mediatorLiveData;


    }

    public LiveData<Resource<List<CommentUI>>> getFeedComments(String feedId, long after, int limit) {

        return new NetworkBoundResource<List<CommentUI>, List<Comment>>(mAppExecutors) {
            @Override
            protected void saveCallResult(@NonNull List<Comment> items) {

                List<String> listId = new ArrayList<>(items.size());
                String pagingId = Paging.feedCommentsPagingId(feedId);
                for (Comment item : items) {
                    listId.add(item.getId());
                }
                Paging paging = new Paging(pagingId,
                        listId, false,
                        listId.isEmpty() ? null : listId.get(listId.size() - 1));
                mPetDb.runInTransaction(() -> {
                    mCommentDao.insertListComment(items);
                    for (Comment item : items) {
                        mUserDao.insert(item.getFeedUser());
                        mCommentDao.insertFromComment(item.getLatestComment());
                    }
                    mPetDb.pagingDao().insert(paging);
                });
            }

            @Override
            protected boolean shouldFetch(@Nullable List<CommentUI> data) {
                return data == null || data.isEmpty() || mRateLimiter.shouldFetch(Paging.feedCommentsPagingId(feedId));
            }

            @Override
            protected void deleteDataFromDb(List<Comment> body) {
                mPetDb.pagingDao().deleteFeedPaging(Paging.feedCommentsPagingId(feedId));
            }

            @Override
            protected boolean shouldDeleteOldData(List<Comment> body) {
                return false;
            }

            @NonNull
            @Override
            protected LiveData<List<CommentUI>> loadFromDb() {
                return Transformations.switchMap(mPetDb.pagingDao().loadFeedPaging(Paging.feedCommentsPagingId(feedId)), input -> {
                    if (input == null) {
                        MutableLiveData<List<CommentUI>> data = new MutableLiveData<>();
                        data.postValue(null);
                        return data;
                    } else {
                        Timber.i("loadFeedsFromDb paging: %s", input.toString());
                        return mCommentDao.loadFeedComments(input.getIds(), feedId);
                    }
                });
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<List<Comment>>> createCall() {
                return mWebService.getFeedComments(feedId, after, limit);
            }
        }.asLiveData();

    }

    public LiveData<Resource<List<CommentUI>>> getSubComments(String parentCommentId, long after, int limit) {

        return new NetworkBoundResource<List<CommentUI>, List<Comment>>(mAppExecutors) {
            @Override
            protected void saveCallResult(@NonNull List<Comment> items) {

                List<String> listId = new ArrayList<>(items.size());
                String pagingId = Paging.subCommentsPagingId(parentCommentId);
                for (Comment item : items) {
                    listId.add(item.getId());
                }
                Paging paging = new Paging(pagingId,
                        listId, false,
                        listId.isEmpty() ? null : listId.get(listId.size() - 1));
                mPetDb.runInTransaction(() -> {
                    mCommentDao.insertListComment(items);
                    for (Comment item : items) {
                        mUserDao.insert(item.getFeedUser());
                        mCommentDao.insertFromComment(item.getLatestComment());
                    }
                    mPetDb.pagingDao().insert(paging);
                });
            }

            @Override
            protected boolean shouldFetch(@Nullable List<CommentUI> data) {
                return data == null || data.isEmpty() || mRateLimiter.shouldFetch(Paging.subCommentsPagingId(parentCommentId));
            }

            @Override
            protected void deleteDataFromDb(List<Comment> body) {
                mPetDb.pagingDao().deleteFeedPaging(Paging.subCommentsPagingId(parentCommentId));
            }

            @Override
            protected boolean shouldDeleteOldData(List<Comment> body) {
                return false;
            }

            @NonNull
            @Override
            protected LiveData<List<CommentUI>> loadFromDb() {
                return Transformations.switchMap(mPetDb.pagingDao().loadFeedPaging(Paging.subCommentsPagingId(parentCommentId)), input -> {
                    if (input == null) {
                        MutableLiveData<List<CommentUI>> data = new MutableLiveData<>();
                        data.postValue(null);
                        return data;
                    } else {
                        Timber.i("loadSubCommentsFromDb paging: %s", input.toString());
                        return mCommentDao.loadSubComments(input.getIds(), parentCommentId);
                    }
                });
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<List<Comment>>> createCall() {
                return mWebService.getSubComments(parentCommentId, after, limit);
            }
        }.asLiveData();

    }

    public LiveData<Resource<Boolean>> fetchCommentsNextPage(String feedId, String pagingId) {
        FetchNextPageFeedComment fetchNextPageTask = new FetchNextPageFeedComment(feedId, pagingId, mWebService, mPetDb, mAppExecutors);
        mAppExecutors.networkIO().execute(fetchNextPageTask);
        return fetchNextPageTask.getLiveData();
    }

    public LiveData<Resource<Boolean>> fetchSubCommentsNextPage(String commentid, String pagingId) {
        FetchNextPageSubComment fetchNextPageTask = new FetchNextPageSubComment(commentid, pagingId, mWebService, mPetDb, mAppExecutors);
        mAppExecutors.networkIO().execute(fetchNextPageTask);
        return fetchNextPageTask.getLiveData();
    }

    private void scheduleSaveCommentWorker(Comment comment) {
        Constraints uploadConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        WorkContinuation photoWorkContinuation = null;
        if(comment.getPhoto() != null) {
            OneTimeWorkRequest compressionWork =
                    new OneTimeWorkRequest.Builder(CompressPhotoWorker.class)
                            .setInputData(CompressPhotoWorker.data(comment.getPhoto()))
                            .build();
            OneTimeWorkRequest uploadWork =
                    new OneTimeWorkRequest.Builder(UploadPhotoWorker.class)
                            .setConstraints(uploadConstraints)
                            .build();
            photoWorkContinuation = WorkManager.getInstance()
                    .beginWith(compressionWork)
                    .then(uploadWork);
        }

        OneTimeWorkRequest createCommentWork =
                new OneTimeWorkRequest.Builder(CreateCommentWorker.class)
                        .setInputData(CreateCommentWorker.data(comment))
                        .setConstraints(uploadConstraints)
                        .build();
        if(photoWorkContinuation != null) {
            photoWorkContinuation
                    .then(createCommentWork)
                    .enqueue();
        } else {
            WorkManager.getInstance().enqueue(createCommentWork);
        }
    }
}
