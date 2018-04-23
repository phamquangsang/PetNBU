package com.petnbu.petnbu.api;

import android.arch.lifecycle.LiveData;

import com.petnbu.petnbu.model.Feed;

import java.util.List;

public interface WebService {

    public void createFeed(Feed feed, SuccessCallback<Void> callback);

    public void updateFeed(Feed feed, SuccessCallback<Void> callback);

    public void getFeed(String feedId, SuccessCallback<Feed> callback);

    public LiveData<List<Feed>> getFeeds(long after, int limit);

}


