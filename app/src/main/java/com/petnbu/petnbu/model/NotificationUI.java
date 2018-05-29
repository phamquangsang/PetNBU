package com.petnbu.petnbu.model;

import android.arch.persistence.room.Embedded;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class NotificationUI {
    @NonNull
    private String id;
    private String targetUserId;
    @Embedded
    private FeedUser fromUser;
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

    public FeedUser getFromUser() {
        return fromUser;
    }

    public void setFromUser(FeedUser fromUser) {
        this.fromUser = fromUser;
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
