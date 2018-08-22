package com.petnbu.petnbu.ui.notification

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.databinding.ObservableBoolean
import com.petnbu.petnbu.PetApplication
import com.petnbu.petnbu.model.NotificationUI
import com.petnbu.petnbu.model.Status
import com.petnbu.petnbu.repo.NotificationRepository
import javax.inject.Inject

class NotificationViewModel : ViewModel() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    val showLoading = ObservableBoolean()

    private val notifications: MutableLiveData<List<NotificationUI>> = MutableLiveData()

    init {
        PetApplication.appComponent.inject(this)
    }

    fun loadNotifications(userId: String, time: Long): LiveData<List<NotificationUI>> {
        return Transformations.switchMap(notificationRepository.getUserNotifications(userId, time)) { notificationsResource  ->
            if (notificationsResource != null) {
                showLoading.set(notificationsResource.status == Status.LOADING)
                notifications.value = notificationsResource.data
            }
            notifications
        }
    }
}
