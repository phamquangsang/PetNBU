package com.petnbu.petnbu.jobs;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
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
import com.petnbu.petnbu.model.Paging;
import com.petnbu.petnbu.model.Photo;
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
                        if(comment.getPhoto() != null) {
                            Photo commentPhoto = comment.getPhoto();
                            Photo uploadedPhoto = null;
                            String key = Uri.parse(commentPhoto.getOriginUrl()).getLastPathSegment();
                            String jsonPhoto = data.getString(key, "");
                            if (!TextUtils.isEmpty(jsonPhoto)) {
                                uploadedPhoto = new Gson().fromJson(jsonPhoto, Photo.class);
                                commentPhoto.setWidth(uploadedPhoto.getWidth());
                                commentPhoto.setHeight(uploadedPhoto.getHeight());
                                commentPhoto.setOriginUrl(uploadedPhoto.getOriginUrl());
                                commentPhoto.setLargeUrl(uploadedPhoto.getLargeUrl());
                                commentPhoto.setMediumUrl(uploadedPhoto.getMediumUrl());
                                commentPhoto.setSmallUrl(uploadedPhoto.getSmallUrl());
                                commentPhoto.setThumbnailUrl(uploadedPhoto.getThumbnailUrl());
                            }
                            if(uploadedPhoto != null) {
                                if(!TextUtils.isEmpty(comment.getParentFeedId())) {
                                    createComment(comment);
                                    workerResult = WorkerResult.SUCCESS;
                                } else if(!TextUtils.isEmpty(comment.getParentCommentId())) {
                                    createSubComment(comment);
                                    workerResult = WorkerResult.SUCCESS;
                                }
                                workerResult = WorkerResult.FAILURE;
                            } else {
                                workerResult = WorkerResult.FAILURE;
                            }
                        } else {
                            if(!TextUtils.isEmpty(comment.getParentFeedId())) {
                                createComment(comment);
                                workerResult = WorkerResult.SUCCESS;
                            } else if(!TextUtils.isEmpty(comment.getParentCommentId())) {
                                createSubComment(comment);
                                workerResult = WorkerResult.SUCCESS;
                            }
                            workerResult = WorkerResult.FAILURE;
                        }
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
        final String oldCommentId = comment.getId();

        LiveData<ApiResponse<Comment>> apiResponse = mWebService.createFeedComment(comment, comment.getParentFeedId());
        apiResponse.observeForever(new Observer<ApiResponse<Comment>>() {
            @Override
            public void onChanged(@Nullable ApiResponse<Comment> commentApiResponse) {
                apiResponse.removeObserver(this);

                if(commentApiResponse != null && commentApiResponse.isSucceed && commentApiResponse.body != null) {
                    Timber.d("create comment %s success", comment.getId());
                    Comment newComment = commentApiResponse.body;

                    mAppExecutors.diskIO().execute(() -> mPetDb.runInTransaction(() -> {
                        Paging feedCommentPaging = mPetDb.pagingDao().findFeedPaging(Paging.feedCommentsPagingId(comment.getParentFeedId()));
                        if(feedCommentPaging != null) {
                            feedCommentPaging.getIds().add(0, newComment.getId());
                            mPetDb.pagingDao().update(feedCommentPaging);
                        }
                        mCommentDao.updateCommentId(oldCommentId, newComment.getId());
                        newComment.setLocalStatus(STATUS_DONE);
                        mCommentDao.update(newComment.toEntity());
                    }));
                } else {
                    Timber.d("create comment %s error : %s", comment.getId(), commentApiResponse.errorMessage);
                    comment.setLocalStatus(STATUS_ERROR);
                    mAppExecutors.diskIO().execute(() -> mCommentDao.update(comment.toEntity()));
                }
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
    }

    private void createSubComment(Comment comment) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final String oldCommentId = comment.getId();

        LiveData<ApiResponse<Comment>> apiResponse = mWebService.createReplyComment(comment, comment.getParentCommentId());
        apiResponse.observeForever(new Observer<ApiResponse<Comment>>() {
            @Override
            public void onChanged(@Nullable ApiResponse<Comment> commentApiResponse) {
                apiResponse.removeObserver(this);

                if(commentApiResponse != null && commentApiResponse.isSucceed && commentApiResponse.body != null) {
                    Timber.d("create comment %s success", comment.getId());
                    Comment newComment = commentApiResponse.body;

                    mAppExecutors.diskIO().execute(() -> mPetDb.runInTransaction(() -> {
                        Paging subCommentPaging = mPetDb.pagingDao().findFeedPaging(Paging.subCommentsPagingId(comment.getParentCommentId()));
                        if(subCommentPaging != null) {
                            subCommentPaging.getIds().add(0, newComment.getId());
                            mPetDb.pagingDao().update(subCommentPaging);
                        }
                        mCommentDao.updateCommentId(oldCommentId, newComment.getId());
                        newComment.setLocalStatus(STATUS_DONE);
                        mCommentDao.update(newComment.toEntity());

                        CommentEntity parentComment = mCommentDao.getCommentById(comment.getParentCommentId());
                        parentComment.setLatestCommentId(comment.getId());
                        parentComment.setCommentCount(parentComment.getCommentCount()+1);
                        mCommentDao.update(parentComment);
                    }));
                } else {
                    Timber.d("create comment %s error : %s", comment.getId(), commentApiResponse.errorMessage);
                    comment.setLocalStatus(STATUS_ERROR);
                    mAppExecutors.diskIO().execute(() -> mCommentDao.update(comment.toEntity()));
                }
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
    }
}
