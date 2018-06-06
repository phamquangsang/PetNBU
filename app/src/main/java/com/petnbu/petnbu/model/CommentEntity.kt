package com.petnbu.petnbu.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import com.petnbu.petnbu.db.PetTypeConverters
import com.petnbu.petnbu.db.PhotoConverters

import java.util.Date

@Entity(tableName = "comments")
@TypeConverters(PetTypeConverters::class, PhotoConverters::class)
data class CommentEntity(

        @PrimaryKey
        val id: String,
        @ForeignKey(entity = UserEntity::class, parentColumns = arrayOf("userId"), childColumns = arrayOf("userId"))
        val ownerId: String,
        var content: String = "",
        var photo: Photo? = null,
        var likeCount: Int = 0,
        var isLiked: Boolean = false,
        var likeInProgress: Boolean = false,
        var commentCount: Int = 0,
        val parentCommentId: String = "",
        val parentFeedId: String = "",

        var latestCommentId: String? = null,

        @Exclude
        var localStatus: Int = 0,

        @ServerTimestamp
        var timeCreated: Date,
        @ServerTimestamp
        var timeUpdated: Date
)
