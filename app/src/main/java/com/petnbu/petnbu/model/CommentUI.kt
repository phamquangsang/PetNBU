package com.petnbu.petnbu.model

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.TypeConverters
import com.google.firebase.firestore.Exclude
import com.petnbu.petnbu.db.PhotoConverters
import java.util.*

@TypeConverters(PhotoConverters::class)
data class CommentUI(
        val id: String ,
        @Embedded
        val owner: FeedUser,
        val content: String = "",
        val photo: Photo? = null,
        val likeCount: Int = 0,
        @Exclude
        @get:Exclude
        val isLiked: Boolean = false,
        @Exclude
        @get:Exclude
        val likeInProgress: Boolean = false,
        val commentCount: Int = 0,
        val parentCommentId: String = "",
        val parentFeedId: String = "",

        @Exclude
        @LocalStatus.LOCAL_STATUS
        val localStatus: Int = 0,
        val timeCreated: Date,

        val latestCommentId: String? = null,
        val latestCommentContent: String? = null,
        val latestCommentOwnerId: String? = null,
        val latestCommentOwnerName: String? = null,
        val latestCommentOwnerAvatar: Photo? = null,
        val latestCommentPhoto: Photo? = null
)

fun CommentUI.isUploading() = LocalStatus.STATUS_UPLOADING == localStatus