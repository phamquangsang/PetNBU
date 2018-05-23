package com.petnbu.petnbu.model;

import android.arch.persistence.room.Embedded;
import android.support.annotation.NonNull;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class CommentUI {
    @NonNull
    private String id;
    @Embedded
    public FeedUser owner;
    public String content;
    public Photo photo;
    public int likeCount;
    public int commentCount;
    public String parentCommentId;
    public String parentFeedId;

    @Exclude
    @LocalStatus.LOCAL_STATUS
    public int localStatus;
    @ServerTimestamp public Date timeCreated;

    public String latestCommentId;
    public String latestCommentContent;
    public String latestCommentOwnerId;
    public String latestCommentOwnerName;
    public Photo latestCommentOwnerAvatar;
    public Photo latestCommentPhoto;

    @NonNull
    public String getId() {
        return id;
    }

    public FeedUser getOwner() {
        return owner;
    }

    public String getContent() {
        return content;
    }

    public Photo getPhoto() {
        return photo;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public String getParentCommentId() {
        return parentCommentId;
    }

    public String getParentFeedId() {
        return parentFeedId;
    }

    public int getLocalStatus() {
        return localStatus;
    }

    public Date getTimeCreated() {
        return timeCreated;
    }

    public String getLatestCommentId() {
        return latestCommentId;
    }

    public String getLatestCommentContent() {
        return latestCommentContent;
    }

    public String getLatestCommentOwnerId() {
        return latestCommentOwnerId;
    }

    public String getLatestCommentOwnerName() {
        return latestCommentOwnerName;
    }

    public Photo getLatestCommentOwnerAvatar() {
        return latestCommentOwnerAvatar;
    }

    public Photo getLatestCommentPhoto() {
        return latestCommentPhoto;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public void setOwner(FeedUser owner) {
        this.owner = owner;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setPhoto(Photo photo) {
        this.photo = photo;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public void setParentFeedId(String parentFeedId) {
        this.parentFeedId = parentFeedId;
    }

    public void setLocalStatus(int localStatus) {
        this.localStatus = localStatus;
    }

    public void setTimeCreated(Date timeCreated) {
        this.timeCreated = timeCreated;
    }

    public void setLatestCommentId(String latestCommentId) {
        this.latestCommentId = latestCommentId;
    }

    public void setLatestCommentContent(String latestCommentContent) {
        this.latestCommentContent = latestCommentContent;
    }

    public void setLatestCommentOwnerId(String latestCommentOwnerId) {
        this.latestCommentOwnerId = latestCommentOwnerId;
    }

    public void setLatestCommentOwnerName(String latestCommentOwnerName) {
        this.latestCommentOwnerName = latestCommentOwnerName;
    }

    public void setLatestCommentOwnerAvatar(Photo latestCommentOwnerAvatar) {
        this.latestCommentOwnerAvatar = latestCommentOwnerAvatar;
    }

    public void setLatestCommentPhoto(Photo latestCommentPhoto) {
        this.latestCommentPhoto = latestCommentPhoto;
    }

    @Override
    public String toString() {
        return "CommentUI{" +
                "id='" + id + '\'' +
                ", owner=" + owner +
                ", content='" + content + '\'' +
                ", photo=" + photo +
                ", likeCount=" + likeCount +
                ", commentCount=" + commentCount +
                ", parentCommentId='" + parentCommentId + '\'' +
                ", parentFeedId='" + parentFeedId + '\'' +
                ", localStatus=" + localStatus +
                ", timeCreated=" + timeCreated +
                ", latestCommentId='" + latestCommentId + '\'' +
                ", latestCommentContent='" + latestCommentContent + '\'' +
                ", latestCommentOwnerId='" + latestCommentOwnerId + '\'' +
                ", latestCommentOwnerName='" + latestCommentOwnerName + '\'' +
                ", latestCommentOwnerAvatar=" + latestCommentOwnerAvatar +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CommentUI commentUI = (CommentUI) o;

        if (likeCount != commentUI.likeCount) return false;
        if (commentCount != commentUI.commentCount) return false;
        if (localStatus != commentUI.localStatus) return false;
        if (!id.equals(commentUI.id)) return false;
        if (owner != null ? !owner.equals(commentUI.owner) : commentUI.owner != null) return false;
        if (content != null ? !content.equals(commentUI.content) : commentUI.content != null)
            return false;
        if (photo != null ? !photo.equals(commentUI.photo) : commentUI.photo != null) return false;
        if (parentCommentId != null ? !parentCommentId.equals(commentUI.parentCommentId) : commentUI.parentCommentId != null)
            return false;
        if (parentFeedId != null ? !parentFeedId.equals(commentUI.parentFeedId) : commentUI.parentFeedId != null)
            return false;
        if (timeCreated != null ? !timeCreated.equals(commentUI.timeCreated) : commentUI.timeCreated != null)
            return false;
        if (latestCommentId != null ? !latestCommentId.equals(commentUI.latestCommentId) : commentUI.latestCommentId != null)
            return false;
        if (latestCommentContent != null ? !latestCommentContent.equals(commentUI.latestCommentContent) : commentUI.latestCommentContent != null)
            return false;
        if (latestCommentOwnerId != null ? !latestCommentOwnerId.equals(commentUI.latestCommentOwnerId) : commentUI.latestCommentOwnerId != null)
            return false;
        if (latestCommentOwnerName != null ? !latestCommentOwnerName.equals(commentUI.latestCommentOwnerName) : commentUI.latestCommentOwnerName != null)
            return false;
        if (latestCommentOwnerAvatar != null ? !latestCommentOwnerAvatar.equals(commentUI.latestCommentOwnerAvatar) : commentUI.latestCommentOwnerAvatar != null)
            return false;
        return latestCommentPhoto != null ? latestCommentPhoto.equals(commentUI.latestCommentPhoto) : commentUI.latestCommentPhoto == null;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (photo != null ? photo.hashCode() : 0);
        result = 31 * result + likeCount;
        result = 31 * result + commentCount;
        result = 31 * result + (parentCommentId != null ? parentCommentId.hashCode() : 0);
        result = 31 * result + (parentFeedId != null ? parentFeedId.hashCode() : 0);
        result = 31 * result + localStatus;
        result = 31 * result + (timeCreated != null ? timeCreated.hashCode() : 0);
        result = 31 * result + (latestCommentId != null ? latestCommentId.hashCode() : 0);
        result = 31 * result + (latestCommentContent != null ? latestCommentContent.hashCode() : 0);
        result = 31 * result + (latestCommentOwnerId != null ? latestCommentOwnerId.hashCode() : 0);
        result = 31 * result + (latestCommentOwnerName != null ? latestCommentOwnerName.hashCode() : 0);
        result = 31 * result + (latestCommentOwnerAvatar != null ? latestCommentOwnerAvatar.hashCode() : 0);
        result = 31 * result + (latestCommentPhoto != null ? latestCommentPhoto.hashCode() : 0);
        return result;
    }
}
