package com.petnbu.petnbu.repo;

import android.app.Application;

import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.SharedPrefUtil;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.CommentDao;
import com.petnbu.petnbu.db.FeedDao;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.db.UserDao;
import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.LocalStatus;
import com.petnbu.petnbu.model.UserEntity;
import com.petnbu.petnbu.util.IdUtil;
import com.petnbu.petnbu.util.RateLimiter;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

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
}
