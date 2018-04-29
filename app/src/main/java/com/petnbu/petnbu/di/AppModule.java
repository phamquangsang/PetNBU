package com.petnbu.petnbu.di;

import android.app.Application;
import android.arch.persistence.room.Room;
import android.content.SharedPreferences;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.petnbu.petnbu.db.FeedDao;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.db.UserDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

    private Application mApplication;

    public AppModule(Application application){
        mApplication = application;
    }

    @Provides
    @Singleton
    Application providesApplication() {
        return mApplication;
    }

    @Provides
    @Singleton
    PetDb provideDb(Application app){
        return Room.databaseBuilder(app, PetDb.class, "pet.db").fallbackToDestructiveMigration().build();
    }

    @Provides
    @Singleton
    FeedDao provideFeedDao(PetDb db){
        return db.feedDao();
    }

    @Provides
    @Singleton
    UserDao provideUserDao(PetDb db){
        return db.userDao();
    }

    @Provides
    @Singleton
    FirebaseJobDispatcher provideDispatcher(Application application){
        return new FirebaseJobDispatcher(new GooglePlayDriver(application));
    }

}
