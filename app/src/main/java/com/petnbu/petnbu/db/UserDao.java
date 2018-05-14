package com.petnbu.petnbu.db;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.petnbu.petnbu.model.FeedUser;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.UserEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Dao
public abstract class UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(UserEntity userEntity);

    @Query("SELECT * FROM users WHERE userId = :id")
    public abstract LiveData<UserEntity> findLiveUserById(String id);

    @Query("SELECT * FROM users WHERE userId = :id")
    public abstract UserEntity findUserById(String id);

    public void insert(FeedUser feedUser){
        insert(new UserEntity(feedUser.getUserId(), feedUser.getAvatar(), feedUser.getName(), null, null, null));
    }

    public void insertFromFeedUsers(List<FeedUser> feedUserList){
        Map<String, Boolean> record = new HashMap<>();
        for (FeedUser feedUser : feedUserList) {
            if(record.get(feedUser.getUserId())){
                continue;
            }
            insert(feedUser);
            record.put(feedUser.getUserId(), true);
        }
    }

}