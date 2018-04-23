package com.petnbu.petnbu.model;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class User {

    private String userId;

    private Photo avatar;

    private String name;

    private String email;

    @ServerTimestamp
    private Date timeCreated;

    @ServerTimestamp
    private Date timeUpdated;

    public User() {
    }

    public User(String userId, Photo avatar, String name, String email, Date timeCreated, Date timeUpdated) {
        this.userId = userId;
        this.avatar = avatar;
        this.name = name;
        this.email = email;
        this.timeCreated = timeCreated;
        this.timeUpdated = timeUpdated;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Photo getAvatar() {
        return avatar;
    }

    public void setAvatar(Photo avatar) {
        this.avatar = avatar;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
}
