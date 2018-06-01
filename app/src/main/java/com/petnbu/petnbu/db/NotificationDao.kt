package com.petnbu.petnbu.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import android.arch.persistence.room.Transaction

import com.petnbu.petnbu.model.Notification
import com.petnbu.petnbu.model.NotificationEntity
import com.petnbu.petnbu.model.NotificationUI

@Dao
abstract class NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @Transaction
    abstract fun insert(vararg entities: NotificationEntity)

    fun insertFromModel(notification: Notification) {
        insert(notification.toEntity())
    }

    @Transaction
    open fun insertFromModels(list: List<Notification>) {
        list.forEach{insert(it.toEntity())}
    }

    @Query("SELECT id, targetUserId, targetFeedId, targetCommentId, targetReplyId, type, " +
            "notifications.timeCreated, isRead, userId, avatar, name " +
            "FROM notifications, users " +
            "WHERE id IN (:ids) AND fromUserId == userId " +
            "ORDER BY notifications.timeCreated DESC")
    abstract fun getNotifcations(ids: List<String>): LiveData<List<NotificationUI>>
}
