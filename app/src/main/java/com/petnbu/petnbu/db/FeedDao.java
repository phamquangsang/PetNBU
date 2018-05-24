package com.petnbu.petnbu.db;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.TypeConverters;
import android.arch.persistence.room.Update;

import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.FeedUI;
import com.petnbu.petnbu.model.LocalStatus;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.util.TraceUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Dao
public abstract class FeedDao {

    public void insertFromFeed(Feed feed) {
        insert(feed.toEntity());
    }

    public void insertFromFeedList(List<Feed> listFeed) {
        TraceUtils.begin("insertFromFeedList");
        List<FeedEntity> entities = new ArrayList<>(listFeed.size());
        for (Feed feed : listFeed) {
            entities.add(feed.toEntity());
        }
        TraceUtils.end();
        insert(entities);
    }

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(FeedEntity... feeds);

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(List<FeedEntity> feedList);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public abstract void update(FeedEntity feed);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public abstract void update(List<FeedEntity> feedList);


    @Query("UPDATE feeds SET photos = (:photos), content = :content, timeUpdated = :timeUpdated " +
            "WHERE feedId = :feedId")
    @TypeConverters(ListPhotoConverters.class)
    public abstract void updateFeed(List<Photo> photos, String content, String feedId, Date timeUpdated);

    @Query("UPDATE feeds set status = :status where feedId = :feedId")
    public abstract void updateFeedLocalStatus(int status, String feedId);

    @Query("UPDATE feeds set feedId = :newId where feedId = :feedId")
    public abstract void updateFeedId(String feedId, String newId);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateAll(List<FeedEntity> feeds);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateAll(FeedEntity... feeds);

    @Delete
    public abstract void deleteFeed(FeedEntity feed);

    @Query("DELETE FROM feeds WHERE feedId = :feedId")
    public abstract void deleteFeedById(String feedId);


    @Query("SELECT feedId, feeds.fromUserId as ownerId,feedUsers.name, feedUsers.avatar, feeds.timeCreated, " +
            "feeds.likeCount, feeds.isLiked, feeds.likeInProgress, feeds.commentCount, feeds.content AS feedContent, feeds.status, feeds.photos, " +
            "commentUsers.userId as commentOwnerId ,commentUsers.name AS commentOwnerName, " +
            "commentUsers.avatar AS commentUserAvatar, comments.content AS commentContent, comments.photo as commentPhoto  " +
            "FROM feeds " +
            "LEFT JOIN users AS feedUsers ON feeds.fromUserId = feedUsers.userId " +
            "LEFT JOIN comments ON feeds.latestCommentId = comments.id " +
            "LEFT JOIN users AS commentUsers ON comments.ownerId = commentUsers.userId " +
            "WHERE feedId IN (:ids) OR (status = 1 AND fromUserId = :ownerId) ORDER BY feeds.timeCreated DESC")
    public abstract LiveData<List<FeedUI>> loadFeedsIds(List<String> ids, String ownerId);

    @Query("SELECT feedId, name, userId, avatar, photos, commentCount, likeCount, isLiked, content, feeds.timeCreated, feeds.timeUpdated, status, likeInProgress  " +
            "FROM feeds, users " +
            "WHERE feeds.fromUserId = users.userId AND feedId = :feedId")
    public abstract LiveData<Feed> loadFeedById(String feedId);

    @Query("SELECT feedId, feeds.fromUserId as ownerId,feedUsers.name, feedUsers.avatar, feeds.timeCreated, " +
            "feeds.likeCount, feeds.isLiked, feeds.likeInProgress, feeds.commentCount, feeds.content AS feedContent, feeds.status, feeds.photos, " +
            "commentUsers.userId as commentOwnerId ,commentUsers.name AS commentOwnerName, " +
            "commentUsers.avatar AS commentUserAvatar, comments.content AS commentContent, comments.photo as commentPhoto  " +
            "FROM feeds " +
            "LEFT JOIN users AS feedUsers ON feeds.fromUserId = feedUsers.userId " +
            "LEFT JOIN comments ON feeds.latestCommentId = comments.id " +
            "LEFT JOIN users AS commentUsers ON comments.ownerId = commentUsers.userId " +
            "WHERE feeds.feedId = :feedId")
    public abstract FeedUI getFeedUI(String feedId);

    @Query("SELECT * FROM feeds WHERE feedId = :feedId")
    public abstract FeedEntity findFeedEntityById(String feedId);

    @Query("DELETE FROM feeds")
    public abstract void deleteAll();

    @Query("DELETE FROM feeds WHERE status != :status")
    public abstract void deleteAllExcludeStatus(@LocalStatus.LOCAL_STATUS int status);

    @Query("DELETE FROM feeds WHERE feedId IN (:ids) AND status != 1")
    public abstract void deleteFeeds(List<String> ids);




}
