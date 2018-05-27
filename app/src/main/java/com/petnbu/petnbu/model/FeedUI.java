package com.petnbu.petnbu.model;

import android.arch.persistence.room.TypeConverters;

import com.petnbu.petnbu.db.ListPhotoConverters;
import com.petnbu.petnbu.db.PhotoConverters;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@TypeConverters(value = {ListPhotoConverters.class, PhotoConverters.class})
public class FeedUI {
    public String feedId;
    public String ownerId;
    public String name;
    public Photo avatar;
    public List<Photo> photos;
    public Date timeCreated;
    public int likeCount;
    public boolean isLiked;
    public boolean likeInProgress;
    public int commentCount;
    public String feedContent;
    public String latestCommentId;
    public String commentOwnerId;
    public String commentOwnerName;
    public Photo commentUserAvatar;
    public String commentContent;
    public Photo commentPhoto;
    public int status;

    public String getFeedId() {
        return feedId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public Photo getAvatar() {
        return avatar;
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public Date getTimeCreated() {
        return timeCreated;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public String getFeedContent() {
        return feedContent;
    }

    public String getCommentOwnerId() {
        return commentOwnerId;
    }

    public String getCommentOwnerName() {
        return commentOwnerName;
    }

    public Photo getCommentUserAvatar() {
        return commentUserAvatar;
    }

    public String getCommentContent() {
        return commentContent;
    }

    public int getStatus() {
        return status;
    }

    public Photo getCommentPhoto() {
        return commentPhoto;
    }

    @Override
    public String toString() {
        return "FeedUI{" +
                "feedId='" + feedId + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", name='" + name + '\'' +
                ", avatar=" + avatar +
                ", photos=" + photos +
                ", timeCreated=" + timeCreated +
                ", likeCount=" + likeCount +
                ", isLiked=" + isLiked +
                ", likeInProgress=" + likeInProgress +
                ", commentCount=" + commentCount +
                ", feedContent='" + feedContent + '\'' +
                ", latestCommentId='" + latestCommentId + '\'' +
                ", commentOwnerId='" + commentOwnerId + '\'' +
                ", commentOwnerName='" + commentOwnerName + '\'' +
                ", commentUserAvatar=" + commentUserAvatar +
                ", commentContent='" + commentContent + '\'' +
                ", commentPhoto=" + commentPhoto +
                ", status=" + status +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeedUI feedUI = (FeedUI) o;

        if (likeCount != feedUI.likeCount) return false;
        if (isLiked != feedUI.isLiked) return false;
        if (likeInProgress != feedUI.likeInProgress) return false;
        if (commentCount != feedUI.commentCount) return false;
        if (status != feedUI.status) return false;
        if (!feedId.equals(feedUI.feedId)) return false;
        if (!ownerId.equals(feedUI.ownerId)) return false;
        if (name != null ? !name.equals(feedUI.name) : feedUI.name != null) return false;
        if (avatar != null ? !avatar.equals(feedUI.avatar) : feedUI.avatar != null) return false;
        if (photos != null ? !photos.equals(feedUI.photos) : feedUI.photos != null) return false;
        if (timeCreated != null ? !timeCreated.equals(feedUI.timeCreated) : feedUI.timeCreated != null)
            return false;
        if (feedContent != null ? !feedContent.equals(feedUI.feedContent) : feedUI.feedContent != null)
            return false;
        if (latestCommentId != null ? !latestCommentId.equals(feedUI.latestCommentId) : feedUI.latestCommentId != null)
            return false;
        if (commentOwnerId != null ? !commentOwnerId.equals(feedUI.commentOwnerId) : feedUI.commentOwnerId != null)
            return false;
        if (commentOwnerName != null ? !commentOwnerName.equals(feedUI.commentOwnerName) : feedUI.commentOwnerName != null)
            return false;
        if (commentUserAvatar != null ? !commentUserAvatar.equals(feedUI.commentUserAvatar) : feedUI.commentUserAvatar != null)
            return false;
        if (commentContent != null ? !commentContent.equals(feedUI.commentContent) : feedUI.commentContent != null)
            return false;
        return commentPhoto != null ? commentPhoto.equals(feedUI.commentPhoto) : feedUI.commentPhoto == null;
    }

    @Override
    public int hashCode() {
        int result = feedId.hashCode();
        result = 31 * result + ownerId.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (avatar != null ? avatar.hashCode() : 0);
        result = 31 * result + (photos != null ? photos.hashCode() : 0);
        result = 31 * result + (timeCreated != null ? timeCreated.hashCode() : 0);
        result = 31 * result + likeCount;
        result = 31 * result + (isLiked ? 1 : 0);
        result = 31 * result + (likeInProgress ? 1 : 0);
        result = 31 * result + commentCount;
        result = 31 * result + (feedContent != null ? feedContent.hashCode() : 0);
        result = 31 * result + (latestCommentId != null ? latestCommentId.hashCode() : 0);
        result = 31 * result + (commentOwnerId != null ? commentOwnerId.hashCode() : 0);
        result = 31 * result + (commentOwnerName != null ? commentOwnerName.hashCode() : 0);
        result = 31 * result + (commentUserAvatar != null ? commentUserAvatar.hashCode() : 0);
        result = 31 * result + (commentContent != null ? commentContent.hashCode() : 0);
        result = 31 * result + (commentPhoto != null ? commentPhoto.hashCode() : 0);
        result = 31 * result + status;
        return result;
    }
}


