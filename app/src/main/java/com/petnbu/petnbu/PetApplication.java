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
                    .detectAll() // or .detectAll() for all detectable problems
                    .penaltyDeath()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .build());
        }
        sAppComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();

    }
}
