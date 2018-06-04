package com.petnbu.petnbu.db

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import com.petnbu.petnbu.model.Comment
import com.petnbu.petnbu.model.CommentEntity
import com.petnbu.petnbu.model.CommentUI
import timber.log.Timber

@Dao
abstract class CommentDao {

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(vararg comments: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertIfNotExists(vararg comments: CommentEntity)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(comments: List<CommentEntity>)

    fun insertFromComment(comment: Comment?) {
        Timber.i("insertComment: %s", comment)
        comment?.toEntity()?.let { insert(it) }
    }

    @Transaction
    open fun insertListComment(comments: List<Comment>) {
        comments.forEach({ insertFromComment(it) })
    }

    @Query("SELECT * FROM comments where id = :commentId")
    abstract fun getCommentById(commentId: String): CommentEntity?

    @Query("SELECT comments.id, users.userId, users.avatar, users.name, comments.content, " +
            "comments.photo, comments.likeCount, comments.isLiked, comments.likeInProgress, comments.commentCount, comments.parentCommentId, " +
            "comments.parentFeedId, comments.localStatus, comments.timeCreated, " +
            "subComments.id as latestCommentId, subcomments.content as latestCommentContent, " +
            "subCommentUser.userId as latestCommentOwnerId, subCommentUser.name as latestCommentOwnerName, " +
            "subCommentUser.avatar as latestCommentOwnerAvatar, subComments.photo as latestCommentPhoto " +
            "from comments " +
            "left join users on comments.ownerId = users.userId " +
            "left join comments as subComments on comments.latestCommentId = subComments.id " +
            "left join users as subCommentUser on subComments.ownerId = subCommentUser.userId " +
            "where comments.id in (:ids) or (comments.parentFeedId = :feedId and comments.localStatus = 1)" +
            "order by comments.timeCreated DESC")
    abstract fun loadFeedComments(ids: List<String>, feedId: String): LiveData<List<CommentUI>>

    //sub comments does not have latestComment fields
    @Query("SELECT comments.id, users.userId, users.avatar, users.name, comments.content, " +
            "comments.photo, comments.likeCount, comments.isLiked, comments.likeInProgress, comments.commentCount, comments.parentCommentId, " +
            "comments.parentFeedId, comments.localStatus, comments.timeCreated " +
            "from comments " +
            "left join users on comments.ownerId = users.userId " +
            "where comments.id in (:ids) or (comments.parentCommentId = :parentCommentId and comments.localStatus = 1)" +
            "order by comments.timeCreated DESC")
    abstract fun loadSubComments(ids: List<String>, parentCommentId: String): LiveData<List<CommentUI>>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract fun update(commentEntity: CommentEntity)

    @Query("UPDATE comments set id = :newId where id = :oldId")
    abstract fun updateCommentId(oldId: String, newId: String)
}
