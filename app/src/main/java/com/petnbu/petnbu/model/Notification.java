package com.petnbu.petnbu.model;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class Notification {

    @Retention(SOURCE)
    @IntDef(value = {TYPE_LIKE_FEED, TYPE_LIKE_COMMENT, TYPE_LIKE_REPLY, TYPE_NEW_COMMENT, TYPE_NEW_REPLY})
    public @interface NotificationType { }

    public static final int TYPE_LIKE_FEED = 1;
    public static final int TYPE_LIKE_COMMENT = 2;
    public static final int TYPE_LIKE_REPLY = 3;
    public static final int TYPE_NEW_COMMENT = 4;
    public static final int TYPE_NEW_REPLY = 5;


    @NonNull
    private String id;
    private FeedUser targetUser;
    private FeedUser fromUser;
    @NotificationType
    private int type;
    private long timeCreated;

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public FeedUser getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(FeedUser targetUser) {
        this.targetUser = targetUser;
    }

    public FeedUser getFromUser() {
        return fromUser;
    }

    public void setFromUser(FeedUser fromUser) {
        this.fromUser = fromUser;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }
}
