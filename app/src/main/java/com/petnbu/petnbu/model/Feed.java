package com.petnbu.petnbu.model;

import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import com.google.firebase.firestore.Exclude;
import com.petnbu.petnbu.db.PetTypeConverters;

import java.util.Date;
import java.util.List;


@TypeConverters(value = PetTypeConverters.class)
public class Feed {
    @NonNull
    private String feedId;

    private String name;
    private String userId;
    private Photo avatar;

    private List<Photo> photos;
    private int commentCount;
    private int likeCount;
    private String content;
    private Date timeCreated;
    private Date timeUpdated;

    @FeedEntity.LOCAL_STATUS
    @Exclude
    private int status;

    @Exclude
    private boolean likeInProgress;

    public Feed() {
    }

    @Ignore
    public Feed(@NonNull String feedId, String name, String userId, Photo avatar, List<Photo> photos, int commentCount, int likeCount, String content, Date timeCreated, Date timeUpdated, int status, boolean likeInProgress) {
        this.feedId = feedId;
        this.name = name;
        this.userId = userId;
        this.avatar = avatar;
        this.photos = photos;
        this.commentCount = commentCount;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Photo getAvatar() {
        return avatar;
    }

    public void setAvatar(Photo avatar) {
        this.avatar = avatar;
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

    @Exclude
    public boolean isLikeInProgress() {
        return likeInProgress;
    }

    public void setLikeInProgress(boolean likeInProgress) {
        this.likeInProgress = likeInProgress;
    }

    @FeedEntity.LOCAL_STATUS
    @Exclude
    public int getStatus() {
        return status;
    }

    public void setStatus(@FeedEntity.LOCAL_STATUS int status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Feed that = (Feed) o;

        if (commentCount != that.commentCount) return false;
        if (likeCount != that.likeCount) return false;
        if (status != that.status) return false;
        if (likeInProgress != that.likeInProgress) return false;
        if (!feedId.equals(that.feedId)) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (!userId.equals(that.userId)) return false;
        if (avatar != null ? !avatar.equals(that.avatar) : that.avatar != null) return false;
        if (photos != null ? !photos.equals(that.photos) : that.photos != null) return false;
        if (content != null ? !content.equals(that.content) : that.content != null) return false;
        if (timeCreated != null ? !timeCreated.equals(that.timeCreated) : that.timeCreated != null)
            return false;
        return timeUpdated != null ? timeUpdated.equals(that.timeUpdated) : that.timeUpdated == null;
    }

    @Override
    public int hashCode() {
        int result = feedId.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + userId.hashCode();
        result = 31 * result + (avatar != null ? avatar.hashCode() : 0);
        result = 31 * result + (photos != null ? photos.hashCode() : 0);
        result = 31 * result + commentCount;
        result = 31 * result + likeCount;
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (timeCreated != null ? timeCreated.hashCode() : 0);
        result = 31 * result + (timeUpdated != null ? timeUpdated.hashCode() : 0);
        result = 31 * result + status;
        result = 31 * result + (likeInProgress ? 1 : 0);
        return result;
    }
}
