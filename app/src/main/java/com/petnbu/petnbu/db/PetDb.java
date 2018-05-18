package com.petnbu.petnbu.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverter;
import android.arch.persistence.room.TypeConverters;

import com.petnbu.petnbu.model.CommentEntity;
import com.petnbu.petnbu.model.FeedCommentEntity;
import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.Paging;
import com.petnbu.petnbu.model.SubCommentEntity;
import com.petnbu.petnbu.model.UserEntity;

@Database(entities = {UserEntity.class, FeedEntity.class, Paging.class, CommentEntity.class, FeedCommentEntity.class, SubCommentEntity.class} , version = 12)
@TypeConverters(value = PetTypeConverters.class)
public abstract class PetDb extends RoomDatabase {

    abstract public FeedDao feedDao();

    abstract public UserDao userDao();

    abstract public CommentDao commentDao();
}
