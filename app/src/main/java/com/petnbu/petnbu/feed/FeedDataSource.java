package com.petnbu.petnbu.feed;

import android.arch.lifecycle.LiveData;

import java.util.ArrayList;

public interface FeedDataSource {

    LiveData<ArrayList<Feed>> getFeeds();
}
