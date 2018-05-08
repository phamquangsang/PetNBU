package com.petnbu.petnbu.model;

import android.arch.persistence.room.TypeConverters;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import com.petnbu.petnbu.db.PetTypeConverters;

import java.util.Date;
import java.util.List;

@TypeConverters(value = PetTypeConverters.class)
public class FeedResponse implements Parcelable {

    private String feedId;
    private FeedUser feedUser;
    private List<Photo> photos;
    private int commentCount;
    private int likeCount;
    private String content;
    @ServerTimestamp private Date timeCreated;
    @ServerTimestamp private Date timeUpdated;

    @FeedEntity.LOCAL_STATUS
    @Exclude
    private int status;

    @Exclude
    private boolean likeInProgress;

    public FeedResponse() {
    }

    public FeedResponse(String feedId, FeedUser feedUser, List<Photo> photos, int commentCount, int likeCount, String content, Date timeCreated, Date timeUpdated, int status) {
        this.feedId = feedId;
        this.feedUser = feedUser;
        this.photos = photos;
        this.commentCount = commentCount;
        this.likeCount = likeCount;
        this.content = content;
        this.timeCreated = timeCreated;
        this.timeUpdated = timeUpdated;
        this.status = status;
        this.likeInProgress = false;
    }

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
    public String toString() {
        return "FeedResponse{" +
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

        FeedResponse feedResponse = (FeedResponse) o;

        if (commentCount != feedResponse.commentCount) return false;
        if (likeCount != feedResponse.likeCount) return false;
        if (status != feedResponse.status) return false;
        if (likeInProgress != feedResponse.likeInProgress) return false;
        if (!feedId.equals(feedResponse.feedId)) return false;
        if (feedUser != null ? !feedUser.equals(feedResponse.feedUser) : feedResponse.feedUser != null)
            return false;
        if (photos != null ? !photos.equals(feedResponse.photos) : feedResponse.photos != null) return false;
        if (content != null ? !content.equals(feedResponse.content) : feedResponse.content != null) return false;
        if (timeCreated != null ? !timeCreated.equals(feedResponse.timeCreated) : feedResponse.timeCreated != null)
            return false;
        return timeUpdated != null ? timeUpdated.equals(feedResponse.timeUpdated) : feedResponse.timeUpdated == null;
    }

    @Override
    public int hashCode() {
        int result = feedId.hashCode();
        result = 31 * result + (feedUser != null ? feedUser.hashCode() : 0);
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.feedId);
        dest.writeParcelable(this.feedUser, flags);
        dest.writeTypedList(this.photos);
        dest.writeInt(this.commentCount);
        dest.writeInt(this.likeCount);
        dest.writeString(this.content);
        dest.writeLong(this.timeCreated != null ? this.timeCreated.getTime() : -1);
        dest.writeLong(this.timeUpdated != null ? this.timeUpdated.getTime() : -1);
        dest.writeInt(this.status);
        dest.writeByte(this.likeInProgress ? (byte) 1 : (byte) 0);
    }

    protected FeedResponse(Parcel in) {
        this.feedId = in.readString();
        this.feedUser = in.readParcelable(FeedUser.class.getClassLoader());
        this.photos = in.createTypedArrayList(Photo.CREATOR);
        this.commentCount = in.readInt();
        this.likeCount = in.readInt();
        this.content = in.readString();
        long tmpTimeCreated = in.readLong();
        this.timeCreated = tmpTimeCreated == -1 ? null : new Date(tmpTimeCreated);
        long tmpTimeUpdated = in.readLong();
        this.timeUpdated = tmpTimeUpdated == -1 ? null : new Date(tmpTimeUpdated);
        this.status = in.readInt();
        this.likeInProgress = in.readByte() != 0;
    }

    public static final Creator<FeedResponse> CREATOR = new Creator<FeedResponse>() {
        @Override
        public FeedResponse createFromParcel(Parcel source) {
            return new FeedResponse(source);
        }

        @Override
        public FeedResponse[] newArray(int size) {
            return new FeedResponse[size];
        }
    };

    public FeedEntity toEntity(){
        return new FeedEntity(getFeedId(), getFeedUser().getUserId(),
                getPhotos(), getCommentCount(), getLikeCount(), getContent(),
                getTimeCreated(), getTimeUpdated(), getStatus(), isLikeInProgress());
    }
}
