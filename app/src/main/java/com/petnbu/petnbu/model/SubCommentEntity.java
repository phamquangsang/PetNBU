package com.petnbu.petnbu.model;

import android.arch.persistence.room.Entity;
import android.support.annotation.NonNull;

@Entity(tableName = "sub_comments", primaryKeys = {"parentId", "subCommentId"})
public class SubCommentEntity {

    @NonNull
    private String parentId;

    @NonNull
    private String subCommentId;

    public SubCommentEntity() {
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getSubCommentId() {
        return subCommentId;
    }

    public void setSubCommentId(String subCommentId) {
        this.subCommentId = subCommentId;
    }
}
