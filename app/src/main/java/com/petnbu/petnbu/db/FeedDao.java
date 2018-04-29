package com.petnbu.petnbu.db;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
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

    @Query("UPDATE feeds set feedId = :newId where feedId = :feedId")
    public abstract void updateFeedId(String feedId, String newId);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateAll(List<Feed> feeds);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateAll(Feed... feeds);

    @Delete
    public abstract void deleteFeed(Feed feed);

    @Query("DELETE FROM feeds Where feedId = :feedId")
    public abstract void deleteFeedById(String feedId);

    @Query("SELECT * FROM feeds ORDER BY timeCreated DESC")
    public abstract LiveData<List<Feed>> loadFeeds();

    @Query("SELECT * FROM feeds WHERE feedId = :feedId")
    public abstract Feed findFeedById(String feedId);

    @Query("DELETE FROM feeds")
    public abstract void deleteAll();

}
