package com.petnbu.petnbu.feed.comment;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;
import android.databinding.ObservableBoolean;
import android.support.annotation.Nullable;

import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.SharedPrefUtil;
import com.petnbu.petnbu.SingleLiveEvent;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.CommentUI;
import com.petnbu.petnbu.model.Paging;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.Status;
import com.petnbu.petnbu.model.UserEntity;
import com.petnbu.petnbu.repo.CommentRepository;
import com.petnbu.petnbu.repo.FeedRepository;
import com.petnbu.petnbu.repo.LoadMoreState;
import com.petnbu.petnbu.repo.UserRepository;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class CommentsViewModel extends ViewModel {

    @Inject
    FeedRepository mFeedRepository;

    @Inject
    CommentRepository mCommentRepository;

    @Inject
    UserRepository mUserRepository;

    @Inject
    WebService mWebService;

    @Inject
    Application mApplication;

    private LoadMoreHandler loadMoreHandler;

    private final MutableLiveData<List<CommentUI>> mCommentsLiveData = new MutableLiveData<>();

    private final MutableLiveData<List<CommentUI>> mSubCommentsLiveData = new MutableLiveData<>();

    public final ObservableBoolean showLoading = new ObservableBoolean(false);

    private final SingleLiveEvent<String> mOpenRepliesEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> mOpenUserProfileEvent = new SingleLiveEvent<>();

    public CommentsViewModel() {
        PetApplication.getAppComponent().inject(this);
        loadMoreHandler = new LoadMoreHandler(mCommentRepository);
    }

    public LiveData<UserEntity> loadUserInfo() {
        return Transformations.switchMap(mUserRepository.getUserById(SharedPrefUtil.getUserId()), userResource -> {
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

    public LiveData<List<CommentUI>> loadComments(String feedId) {
        showLoading.set(true);
        return Transformations.switchMap(mCommentRepository.getFeedCommentsIncludeFeedContentHeader(feedId, new Date().getTime(), CommentRepository.COMMENT_PER_PAGE), commentsResource -> {
            showLoading.set(false);
            if (commentsResource != null && commentsResource.data != null) {
                mCommentsLiveData.setValue(commentsResource.data);
            }
            return mCommentsLiveData;
        });
    }


    public LiveData<Resource<List<CommentUI>>> loadSubComments(String commentId) {
        return mCommentRepository.getSubComments(commentId, new Date().getTime(), CommentRepository.COMMENT_PER_PAGE);
    }

    public LiveData<LoadMoreState> getLoadMoreState() {
        return loadMoreHandler.loadMoreState;
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

    public void loadNextPage(String feedId) {
        Timber.i("loadNextPage :");
        if (mCommentsLiveData.getValue() != null) {
            loadMoreHandler.loadNextPage(feedId, Paging.feedCommentsPagingId(feedId));
        }
    }

    public void openUserProfile(String userId) {
        mOpenUserProfileEvent.setValue(userId);
    }

    public void openRepliesForComment(String commentId) {
        mOpenRepliesEvent.setValue(commentId);
    }

    public LiveData<String> getOpenRepliesEvent() {
        return mOpenRepliesEvent;
    }

    public LiveData<String> getOpenUserProfileEvent() {
        return mOpenUserProfileEvent;
    }

    private static class LoadMoreHandler implements Observer<Resource<Boolean>> {

        private final CommentRepository commentRepo;

        private MutableLiveData<LoadMoreState> loadMoreState = new MutableLiveData<>();

        private LiveData<Resource<Boolean>> nextPageLiveData;

        private boolean hasMore = true;

        public LoadMoreHandler(CommentRepository feedRepository) {
            commentRepo = feedRepository;
            loadMoreState.setValue(new LoadMoreState(false, null));
        }

        public void loadNextPage(String feedId, String pagingId) {
            if (!hasMore || loadMoreState.getValue() == null || loadMoreState.getValue().isRunning()) {
                Timber.i("hasMore = %s", hasMore);
                return;
            }
            Timber.i("loadNextPage");
            unregister();
            nextPageLiveData = commentRepo.fetchNextPage(feedId, pagingId);
            loadMoreState.setValue(new LoadMoreState(true, null));
            nextPageLiveData.observeForever(this);

        }

        @Override
        public void onChanged(@Nullable Resource<Boolean> result) {
            if (result == null) {
                reset();
            } else {
                Timber.i(result.toString());
                switch (result.status) {
                    case SUCCESS:
                        hasMore = Boolean.TRUE.equals(result.data);
                        unregister();
                        loadMoreState.setValue(new LoadMoreState(false, null));
                        break;
                    case ERROR:
                        hasMore = true;
                        unregister();
                        loadMoreState.setValue(new LoadMoreState(false, result.message));
                        break;
                }
            }
        }

        private void unregister() {
            if (nextPageLiveData != null) {
                nextPageLiveData.removeObserver(this);
                nextPageLiveData = null;
            }
        }

        private void reset() {
            unregister();
            hasMore = true;
            loadMoreState.setValue(new LoadMoreState(false, null));
        }
    }
}
