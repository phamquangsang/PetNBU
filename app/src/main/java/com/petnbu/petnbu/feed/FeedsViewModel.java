package com.petnbu.petnbu.feed;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.Nullable;

import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.model.Paging;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.repo.FeedRepository;
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

    private LiveData<Resource<List<Feed>>> mFeedsLiveData;

    private LoadMoreHandler loadMoreHandler;

    public FeedsViewModel() {
        PetApplication.getAppComponent().inject(this);
        loadMoreHandler = new LoadMoreHandler(mFeedRepository);
    }

    public LiveData<Resource<List<Feed>>> getFeeds(String pagingId) {
        mFeedsLiveData = mFeedRepository.loadFeeds(pagingId);
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
        return mFeedRepository.refresh();
    }

    static class LoadMoreState {
        private final boolean running;
        private final String errorMessage;
        private boolean handledError = false;

        LoadMoreState(boolean running, String errorMessage) {
            this.running = running;
            this.errorMessage = errorMessage;
        }

        boolean isRunning() {
            return running;
        }

        String getErrorMessage() {
            return errorMessage;
        }

        String getErrorMessageIfNotHandled() {
            if (handledError) {
                return null;
            }
            handledError = true;
            return errorMessage;
        }

        @Override
        public String toString() {
            return "LoadMoreState{" +
                    "running=" + running +
                    ", errorMessage='" + errorMessage + '\'' +
                    ", handledError=" + handledError +
                    '}';
        }
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
            if (!hasMore || loadMoreState.getValue() == null || loadMoreState.getValue().running) {
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
