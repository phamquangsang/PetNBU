package com.petnbu.petnbu.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

@Entity(tableName = "notifications")
public class NotificationEntity {

    @PrimaryKey @NonNull
    private String id;
    private String targetUserId;
    @ForeignKey(entity = UserEntity.class, parentColumns = "userId", childColumns = "fromUserId")
    private String fromUserId;
    @Nullable
    private String targetFeedId;
    @Nullable
    private String targetCommentId;
    @Nullable
    private String targetReplyId;
    @Notification.NotificationType
    private int type;
    @ServerTimestamp
    private Date timeCreated;

    private boolean isRead;

    public NotificationEntity() {
    }

    @Ignore
    public NotificationEntity(@NonNull String id, String targetUserId, String fromUserId,
                              @Nullable String targetFeedId, @Nullable String targetCommentId,
                              @Nullable String targetReplyId, int type, Date timeCreated, boolean isRead) {
        this.id = id;
        this.targetUserId = targetUserId;
        this.fromUserId = fromUserId;
        this.targetFeedId = targetFeedId;
        this.targetCommentId = targetCommentId;
        this.targetReplyId = targetReplyId;
        this.type = type;
        this.timeCreated = timeCreated;
        this.isRead = isRead;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    @Notification.NotificationType
    public int getType() {
        return type;
    }

    public void setType(@Notification.NotificationType int type) {
        this.type = type;
    }

    public Date getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(Date timeCreated) {
        this.timeCreated = timeCreated;
    }

    @Nullable
    public String getTargetFeedId() {
        return targetFeedId;
    }

    public void setTargetFeedId(@Nullable String targetFeedId) {
        this.targetFeedId = targetFeedId;
    }

    @Nullable
    public String getTargetCommentId() {
        return targetCommentId;
    }

    public void setTargetCommentId(@Nullable String targetCommentId) {
        this.targetCommentId = targetCommentId;
    }

    @Nullable
    public String getTargetReplyId() {
        return targetReplyId;
    }

    public void setTargetReplyId(@Nullable String targetReplyId) {
        this.targetReplyId = targetReplyId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
