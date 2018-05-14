package com.petnbu.petnbu.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Ignore;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class FeedUser implements Parcelable {
    private String userId;
    private String photoUrl;
    private String displayName;

    public FeedUser() {
    }

    @Ignore
    public FeedUser(String userId, String photoUrl, String displayName) {
        this.userId = userId;
        this.photoUrl = photoUrl;
        this.displayName = displayName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return "FeedUser{" +
                "userId='" + userId + '\'' +
                ", photoUrl='" + photoUrl + '\'' +
                ", displayName='" + displayName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeedUser feedUser = (FeedUser) o;

        if (!getUserId().equals(feedUser.getUserId())) return false;
        if (getPhotoUrl() != null ? !getPhotoUrl().equals(feedUser.getPhotoUrl()) : feedUser.getPhotoUrl() != null)
            return false;
        return getDisplayName() != null ? getDisplayName().equals(feedUser.getDisplayName()) : feedUser.getDisplayName() == null;
    }

    @Override
    public int hashCode() {
        int result = getUserId().hashCode();
        result = 31 * result + (getPhotoUrl() != null ? getPhotoUrl().hashCode() : 0);
        result = 31 * result + (getDisplayName() != null ? getDisplayName().hashCode() : 0);
        return result;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.userId);
        dest.writeString(this.photoUrl);
        dest.writeString(this.displayName);
    }

    protected FeedUser(Parcel in) {
        this.userId = in.readString();
        this.photoUrl = in.readString();
        this.displayName = in.readString();
    }

    public static final Creator<FeedUser> CREATOR = new Creator<FeedUser>() {
        @Override
        public FeedUser createFromParcel(Parcel source) {
            return new FeedUser(source);
        }

        @Override
        public FeedUser[] newArray(int size) {
            return new FeedUser[size];
        }
    };

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("photoUrl", photoUrl);
        result.put("displayName", displayName);
        return result;
    }
}
