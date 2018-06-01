package com.petnbu.petnbu.model

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.TypeConverters

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import com.petnbu.petnbu.db.PhotoConverters

import java.util.Date

@TypeConverters(PhotoConverters::class)
data class CommentUI(
        var id: String = "",
        @Embedded
        var owner: FeedUser? = null,
        var content: String? = null,
        var photo: Photo? = null,
        var likeCount: Int = 0,
        @Exclude
        @get:Exclude
        var isLiked: Boolean = false,
        @Exclude
        @get:Exclude
        var likeInProgress: Boolean = false,
        var commentCount: Int = 0,
        var parentCommentId: String? = null,
        var parentFeedId: String? = null,

        @Exclude
        @LocalStatus.LOCAL_STATUS
        var localStatus: Int = 0,
        @ServerTimestamp
        var timeCreated: Date? = null,

        var latestCommentId: String? = null,
        var latestCommentContent: String? = null,
        var latestCommentOwnerId: String? = null,
        var latestCommentOwnerName: String? = null,
        var latestCommentOwnerAvatar: Photo? = null,
        var latestCommentPhoto: Photo? = null
)
