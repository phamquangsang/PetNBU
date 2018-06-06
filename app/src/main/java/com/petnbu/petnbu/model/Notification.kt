package com.petnbu.petnbu.model

import android.support.annotation.IntDef

import com.google.firebase.firestore.ServerTimestamp

import java.util.Date


data class Notification(var id: String) {
    lateinit var targetUserId: String
    lateinit var fromUser: FeedUser
    var targetFeedId: String? = null
    var targetCommentId: String? = null
    var targetReplyId: String? = null
    @NotificationType
    var type: Int = 0
    @ServerTimestamp
    var timeCreated: Date? = null

    var isRead: Boolean = false

    constructor():this("")

    constructor(id: String, targetUserId: String, fromUser: FeedUser, targetFeedId: String?,
                targetCommentId: String?, targetReplyId: String?, type: Int, timeCreated: Date?,
                isRead: Boolean):this(id) {
        this.targetUserId = targetUserId
        this.fromUser = fromUser
        this.targetFeedId = targetFeedId
        this.targetCommentId = targetCommentId
        this.targetReplyId = targetReplyId
        this.type = type
        this.timeCreated = timeCreated
        this.isRead = isRead
    }

    fun toEntity(): NotificationEntity {
        return NotificationEntity(id, targetUserId, fromUser.userId, targetFeedId, targetCommentId,
                targetReplyId, type, timeCreated ?: Date(), isRead)
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [TYPE_LIKE_FEED, TYPE_LIKE_COMMENT, TYPE_LIKE_REPLY, TYPE_NEW_COMMENT, TYPE_NEW_REPLY])
    annotation class NotificationType

    companion object {

        const val TYPE_LIKE_FEED = 1
        const val TYPE_LIKE_COMMENT = 2
        const val TYPE_LIKE_REPLY = 3
        const val TYPE_NEW_COMMENT = 4
        const val TYPE_NEW_REPLY = 5
    }

}
