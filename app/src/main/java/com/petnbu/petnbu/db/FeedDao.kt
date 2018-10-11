package com.petnbu.petnbu.db

import android.arch.lifecycle.LiveData
import android.arch.paging.DataSource
import android.arch.persistence.room.*
import com.petnbu.petnbu.model.*
import java.util.*

@Dao
abstract class FeedDao {

    fun insertFromFeed(feed: Feed) {
        insert(feed.toEntity())
    }

    fun insertFromFeedList(listFeed: List<Feed>) {
        val entities = ArrayList<FeedEntity>(listFeed.size)
        listFeed.forEach { entities.add(it.toEntity()) }
        insert(entities)
    }

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(vararg feeds: FeedEntity)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(feedList: List<FeedEntity>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract fun update(feed: FeedEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract fun update(feedList: List<FeedEntity>)


    @Query("UPDATE feeds SET photos = (:photos), content = :content, timeUpdated = :timeUpdated " + "WHERE feedId = :feedId")
    @TypeConverters(ListPhotoConverters::class)
    abstract fun updateContentPhotosFeed(photos: MutableList<Photo>, content: String, feedId: String, timeUpdated: Date)

    @Query("UPDATE feeds SET latestCommentId = :latestCommentId, commentCount = :commentCount " + "WHERE feedId = :feedId")
    @TypeConverters(ListPhotoConverters::class)
    abstract fun updateLatestCommentId(latestCommentId: String, commentCount: Int, feedId: String)

    @Query("UPDATE feeds set status = :status where feedId = :feedId")
    abstract fun updateFeedLocalStatus(status: Int, feedId: String)

    @Query("UPDATE feeds set feedId = :newId where feedId = :feedId")
    abstract fun updateFeedId(feedId: String, newId: String)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract fun updateAll(feeds: List<FeedEntity>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract fun updateAll(vararg feeds: FeedEntity)

    @Delete
    abstract fun deleteFeed(feed: FeedEntity)

    @Query("DELETE FROM feeds WHERE feedId = :feedId")
    abstract fun deleteFeedById(feedId: String)


    @Query("SELECT feedId, feeds.fromUserId as ownerId, feedUsers.name, feedUsers.avatar, feeds.timeCreated, " +
            "feeds.likeCount, feeds.isLiked, feeds.likeInProgress, feeds.commentCount, feeds.content AS feedContent, feeds.status, feeds.photos, " +
            "feeds.latestCommentId, commentUsers.userId as commentOwnerId, commentUsers.name AS commentOwnerName, " +
            "commentUsers.avatar AS commentUserAvatar, comments.content AS commentContent, comments.photo as commentPhoto  " +
            "FROM feeds " +
            "LEFT JOIN users AS feedUsers ON feeds.fromUserId = feedUsers.userId " +
            "LEFT JOIN comments ON feeds.latestCommentId = comments.id " +
            "LEFT JOIN users AS commentUsers ON comments.ownerId = commentUsers.userId " +
            "WHERE feedId IN (:ids) OR (status = 1 AND fromUserId = :ownerId) ORDER BY feeds.timeCreated DESC")
    abstract fun loadFeedsIds(ids: List<String>, ownerId: String): LiveData<List<FeedUI>>

    @Query("SELECT feedId, feeds.fromUserId as ownerId,feedUsers.name, feedUsers.avatar, feeds.timeCreated, " +
            "feeds.likeCount, feeds.isLiked, feeds.likeInProgress, feeds.commentCount, feeds.content AS feedContent, feeds.status, feeds.photos, " +
            "feeds.latestCommentId, feeds.pagingIds, commentUsers.userId as commentOwnerId, commentUsers.name AS commentOwnerName, " +
            "commentUsers.avatar AS commentUserAvatar, comments.content AS commentContent, comments.photo as commentPhoto  " +
            "FROM feeds " +
            "LEFT JOIN users AS feedUsers ON feeds.fromUserId = feedUsers.userId " +
            "LEFT JOIN comments ON feeds.latestCommentId = comments.id " +
            "LEFT JOIN users AS commentUsers ON comments.ownerId = commentUsers.userId " +
            "WHERE pagingIds LIKE '%' || :pagingId || '%' OR (status = 1 AND fromUserId = :ownerId) ORDER BY feeds.timeCreated DESC")
    abstract fun loadFeedsIds(pagingId: String, ownerId: String): DataSource.Factory<Int, FeedUI>


    @Query("SELECT feedId, userId as ownerId, name, avatar, photos, commentCount, likeCount, isLiked, content, feeds.timeCreated, feeds.timeUpdated, status, likeInProgress  " +
            "FROM feeds, users " +
            "WHERE feeds.fromUserId = users.userId AND feedId = :feedId")
    abstract fun loadFeedById(feedId: String): LiveData<FeedUI>

    @Query("SELECT feedId, feeds.fromUserId as ownerId,feedUsers.name, feedUsers.avatar, feeds.timeCreated, " +
            "feeds.likeCount, feeds.isLiked, feeds.likeInProgress, feeds.commentCount, feeds.content AS feedContent, " +
            "feeds.status, feeds.photos, feeds.latestCommentId, " +
            "commentUsers.userId as commentOwnerId ,commentUsers.name AS commentOwnerName, " +
            "commentUsers.avatar AS commentUserAvatar, comments.content AS commentContent, comments.photo as commentPhoto  " +
            "FROM feeds " +
            "LEFT JOIN users AS feedUsers ON feeds.fromUserId = feedUsers.userId " +
            "LEFT JOIN comments ON feeds.latestCommentId = comments.id " +
            "LEFT JOIN users AS commentUsers ON comments.ownerId = commentUsers.userId " +
            "WHERE feeds.feedId = :feedId")
    abstract fun getFeedUI(feedId: String): FeedUI?

    @Query("SELECT * FROM feeds WHERE feedId = :feedId")
    abstract fun findFeedEntityById(feedId: String): FeedEntity?

    @Query("DELETE FROM feeds")
    abstract fun deleteAll()

    @Query("DELETE FROM feeds WHERE status != :status")
    abstract fun deleteAllExcludeStatus(@LocalStatus.LOCAL_STATUS status: Int)

    @Query("DELETE FROM feeds WHERE feedId IN (:ids) AND status != 1")
    abstract fun deleteFeeds(ids: List<String>)


}
