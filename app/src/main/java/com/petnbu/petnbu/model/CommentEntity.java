package com.petnbu.petnbu.model;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import com.petnbu.petnbu.db.PetTypeConverters;

import java.util.Date;

@Entity(tableName = "comments")
@TypeConverters(PetTypeConverters.class)
public class CommentEntity {

    @PrimaryKey
    @NonNull
    private String id;
    @ForeignKey(entity = UserEntity.class, parentColumns = {"userId"}, childColumns = "userId")
    private String ownerId;
    private String content;
    private Photo photo;
    private int likeCount;
    private int commentCount;
    @Nullable
    private String parentCommentId;
    @Nullable
    private String parentFeedId;

    @Exclude
    private int localStatus;

    @ServerTimestamp
    private Date timeCreated;
    @ServerTimestamp private Date timeUpdated;

    public CommentEntity() {
    }

    @Ignore
    public CommentEntity(@NonNull String id, String ownerId, String content, Photo photo, int likeCount,
                         int commentCount, @Nullable String parentCommentId, @Nullable String parentFeedId,
                         int localStatus, Date timeCreated, Date timeUpdated) {
        this.id = id;
        this.ownerId = ownerId;
        this.content = content;
        this.photo = photo;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.parentCommentId = parentCommentId;
        this.parentFeedId = parentFeedId;
        this.localStatus = localStatus;
        this.timeCreated = timeCreated;
        this.timeUpdated = timeUpdated;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public int getLocalStatus() {
        return localStatus;
    }

    public void setLocalStatus(int localStatus) {
        this.localStatus = localStatus;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Photo getPhoto() {
        return photo;
    }

    public void setPhoto(Photo photo) {
        this.photo = photo;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public Date getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(Date timeCreated) {
        this.timeCreated = timeCreated;
    }

    public Date getTimeUpdated() {
        return timeUpdated;
    }

    public void setTimeUpdated(Date timeUpdated) {
        this.timeUpdated = timeUpdated;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public String getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public String getParentFeedId() {
        return parentFeedId;
    }

    public void setParentFeedId(String parentFeedId) {
        this.parentFeedId = parentFeedId;
    }
}
