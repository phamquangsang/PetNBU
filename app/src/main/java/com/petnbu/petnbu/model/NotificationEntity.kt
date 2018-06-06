package com.petnbu.petnbu.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey

import com.google.firebase.firestore.ServerTimestamp

import java.util.Date

@Entity(tableName = "notifications")
class NotificationEntity {

    @PrimaryKey
    var id: String = ""
    var targetUserId: String? = null
    @ForeignKey(entity = UserEntity::class, parentColumns = ["userId"], childColumns = ["fromUserId"])
    var fromUserId: String = ""
    var targetFeedId: String? = null
    var targetCommentId: String? = null
    var targetReplyId: String? = null
    @Notification.NotificationType
    @get:Notification.NotificationType
    var type: Int = 0
    @ServerTimestamp
    var timeCreated: Date? = null

    var isRead: Boolean = false

    constructor() {}

    @Ignore
    constructor(id: String, targetUserId: String, fromUserId: String,
                targetFeedId: String?, targetCommentId: String?,
                targetReplyId: String?, type: Int, timeCreated: Date, isRead: Boolean) {
        this.id = id
        this.targetUserId = targetUserId
        this.fromUserId = fromUserId
        this.targetFeedId = targetFeedId
        this.targetCommentId = targetCommentId
        this.targetReplyId = targetReplyId
        this.type = type
        this.timeCreated = timeCreated
        this.isRead = isRead
    }
}
