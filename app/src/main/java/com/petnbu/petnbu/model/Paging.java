package com.petnbu.petnbu.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import com.petnbu.petnbu.db.PetTypeConverters;

import java.util.List;

@Entity(tableName = "paging")
@TypeConverters(PetTypeConverters.class)
public class Paging {

    public static final String GLOBAL_FEEDS_PAGING_ID = "global-feeds-paging-id-feeds";
    public static String feedCommentsPagingId(String feedId){
        return feedId + "-comments";
    }

    public static String subCommentsPagingId(String parentCmtId){
        return parentCmtId + "-subComments";
    }
    public static String userFeedsPagingId(String userId){
        return userId + "-feeds";
    }

    public static boolean isFeedPagingId(@NonNull String pagingId){
        return pagingId.endsWith("feeds");
    }

    public static boolean isCommentPagingId(@NonNull String pagingId){
        return pagingId.endsWith("comments") || pagingId.endsWith("subComments");
    }

    @PrimaryKey
    @NonNull
    private String pagingId;

    private List<String> ids;

    private boolean ended;

    private String oldestId;

    public Paging() {
    }

    @Ignore
    public Paging(@NonNull String pagingId, List<String> ids, boolean ended, String oldestId) {
        this.pagingId = pagingId;
        this.ids = ids;
        this.ended = ended;
        this.oldestId = oldestId;
    }

    public String getPagingId() {
        return pagingId;
    }

    public void setPagingId(String pagingId) {
        this.pagingId = pagingId;
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }

    public String getOldestId() {
        return oldestId;
    }

    public void setOldestId(String oldestId) {
        this.oldestId = oldestId;
    }

    public boolean isEnded() {
        return ended;
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
    }

    public  void replaceId(String oldId, String newId){
        if(ids.size() == 1){
            ids.set(0, newId);
            this.oldestId = newId;
            return;
        }
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            if (id.equals(oldId)) {
                ids.set(i, newId);
                return;
            }
        }
    }
}
