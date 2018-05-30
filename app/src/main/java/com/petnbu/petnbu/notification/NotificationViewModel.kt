package com.petnbu.petnbu.notification

import android.app.Application
import android.arch.core.util.Function
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.databinding.ObservableBoolean

import com.petnbu.petnbu.PetApplication
import com.petnbu.petnbu.SharedPrefUtil
import com.petnbu.petnbu.api.WebService
import com.petnbu.petnbu.model.NotificationUI
import com.petnbu.petnbu.model.Resource
import com.petnbu.petnbu.model.Status
import com.petnbu.petnbu.repo.CommentRepository
import com.petnbu.petnbu.repo.NotificationRepository
import com.petnbu.petnbu.repo.UserRepository

import java.util.Date

import javax.inject.Inject

class NotificationViewModel : ViewModel() {

    @Inject
    lateinit var mNotificationRepository: NotificationRepository

    @Inject
    lateinit var mCommentRepository: CommentRepository

    @Inject
    lateinit var mUserRepository: UserRepository

    @Inject
    lateinit var mWebService: WebService

    @Inject
    lateinit var mApplication: Application

    val showLoading = ObservableBoolean()

    private val notifications: MutableLiveData<List<NotificationUI>> = MutableLiveData()

    fun loadNotifications(): LiveData<List<NotificationUI>> {
        return Transformations.switchMap(mNotificationRepository.getUserNotifications(SharedPrefUtil.getUserId(), Date().time)) { notificationsResource ->
            if (notificationsResource != null) {
                showLoading.set(notificationsResource.status == Status.LOADING)
                notifications.value = notificationsResource.data
            }
            notifications
        }
    }

    init {
        PetApplication.getAppComponent().inject(this)
    }
}
