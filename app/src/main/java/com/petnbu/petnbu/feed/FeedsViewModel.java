package com.petnbu.petnbu.feed;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.Nullable;

import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.SingleLiveEvent;
import com.petnbu.petnbu.model.FeedUI;
import com.petnbu.petnbu.model.Paging;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.repo.FeedRepository;
import com.petnbu.petnbu.repo.LoadMoreState;
import com.petnbu.petnbu.repo.UserRepository;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class FeedsViewModel extends ViewModel {

    @Inject
    FeedRepository mFeedRepository;

    @Inject
    UserRepository mUserRepository;

    @Inject
    Application mApplication;

    private final SingleLiveEvent<String> mOpenUserProfileEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> mOpenCommentsEvent = new SingleLiveEvent<>();

    private LiveData<Resource<List<FeedUI>>> mFeedsLiveData;

    private LoadMoreHandler loadMoreHandler;

    public FeedsViewModel() {
        PetApplication.getAppComponent().inject(this);
        loadMoreHandler = new LoadMoreHandler(mFeedRepository);
    }

    public LiveData<Resource<List<FeedUI>>> getFeeds(String pagingId, String loggedUserId) {
        mFeedsLiveData = mFeedRepository.loadFeeds(pagingId, loggedUserId);
        return mFeedsLiveData;
    }

    public LiveData<LoadMoreState> getLoadMoreState() {
        return loadMoreHandler.getLoadMoreState();
    }

    public void loadNextPage() {
        if (mFeedsLiveData.getValue() != null) {
            loadMoreHandler.loadNextPage(Paging.GLOBAL_FEEDS_PAGING_ID);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }

    public LiveData<Resource<List<Feed>>> refresh() {
        loadMoreHandler.reset();
        return mFeedRepository.refresh();
    }

    public void openUserProfile(String userId) {
        mOpenUserProfileEvent.setValue(userId);
    }

    public LiveData<String> getOpenUserProfileEvent() {
        return mOpenUserProfileEvent;
    }

    public void openComments(String feedId) {
        mOpenCommentsEvent.setValue(feedId);
    }

    public LiveData<String> getOpenCommentsEvent() {
        return mOpenCommentsEvent;
    }

    static class LoadMoreHandler implements Observer<Resource<Boolean>> {

        private final FeedRepository feedRepo;

        private MutableLiveData<LoadMoreState> loadMoreState = new MutableLiveData<>();

        private LiveData<Resource<Boolean>> nextPageLiveData;

        private boolean hasMore = true;

        public LoadMoreHandler(FeedRepository feedRepository) {
            feedRepo = feedRepository;
            loadMoreState.setValue(new LoadMoreState(false, null));
        }

        public void loadNextPage(String pagingId) {
            if (!hasMore || loadMoreState.getValue() == null || loadMoreState.getValue().isRunning()) {
                Timber.i("hasMore = %s", hasMore);
                return;
            }
            Timber.i("loadNextPage");
            unregister();
            nextPageLiveData = feedRepo.fetchNextPage(pagingId);
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
                        loadMoreState.setValue(new LoadMoreState(false,
                                result.message));
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

        public MutableLiveData<LoadMoreState> getLoadMoreState() {
            return loadMoreState;
        }
    }
}
