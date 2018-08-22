package com.petnbu.petnbu.ui.notification

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.FragmentNotificationsBinding
import com.petnbu.petnbu.util.SharedPrefUtil
import java.util.*

class NotificationsFragment : Fragment() {

    private lateinit var mNotificationViewModel: NotificationViewModel
    private lateinit var mBinding: FragmentNotificationsBinding

    private var notificationsRecyclerViewAdapter: NotificationsRecyclerViewAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_notifications, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        mNotificationViewModel = ViewModelProviders.of(this).get(NotificationViewModel::class.java)
        mBinding.viewModel = mNotificationViewModel
        mNotificationViewModel.loadNotifications(SharedPrefUtil.userId, Date().time).observe(this, Observer { notifications ->
            notifications?.let { notificationsRecyclerViewAdapter?.submitList(it) }
        })

        mBinding.rvNotifications.layoutManager = LinearLayoutManager(activity)
        notificationsRecyclerViewAdapter = activity?.let { NotificationsRecyclerViewAdapter(context = it) }
        mBinding.rvNotifications.adapter = notificationsRecyclerViewAdapter
    }
}
