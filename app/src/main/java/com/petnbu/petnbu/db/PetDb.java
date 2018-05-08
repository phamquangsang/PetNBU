package com.petnbu.petnbu.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.FeedPaging;
import com.petnbu.petnbu.model.UserEntity;

@Database(entities = {UserEntity.class, FeedEntity.class, FeedPaging.class}, version = 6)
public abstract class PetDb extends RoomDatabase{

    abstract public FeedDao feedDao();

    abstract public UserDao userDao();

}
