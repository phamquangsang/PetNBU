package com.petnbu.petnbu.repo;

import android.app.Application;

import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.SharedPrefUtil;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.FeedDao;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.db.UserDao;
import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.FeedUser;
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

    private final AppExecutors mAppExecutors;

    private final WebService mWebService;

    private final Application mApplication;

    private final RateLimiter<String> mRateLimiter = new RateLimiter<>(10, TimeUnit.MINUTES);

    @Inject
    public CommentRepository(PetDb petDb, FeedDao feedDao, UserDao userDao, AppExecutors appExecutors, WebService webService, Application application) {
        mPetDb = petDb;
        mFeedDao = feedDao;
        mUserDao = userDao;
        mAppExecutors = appExecutors;
        mWebService = webService;
        mApplication = application;
    }

//    public void createNewFeedComment(Comment comment, String feedId) {
//        mAppExecutors.diskIO().execute(() -> {
//            mPetDb.runInTransaction(() -> {
//                UserEntity userEntity = mUserDao.findUserById(SharedPrefUtil.getUserId(mApplication));
//                FeedUser feedUser = new FeedUser(userEntity.getUserId(), userEntity.getAvatar().getOriginUrl(), userEntity.getName());
//                comment.set
//                comment.setFeedUser(feedUser);
//                comment.setTimeCreated(new Date());
//                comment.setTimeUpdated(new Date());
//                comment.setId(IdUtil.generateID("feed"));
//                mCommentDao.insertFromFeed(feedResponse);
//            });
//            scheduleSaveFeedJob(feedResponse, false);
//        });
//    }
}
