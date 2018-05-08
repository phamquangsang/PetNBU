package com.petnbu.petnbu.db;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.TypeConverter;
import android.arch.persistence.room.TypeConverters;
import android.arch.persistence.room.Update;

import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedResponse;
import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.Paging;
import com.petnbu.petnbu.model.Photo;

import java.util.ArrayList;
import java.util.List;

@Dao
public abstract class FeedDao {

    public void insertFromFeed(FeedResponse feedResponse){
        FeedEntity feedEntity = new FeedEntity(feedResponse.getFeedId(), feedResponse.getFeedUser().getUserId(),
                feedResponse.getPhotos(), feedResponse.getCommentCount(), feedResponse.getLikeCount(), feedResponse.getContent(),
                feedResponse.getTimeCreated(), feedResponse.getTimeUpdated(), feedResponse.getStatus(), feedResponse.isLikeInProgress());
        insert(feedEntity);
    }

    public void insertFromFeedList(List<FeedResponse> listFeedResponse){
        List<FeedEntity> entities = new ArrayList<>(listFeedResponse.size());
        for (FeedResponse feedResponse : listFeedResponse){
            entities.add(new FeedEntity(feedResponse.getFeedId(), feedResponse.getFeedUser().getUserId(),
                    feedResponse.getPhotos(), feedResponse.getCommentCount(), feedResponse.getLikeCount(), feedResponse.getContent(),
                    feedResponse.getTimeCreated(), feedResponse.getTimeUpdated(), feedResponse.getStatus(), feedResponse.isLikeInProgress()));

        }
        insert(entities);
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(FeedEntity... feeds);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(List<FeedEntity> feedList);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public abstract void update(FeedEntity feed);

    @Query("UPDATE feeds SET content = :content WHERE feedId = :feedId")
    public abstract void updateContent(String content, String feedId);

    @TypeConverters(value = PetTypeConverters.class)
    @Query("UPDATE feeds SET photos = :photos WHERE feedId = :feedId")
    public abstract void updatePhotos(List<Photo> photos, String feedId);

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
    public abstract LiveData<List<Feed>> loadFeeds(List<String> ids);

    @Query("SELECT feedId, name, userId, avatar, photos, commentCount, likeCount, content, feeds.timeCreated, feeds.timeUpdated, status, likeInProgress " +
            "FROM feeds, users WHERE feeds.fromUserId = users.userId AND (feedId IN (:ids) OR status == 1) ORDER BY feeds.timeCreated DESC")
    public abstract LiveData<List<Feed>> loadFeedsIncludeUploadingPost(List<String> ids);

    @Query("SELECT feedId, name, userId, avatar, photos, commentCount, likeCount, content, feeds.timeCreated, feeds.timeUpdated, status, likeInProgress  " +
            "FROM feeds, users WHERE feeds.fromUserId = users.userId AND feedId = :feedId")
    public abstract LiveData<Feed> loadFeedById(String feedId);

    @Query("SELECT * FROM feeds WHERE feedId = :feedId")
    public abstract FeedEntity findFeedEntityById(String feedId);

    @Query("DELETE FROM feeds")
    public abstract void deleteAll();

    @Query("DELETE FROM feeds where status != :status")
    public abstract void deleteAllExcludeStatus(@FeedEntity.LOCAL_STATUS int status);

    @Query("DELETE FROM feeds WHERE feedId in (:ids) AND status != 1")
    public abstract void deleteFeeds(List<String> ids);

    @Query("Select * from paging where pagingId = :id")
    abstract public LiveData<Paging> loadFeedPaging(String id);

    @Query("Select * from paging where pagingId = :id")
    abstract public Paging findFeedPaging(String id);



    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract public void insert(Paging paging);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract public void update(Paging paging);

    @Query("DELETE FROM paging where pagingId = :pagingId")
    abstract public void deleteFeedPaging(String pagingId);

}
