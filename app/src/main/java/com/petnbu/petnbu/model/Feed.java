package com.petnbu.petnbu.model;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import com.petnbu.petnbu.db.PetTypeConverters;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@Entity(tableName = "feeds")
@TypeConverters(PetTypeConverters.class)
public class Feed implements Parcelable {

    @Retention(SOURCE)
    @IntDef(value = {STATUS_NEW, STATUS_UPLOADING, STATUS_ERROR, STATUS_DONE})
    public @interface LOCAL_STATUS{}

    public static final int STATUS_NEW = 0;
    public static final int STATUS_UPLOADING = 1;
    public static final int STATUS_ERROR = 2;
    public static final int STATUS_DONE = 3;

    @PrimaryKey @NonNull
    private String feedId;
    @Embedded
    private FeedUser mFeedUser;
    private List<Photo> photos;
    private int commentCount;
    private int likeCount;
    private String content;
    @ServerTimestamp private Date timeCreated;
    @ServerTimestamp private Date timeUpdated;

    @LOCAL_STATUS
    @Exclude
    private int status;

    @Exclude
    private boolean likeInProgress;

    public Feed() {
    }

    @Ignore
    public Feed(String feedId, FeedUser feedUser, List<Photo> photos, int commentCount, int likeCount, String content, Date timeCreated, Date timeUpdated) {
        this.feedId = feedId;
        mFeedUser = feedUser;
        this.photos = photos;
        this.commentCount = commentCount;
        this.likeCount = likeCount;
        this.content = content;
        this.timeCreated = timeCreated;
        this.timeUpdated = timeUpdated;
        status = STATUS_NEW;
        this.likeInProgress = false;
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

    @Exclude
    public boolean isLikeInProgress() {
        return likeInProgress;
    }

    public void setLikeInProgress(boolean likeInProgress) {
        this.likeInProgress = likeInProgress;
    }

    @LOCAL_STATUS
    @Exclude
    public int getStatus() {
        return status;
    }

    public void setStatus(@LOCAL_STATUS int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Feed{" +
                "feedId='" + feedId + '\'' +
                ", mFeedUser=" + mFeedUser +
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

        Feed feed = (Feed) o;

        if (getCommentCount() != feed.getCommentCount()) return false;
        if (getLikeCount() != feed.getLikeCount()) return false;
        if (getStatus() != feed.getStatus()) return false;
        if (!getFeedId().equals(feed.getFeedId())) return false;
        if (!getFeedUser().equals(feed.getFeedUser())) return false;
        if (getPhotos() != null ? !getPhotos().equals(feed.getPhotos()) : feed.getPhotos() != null)
            return false;
        if (getContent() != null ? !getContent().equals(feed.getContent()) : feed.getContent() != null)
            return false;
        if (!getTimeCreated().equals(feed.getTimeCreated())) return false;
        return getTimeUpdated().equals(feed.getTimeUpdated());
    }

    @Override
    public int hashCode() {
        int result = getFeedId().hashCode();
        result = 31 * result + getFeedUser().hashCode();
        result = 31 * result + (getPhotos() != null ? getPhotos().hashCode() : 0);
        result = 31 * result + getCommentCount();
        result = 31 * result + getLikeCount();
        result = 31 * result + (getContent() != null ? getContent().hashCode() : 0);
        result = 31 * result + getTimeCreated().hashCode();
        result = 31 * result + getTimeUpdated().hashCode();
        result = 31 * result + getStatus();
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.feedId);
        dest.writeParcelable(this.mFeedUser, flags);
        dest.writeTypedList(this.photos);
        dest.writeInt(this.commentCount);
        dest.writeInt(this.likeCount);
        dest.writeString(this.content);
        dest.writeLong(this.timeCreated != null ? this.timeCreated.getTime() : -1);
        dest.writeLong(this.timeUpdated != null ? this.timeUpdated.getTime() : -1);
        dest.writeInt(this.status);
    }

    protected Feed(Parcel in) {
        this.feedId = in.readString();
        this.mFeedUser = in.readParcelable(getClass().getClassLoader());
        this.photos = in.createTypedArrayList(Photo.CREATOR);
        this.commentCount = in.readInt();
        this.likeCount = in.readInt();
        this.content = in.readString();
        long tmpTimeCreated = in.readLong();
        this.timeCreated = tmpTimeCreated == -1 ? null : new Date(tmpTimeCreated);
        long tmpTimeUpdated = in.readLong();
        this.timeUpdated = tmpTimeUpdated == -1 ? null : new Date(tmpTimeUpdated);
        this.status = in.readInt();
    }

    public static final Creator<Feed> CREATOR = new Creator<Feed>() {
        @Override
        public Feed createFromParcel(Parcel source) {
            return new Feed(source);
        }

        @Override
        public Feed[] newArray(int size) {
            return new Feed[size];
        }
    };
}
