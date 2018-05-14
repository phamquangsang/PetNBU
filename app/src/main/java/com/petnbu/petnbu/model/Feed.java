package com.petnbu.petnbu.model;

import android.arch.persistence.room.Embedded;
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

    @Embedded
    private FeedUser feedUser;

    private List<Photo> photos;
    private int commentCount;
    @Ignore
    private Comment latestComment;
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

    @NonNull
    public String getFeedId() {
        return feedId;
    }

    public void setFeedId(@NonNull String feedId) {
        this.feedId = feedId;
    }

    public FeedUser getFeedUser() {
        return feedUser;
    }

    public void setFeedUser(FeedUser feedUser) {
        this.feedUser = feedUser;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    @Exclude
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Exclude
    public boolean isLikeInProgress() {
        return likeInProgress;
    }

    public void setLikeInProgress(boolean likeInProgress) {
        this.likeInProgress = likeInProgress;
    }

    public Comment getLatestComment() {
        return latestComment;
    }

    public void setLatestComment(Comment latestComment) {
        this.latestComment = latestComment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Feed feed = (Feed) o;

        if (commentCount != feed.commentCount) return false;
        if (likeCount != feed.likeCount) return false;
        if (status != feed.status) return false;
        if (likeInProgress != feed.likeInProgress) return false;
        if (!feedId.equals(feed.feedId)) return false;
        if (feedUser != null ? !feedUser.equals(feed.feedUser) : feed.feedUser != null)
            return false;
        if (photos != null ? !photos.equals(feed.photos) : feed.photos != null) return false;
        if (latestComment != null ? !latestComment.equals(feed.latestComment) : feed.latestComment != null)
            return false;
        if (content != null ? !content.equals(feed.content) : feed.content != null) return false;
        if (timeCreated != null ? !timeCreated.equals(feed.timeCreated) : feed.timeCreated != null)
            return false;
        return timeUpdated != null ? timeUpdated.equals(feed.timeUpdated) : feed.timeUpdated == null;
    }

    @Override
    public int hashCode() {
        int result = feedId.hashCode();
        result = 31 * result + (feedUser != null ? feedUser.hashCode() : 0);
        result = 31 * result + (photos != null ? photos.hashCode() : 0);
        result = 31 * result + commentCount;
        result = 31 * result + (latestComment != null ? latestComment.hashCode() : 0);
        result = 31 * result + likeCount;
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (timeCreated != null ? timeCreated.hashCode() : 0);
        result = 31 * result + (timeUpdated != null ? timeUpdated.hashCode() : 0);
        result = 31 * result + status;
        result = 31 * result + (likeInProgress ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Feed{" +
                "feedId='" + feedId + '\'' +
                ", feedUser=" + feedUser +
                ", photos=" + photos +
                ", commentCount=" + commentCount +
                ", likeCount=" + likeCount +
                ", content='" + content + '\'' +
                ", timeCreated=" + timeCreated +
                ", timeUpdated=" + timeUpdated +
                ", status=" + status +
                ", likeInProgress=" + likeInProgress +
                '}';
    }
}
