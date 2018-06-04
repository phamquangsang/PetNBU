package com.petnbu.petnbu.repo

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations

import com.petnbu.petnbu.AppExecutors
import com.petnbu.petnbu.api.WebService
import com.petnbu.petnbu.db.PetDb
import com.petnbu.petnbu.model.Notification
import com.petnbu.petnbu.model.NotificationUI
import com.petnbu.petnbu.model.Paging
import com.petnbu.petnbu.model.Resource
import com.petnbu.petnbu.util.RateLimiter

import java.util.ArrayList
import java.util.concurrent.TimeUnit

import javax.inject.Inject

class NotificationRepository
    @Inject
    constructor(private val petDb: PetDb,
                private val appExecutors: AppExecutors,
                private val webService: WebService) {

    private val rateLimiter = RateLimiter<String>(10, TimeUnit.MINUTES)

    fun getUserNotifications(userId: String, after: Long): LiveData<Resource<List<NotificationUI>>> {
        return object : NetworkBoundResource<List<NotificationUI>, List<Notification>>(appExecutors) {
            override fun saveCallResult(items: List<Notification>) {
                val listId = ArrayList<String>(items.size)
                val pagingId = Paging.notificationsPagingId()
                for (item in items) {
                    listId.add(item.id)
                }

                val oldestId = if (listId.isEmpty()) null else listId[listId.size - 1]
                val paging = Paging(pagingId, listId, false, oldestId)

                petDb.runInTransaction {
                    petDb.notificationDao().insertFromModels(items)
                    for (notification in items) {
                        petDb.userDao().insert(notification.fromUser)
                    }
                    petDb.pagingDao().insert(paging)
                }
            }

            override fun shouldFetch(data: List<NotificationUI>?) =
                    data == null || data.isEmpty() || rateLimiter.shouldFetch(Paging.notificationsPagingId())


            override fun deleteDataFromDb(body: List<Notification>?) {
                petDb.pagingDao().deleteFeedPaging(Paging.notificationsPagingId())
            }

            override fun loadFromDb(): LiveData<List<NotificationUI>> {
                return Transformations.switchMap(petDb.pagingDao().loadFeedPaging(Paging.notificationsPagingId())) { input ->
                    if (input == null) {
                        val data = MutableLiveData<List<NotificationUI>>()
                        data.postValue(null)
                        data
                    } else {
                        petDb.notificationDao().getNotifcations(input.ids)
                    }
                }
            }

            override fun createCall() = webService.getNotifications(userId, after, NOTIFICATION_PER_PAGE)

        }.asLiveData()
    }

    companion object {
        private const val NOTIFICATION_PER_PAGE = 10
    }
}
