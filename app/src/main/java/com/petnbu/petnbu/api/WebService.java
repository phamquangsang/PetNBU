package com.petnbu.petnbu.api;

import android.arch.lifecycle.LiveData;

import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.FeedResponse;
import com.petnbu.petnbu.model.UserEntity;

import java.util.List;

public interface WebService {

    LiveData<ApiResponse<FeedResponse>> createFeed(FeedResponse feedResponse);

    LiveData<ApiResponse<FeedResponse>> updateFeed(FeedResponse feedResponse);

    LiveData<ApiResponse<List<FeedResponse>>> getGlobalFeeds(long after, int limit);

    LiveData<ApiResponse<List<FeedResponse>>> getGlobalFeeds(String afterFeedId, int limit);

    LiveData<ApiResponse<List<FeedResponse>>> getUserFeed(String userId, long after, int limit);

    LiveData<ApiResponse<List<FeedResponse>>> getUserFeed(String userId, String afterFeedId, int limit);

    LiveData<ApiResponse<FeedResponse>> getFeed(String feedId);

    LiveData<ApiResponse<UserEntity>> createUser(UserEntity userEntity);

    LiveData<ApiResponse<FeedResponse>> likeFeed(String feedId);

    LiveData<ApiResponse<UserEntity>> getUser(String userId);

    void updateUser(UserEntity userEntity, SuccessCallback<Void> callback);

    LiveData<ApiResponse<List<Comment>>> getComments(String feedId);

    LiveData<ApiResponse<List<Comment>>> getCommentsByComment(String commentId);
}


