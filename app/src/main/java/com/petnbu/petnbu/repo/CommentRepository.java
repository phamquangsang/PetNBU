package com.petnbu.petnbu.repo;

import android.app.Application;

import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.SharedPrefUtil;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.CommentDao;
import com.petnbu.petnbu.db.FeedDao;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.db.UserDao;
import com.petnbu.petnbu.jobs.CompressPhotoWorker;
import com.petnbu.petnbu.jobs.CreateCommentWorker;
import com.petnbu.petnbu.jobs.UploadPhotoWorker;
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

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkManager;

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

    public void createNewFeedComment(Comment comment) {
        mAppExecutors.diskIO().execute(() -> {
            mPetDb.runInTransaction(() -> {
                UserEntity userEntity = mUserDao.findUserById(SharedPrefUtil.getUserId(mApplication));
                FeedUser feedUser = new FeedUser(userEntity.getUserId(), userEntity.getAvatar(), userEntity.getName());
                comment.setFeedUser(feedUser);
                comment.setLocalStatus(LocalStatus.STATUS_UPLOADING);
                comment.setTimeCreated(new Date());
                comment.setTimeUpdated(new Date());
                comment.setId(IdUtil.generateID("comment"));
                mCommentDao.insertFromComment(comment);
            });
            scheduleSaveCommentWorker(comment);
        });
    }

    private void scheduleSaveCommentWorker(Comment comment) {
        Constraints uploadConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.METERED)
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
