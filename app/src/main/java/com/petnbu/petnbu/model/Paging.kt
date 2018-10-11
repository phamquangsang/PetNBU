package com.petnbu.petnbu.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters

import com.petnbu.petnbu.db.PetTypeConverters

@Entity(tableName = "paging")
@TypeConverters(PetTypeConverters::class)
class Paging {

    @PrimaryKey
    lateinit var pagingId: String

    private var ids: MutableList<String>? = null

    var isEnded: Boolean = false

    var oldestId: String? = null

    constructor()

    @Ignore
    constructor(pagingId: String, ids: MutableList<String>?, ended: Boolean, oldestId: String?) {
        this.pagingId = pagingId
        this.ids = ids
        this.isEnded = ended
        this.oldestId = oldestId
    }

    fun getIds() = ids

    fun setIds(ids: MutableList<String>) {
        this.ids = ids
    }

    companion object {

        val GLOBAL_FEEDS_PAGING_ID = "global-feeds-paging-id-feeds"

        val NOTIFICATION_PAGING_ID = "notifcation-paging-id"

        fun notificationsPagingId(): String {
            return NOTIFICATION_PAGING_ID
        }

        fun feedCommentsPagingId(feedId: String): String {
            return "$feedId-comments"
        }

        fun subCommentsPagingId(parentCmtId: String): String {
            return "$parentCmtId-subComments"
        }

        fun userFeedsPagingId(userId: String): String {
            return "$userId-feeds"
        }

        fun isFeedPagingId(pagingId: String): Boolean {
            return pagingId.endsWith("feeds")
        }

        fun isCommentPagingId(pagingId: String): Boolean {
            return pagingId.endsWith("comments") || pagingId.endsWith("subComments")
        }
    }
}
