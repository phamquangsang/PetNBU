package com.petnbu.petnbu.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import android.arch.persistence.room.Update

import com.petnbu.petnbu.model.Paging

@Dao
abstract class PagingDao {

    @Query("SELECT * FROM paging WHERE pagingId = :id")
    abstract fun loadFeedPaging(id: String): LiveData<Paging>

    @Query("SELECT * FROM paging WHERE pagingId = :id")
    abstract fun findFeedPaging(id: String): Paging?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(paging: Paging)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract fun update(paging: Paging)

    @Query("DELETE FROM paging WHERE pagingId = :pagingId")
    abstract fun deleteFeedPaging(pagingId: String)

    @Query("Select feedId from feeds where status = 1 and feedId in (:feedIds)")
    abstract fun filterUploadingFeedId(feedIds: List<String>): List<String>

}
