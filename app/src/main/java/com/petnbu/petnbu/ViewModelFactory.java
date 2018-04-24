package com.petnbu.petnbu;

import android.annotation.SuppressLint;
import android.app.Application;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.VisibleForTesting;

import com.petnbu.petnbu.api.FakeWebService;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.feed.FeedsRepository;
import com.petnbu.petnbu.feed.FeedsViewModel;

public class ViewModelFactory extends ViewModelProvider.NewInstanceFactory {

    @SuppressLint("StaticFieldLeak")
    private static volatile ViewModelFactory INSTANCE;

    private final Application mApplication;

    private final FeedsRepository mFeedsRepository;

    private final WebService mWebService;

    public static ViewModelFactory getInstance(Application application) {

        if (INSTANCE == null) {
            synchronized (ViewModelFactory.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ViewModelFactory(application, FeedsRepository.getInstance());
                }
            }
        }
        return INSTANCE;
    }

    @VisibleForTesting
    public static void destroyInstance() {
        INSTANCE = null;
    }

    private ViewModelFactory(Application application, FeedsRepository feedsRepository) {
        mApplication = application;
        mFeedsRepository = feedsRepository;
        mWebService = new FakeWebService();
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        if (modelClass.isAssignableFrom(FeedsViewModel.class)) {
            //noinspection unchecked
            return (T) new FeedsViewModel(mApplication, mWebService);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
