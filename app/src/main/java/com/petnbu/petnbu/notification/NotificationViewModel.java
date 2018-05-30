package com.petnbu.petnbu.notification;

import android.app.Application;
import android.arch.core.util.Function;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;
import android.databinding.ObservableBoolean;

import com.petnbu.petnbu.PetApplication;
import com.petnbu.petnbu.SharedPrefUtil;
import com.petnbu.petnbu.api.WebService;
import com.petnbu.petnbu.model.NotificationUI;
import com.petnbu.petnbu.model.Resource;
import com.petnbu.petnbu.model.Status;
import com.petnbu.petnbu.repo.CommentRepository;
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

    public final ObservableBoolean showLoading = new ObservableBoolean();

    public NotificationViewModel() {
        PetApplication.getAppComponent().inject(this);
    }

    public LiveData<List<NotificationUI>> getNotifications(){
        return Transformations.switchMap(mNotificationRepository.getUserNotifications(SharedPrefUtil.getUserId(), new Date().getTime()), new Function<Resource<List<NotificationUI>>, LiveData<List<NotificationUI>>>() {
            @Override
            public LiveData<List<NotificationUI>> apply(Resource<List<NotificationUI>> notificationsResource) {
                MutableLiveData<List<NotificationUI>> notificationsLiveData = new MutableLiveData<>();
                if(notificationsResource != null){
                    showLoading.set(notificationsResource.status == Status.LOADING);
                    if(notificationsResource.data != null){
                        notificationsLiveData.setValue(notificationsResource.data);
                    }
                }
                return notificationsLiveData;
            }
        });
    }

}
