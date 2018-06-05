package com.petnbu.petnbu.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters

import com.google.firebase.firestore.Exclude
import com.petnbu.petnbu.db.ListPhotoConverters
import com.petnbu.petnbu.db.PetTypeConverters
import com.petnbu.petnbu.db.PhotoConverters

import java.util.Date

@Entity(tableName = "feeds")
@TypeConverters(value = arrayOf(ListPhotoConverters::class, PetTypeConverters::class))
class FeedEntity {


    @PrimaryKey
    lateinit var feedId: String
    @ForeignKey(entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["fromUserId"])
    var fromUserId: String? = null
    var photos: MutableList<Photo>? = null
    var commentCount: Int = 0
    var latestCommentId: String? = null
    var likeCount: Int = 0
    @Exclude
    @get:Exclude
    var isLiked: Boolean = false
    var content: String? = null
    var timeCreated: Date? = null
    var timeUpdated: Date? = null

    @LocalStatus.LOCAL_STATUS
    @get:LocalStatus.LOCAL_STATUS
    @get:Exclude
    var status: Int = 0

    @get:Exclude
    var likeInProgress: Boolean = false

    constructor()

    @Ignore
    constructor(feedId: String, fromUserId: String, photos: MutableList<Photo>, commentCount: Int,
                latestCommentId: String?, likeCount: Int, isLiked: Boolean, content: String,
                timeCreated: Date, timeUpdated: Date, status: Int, likeInProgress: Boolean) {
        this.feedId = feedId
        this.fromUserId = fromUserId
        this.photos = photos
        this.commentCount = commentCount
        this.latestCommentId = latestCommentId
        this.likeCount = likeCount
        this.isLiked = isLiked
        this.content = content
        this.timeCreated = timeCreated
        this.timeUpdated = timeUpdated
        this.status = status
        this.likeInProgress = likeInProgress
    }

}
