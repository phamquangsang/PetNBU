package com.petnbu.petnbu.model;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverter;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import com.google.firebase.firestore.ServerTimestamp;
import com.petnbu.petnbu.db.PetTypeConverters;

import java.util.Date;
import java.util.List;

@Entity(tableName = "feeds")
@TypeConverters(PetTypeConverters.class)
public class Feed {
<<<<<<< HEAD
    @PrimaryKey @NonNull
=======

>>>>>>> 842164d70d2760d8e8d8761246f4f9446c3e8cee
    private String feedId;
    @Embedded
    private FeedUser mFeedUser;
    private List<Photo> photos;
    private int commentCount;
    private int likeCount;
    private String content;
    @ServerTimestamp private Date timeCreated;
    @ServerTimestamp private Date timeUpdated;

    public Feed() {
    }

    public Feed(String feedId, FeedUser feedUser, List<Photo> photos, int commentCount, int likeCount, String content, Date timeCreated, Date timeUpdated) {
        this.feedId = feedId;
        mFeedUser = feedUser;
        this.photos = photos;
        this.commentCount = commentCount;
        this.likeCount = likeCount;
        this.content = content;
        this.timeCreated = timeCreated;
        this.timeUpdated = timeUpdated;
    }

    public String getFeedId() {
        return feedId;
    }

    public void setFeedId(String feedId) {
        this.feedId = feedId;
    }

    public FeedUser getFeedUser() {
        return mFeedUser;
    }

    public void setFeedUser(FeedUser feedUser) {
        mFeedUser = feedUser;
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

    @Override
    public String toString() {
        return "Feed{" +
                "feedId='" + feedId + '\'' +
                ", mFeedUser=" + mFeedUser +
                ", photos=" + photos +
                ", commentCount=" + commentCount +
                ", likeCount=" + likeCount +
                ", timeCreated=" + timeCreated +
                ", timeUpdated=" + timeUpdated +
                '}';
    }
}
