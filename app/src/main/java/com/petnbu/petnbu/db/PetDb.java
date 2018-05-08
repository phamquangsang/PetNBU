package com.petnbu.petnbu.db;

import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.DatabaseConfiguration;
import android.arch.persistence.room.InvalidationTracker;
import android.arch.persistence.room.RoomDatabase;

import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.FeedPaging;
import com.petnbu.petnbu.model.User;

@Database(entities = {User.class, FeedEntity.class, FeedPaging.class}, version = 6)
public abstract class PetDb extends RoomDatabase{

    abstract public FeedDao feedDao();

    abstract public UserDao userDao();

}
