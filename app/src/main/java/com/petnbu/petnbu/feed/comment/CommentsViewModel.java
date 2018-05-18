package com.petnbu.petnbu.feed.comment;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;
import android.databinding.ObservableBoolean;

import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.SharedPrefUtil;
import com.petnbu.petnbu.SingleLiveEvent;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.CommentUI;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.Status;
import com.petnbu.petnbu.model.UserEntity;
import com.petnbu.petnbu.repo.CommentRepository;
import com.petnbu.petnbu.repo.FeedRepository;
import com.petnbu.petnbu.repo.UserRepository;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

public class CommentsViewModel extends ViewModel {

    private static final int COMMENT_PAGING_LIMIT = 10;
    @Inject
    FeedRepository mFeedRepository;

    @Inject
    CommentRepository mCommentRepository;

    @Inject
    UserRepository mUserRepository;

    @Inject
    CommentRepository mCommentRepo;

    @Inject
    WebService mWebService;

    @Inject
    Application mApplication;

    private final MutableLiveData<Resource<List<Comment>>> mCommentsLiveData = new MutableLiveData<>();
    public final ObservableBoolean showLoading = new ObservableBoolean(false);
    public final SingleLiveEvent<String> openRepliesEvent = new SingleLiveEvent<>();


    public CommentsViewModel() {
        PetApplication.getAppComponent().inject(this);
    }

    public LiveData<UserEntity> loadUserInfo() {
        return Transformations.switchMap(mUserRepository.getUserById(SharedPrefUtil.getUserId(mApplication)), userResource -> {
            MutableLiveData<UserEntity> userLiveData = new MutableLiveData<>();
            if(userResource.data != null){
                userLiveData.setValue(userResource.data);
            } else {
                if(userResource.status.equals(Status.ERROR)) {
                    userLiveData.setValue(null);
                }
            }
            return userLiveData;
        });
    }

    public LiveData<Resource<List<CommentUI>>> loadComments(String feedId) {
        return mCommentRepo.getFeedCommentsIncludeFeedContentHeader(feedId, new Date().getTime(), COMMENT_PAGING_LIMIT);
    }


    public LiveData<Resource<List<Comment>>> loadSubComments(String commentId) {
        showLoading.set(true);
        return Transformations.switchMap(mWebService.getCommentsByComment(commentId), input -> {
            showLoading.set(false);
            mCommentsLiveData.setValue(Resource.success(input.body));
            return mCommentsLiveData;
        });
    }

    public void sendComment(String feedId, String content, Photo photo) {
        Comment comment = new Comment();
        comment.setParentFeedId(feedId);
        comment.setContent(content);
        comment.setPhoto(photo);
        mCommentRepository.createNewFeedComment(comment);
    }

    public void sendCommentByComment(String commendId, String content, Photo photo) {

    }
}
