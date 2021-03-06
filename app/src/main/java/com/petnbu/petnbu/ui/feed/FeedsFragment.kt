package com.petnbu.petnbu.ui.feed


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.FragmentFeedsBinding
import com.petnbu.petnbu.model.FeedUI
import com.petnbu.petnbu.model.Paging
import com.petnbu.petnbu.model.Photo
import com.petnbu.petnbu.ui.addeditfeed.CreateEditFeedActivity
import com.petnbu.petnbu.ui.comment.CommentsActivity
import com.petnbu.petnbu.ui.user.UserProfileActivity
import com.petnbu.petnbu.util.RateLimiter
import com.petnbu.petnbu.util.SharedPrefUtil
import com.petnbu.petnbu.util.SnackbarUtils
import com.petnbu.petnbu.util.Utils
import timber.log.Timber
import java.util.concurrent.TimeUnit


class FeedsFragment : Fragment() {

    private lateinit var mBinding: FragmentFeedsBinding
    private lateinit var feedsViewModel: FeedsViewModel
    private lateinit var feedsAdapter: FeedsRecyclerViewAdapter
    private val rateLimiter = RateLimiter<String>(1, TimeUnit.SECONDS)
    private lateinit var userId: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_feeds, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        activity?.run activity@ {
            userId = SharedPrefUtil.userId
            feedsViewModel = ViewModelProviders.of(this).get(FeedsViewModel::class.java)
            feedsAdapter = FeedsRecyclerViewAdapter(this, object : FeedsRecyclerViewAdapter.OnItemClickListener {

                override fun onPhotoClicked(photo: Photo) {}

                override fun onLikeClicked(feedId: String) {
                    Timber.i("like clicked")
                    if (rateLimiter.shouldFetch(feedId))
                        feedsViewModel.likeClicked(userId, feedId)
                }

                override fun onOptionClicked(view: View, feed: FeedUI) {
                    if (feed.ownerId == userId) {
                        PopupMenu(view.context, view).apply {
                            menu.add("Edit")

                            setOnMenuItemClickListener { item ->
                                if ("Edit" == item.title) {
                                    startActivity(CreateEditFeedActivity.newIntent(this@activity, feed.feedId))
                                }
                                true
                            }
                        }.show()
                    }
                }
            }, feedsViewModel)
        }

        feedsViewModel.getFeeds(Paging.GLOBAL_FEEDS_PAGING_ID, SharedPrefUtil.userId).observe(this, Observer { feeds -> feedsAdapter.submitList(feeds) })
        feedsViewModel.showLoadingEvent.observe(this, Observer { value -> mBinding.pullToRefresh.isRefreshing = value ?: false })
        feedsViewModel.showLoadingErrorEvent.observe(this, Observer { value -> SnackbarUtils.showSnackbar(mBinding.root, value) })
        feedsViewModel.loadMoreState.observe(this, Observer { state ->
            state?.run {
                Timber.i(toString())
                mBinding.progressBar.isVisible = isRunning

                errorMessageIfNotHandled?.let { errorMessage->
                    SnackbarUtils.showSnackbar(mBinding.root, errorMessage)
                }
            }
        })
        feedsViewModel.openUserProfileEvent.observe(this, Observer { userId -> userId?.let { showUserProfile(it) } })
        feedsViewModel.openCommentsEvent.observe(this, Observer { feedId -> feedId?.let { showCommentsByFeed(it) } })

        mBinding.rvFeeds.layoutManager = LinearLayoutManager(activity)
        mBinding.rvFeeds.post {
            val maxHeight = mBinding.rvFeeds.height
            feedsAdapter.setMaxPhotoHeight(maxHeight - Utils.getToolbarHeight(context) * 3 / 2)
            mBinding.rvFeeds.adapter = feedsAdapter
        }
        mBinding.rvFeeds.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 50)
                    mBinding.fabNewPost.hide()
                else if (dy < -10)
                    mBinding.fabNewPost.show()

                val layoutManager = recyclerView.layoutManager
                if(layoutManager is LinearLayoutManager) {
                    if (layoutManager.findLastVisibleItemPosition() == feedsAdapter.itemCount - 1 && rateLimiter.shouldFetch("global-feed-load-next-page"))
                            feedsViewModel.loadNextPage()
                }
                super.onScrolled(recyclerView, dx, dy)
            }
        })

        mBinding.pullToRefresh.setOnRefreshListener {
            feedsViewModel.refresh()
        }

        mBinding.fabNewPost.setOnClickListener {
            startActivity(Intent(activity, CreateEditFeedActivity::class.java))
        }
    }

    private fun showUserProfile(userId: String) {
        activity?.run {
            startActivity(UserProfileActivity.newIntent(this, userId))
        }
    }

    private fun showCommentsByFeed(feedId: String) {
        activity?.run {
            startActivity(CommentsActivity.newIntent(this, feedId))
        }
    }
}
