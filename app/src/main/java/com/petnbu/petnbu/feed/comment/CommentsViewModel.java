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
import com.petnbu.petnbu.api.ApiResponse;
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

    @Inject
    FeedRepository mFeedRepository;

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
        showLoading.set(true);
        LiveData<Resource<Feed>> dbSource = mFeedRepository.getFeed(feedId);
        MediatorLiveData<Resource<List<CommentUI>>> mediatorLiveData = new MediatorLiveData<>();
        mediatorLiveData.addSource(dbSource, feedResource -> {
            if(feedResource != null){
                if(feedResource.status == Status.SUCCESS && feedResource.data != null){
                    mediatorLiveData.removeSource(dbSource);
                    CommentUI feedComment = createCommentUIFromFeed(feedResource.data);
                    LiveData<Resource<List<CommentUI>>> commentsLiveData = mCommentRepo.getFeedComments(feedId, new Date().getTime(), 10);
                    mediatorLiveData.addSource(commentsLiveData, resourceComments -> {
                        if(resourceComments != null) {
                            if(resourceComments.status != Status.LOADING){
                                showLoading.set(false);
                            }
                            if(resourceComments.status == Status.SUCCESS && resourceComments.data != null)
                                resourceComments.data.add(0, feedComment);
                            mediatorLiveData.setValue(Resource.success(resourceComments.data));
                        }
                    });
                }else if(feedResource.status == Status.ERROR){
                    showLoading.set(false);
                }
            }
        });
        return mediatorLiveData;
    }

    private CommentUI createCommentUIFromFeed(Feed feed) {
        CommentUI comment = new CommentUI();
        comment.setId(feed.getFeedId());
        comment.setOwner(feed.getFeedUser());
        comment.setContent(feed.getContent());
        comment.setTimeCreated(feed.getTimeCreated());
        return comment;
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

    }

    public void sendCommentByComment(String commendId, String content, Photo photo) {

    }
}
