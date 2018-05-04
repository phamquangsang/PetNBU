package com.petnbu.petnbu.userprofile;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;

import com.petnbu.petnbu.AppExecutors;
import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.db.PetDb;
import com.petnbu.petnbu.feed.FeedsViewModel;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.repo.FeedRepository;

import java.util.List;

import javax.inject.Inject;

public class UserProfileViewModel extends ViewModel {

    @Inject
    FeedRepository mFeedRepository;

    @Inject
    Application mApplication;

    private LiveData<Resource<List<Feed>>> mFeedsLiveData;


    public UserProfileViewModel() {
        PetApplication.getAppComponent().inject(this);
    }


}
