package com.petnbu.petnbu.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.Paging;
import com.petnbu.petnbu.model.UserEntity;

@Database(entities = {UserEntity.class, FeedEntity.class, Paging.class}, version = 7)
public abstract class PetDb extends RoomDatabase{

    abstract public FeedDao feedDao();

    abstract public UserDao userDao();

}
