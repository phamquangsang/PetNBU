package com.petnbu.petnbu.model

import android.arch.persistence.room.TypeConverters
import com.petnbu.petnbu.db.ListPhotoConverters
import com.petnbu.petnbu.db.PhotoConverters
import java.util.*

@TypeConverters(value = arrayOf(ListPhotoConverters::class, PhotoConverters::class))
data class FeedUI (val feedId: String,
                   val ownerId: String,
    val name: String? = null,
    val avatar: Photo? = null,
    val photos: MutableList<Photo>? = null,
    val timeCreated: Date? = null,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val likeInProgress: Boolean = false,
    val commentCount: Int = 0,
    val feedContent: String? = null,
    val latestCommentId: String? = null,
    val commentOwnerId: String? = null,
    val commentOwnerName: String? = null,
    val commentUserAvatar: Photo? = null,
    val commentContent: String? = null,
    val commentPhoto: Photo? = null,
    val status: Int = 0)


