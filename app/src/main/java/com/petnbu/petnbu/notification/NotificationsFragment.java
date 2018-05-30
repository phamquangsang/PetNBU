package com.petnbu.petnbu.notification;

import android.arch.lifecycle.ViewModelProviders;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.FragmentNotificationsBinding;
import com.petnbu.petnbu.model.Status;

import java.util.ArrayList;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding mBinding;

    private NotificationsRecyclerViewAdapter mAdapter;

    private NotificationViewModel mViewModel;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_notifications, container, false);
        mViewModel = ViewModelProviders.of(this).get(NotificationViewModel.class);
        initialize();
        return mBinding.getRoot();
    }

    private void initialize() {
//        List<Notification> notificationList = new ArrayList<>();
//        Notification notification = new Notification();
//        notification.setId("1");
//
//        FeedUser feedUser = new FeedUser();
//        feedUser.setUserId("1");
//        feedUser.setName("Nhat Pham");
//        feedUser.setAvatarUrl("https://academy-stg-assets.s3.amazonaws.com/user_50/hN5JRUzjs8nXifDEWnBc1522315546980_optimized.jpg");
//        notification.setFromUser(feedUser);
//        notification.setTimeCreated(Calendar.getInstance().getTime());
//        notification.setType(Notification.TYPE_LIKE_FEED);
//        notificationList.add(notification);
//
//        notification = new Notification();
//        notification.setId("1");
//        notification.setFromUser(feedUser);
//        notification.setTimeCreated(Calendar.getInstance().getTime());
//        notification.setType(Notification.TYPE_LIKE_COMMENT);
//        notificationList.add(notification);
//
//
//        notification = new Notification();
//        notification.setId("1");
//        notification.setFromUser(feedUser);
//        notification.setTimeCreated(Calendar.getInstance().getTime());
//        notification.setType(Notification.TYPE_LIKE_REPLY);
//        notificationList.add(notification);
//
//        notification = new Notification();
//        notification.setId("1");
//        notification.setFromUser(feedUser);
//        notification.setTimeCreated(Calendar.getInstance().getTime());
//        notification.setType(Notification.TYPE_NEW_COMMENT);
//        notificationList.add(notification);
//
//        notification = new Notification();
//        notification.setId("1");
//        notification.setFromUser(feedUser);
//        notification.setTimeCreated(Calendar.getInstance().getTime());
//        notification.setType(Notification.TYPE_NEW_REPLY);
//        notificationList.add(notification);

        mBinding.rvNotifications.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new NotificationsRecyclerViewAdapter(getActivity(), new ArrayList<>());
        mBinding.rvNotifications.setAdapter(mAdapter);

        mViewModel.getNotifications().observe(this, listResource -> {
            if(listResource != null){
                if(listResource.data != null){
                    mAdapter.setData(listResource.data);
                }
                if(listResource.status == Status.LOADING){
                    //todo show loading
                }else{
                    //todo hide loading
                }
            }
        });
    }
}
