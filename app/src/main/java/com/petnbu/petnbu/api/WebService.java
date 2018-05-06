package com.petnbu.petnbu.api;

import android.arch.lifecycle.LiveData;

import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.User;

import java.util.List;

public interface WebService {

    public LiveData<ApiResponse<Feed>> createFeed(Feed feed);

    public LiveData<ApiResponse<List<Feed>>> getFeeds(long after, int limit);

    public LiveData<ApiResponse<Feed>> getFeed(String feedId);

    public LiveData<ApiResponse<User>> createUser(User user);

    public LiveData<ApiResponse<Feed>> likeFeed(String feedId);

    public LiveData<ApiResponse<User>> getUser(String userId);

    public void updateUser(User user, SuccessCallback<Void> callback);

    public LiveData<ApiResponse<List<Comment>>> getComments(String feedId);

    public LiveData<ApiResponse<List<Comment>>> getCommentsByComment(String commentId);
}


