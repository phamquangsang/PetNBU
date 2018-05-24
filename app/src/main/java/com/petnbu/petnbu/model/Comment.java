package com.petnbu.petnbu.model;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Ignore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Comment {
    @NonNull
    private String id;
    private FeedUser feedUser;
    private String content;
    private Photo photo;
    private int likeCount;
    @Exclude
    private boolean isLiked;
    @Exclude
    private boolean likeInProgress;
    private int commentCount;
    @Nullable
    private Comment latestComment;
    @Nullable
    private String parentCommentId;
    @Nullable
    private String parentFeedId;

    @Exclude
    @LocalStatus.LOCAL_STATUS
    private int localStatus;

    @ServerTimestamp
    private Date timeCreated;
    @ServerTimestamp
    private Date timeUpdated;

    public Comment() {
    }

    public Comment(@NonNull String id, FeedUser feedUser, String content, Photo photo, int likeCount,
                   int commentCount, Comment latestComment, String parentCommentId, String parentFeedId,
                   int localStatus, Date timeCreated, Date timeUpdated) {
        this.id = id;
        this.feedUser = feedUser;
        this.content = content;
        this.photo = photo;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.latestComment = latestComment;
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

    public FeedUser getFeedUser() {
        return feedUser;
    }

    public void setFeedUser(FeedUser feedUser) {
        this.feedUser = feedUser;
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

    public Comment getLatestComment() {
        return latestComment;
    }

    public void setLatestComment(Comment latestComment) {
        this.latestComment = latestComment;
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

    @Exclude
    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    @Exclude
    public boolean isLikeInProgress() {
        return likeInProgress;
    }

    public void setLikeInProgress(boolean likeInProgress) {
        this.likeInProgress = likeInProgress;
    }

    @Exclude
    @LocalStatus.LOCAL_STATUS
    public int getLocalStatus() {
        return localStatus;
    }

    public void setLocalStatus(@LocalStatus.LOCAL_STATUS int localStatus) {
        this.localStatus = localStatus;
    }

    public void setParentFeedId(String parentFeedId) {
        this.parentFeedId = parentFeedId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Comment comment = (Comment) o;

        if (likeCount != comment.likeCount) return false;
        if (commentCount != comment.commentCount) return false;
        if (localStatus != comment.localStatus) return false;
        if (!id.equals(comment.id)) return false;
        if (feedUser != null ? !feedUser.equals(comment.feedUser) : comment.feedUser != null)
            return false;
        if (content != null ? !content.equals(comment.content) : comment.content != null)
            return false;
        if (photo != null ? !photo.equals(comment.photo) : comment.photo != null) return false;
        if (latestComment != null ? !latestComment.equals(comment.latestComment) : comment.latestComment != null)
            return false;
        if (parentCommentId != null ? !parentCommentId.equals(comment.parentCommentId) : comment.parentCommentId != null)
            return false;
        if (parentFeedId != null ? !parentFeedId.equals(comment.parentFeedId) : comment.parentFeedId != null)
            return false;
        if (timeCreated != null ? !timeCreated.equals(comment.timeCreated) : comment.timeCreated != null)
            return false;
        return timeUpdated != null ? timeUpdated.equals(comment.timeUpdated) : comment.timeUpdated == null;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (feedUser != null ? feedUser.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (photo != null ? photo.hashCode() : 0);
        result = 31 * result + likeCount;
        result = 31 * result + commentCount;
        result = 31 * result + (latestComment != null ? latestComment.hashCode() : 0);
        result = 31 * result + (parentCommentId != null ? parentCommentId.hashCode() : 0);
        result = 31 * result + (parentFeedId != null ? parentFeedId.hashCode() : 0);
        result = 31 * result + localStatus;
        result = 31 * result + (timeCreated != null ? timeCreated.hashCode() : 0);
        result = 31 * result + (timeUpdated != null ? timeUpdated.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id='" + id + '\'' +
                ", feedUser=" + feedUser +
                ", content='" + content + '\'' +
                ", photo=" + photo +
                ", likeCount=" + likeCount +
                ", commentCount=" + commentCount +
                ", latestComment=" + latestComment +
                ", parentCommentId='" + parentCommentId + '\'' +
                ", parentFeedId='" + parentFeedId + '\'' +
                ", timeCreated=" + timeCreated +
                ", timeUpdated=" + timeUpdated +
                '}';
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", getId());
        map.put("feedUser", feedUser != null ? feedUser.toMap() : null);
        map.put("content", content);
        map.put("photo", photo != null ? photo.toMap() : null);
        map.put("likeCount", likeCount);
        map.put("commentCount", commentCount);
        map.put("latestComment", latestComment != null ? latestComment.toMap() : null);
        map.put("parentCommentId", parentCommentId);
        map.put("parentFeedId", parentFeedId);
        map.put("timeCreated", timeCreated);
        map.put("timeUpdated", timeUpdated);
        return map;
    }

    public CommentEntity toEntity() {
        return new CommentEntity(id, feedUser.getUserId(), content, photo, likeCount, isLiked(), isLikeInProgress() ,commentCount,
                parentCommentId, parentFeedId, latestComment == null ? null : latestComment.getId(), localStatus, timeCreated, timeUpdated);
    }
}
