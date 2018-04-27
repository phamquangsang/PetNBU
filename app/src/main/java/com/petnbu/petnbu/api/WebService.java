package com.petnbu.petnbu.api;

import android.arch.lifecycle.LiveData;

import com.google.android.gms.common.api.Api;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.User;

import java.util.List;

public interface WebService {

    public void createFeed(Feed feed, SuccessCallback<Feed> callback);

    public LiveData<ApiResponse<Feed>> createFeed(Feed feed);

    public void updateFeed(Feed feed, SuccessCallback<Void> callback);

    public void getFeed(String feedId, SuccessCallback<Feed> callback);

    public LiveData<ApiResponse<List<Feed>>> getFeeds(long after, int limit);

    public void createUser(User user, SuccessCallback<Void> callback);

    public void getUser(String userId, SuccessCallback<User> callback);

    public LiveData<ApiResponse<User>> getUser(String userId);

    public void updateUser(User user, SuccessCallback<Void> callback);

}


