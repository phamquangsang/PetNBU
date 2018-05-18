package com.petnbu.petnbu.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.support.annotation.NonNull;

@Entity(tableName = "feed_comments", primaryKeys = {"feedId", "commentId"})
public class FeedCommentEntity {

    @ForeignKey(entity = FeedEntity.class, parentColumns = {"feedId"}, childColumns = {"feedId"})
    @NonNull
    private String feedId;

    @NonNull
    private String commentId;

    @NonNull
    public String getFeedId() {
        return feedId;
    }

    public void setFeedId(@NonNull String feedId) {
        this.feedId = feedId;
    }

    @NonNull
    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(@NonNull String commentId) {
        this.commentId = commentId;
    }
}
