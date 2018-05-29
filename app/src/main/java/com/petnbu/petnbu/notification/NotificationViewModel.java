package com.petnbu.petnbu.notification;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;

import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.SharedPrefUtil;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.model.NotificationUI;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.repo.CommentRepository;
import com.petnbu.petnbu.repo.FeedRepository;
import com.petnbu.petnbu.repo.NotificationRepository;
import com.petnbu.petnbu.repo.UserRepository;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

public class NotificationViewModel extends ViewModel {

    @Inject
    NotificationRepository mNotificationRepository;

    @Inject
    CommentRepository mCommentRepository;

    @Inject
    UserRepository mUserRepository;

    @Inject
    WebService mWebService;

    @Inject
    Application mApplication;

    public NotificationViewModel() {
        PetApplication.getAppComponent().inject(this);
    }

    public LiveData<Resource<List<NotificationUI>>> getNotifications(){
        return mNotificationRepository.getUserNotifications(SharedPrefUtil.getUserId(), new Date().getTime());
    }

}
