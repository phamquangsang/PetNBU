package com.petnbu.petnbu.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query

import com.petnbu.petnbu.model.FeedUser
import com.petnbu.petnbu.model.UserEntity


@Dao
abstract class UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(userEntity: UserEntity)

    @Query("SELECT * FROM users WHERE userId = :id")
    abstract fun findLiveUserById(id: String): LiveData<UserEntity>

    @Query("SELECT * FROM users WHERE userId = :id")
    abstract fun findUserById(id: String?): UserEntity?

    fun insert(feedUser: FeedUser?) {
        feedUser?.run {
            insert(UserEntity(this.userId, this.avatar, this.name, null, null, null))
        }
    }
}