package com.petnbu.petnbu.model;

import android.arch.persistence.room.Ignore;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public class FeedUser {
    @NonNull
    private String userId;
    private Photo avatar;
    private String name;

    public FeedUser() {
    }

    @Ignore
    public FeedUser(@NonNull String userId, Photo avatar, String name) {
        this.userId = userId;
        this.avatar = avatar;
        this.name = name;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    public Photo getAvatar() {
        return avatar;
    }

    public void setAvatar(Photo avatar) {
        this.avatar = avatar;
    }

    public void setAvatarUrl(String originUrl){
        Photo photo = new Photo(originUrl, null, null, null, 0, 0);
        setAvatar(photo);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeedUser feedUser = (FeedUser) o;

        if (!userId.equals(feedUser.userId)) return false;
        if (avatar != null ? !avatar.equals(feedUser.avatar) : feedUser.avatar != null)
            return false;
        return name != null ? name.equals(feedUser.name) : feedUser.name == null;
    }

    @Override
    public int hashCode() {
        int result = userId.hashCode();
        result = 31 * result + (avatar != null ? avatar.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public Map<String, Object> toMap(){
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("avatar", avatar.toMap());
        map.put("name", name);
        return map;
    }

    @Override
    public String toString() {
        return "FeedUser{" +
                "userId='" + userId + '\'' +
                ", avatar=" + avatar +
                ", name='" + name + '\'' +
                '}';
    }
}
