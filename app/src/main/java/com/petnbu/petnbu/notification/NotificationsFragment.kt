package com.petnbu.petnbu.notification

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.FragmentCommentsBinding
import com.petnbu.petnbu.databinding.FragmentNotificationsBinding
import com.petnbu.petnbu.model.NotificationUI
import kotlinx.android.synthetic.main.fragment_notifications.*

import java.util.ArrayList

class NotificationsFragment : Fragment() {

    private lateinit var mNotificationViewModel: NotificationViewModel
    private lateinit var mBinding: FragmentNotificationsBinding

    private var mAdapter: NotificationsRecyclerViewAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_notifications, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        activity?.let {
            mNotificationViewModel = ViewModelProviders.of(it).get(NotificationViewModel::class.java)
            mBinding.viewModel = mNotificationViewModel
            mNotificationViewModel.loadNotifications().observe(this, Observer { notifications ->
                if (notifications != null) {
                    mAdapter?.submitList(notifications)
                }
            })

            mBinding.rvNotifications.layoutManager = LinearLayoutManager(activity)
            mAdapter = NotificationsRecyclerViewAdapter(context = it)
            mBinding.rvNotifications.adapter = mAdapter
        }

    }
}
