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
import com.petnbu.petnbu.api.FakeWebService;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedUIModel;
import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.Status;
import com.petnbu.petnbu.model.User;
import com.petnbu.petnbu.repo.FeedRepository;
import com.petnbu.petnbu.repo.UserRepository;

import java.util.List;

import javax.inject.Inject;

public class CommentsViewModel extends ViewModel {

    @Inject
    FeedRepository mFeedRepository;

    @Inject
    UserRepository mUserRepository;

    @Inject
    WebService mWebService;

    @Inject
    Application mApplication;

    private final MutableLiveData<Resource<List<Comment>>> mCommentsLiveData = new MutableLiveData<>();
    public final ObservableBoolean showLoading = new ObservableBoolean(false);
    public final SingleLiveEvent<String> openRepliesEvent = new SingleLiveEvent<>();

    public CommentsViewModel() {
        PetApplication.getAppComponent().inject(this);
        mWebService = new FakeWebService();
    }

    public LiveData<User> loadUserInfo() {
        return Transformations.switchMap(mUserRepository.getUserById(SharedPrefUtil.getUserId(mApplication)), userResource -> {
            MutableLiveData<User> userLiveData = new MutableLiveData<>();
            if(userResource != null && userResource.data != null) {
                userLiveData.setValue(userResource.data);
            } else {
                userLiveData.setValue(null);
            }
            return userLiveData;
        });
    }

    public LiveData<Resource<List<Comment>>> loadComments(String feedId) {
        showLoading.set(true);
        LiveData<Resource<FeedUIModel>> dbSource = mFeedRepository.getFeed(feedId);
        MediatorLiveData<Resource<List<Comment>>> mediatorLiveData = new MediatorLiveData<>();
        mediatorLiveData.addSource(dbSource, feedResource -> {
            if(feedResource!= null && feedResource.status == Status.SUCCESS){
                mediatorLiveData.removeSource(dbSource);
                Comment feedComment = createCommentFromFeed(feedResource.data);
                LiveData<ApiResponse<List<Comment>>> commentsLiveData = mWebService.getComments(feedId);
                mediatorLiveData.addSource(commentsLiveData, listApiResponse -> {
                    showLoading.set(false);
                    if(listApiResponse.isSucceed) {
                        listApiResponse.body.add(0, feedComment);
                        mediatorLiveData.setValue(Resource.success(listApiResponse.body));
                    }
                });
            }
        });
        return mediatorLiveData;
    }

    private Comment createCommentFromFeed(FeedUIModel feed) {
        Comment comment = new Comment();
        comment.setId(feed.getFeedId());
        FeedUser feedUser = new FeedUser(feed.getUserId(), feed.getAvatar().getOriginUrl(), feed.getName());
        comment.setFeedUser(feedUser);
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
