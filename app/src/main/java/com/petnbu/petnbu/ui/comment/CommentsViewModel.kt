package com.petnbu.petnbu.ui.comment

import android.arch.lifecycle.*
import android.arch.lifecycle.Observer
import android.databinding.ObservableBoolean
import com.petnbu.petnbu.PetApplication
import com.petnbu.petnbu.SharedPrefUtil
import com.petnbu.petnbu.SingleLiveEvent
import com.petnbu.petnbu.api.WebService
import com.petnbu.petnbu.model.*
import com.petnbu.petnbu.repo.CommentRepository
import com.petnbu.petnbu.repo.LoadMoreState
import com.petnbu.petnbu.repo.UserRepository
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class CommentsViewModel : ViewModel() {

    @Inject
    lateinit var mCommentRepository: CommentRepository

    @Inject
    lateinit var mUserRepository: UserRepository

    @Inject
    lateinit var mWebService: WebService

    private val loadMoreHandler: LoadMoreHandler
    private val subCommentLoadMoreHandler: SubCommentLoadMoreHandler

    val showLoadingFeedReplies = ObservableBoolean(false)
    val showLoadingCommentReplies = ObservableBoolean(false)

    private val mOpenRepliesEvent = SingleLiveEvent<String>()
    private val mOpenUserProfileEvent = SingleLiveEvent<String>()

    val commentLoadMoreState: LiveData<LoadMoreState>
        get() = loadMoreHandler.loadMoreState

    val subCommentLoadMoreState: LiveData<LoadMoreState>
        get() = subCommentLoadMoreHandler.loadMoreState

    val openRepliesEvent: LiveData<String>
        get() = mOpenRepliesEvent

    val openUserProfileEvent: LiveData<String>
        get() = mOpenUserProfileEvent

    init {
        PetApplication.appComponent.inject(this)
        loadMoreHandler = LoadMoreHandler(mCommentRepository)
        subCommentLoadMoreHandler = SubCommentLoadMoreHandler(mCommentRepository)
    }

    fun loadUserInfo(): LiveData<UserEntity> {
        return Transformations.switchMap(mUserRepository.getUserById(SharedPrefUtil.userId)) { userResource ->
            val userLiveData = MutableLiveData<UserEntity>()
            userResource?.run {
                data?.run {
                    userLiveData.value = this
                } ?: takeIf { status == Status.ERROR }?.run {
                    userLiveData.value = null
                }
            }
            userLiveData
        }
    }

    fun loadComments(feedId: String): LiveData<List<CommentUI>> {
        showLoadingFeedReplies.set(true)
        return Transformations.switchMap(mCommentRepository.getFeedCommentsIncludeFeedContentHeader(feedId, Date().time, CommentRepository.COMMENT_PER_PAGE)) { commentsResource ->
            showLoadingFeedReplies.set(false)
            val commentsByFeedLiveData = MutableLiveData<List<CommentUI>>()
            commentsResource?.data?.run {
                commentsByFeedLiveData.value = this
            }
            commentsByFeedLiveData
        }
    }


    fun loadSubComments(commentId: String): LiveData<List<CommentUI>> {
        showLoadingCommentReplies.set(true)
        return Transformations.switchMap(mCommentRepository.getSubComments(commentId, Date().time, CommentRepository.COMMENT_PER_PAGE)) { commentsResource ->
            showLoadingCommentReplies.set(false)
            val mCommentsByCommentLiveData = MutableLiveData<List<CommentUI>>()
            commentsResource?.data?.run {
                mCommentsByCommentLiveData.value = this
            }
            mCommentsByCommentLiveData
        }
    }

    fun sendComment(feedId: String, content: String, photo: Photo?) {
        val comment = Comment()
        comment.parentFeedId = feedId
        comment.content = content
        comment.photo = photo
        mCommentRepository.createComment(comment)
    }

    fun sendCommentByComment(commendId: String, content: String, photo: Photo?) {
        val comment = Comment()
        comment.parentCommentId = commendId
        comment.content = content
        comment.photo = photo
        mCommentRepository.createComment(comment)
    }

    fun loadNextPage(feedId: String) {
        Timber.i("loadNextPage :")
        loadMoreHandler.loadNextPage(feedId, Paging.feedCommentsPagingId(feedId))
    }

    fun loadSubCommentsNextPage(commentId: String) {
        Timber.i("loadSubCommentsNextPage :")
        subCommentLoadMoreHandler.loadNextPage(commentId, Paging.subCommentsPagingId(commentId))
    }

    fun openUserProfile(userId: String) {
        mOpenUserProfileEvent.value = userId
    }

    fun openRepliesForComment(commentId: String) {
        mOpenRepliesEvent.value = commentId
    }

    fun likeCommentClicked(commentId: String) {
        mCommentRepository.likeCommentHandler(SharedPrefUtil.userId, commentId)
    }

    fun likeSubCommentClicked(subCommentId: String) {
        mCommentRepository.likeSubCommentHandler(SharedPrefUtil.userId, subCommentId)
    }

    private class LoadMoreHandler(private val commentRepo: CommentRepository,
                          val loadMoreState: MutableLiveData<LoadMoreState> = MutableLiveData()) : Observer<Resource<Boolean>> {

        private lateinit var nextPageLiveData: LiveData<Resource<Boolean>>

        private var hasMore = true

        init {
            loadMoreState.value = LoadMoreState(false, null)
        }

        fun loadNextPage(feedId: String, pagingId: String) {
            if (!hasMore || loadMoreState.value == null || loadMoreState.value!!.isRunning) {
                Timber.i("hasMore = %s", hasMore)
                return
            }
            Timber.i("loadNextPage")
            unregister()
            nextPageLiveData = commentRepo.fetchCommentsNextPage(feedId, pagingId)
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
                        loadMoreState.setValue(LoadMoreState(false, result.message))
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

    private class SubCommentLoadMoreHandler(private val commentRepo: CommentRepository,
                                            val loadMoreState: MutableLiveData<LoadMoreState> = MutableLiveData()) : Observer<Resource<Boolean>> {

        private lateinit var nextPageLiveData: LiveData<Resource<Boolean>>

        private var hasMore = true

        init {
            loadMoreState.value = LoadMoreState(false, null)
        }

        fun loadNextPage(commentId: String, pagingId: String) {
            if (!hasMore || loadMoreState.value == null || loadMoreState.value!!.isRunning) {
                Timber.i("hasMore = %s", hasMore)
                return
            }
            Timber.i("loadNextPage")
            unregister()
            nextPageLiveData = commentRepo.fetchSubCommentsNextPage(commentId, pagingId)
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
                        loadMoreState.setValue(LoadMoreState(false, result.message))
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
