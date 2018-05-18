package com.petnbu.petnbu.jobs;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.api.ApiResponse;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.db.CommentDao;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.db.UserDao;
import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.CommentEntity;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.UserEntity;

import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.Worker;
import timber.log.Timber;

import static com.petnbu.petnbu.model.LocalStatus.STATUS_DONE;
import static com.petnbu.petnbu.model.LocalStatus.STATUS_ERROR;
import static com.petnbu.petnbu.model.LocalStatus.STATUS_UPLOADING;

public class CreateCommentWorker extends Worker {

    private static final String KEY_COMMENT_ID = "key-comment-id";

    public static Data data(Comment comment) {
        Data data = new Data.Builder()
                .putString(KEY_COMMENT_ID, comment.getId())
                .build();
        return data;
    }

    @Inject
    WebService mWebService;

    @Inject
    CommentDao mCommentDao;

    @Inject
    UserDao mUserDao;

    @Inject
    PetDb mPetDb;

    @Inject
    AppExecutors mAppExecutors;

    @NonNull
    @Override
    public WorkerResult doWork() {
        PetApplication.getAppComponent().inject(this);

        WorkerResult workerResult = WorkerResult.FAILURE;
        Data data = getInputData();
        String commentId = data.getString(KEY_COMMENT_ID, "");

        if(!TextUtils.isEmpty(commentId)) {
            CommentEntity commentEntity = mCommentDao.getCommentById(commentId);
            if(commentEntity != null) {
                UserEntity userEntity = mUserDao.findUserById(commentEntity.getOwnerId());
                FeedUser feedUser = new FeedUser(userEntity.getUserId(), userEntity.getAvatar(), userEntity.getName());
                Comment comment = new Comment(commentEntity.getId(), feedUser, commentEntity.getContent(), commentEntity.getPhoto(),
                        commentEntity.getLikeCount(), commentEntity.getCommentCount(), null, commentEntity.getParentCommentId(),
                        commentEntity.getParentFeedId(), commentEntity.getLocalStatus(), commentEntity.getTimeCreated(),
                        commentEntity.getTimeUpdated());
                if (comment.getLocalStatus() == STATUS_UPLOADING) {
                    try {
                        createComment(comment);
                        workerResult = WorkerResult.SUCCESS;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return workerResult;
    }

    private void createComment(Comment comment) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LiveData<ApiResponse<Comment>> apiResponse = mWebService.createFeedComment(comment, comment.getParentFeedId());
        apiResponse.observeForever(new Observer<ApiResponse<Comment>>() {
            @Override
            public void onChanged(@Nullable ApiResponse<Comment> commentApiResponse) {
                apiResponse.removeObserver(this);

                if(commentApiResponse != null && commentApiResponse.isSucceed && commentApiResponse.body != null) {
                    Timber.d("create comment %s success", comment.getId());
                    Comment newComment = commentApiResponse.body;
                    newComment.setLocalStatus(STATUS_DONE);
                    mAppExecutors.diskIO().execute(() -> mCommentDao.insertFromComment(newComment));

                } else {
                    Timber.d("create comment %s error : %s", comment.getId(), commentApiResponse.errorMessage);
                    comment.setLocalStatus(STATUS_ERROR);
                    mAppExecutors.diskIO().execute(() -> mCommentDao.insertFromComment(comment));
                }
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
    }
}
