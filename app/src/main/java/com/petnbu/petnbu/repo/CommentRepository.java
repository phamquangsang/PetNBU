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
import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.CommentUI;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.LocalStatus;
import com.petnbu.petnbu.model.Paging;
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
public class CommentRepository {

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

    public void createNewFeedComment(Comment comment, String feedId) {
        mAppExecutors.diskIO().execute(() -> {
            mPetDb.runInTransaction(() -> {
                UserEntity userEntity = mUserDao.findUserById(SharedPrefUtil.getUserId(mApplication));
                FeedUser feedUser = new FeedUser(userEntity.getUserId(), userEntity.getAvatar(), userEntity.getName());
                comment.setFeedUser(feedUser);
                comment.setParentFeedId(feedId);
                comment.setLocalStatus(LocalStatus.STATUS_UPLOADING);
                comment.setTimeCreated(new Date());
                comment.setTimeUpdated(new Date());
                comment.setId(IdUtil.generateID("comment"));
                mCommentDao.insertFromComment(comment);
            });
            //todo
//            scheduleSaveFeedJob(feedResponse, false);
        });
    }

    public LiveData<Resource<List<CommentUI>>> getFeedComments(String feedId, long after, int limit){

        return new NetworkBoundResource<List<CommentUI>, List<Comment>>(mAppExecutors){
            @Override
            protected void saveCallResult(@NonNull List<Comment> items) {
                List<String> listId = new ArrayList<>(items.size());
                String pagingId = Paging.feedCommentsPagingId(feedId);
                Paging paging;
                if(items.isEmpty()){
                    paging = new Paging(pagingId, listId, true, null);
                }else{
                    for (Comment item : items) {
                        listId.add(item.getId());
                    }
                    paging = new Paging(pagingId,
                            listId, false,
                            listId.get(listId.size() - 1));
                }
                mPetDb.runInTransaction(() -> {
                    mCommentDao.insertListComment(items);
                    for (Comment item : items) {
                        mUserDao.insert(item.getFeedUser());
                        mCommentDao.insertFromComment(item.getLatestComment());
                    }
                    mFeedDao.insert(paging);
                });
            }

            @Override
            protected boolean shouldFetch(@Nullable List<CommentUI> data) {
                return true;
            }

            @Override
            protected void deleteDataFromDb(List<Comment> body) {
                mFeedDao.deleteFeedPaging(Paging.feedCommentsPagingId(feedId));
            }

            @Override
            protected boolean shouldDeleteOldData(List<Comment> body) {
                return true;
            }

            @NonNull
            @Override
            protected LiveData<List<CommentUI>> loadFromDb() {
                return Transformations.switchMap(mFeedDao.loadFeedPaging(Paging.feedCommentsPagingId(feedId)), input -> {
                    if (input == null) {
                        MutableLiveData<List<CommentUI>> data = new MutableLiveData<>();
                        data.postValue(null);
                        return data;
                    } else {
                        Timber.i("loadFeedsFromDb paging: %s", input.toString());
                        return mCommentDao.loadCommentsIncludeUploadingPost(input.getIds());
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
}
