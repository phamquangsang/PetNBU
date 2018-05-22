package com.petnbu.petnbu.db;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.petnbu.petnbu.model.Paging;

import java.util.List;

@Dao
public abstract class PagingDao {


    @Query("SELECT * FROM paging WHERE pagingId = :id")
    abstract public LiveData<Paging> loadFeedPaging(String id);

    @Query("SELECT * FROM paging WHERE pagingId = :id")
    abstract public Paging findFeedPaging(String id);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract public void insert(Paging paging);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract public void update(Paging paging);

    @Query("DELETE FROM paging WHERE pagingId = :pagingId")
    abstract public void deleteFeedPaging(String pagingId);

    @Query("Select feedId from feeds where status = 1 and feedId in (:feedIds)")
    abstract public List<String> filterUploadingFeedId(List<String> feedIds);

}
