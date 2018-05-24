package com.petnbu.petnbu.api;

import android.arch.lifecycle.LiveData;

import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.UserEntity;

import java.util.List;

public interface WebService {

    LiveData<ApiResponse<Feed>> createFeed(Feed feed);

    LiveData<ApiResponse<Feed>> updateFeed(Feed feed);

    LiveData<ApiResponse<List<Feed>>> getGlobalFeeds(long after, int limit);

    LiveData<ApiResponse<List<Feed>>> getGlobalFeeds(String afterFeedId, int limit);

    LiveData<ApiResponse<List<Feed>>> getUserFeed(String userId, long after, int limit);

    LiveData<ApiResponse<List<Feed>>> getUserFeed(String userId, String afterFeedId, int limit);

    LiveData<ApiResponse<Feed>> getFeed(String feedId);

    LiveData<ApiResponse<UserEntity>> createUser(UserEntity userEntity);

    LiveData<ApiResponse<Feed>> likeFeed(String userId , String feedId);

    LiveData<ApiResponse<Feed>> unLikeFeed(String userId , String feedId);

    LiveData<ApiResponse<UserEntity>> getUser(String userId);

    void updateUser(UserEntity userEntity, SuccessCallback<Void> callback);

    LiveData<ApiResponse<List<Comment>>> getFeedComments(String feedId, long after, int limit);

    LiveData<ApiResponse<List<Comment>>> getCommentsPaging(String feedId, String commentId, int limit);

    LiveData<ApiResponse<Comment>> createFeedComment(Comment comment, String feedId);

    LiveData<ApiResponse<Comment>> likeComment(String userId, String commentId);

    LiveData<ApiResponse<Comment>> unLikeComment(String userId, String commentId);

    LiveData<ApiResponse<Comment>> createReplyComment(Comment comment, String parentCommentId);

    LiveData<ApiResponse<List<Comment>>> getSubComments(String commentId, long after, int limit);

    LiveData<ApiResponse<List<Comment>>> getSubCommentsPaging(String commentId, String afterCommentId, int limit);
}


