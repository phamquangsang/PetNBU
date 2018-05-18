package com.petnbu.petnbu;

import android.app.Application;
import android.os.StrictMode;

import com.petnbu.petnbu.di.AppModule;
import com.petnbu.petnbu.di.DaggerAppComponent;
import com.petnbu.petnbu.di.AppComponent;

import timber.log.Timber;

public class PetApplication  extends Application{

    private static AppComponent sAppComponent;

    public static AppComponent getAppComponent(){
        return sAppComponent;
    }


    public PetApplication() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectNetwork().detectDiskReads().detectDiskWrites()
                    .penaltyLog()
                    .penaltyDeath().build());
        }
        sAppComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();

    }
}
