package com.petnbu.petnbu.model;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import com.petnbu.petnbu.db.ListPhotoConverters;
import com.petnbu.petnbu.db.PetTypeConverters;

import java.util.Date;
import java.util.List;

public class Feed {

    @NonNull
    private String feedId;
    @Embedded
    private FeedUser feedUser;
    @TypeConverters(ListPhotoConverters.class)
    private List<Photo> photos;
    private int commentCount;
    private int likeCount;
    private boolean isLiked;
    private String content;
    @Nullable
    @Ignore
    private Comment latestComment;
    @ServerTimestamp private Date timeCreated;
    @ServerTimestamp private Date timeUpdated;

    @LocalStatus.LOCAL_STATUS
    @Exclude
    private int status;

    @Exclude
    private boolean likeInProgress;

    public Feed() {
    }

    @Ignore
    public Feed(String feedId, FeedUser feedUser, List<Photo> photos, int commentCount, @Nullable Comment latestComment, int likeCount, boolean isLiked, boolean likeInProgress, String content, Date timeCreated, Date timeUpdated, int status) {
        this.feedId = feedId;
        this.feedUser = feedUser;
        this.photos = photos;
        this.commentCount = commentCount;
        this.latestComment = latestComment;
        this.likeCount = likeCount;
        this.isLiked = isLiked;
        this.content = content;
        this.timeCreated = timeCreated;
        this.timeUpdated = timeUpdated;
        this.status = status;
        this.likeInProgress = likeInProgress;
    }

    @NonNull
    public String getFeedId() {
        return feedId;
    }

    public void setFeedId(String feedId) {
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

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
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
    public Comment getLatestComment() {
        return latestComment;
    }

    public void setLatestComment(Comment latestComment) {
        this.latestComment = latestComment;
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
                '}';
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
        if (feedUser != null ? !feedUser.equals(that.feedUser) : that.feedUser != null)
            return false;
        if (photos != null ? !photos.equals(that.photos) : that.photos != null) return false;
        if (content != null ? !content.equals(that.content) : that.content != null) return false;
        if (latestComment != null ? !latestComment.equals(that.latestComment) : that.latestComment != null)
            return false;
        if (timeCreated != null ? !timeCreated.equals(that.timeCreated) : that.timeCreated != null)
            return false;
        return timeUpdated != null ? timeUpdated.equals(that.timeUpdated) : that.timeUpdated == null;
    }

    @Override
    public int hashCode() {
        int result = feedId.hashCode();
        result = 31 * result + (feedUser != null ? feedUser.hashCode() : 0);
        result = 31 * result + (photos != null ? photos.hashCode() : 0);
        result = 31 * result + commentCount;
        result = 31 * result + likeCount;
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (latestComment != null ? latestComment.hashCode() : 0);
        result = 31 * result + (timeCreated != null ? timeCreated.hashCode() : 0);
        result = 31 * result + (timeUpdated != null ? timeUpdated.hashCode() : 0);
        result = 31 * result + status;
        result = 31 * result + (likeInProgress ? 1 : 0);
        return result;
    }

    public FeedEntity toEntity(){
        return new FeedEntity(getFeedId(), getFeedUser().getUserId(),
                getPhotos(), getCommentCount(), getLatestComment() == null ? null : getLatestComment().getId()
                ,getLikeCount(), isLiked(), getContent(),
                getTimeCreated(), getTimeUpdated(), getStatus(), isLikeInProgress());
    }


}
