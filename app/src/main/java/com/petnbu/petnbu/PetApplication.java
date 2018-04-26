package com.petnbu.petnbu;

import android.app.Application;

import com.petnbu.petnbu.di.AppModule;
import com.petnbu.petnbu.di.DaggerAppComponent;
import com.petnbu.petnbu.di.AppComponent;
import com.petnbu.petnbu.di.ViewModelComponent;

import timber.log.Timber;

public class PetApplication  extends Application{

    private static AppComponent sAppComponent;

    private static ViewModelComponent sViewModelComponent;

    public static AppComponent getAppComponent(){
        return sAppComponent;
    }

    public static ViewModelComponent getsViewModelComponent() {
        return sViewModelComponent;
    }

    public PetApplication() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        sAppComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();
//        sViewModelComponent = DaggerViewModelCompoent.builder().appModule(new AppModule(this)).build();
    }
}
