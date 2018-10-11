package com.petnbu.petnbu.ui.addeditfeed

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.databinding.ObservableBoolean
import com.petnbu.petnbu.PetApplication
import com.petnbu.petnbu.model.*
import com.petnbu.petnbu.repo.FeedRepository
import com.petnbu.petnbu.repo.UserRepository
import com.petnbu.petnbu.util.SharedPrefUtil
import java.util.*
import javax.inject.Inject

class CreateEditFeedViewModel : ViewModel() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var feedRepository: FeedRepository

    @Inject
    lateinit var application: Application

    val showLoading = ObservableBoolean()

    private val feedLiveData = MutableLiveData<FeedUI>()

    private var isNewFeed: Boolean = false

    private var feedId: String = ""

    val selectedPhotos = ArrayList<Photo>()

    init {
        PetApplication.appComponent.inject(this)
    }

    fun loadUserInfo(): LiveData<UserEntity> {
        return Transformations.switchMap<Resource<UserEntity>, UserEntity>(userRepository.getUserById(SharedPrefUtil.userId)) { userResource ->
            val userLiveData = MutableLiveData<UserEntity>()
            userLiveData.value = userResource?.data
            userLiveData
        }
    }

    fun getFeed(feedId: String): LiveData<FeedUI> {
        return if (!feedId.isEmpty()) {
            if (feedLiveData.value == null) {
                Transformations.switchMap(feedRepository.getFeed(feedId)) { feedRes ->
                    if (feedRes.data != null) {
                        feedLiveData.value = feedRes.data
                        this.feedId = feedId
                    }
                    showLoading.set(Status.LOADING == feedRes.status)
                    feedLiveData
                }
            } else feedLiveData
        } else {
            isNewFeed = true
            feedLiveData.value = null
            return feedLiveData
        }
    }

    fun saveFeed(content: String, photos: ArrayList<Photo>) {
        if (isNewFeed && feedId.isEmpty()) {
            feedRepository.createNewFeed(content, photos)
        } else {
            feedRepository.updateFeed(Feed(this@CreateEditFeedViewModel.feedId).apply {
                this.content = content
                this.photos = photos
            })
        }
    }
}
