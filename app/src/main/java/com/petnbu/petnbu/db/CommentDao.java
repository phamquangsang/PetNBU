package com.petnbu.petnbu.db;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;
import android.support.annotation.Nullable;

import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.CommentEntity;
import com.petnbu.petnbu.model.CommentUI;

import java.util.List;

import timber.log.Timber;

@Dao
public abstract class CommentDao {

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(CommentEntity... comments);

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(List<CommentEntity> comments);

    public void insertFromComment(@Nullable Comment comment) {
        if (comment != null) {
            Timber.i("insertComment: %s", comment);
            insert(comment.toEntity());
        }
    }

    @Transaction
    public void insertListComment(List<Comment> comments){
        for (Comment comment : comments) {
            insertFromComment(comment);
        }
    }

    @Query("SELECT * FROM comments where id = :commentId")
    public abstract CommentEntity getCommentById(String commentId);

    @Query("SELECT comments.id, users.userId, users.avatar, users.name, comments.content, " +
            "comments.photo, comments.likeCount, comments.commentCount, comments.parentCommentId, " +
            "comments.parentFeedId, comments.localStatus, comments.timeCreated, " +
            "subComments.id as latestCommentId, subcomments.content as latestCommentContent, " +
            "subCommentUser.userId as latestCommentOwnerId, subCommentUser.name as latestCommentOwnerName, " +
            "subCommentUser.avatar as latestCommentOwnerAvatar " +
            "from comments " +
            "left join users on comments.ownerId = users.userId " +
            "left join comments as subComments on comments.latestCommentId = subComments.id " +
            "left join users as subCommentUser on subComments.ownerId = subCommentUser.userId " +
            "where comments.id in (:ids) " +
            "order by comments.timeCreated DESC")
    public abstract LiveData<List<CommentUI>> loadCommentsIncludeUploading(List<String> ids);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public abstract void update(CommentEntity commentEntity);

    @Query("UPDATE comments set id = :newId where id = :oldId")
    public abstract void updateCommentId(String oldId, String newId);
}
