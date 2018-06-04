package com.petnbu.petnbu.feed

import android.arch.lifecycle.*
import com.petnbu.petnbu.PetApplication
import com.petnbu.petnbu.SingleLiveEvent
import com.petnbu.petnbu.model.*
import com.petnbu.petnbu.repo.FeedRepository
import com.petnbu.petnbu.repo.LoadMoreState
import timber.log.Timber
import javax.inject.Inject

class FeedsViewModel : ViewModel() {

    @Inject
    lateinit var feedRepository: FeedRepository

    val showLoadingErrorEvent = SingleLiveEvent<String>()
    val showLoadingEvent = SingleLiveEvent<Boolean>()
    val openUserProfileEvent = SingleLiveEvent<String>()
    val openCommentsEvent = SingleLiveEvent<String>()

    private val feedsLiveData: MutableLiveData<List<FeedUI>> = MutableLiveData()
    private val loadMoreHandler: LoadMoreHandler

    val loadMoreState: LiveData<LoadMoreState>
        get() = loadMoreHandler.loadMoreState

    init {
        PetApplication.appComponent.inject(this)
        loadMoreHandler = LoadMoreHandler(feedRepository)
    }

    fun getFeeds(pagingId: String, loggedUserId: String): LiveData<List<FeedUI>> {
        return Transformations.switchMap(feedRepository.loadFeeds(pagingId, loggedUserId), {
            it?.run {
                data?.run {
                    feedsLiveData.value = this
                }
                showLoadingEvent.value = status == Status.LOADING
                takeIf { status == Status.ERROR }?.run {
                    showLoadingErrorEvent.value = message
                }
            }
            feedsLiveData
        })
    }

    fun loadNextPage() {
        feedsLiveData.value?.run {
            loadMoreHandler.loadNextPage(Paging.GLOBAL_FEEDS_PAGING_ID)
        }
    }

    fun refresh() {
        loadMoreHandler.reset()
        val refreshLiveData = feedRepository.refresh()
        refreshLiveData.observeForever(object :Observer<Resource<List<Feed>>> {
            override fun onChanged(resource: Resource<List<Feed>>?) {
                if(resource != null && resource.status != Status.LOADING) {
                    showLoadingEvent.value = false
                    refreshLiveData.removeObserver(this)
                }
            }
        })
    }

    fun openUserProfile(userId: String) {
        openUserProfileEvent.value = userId
    }

    fun openComments(feedId: String) {
        openCommentsEvent.value = feedId
    }

    fun likeClicked(userId: String, feedId: String) {
        feedRepository.likeFeedHandler(userId, feedId)
    }

    private class LoadMoreHandler(private val feedRepo: FeedRepository,
                                  val loadMoreState: MutableLiveData<LoadMoreState> = MutableLiveData())
        : Observer<Resource<Boolean>> {

        private lateinit var nextPageLiveData: LiveData<Resource<Boolean>>

        private var hasMore = true

        init {
            loadMoreState.value = LoadMoreState(false, null)
        }

        fun loadNextPage(pagingId: String) {
            if (!hasMore || loadMoreState.value == null || loadMoreState.value!!.isRunning) {
                Timber.i("hasMore = %s", hasMore)
                return
            }
            Timber.i("loadNextPage")
            unregister()
            nextPageLiveData = feedRepo.fetchNextPage(pagingId)
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
                        loadMoreState.setValue(LoadMoreState(false, null))
                    }
                    Status.ERROR -> {
                        hasMore = true
                        unregister()
                        loadMoreState.setValue(LoadMoreState(false,
                                result.message ?:"Error Unknown"))
                    }
                }
            }
        }

        private fun unregister() {
            if (this::nextPageLiveData.isInitialized) {
                nextPageLiveData.removeObserver(this)
            }
        }

        fun reset() {
            unregister()
            hasMore = true
            loadMoreState.value = LoadMoreState(false, null)
        }
    }
}
