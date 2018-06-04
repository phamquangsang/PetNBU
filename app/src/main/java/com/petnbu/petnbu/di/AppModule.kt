package com.petnbu.petnbu.di

import android.app.Application
import android.arch.persistence.room.Room
import android.content.Context
import android.content.SharedPreferences

import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.google.gson.Gson
import com.petnbu.petnbu.db.CommentDao
import com.petnbu.petnbu.db.FeedDao
import com.petnbu.petnbu.db.NotificationDao
import com.petnbu.petnbu.db.PetDb
import com.petnbu.petnbu.db.UserDao

import javax.inject.Singleton

import dagger.Module
import dagger.Provides

@Module
class AppModule(private val mApplication: Application) {

    @Provides
    @Singleton
    internal fun providesApplication(): Application {
        return mApplication
    }

    @Provides
    @Singleton
    internal fun provideDb(app: Application): PetDb {
        return Room.databaseBuilder(app, PetDb::class.java, "pet.db").fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    internal fun provideFeedDao(db: PetDb): FeedDao {
        return db.feedDao()
    }

    @Provides
    @Singleton
    internal fun provideUserDao(db: PetDb): UserDao {
        return db.userDao()
    }

    @Provides
    @Singleton
    internal fun provideCommentDao(db: PetDb): CommentDao {
        return db.commentDao()
    }

    @Provides
    @Singleton
    internal fun provideNotificationDao(db: PetDb): NotificationDao {
        return db.notificationDao()
    }

    @Provides
    @Singleton
    internal fun provideDispatcher(application: Application): FirebaseJobDispatcher {
        return FirebaseJobDispatcher(GooglePlayDriver(application))
    }

    @Provides
    @Singleton
    internal fun provideSharedPref(application: Application): SharedPreferences {
        return application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    internal fun provideGson(): Gson {
        return Gson()
    }

    companion object {

        val PREF_NAME = "PET_SETTING"
    }

}
