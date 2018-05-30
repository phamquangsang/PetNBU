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

import java.util.ArrayList;

public class NotificationsFragment extends Fragment {

    private NotificationViewModel mNotificationViewModel;

    private FragmentNotificationsBinding mBinding;
    private NotificationsRecyclerViewAdapter mAdapter;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_notifications, container, false);
        initialize();
        return mBinding.getRoot();
    }

    private void initialize() {
        mNotificationViewModel = ViewModelProviders.of(this).get(NotificationViewModel.class);
        mBinding.setViewModel(mNotificationViewModel);
        mNotificationViewModel.getNotifications().observe(this, notifications -> mAdapter.setData(notifications));

        mBinding.rvNotifications.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new NotificationsRecyclerViewAdapter(getActivity(), new ArrayList<>());
        mBinding.rvNotifications.setAdapter(mAdapter);
    }
}
