package com.petnbu.petnbu.model;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Comment {

    private String mId;
    private FeedUser mFeedUser;
    private String mContent;
    private Photo mPhoto;
    private int mLikeCount;
    private int mCommentCount;
    private Comment mLatestComment;

    @ServerTimestamp private Date timeCreated;
    @ServerTimestamp private Date timeUpdated;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public FeedUser getFeedUser() {
        return mFeedUser;
    }

    public void setFeedUser(FeedUser feedUser) {
        mFeedUser = feedUser;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(String content) {
        mContent = content;
    }

    public Photo getPhoto() {
        return mPhoto;
    }

    public void setPhoto(Photo photo) {
        mPhoto = photo;
    }

    public int getLikeCount() {
        return mLikeCount;
    }

    public void setLikeCount(int likeCount) {
        mLikeCount = likeCount;
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
        return mCommentCount;
    }

    public void setCommentCount(int commentCount) {
        mCommentCount = commentCount;
    }

    public Comment getLatestComment() {
        return mLatestComment;
    }

    public void setLatestComment(Comment latestComment) {
        mLatestComment = latestComment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Comment comment = (Comment) o;

        if (getLikeCount() != comment.getLikeCount()) return false;
        if (getCommentCount() != comment.getCommentCount()) return false;
        if (!getId().equals(comment.getId())) return false;
        if (!getFeedUser().equals(comment.getFeedUser())) return false;
        if (getContent() != null ? !getContent().equals(comment.getContent()) : comment.getContent() != null)
            return false;
        if (getPhoto() != null ? !getPhoto().equals(comment.getPhoto()) : comment.getPhoto() != null)
            return false;
        if (!getTimeCreated().equals(comment.getTimeCreated())) return false;
        return getTimeUpdated().equals(comment.getTimeUpdated());
    }

    @Override
    public int hashCode() {
        int result = getId().hashCode();
        result = 31 * result + getFeedUser().hashCode();
        result = 31 * result + (getContent() != null ? getContent().hashCode() : 0);
        result = 31 * result + (getPhoto() != null ? getPhoto().hashCode() : 0);
        result = 31 * result + getLikeCount();
        result = 31 * result + getCommentCount();
        result = 31 * result + getTimeCreated().hashCode();
        result = 31 * result + getTimeUpdated().hashCode();
        return result;
    }
}
