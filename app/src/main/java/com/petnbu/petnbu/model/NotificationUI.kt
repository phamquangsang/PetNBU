package com.petnbu.petnbu.model

import android.arch.persistence.room.Embedded

import com.google.firebase.firestore.ServerTimestamp

import java.util.Date

data class NotificationUI (var id: String){
    var targetUserId: String? = null
    @Embedded
    var fromUser: FeedUser? = null
    var targetFeedId: String? = null
    var targetCommentId: String? = null
    var targetReplyId: String? = null
    @Notification.NotificationType
    @get:Notification.NotificationType
    var type: Int = 0
    @ServerTimestamp
    var timeCreated: Date? = null

    var isRead: Boolean = false
}
