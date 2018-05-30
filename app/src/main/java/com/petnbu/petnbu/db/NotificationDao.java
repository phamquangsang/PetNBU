package com.petnbu.petnbu.db;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import com.petnbu.petnbu.model.Notification;
import com.petnbu.petnbu.model.NotificationEntity;
import com.petnbu.petnbu.model.NotificationUI;

import java.util.List;

@Dao
public abstract class NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) @Transaction
    public abstract void insert(NotificationEntity... entities);

    public void insertFromModel(Notification notification){
        insert(notification.toEntity());
    }

    @Transaction
    public void insertFromModels(List<Notification> list){
        for (Notification noti : list) {
            insert(noti.toEntity());
        }
    }

    @Query("SELECT id, targetUserId, targetFeedId, targetCommentId, targetReplyId, type, " +
            "notifications.timeCreated, isRead, userId, avatar, name " +
            "FROM notifications, users " +
            "WHERE id IN (:ids) AND fromUserId == userId " +
            "ORDER BY notifications.timeCreated DESC")
    public abstract LiveData<List<NotificationUI>> getNotifcations(List<String> ids);
}
