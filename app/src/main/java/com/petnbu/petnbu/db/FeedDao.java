package com.petnbu.petnbu.db;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.FeedPaging;
import com.petnbu.petnbu.model.FeedUIModel;

import java.util.ArrayList;
import java.util.List;

@Dao
public abstract class FeedDao {

    public void insertFromFeed(Feed feed){
        FeedEntity feedEntity = new FeedEntity(feed.getFeedId(), feed.getFeedUser().getUserId(),
                feed.getPhotos(), feed.getCommentCount(), feed.getLikeCount(), feed.getContent(),
                feed.getTimeCreated(), feed.getTimeUpdated(), feed.getStatus(), feed.isLikeInProgress());
        insert(feedEntity);
    }

    public void insertFromFeedList(List<Feed> listFeed){
        List<FeedEntity> entities = new ArrayList<>(listFeed.size());
        for (Feed feed : listFeed){
            entities.add(new FeedEntity(feed.getFeedId(), feed.getFeedUser().getUserId(),
                    feed.getPhotos(), feed.getCommentCount(), feed.getLikeCount(), feed.getContent(),
                    feed.getTimeCreated(), feed.getTimeUpdated(), feed.getStatus(), feed.isLikeInProgress()));

        }
        insert(entities);
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(FeedEntity... feeds);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(List<FeedEntity> feedList);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public abstract void update(FeedEntity feed);

    @Query("UPDATE feeds set feedId = :newId where feedId = :feedId")
    public abstract void updateFeedId(String feedId, String newId);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateAll(List<FeedEntity> feeds);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateAll(FeedEntity... feeds);

    @Delete
    public abstract void deleteFeed(FeedEntity feed);

    @Query("DELETE FROM feeds Where feedId = :feedId")
    public abstract void deleteFeedById(String feedId);


    @Query("SELECT feedId, name, userId, avatar, photos, commentCount, likeCount, content, feeds.timeCreated, feeds.timeUpdated, status, likeInProgress " +
            "FROM feeds, users WHERE feedId IN (:ids) ORDER BY feeds.timeCreated DESC")
    public abstract LiveData<List<FeedUIModel>> loadFeeds(List<String> ids);

    @Query("SELECT feedId, name, userId, avatar, photos, commentCount, likeCount, content, feeds.timeCreated, feeds.timeUpdated, status, likeInProgress " +
            "FROM feeds, users WHERE feeds.fromUserId = users.userId AND (feedId IN (:ids) OR status == 1) ORDER BY feeds.timeCreated DESC")
    public abstract LiveData<List<FeedUIModel>> loadFeedsIncludeUploadingPost(List<String> ids);

    @Query("SELECT feedId, name, userId, avatar, photos, commentCount, likeCount, content, feeds.timeCreated, feeds.timeUpdated, status, likeInProgress  " +
            "FROM feeds, users WHERE feeds.fromUserId = users.userId AND feedId = :feedId")
    public abstract LiveData<FeedUIModel> loadFeedById(String feedId);

    @Query("SELECT * FROM feeds WHERE feedId = :feedId")
    public abstract FeedEntity findFeedEntityById(String feedId);

    @Query("DELETE FROM feeds")
    public abstract void deleteAll();

    @Query("DELETE FROM feeds where status != :status")
    public abstract void deleteAllExcludeStatus(@FeedEntity.LOCAL_STATUS int status);

    @Query("DELETE FROM feeds WHERE feedId in (:ids) AND status != 1")
    public abstract void deleteFeeds(List<String> ids);

    @Query("Select * from feed_paging where pagingId = :id")
    abstract public LiveData<FeedPaging> loadFeedPaging(String id);

    @Query("Select * from feed_paging where pagingId = :id")
    abstract public FeedPaging findFeedPaging(String id);



    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract public void insert(FeedPaging paging);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract public void update(FeedPaging paging);

    @Query("DELETE FROM feed_paging where pagingId = :pagingId")
    abstract public void deleteFeedPaging(String pagingId);

}
