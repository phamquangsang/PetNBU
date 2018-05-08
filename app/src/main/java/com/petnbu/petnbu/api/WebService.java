package com.petnbu.petnbu.api;

import android.arch.lifecycle.LiveData;

import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.FeedResponse;
import com.petnbu.petnbu.model.UserEntity;

import java.util.List;

public interface WebService {

    public LiveData<ApiResponse<FeedResponse>> createFeed(FeedResponse feedResponse);

    public LiveData<ApiResponse<List<FeedResponse>>> getGlobalFeeds(long after, int limit);

    public LiveData<ApiResponse<List<FeedResponse>>> getGlobalFeeds(String afterFeedId, int limit);

    public LiveData<ApiResponse<List<FeedResponse>>> getUserFeed(String userId, long after, int limit);

    public LiveData<ApiResponse<List<FeedResponse>>> getUserFeed(String userId, String afterFeedId, int limit);

    public LiveData<ApiResponse<FeedResponse>> getFeed(String feedId);

    public LiveData<ApiResponse<UserEntity>> createUser(UserEntity userEntity);

    public LiveData<ApiResponse<FeedResponse>> likeFeed(String feedId);

    public LiveData<ApiResponse<UserEntity>> getUser(String userId);

    public void updateUser(UserEntity userEntity, SuccessCallback<Void> callback);

    public LiveData<ApiResponse<List<Comment>>> getComments(String feedId);

    public LiveData<ApiResponse<List<Comment>>> getCommentsByComment(String commentId);
}


