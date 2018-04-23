package com.petnbu.petnbu.model;

public class FeedUser {
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
}
