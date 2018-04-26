package com.petnbu.petnbu;

import android.annotation.SuppressLint;
import android.app.Application;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.VisibleForTesting;

import com.petnbu.petnbu.api.FakeWebService;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.feed.FeedsViewModel;
import com.petnbu.petnbu.repo.FeedRepository;

public class ViewModelFactory extends ViewModelProvider.NewInstanceFactory {

    @SuppressLint("StaticFieldLeak")
    private static volatile ViewModelFactory INSTANCE;

    private final Application mApplication;

    private final FeedRepository mFeedsRepository;

    private final WebService mWebService;

    public static ViewModelFactory getInstance(Application application, FeedRepository feedRepo) {

        if (INSTANCE == null) {
            synchronized (ViewModelFactory.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ViewModelFactory(application, feedRepo);
                }
            }
        }
        return INSTANCE;
    }

    @VisibleForTesting
    public static void destroyInstance() {
        INSTANCE = null;
    }

    private ViewModelFactory(Application application, FeedRepository feedsRepository) {
        mApplication = application;
        mFeedsRepository = feedsRepository;
        mWebService = new FakeWebService();
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        if (modelClass.isAssignableFrom(FeedsViewModel.class)) {
            //noinspection unchecked
            return (T) new FeedsViewModel();
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
