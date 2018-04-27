package com.petnbu.petnbu.model;

import android.os.Parcel;
import android.os.Parcelable;

public class FeedUser implements Parcelable {
    private String userId;
    private String photoUrl;
    private String displayName;

    public FeedUser() {
    }

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

    public static final Parcelable.Creator<FeedUser> CREATOR = new Parcelable.Creator<FeedUser>() {
        @Override
        public FeedUser createFromParcel(Parcel source) {
            return new FeedUser(source);
        }

        @Override
        public FeedUser[] newArray(int size) {
            return new FeedUser[size];
        }
    };
}
