package com.petnbu.petnbu.model

import android.arch.persistence.room.*
import com.google.firebase.firestore.Exclude
import com.petnbu.petnbu.db.ListPhotoConverters
import com.petnbu.petnbu.db.PetTypeConverters
import java.util.*

@Entity(tableName = "feeds")
@TypeConverters(value = [(ListPhotoConverters::class), (PetTypeConverters::class)])
class FeedEntity {
    @PrimaryKey var feedId: String = ""
    @ForeignKey(entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["fromUserId"])
    var fromUserId: String = ""
    lateinit var photos: MutableList<Photo>
    var commentCount: Int = 0
    var latestCommentId: String? = null
    var likeCount: Int = 0
    @Exclude
    @get:Exclude
    var isLiked: Boolean = false
    var content: String = ""
    var timeCreated: Date? = null
    var timeUpdated: Date? = null

    @LocalStatus.LOCAL_STATUS
    @get:LocalStatus.LOCAL_STATUS
    @get:Exclude
    var status: Int = 0

    @get:Exclude
    var likeInProgress: Boolean = false

    lateinit var pagingIds: MutableList<String>

    constructor()

    @Ignore
    constructor(feedId: String, fromUserId: String, photos: MutableList<Photo>, commentCount: Int,
                latestCommentId: String?, likeCount: Int, isLiked: Boolean, content: String,
                timeCreated: Date?, timeUpdated: Date?, status: Int, likeInProgress: Boolean,
                pagingIds : MutableList<String>) {
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
        this.pagingIds = pagingIds
    }
}
