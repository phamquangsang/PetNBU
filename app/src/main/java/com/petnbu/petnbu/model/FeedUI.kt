package com.petnbu.petnbu.model

import android.arch.persistence.room.TypeConverters
import com.petnbu.petnbu.db.ListPhotoConverters
import com.petnbu.petnbu.db.PhotoConverters
import java.util.*

@TypeConverters(value = arrayOf(ListPhotoConverters::class, PhotoConverters::class))
class FeedUI {
    lateinit var feedId: String
    lateinit var ownerId: String
    var name: String? = null
    var avatar: Photo? = null
    var photos: MutableList<Photo>? = null
    var timeCreated: Date? = null
    var likeCount: Int = 0
    var isLiked: Boolean = false
    var likeInProgress: Boolean = false
    var commentCount: Int = 0
    var feedContent: String? = null
    var latestCommentId: String? = null
    var commentOwnerId: String? = null
    var commentOwnerName: String? = null
    var commentUserAvatar: Photo? = null
    var commentContent: String? = null
    var commentPhoto: Photo? = null
    var status: Int = 0

    override fun toString(): String {
        return "FeedUI{" +
                "feedId='" + feedId + '\''.toString() +
                ", ownerId='" + ownerId + '\''.toString() +
                ", name='" + name + '\''.toString() +
                ", avatar=" + avatar +
                ", photos=" + photos +
                ", timeCreated=" + timeCreated +
                ", likeCount=" + likeCount +
                ", isLiked=" + isLiked +
                ", likeInProgress=" + likeInProgress +
                ", commentCount=" + commentCount +
                ", feedContent='" + feedContent + '\''.toString() +
                ", latestCommentId='" + latestCommentId + '\''.toString() +
                ", commentOwnerId='" + commentOwnerId + '\''.toString() +
                ", commentOwnerName='" + commentOwnerName + '\''.toString() +
                ", commentUserAvatar=" + commentUserAvatar +
                ", commentContent='" + commentContent + '\''.toString() +
                ", commentPhoto=" + commentPhoto +
                ", status=" + status +
                '}'.toString()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val feedUI = o as FeedUI?

        if (likeCount != feedUI!!.likeCount) return false
        if (isLiked != feedUI.isLiked) return false
        if (likeInProgress != feedUI.likeInProgress) return false
        if (commentCount != feedUI.commentCount) return false
        if (status != feedUI.status) return false
        if (feedId != feedUI.feedId) return false
        if (ownerId != feedUI.ownerId) return false
        if (if (name != null) name != feedUI.name else feedUI.name != null) return false
        if (if (avatar != null) avatar != feedUI.avatar else feedUI.avatar != null) return false
        if (if (photos != null) photos != feedUI.photos else feedUI.photos != null) return false
        if (if (timeCreated != null) timeCreated != feedUI.timeCreated else feedUI.timeCreated != null)
            return false
        if (if (feedContent != null) feedContent != feedUI.feedContent else feedUI.feedContent != null)
            return false
        if (if (latestCommentId != null) latestCommentId != feedUI.latestCommentId else feedUI.latestCommentId != null)
            return false
        if (if (commentOwnerId != null) commentOwnerId != feedUI.commentOwnerId else feedUI.commentOwnerId != null)
            return false
        if (if (commentOwnerName != null) commentOwnerName != feedUI.commentOwnerName else feedUI.commentOwnerName != null)
            return false
        if (if (commentUserAvatar != null) commentUserAvatar != feedUI.commentUserAvatar else feedUI.commentUserAvatar != null)
            return false
        if (if (commentContent != null) commentContent != feedUI.commentContent else feedUI.commentContent != null)
            return false
        return if (commentPhoto != null) commentPhoto == feedUI.commentPhoto else feedUI.commentPhoto == null
    }

    override fun hashCode(): Int {
        var result = feedId!!.hashCode()
        result = 31 * result + ownerId!!.hashCode()
        result = 31 * result + if (name != null) name!!.hashCode() else 0
        result = 31 * result + if (avatar != null) avatar!!.hashCode() else 0
        result = 31 * result + if (photos != null) photos!!.hashCode() else 0
        result = 31 * result + if (timeCreated != null) timeCreated!!.hashCode() else 0
        result = 31 * result + likeCount
        result = 31 * result + if (isLiked) 1 else 0
        result = 31 * result + if (likeInProgress) 1 else 0
        result = 31 * result + commentCount
        result = 31 * result + if (feedContent != null) feedContent!!.hashCode() else 0
        result = 31 * result + if (latestCommentId != null) latestCommentId!!.hashCode() else 0
        result = 31 * result + if (commentOwnerId != null) commentOwnerId!!.hashCode() else 0
        result = 31 * result + if (commentOwnerName != null) commentOwnerName!!.hashCode() else 0
        result = 31 * result + if (commentUserAvatar != null) commentUserAvatar!!.hashCode() else 0
        result = 31 * result + if (commentContent != null) commentContent!!.hashCode() else 0
        result = 31 * result + if (commentPhoto != null) commentPhoto!!.hashCode() else 0
        result = 31 * result + status
        return result
    }
}


