package com.petnbu.petnbu.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import com.petnbu.petnbu.db.PetTypeConverters;

import java.util.List;

@Entity(tableName = "feed_paging")
@TypeConverters(PetTypeConverters.class)
public class FeedPaging {

    public static final String GLOBAL_FEEDS_PAGING_ID = "global-feeds-paging-id";

    @PrimaryKey
    @NonNull
    private String pagingId;

    private List<String> feedIds;

    private boolean ended;

    private String oldestFeedId;

    public FeedPaging() {
    }

    @Ignore
    public FeedPaging(String pagingId, List<String> feedIds, boolean ended, String oldestFeedId) {
        this.pagingId = pagingId;
        this.feedIds = feedIds;
        this.ended = ended;
        this.oldestFeedId = oldestFeedId;
    }

    public String getPagingId() {
        return pagingId;
    }

    public void setPagingId(String pagingId) {
        this.pagingId = pagingId;
    }

    public List<String> getFeedIds() {
        return feedIds;
    }

    public void setFeedIds(List<String> feedIds) {
        this.feedIds = feedIds;
    }

    public String getOldestFeedId() {
        return oldestFeedId;
    }

    public void setOldestFeedId(String oldestFeedId) {
        this.oldestFeedId = oldestFeedId;
    }

    public boolean isEnded() {
        return ended;
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
    }
}
