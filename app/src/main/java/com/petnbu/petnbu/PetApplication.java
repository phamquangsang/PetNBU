package com.petnbu.petnbu;

import android.app.Application;

import com.petnbu.petnbu.di.DaggerWebComponent;
import com.petnbu.petnbu.di.WebComponent;

import timber.log.Timber;

public class PetApplication  extends Application{

    private static WebComponent sWebComponent;

    public static WebComponent getWebComponent(){
        return sWebComponent;
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
        sWebComponent = DaggerWebComponent.create();
    }
}
