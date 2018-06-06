package com.petnbu.petnbu.model

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.TypeConverters

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import com.petnbu.petnbu.db.ListPhotoConverters

import java.util.Date

data class Feed(var feedId: String) {
    @Embedded
    lateinit var feedUser: FeedUser
    @TypeConverters(ListPhotoConverters::class)
    lateinit var photos: MutableList<Photo>
    var commentCount: Int = 0
    var likeCount: Int = 0
    var isLiked: Boolean = false
    var content: String = ""
    @Ignore
    var latestComment: Comment? = null
    @ServerTimestamp
    var timeCreated: Date? = null
    @ServerTimestamp
    var timeUpdated: Date? = null

    @LocalStatus.LOCAL_STATUS
    @Exclude
    @get:LocalStatus.LOCAL_STATUS
    @get:Exclude
    var status: Int = 0

    @Exclude
    @get:Exclude
    var likeInProgress: Boolean = false

    constructor() : this("")

    constructor(feedId: String, feedUser: FeedUser, photos: MutableList<Photo>, commentCount: Int,
                likeCount: Int, isLiked: Boolean, content: String, latestComment: Comment?,
                timeCreated: Date?, timeUpdated: Date?, status: Int, likeInProgress: Boolean) : this(feedId) {
        this.feedUser = feedUser
        this.photos = photos
        this.commentCount = commentCount
        this.likeCount = likeCount
        this.isLiked = isLiked
        this.content = content
        this.latestComment = latestComment
        this.timeCreated = timeCreated
        this.timeUpdated = timeUpdated
        this.status = status
        this.likeInProgress = likeInProgress
    }


    fun toEntity(): FeedEntity {
        return FeedEntity(feedId, feedUser.userId,
                photos, commentCount, if (latestComment == null) null else latestComment?.id, likeCount, isLiked, content,
                timeCreated, timeUpdated, status, likeInProgress)
    }


}
