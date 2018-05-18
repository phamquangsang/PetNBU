package com.petnbu.petnbu.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.firebase.firestore.Exclude;
import com.petnbu.petnbu.db.PetTypeConverters;

import java.lang.annotation.Retention;
import java.util.Date;
import java.util.List;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@Entity(tableName = "feeds")
@TypeConverters(PetTypeConverters.class)
public class FeedEntity {


    @PrimaryKey
    @NonNull
    private String feedId;
    @ForeignKey(entity = UserEntity.class, parentColumns = "userId", childColumns = "fromUserId")
    private String fromUserId;
    private List<Photo> photos;
    private int commentCount;
    @Nullable
    private String latestCommentId;
    private int likeCount;
    private String content;
    private Date timeCreated;
    private Date timeUpdated;

    @LocalStatus.LOCAL_STATUS
    private int status;

    private boolean likeInProgress;

    public FeedEntity() {
    }

    @Ignore
    public FeedEntity(@NonNull String feedId, String fromUserId, List<Photo> photos, int commentCount,
                      @Nullable String latestCommentId, int likeCount, String content, Date timeCreated, Date timeUpdated, int status, boolean likeInProgress) {
        this.feedId = feedId;
        this.fromUserId = fromUserId;
        this.photos = photos;
        this.commentCount = commentCount;
        this.latestCommentId = latestCommentId;
        this.likeCount = likeCount;
        this.content = content;
        this.timeCreated = timeCreated;
        this.timeUpdated = timeUpdated;
        this.status = status;
        this.likeInProgress = likeInProgress;
    }

    public String getFeedId() {
        return feedId;
    }

    public void setFeedId(String feedId) {
        this.feedId = feedId;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Nullable
    public String getLatestCommentId() {
        return latestCommentId;
    }

    public void setLatestCommentId(@Nullable String latestCommentId) {
        this.latestCommentId = latestCommentId;
    }

    @Exclude
    public boolean isLikeInProgress() {
        return likeInProgress;
    }

    public void setLikeInProgress(boolean likeInProgress) {
        this.likeInProgress = likeInProgress;
    }

    @LocalStatus.LOCAL_STATUS
    @Exclude
    public int getStatus() {
        return status;
    }

    public void setStatus(@LocalStatus.LOCAL_STATUS int status) {
        this.status = status;
    }

}
