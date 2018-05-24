package com.petnbu.petnbu.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import com.google.firebase.firestore.ServerTimestamp;
import com.petnbu.petnbu.db.PetTypeConverters;
import com.petnbu.petnbu.db.PhotoConverters;

import java.util.Date;

@Entity(tableName = "users")
@TypeConverters(PetTypeConverters.class)
public class UserEntity {

    @PrimaryKey @NonNull
    private String userId;

    @TypeConverters(PhotoConverters.class)
    private Photo avatar;

    private String name;

    private String email;

    @ServerTimestamp
    private Date timeCreated;

    @ServerTimestamp
    private Date timeUpdated;

    public UserEntity() {
    }

    @Ignore
    public UserEntity(String userId, Photo avatar, String name, String email, Date timeCreated, Date timeUpdated) {
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

    @Override
    public String toString() {
        return "UserEntity{" +
                "userId='" + userId + '\'' +
                ", avatar=" + avatar +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", timeCreated=" + timeCreated +
                ", timeUpdated=" + timeUpdated +
                '}';
    }
}
