package com.petnbu.petnbu.db;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Update;

import com.petnbu.petnbu.model.Feed;

import java.util.List;

@Dao
public abstract class FeedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(Feed... feeds);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(List<Feed> feedList);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public abstract void update(Feed feed);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateAll(List<Feed> feeds);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateAll(Feed... feeds);
}
