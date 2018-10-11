package com.petnbu.petnbu.ui.user

import android.arch.lifecycle.*
import com.petnbu.petnbu.PetApplication
import com.petnbu.petnbu.model.FeedUI
import com.petnbu.petnbu.model.Resource
import com.petnbu.petnbu.model.Status
import com.petnbu.petnbu.model.UserEntity
import com.petnbu.petnbu.repo.FeedRepository
import com.petnbu.petnbu.repo.LoadMoreState
import com.petnbu.petnbu.repo.UserRepository
import timber.log.Timber
import javax.inject.Inject

class UserProfileViewModel : ViewModel() {

    @Inject
    lateinit var feedRepository: FeedRepository

    @Inject
    lateinit var userRepository: UserRepository

    private val feedsLiveData: MutableLiveData<List<FeedUI>> = MutableLiveData()
    private val userLiveData: MutableLiveData<UserEntity> = MutableLiveData()

    private val loadMoreHandler: LoadMoreHandler by lazy {
        LoadMoreHandler(feedRepository)
    }

    val loadMoreState: LiveData<LoadMoreState>
        get() = loadMoreHandler.loadMoreState

    init {
        PetApplication.appComponent.inject(this)
    }

    fun getFeeds(userId: String, pagingId: String): LiveData<List<FeedUI>> {
        return Transformations.switchMap(feedRepository.loadUserFeeds(userId, pagingId)) {
            feedsLiveData.apply {
                value = it.data
            }
        }
    }

    fun getUser(userId: String): LiveData<UserEntity> {
        return Transformations.switchMap(userRepository.getUserById(userId)) {
            userLiveData.apply {
                value = it.data
            }
        }
    }

    fun loadNextPage(userId: String, pagingId: String) {
        feedsLiveData.value?.run {
            loadMoreHandler.loadNextPage(userId , pagingId)
        }
    }

    private class LoadMoreHandler(private val feedRepo: FeedRepository) : Observer<Resource<Boolean>> {

        val loadMoreState = MutableLiveData<LoadMoreState>()

        private lateinit var nextPageLiveData: LiveData<Resource<Boolean>>

        private var hasMore = true

        init {
            loadMoreState.value = LoadMoreState(false, null)
        }

        fun loadNextPage(userId: String, pagingId: String) {
            if (!hasMore || loadMoreState.value == null || loadMoreState.value?.isRunning == true) {
                return
            }
            Timber.i("loadNextPage")
            unregister()
            nextPageLiveData = feedRepo.fetchNextUserFeedPage(userId, pagingId)
            loadMoreState.value = LoadMoreState(true, null)
            nextPageLiveData.observeForever(this)

        }

        override fun onChanged(result: Resource<Boolean>?) {
            if (result == null) {
                reset()
            } else {
                Timber.i(result.toString())
                when (result.status) {
                    Status.SUCCESS -> {
                        hasMore = java.lang.Boolean.TRUE == result.data
                        unregister()
                        loadMoreState.value = LoadMoreState(false, null)
                    }
                    Status.ERROR -> {
                        hasMore = true
                        unregister()
                        loadMoreState.value = LoadMoreState(false, result.message ?: "Error Unknown")
                    }
                }
            }
        }

        private fun unregister() {
            if (this::nextPageLiveData.isInitialized) {
                nextPageLiveData.removeObserver(this)
            }
        }

        private fun reset() {
            unregister()
            hasMore = true
            loadMoreState.value = LoadMoreState(false, null)
        }
    }

}
