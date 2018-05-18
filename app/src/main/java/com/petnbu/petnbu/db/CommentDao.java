package com.petnbu.petnbu.db;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.support.annotation.Nullable;

import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.CommentEntity;

import java.util.List;

import timber.log.Timber;

@Dao
public abstract class CommentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(CommentEntity... comments);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(List<CommentEntity> comments);

    public void insertFromComment(@Nullable Comment comment) {
        if (comment != null) {
            Timber.i("insertComment: %s", comment);
            insert(comment.toEntity());
        }
    }

    @Query("SELECT * FROM comments where id = :commentId")
    public abstract CommentEntity getCommentById(String commentId);
}
