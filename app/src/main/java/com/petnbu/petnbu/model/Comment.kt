package com.petnbu.petnbu.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

import java.util.Date
import java.util.HashMap

data class Comment(
        var id: String) {
    lateinit var feedUser: FeedUser
    var content: String = ""
    var photo: Photo? = null
    var likeCount: Int = 0
    @Exclude
    @get:Exclude
    var isLiked: Boolean = false
    @Exclude
    @get:Exclude
    var likeInProgress: Boolean = false
    var commentCount: Int = 0
    var latestComment: Comment? = null
    var parentCommentId: String? = null
    var parentFeedId: String? = null

    @Exclude
    @LocalStatus.LOCAL_STATUS
    @get:Exclude
    @get:LocalStatus.LOCAL_STATUS
    var localStatus: Int = 0

    @ServerTimestamp
    lateinit var timeCreated: Date
    @ServerTimestamp
    lateinit var timeUpdated: Date

    constructor() : this("")
    constructor(id: String, feedUser: FeedUser, content: String = "", photo: Photo?, likeCount: Int = 0,
                liked: Boolean = false, likeInProgress: Boolean = false, commentCount: Int = 0, latestComment: Comment? = null,
                parentCommentId: String = "", parentFeedId: String = "", localStatus: Int = LocalStatus.STATUS_NEW, timeCreated: Date = Date(),
                timeUpdated: Date = Date()) : this(id) {
        this.feedUser = feedUser
        this.content = content
        this.photo = photo
        this.likeCount = likeCount
        this.isLiked = liked
        this.likeInProgress = likeInProgress
        this.commentCount = commentCount
        this.latestComment = latestComment
        this.parentCommentId = parentCommentId
        this.parentFeedId = parentFeedId
        this.localStatus = localStatus
        this.timeCreated = timeCreated
        this.timeUpdated = timeUpdated
    }


    fun toMap(): MutableMap<String, Any> {
        val map = HashMap<String, Any>()
        map["id"] = id
        map["feedUser"] = feedUser.toMap()
        map["content"] = content
        photo?.run { map["photo"] = toMap() }
        map["likeCount"] = likeCount
        map["commentCount"] = commentCount
        latestComment?.run { map["latestComment"] = toMap() }
        map["parentCommentId"] = parentCommentId ?: ""
        map["parentFeedId"] = parentFeedId ?: ""
        map["timeCreated"] = timeCreated
        map["timeUpdated"] = timeUpdated
        return map
    }

    fun toEntity(): CommentEntity {
        return CommentEntity(id, feedUser.userId, content, photo, likeCount, isLiked, likeInProgress, commentCount,
                parentCommentId ?: "", parentFeedId
                ?: "", if (latestComment == null) null else latestComment!!.id, localStatus, timeCreated, timeUpdated)

    }
}






