package com.petnbu.petnbu.feed


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
import com.petnbu.petnbu.R
import com.petnbu.petnbu.SharedPrefUtil
import com.petnbu.petnbu.databinding.FragmentFeedsBinding
import com.petnbu.petnbu.feed.comment.CommentsActivity
import com.petnbu.petnbu.model.FeedUI
import com.petnbu.petnbu.model.Paging
import com.petnbu.petnbu.model.Photo
import com.petnbu.petnbu.userprofile.UserProfileActivity
import com.petnbu.petnbu.util.RateLimiter
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
            userId = SharedPrefUtil.getUserId()
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
                        val popupMenu = PopupMenu(view.context, view)
                        popupMenu.menu.add("Edit")
                        popupMenu.setOnMenuItemClickListener { item ->
                            if ("Edit" == item.title) {
                                startActivity(CreateEditFeedActivity.newIntent(this@activity, feed.feedId))
                            }
                            true
                        }
                        popupMenu.show()
                    }
                }
            }, feedsViewModel)
        }

        feedsViewModel.getFeeds(Paging.GLOBAL_FEEDS_PAGING_ID, SharedPrefUtil.getUserId()).observe(this, Observer { feeds -> feedsAdapter.submitList(feeds) })
        feedsViewModel.showLoadingEvent.observe(this, Observer { value -> mBinding.pullToRefresh.isRefreshing = value ?: false })
        feedsViewModel.showLoadingErrorEvent.observe(this, Observer { value -> SnackbarUtils.showSnackbar(mBinding.root, value) })
        feedsViewModel.loadMoreState.observe(this, Observer { state ->
            state?.run {
                Timber.i(toString())

                mBinding.progressBar.visibility = if (isRunning) View.VISIBLE else View.GONE

                errorMessageIfNotHandled?.run {
                    SnackbarUtils.showSnackbar(mBinding.root, this)
                }
            }
        })
        feedsViewModel.openUserProfileEvent.observe(this, Observer { it?.run { showUserProfile(this) } })
        feedsViewModel.openCommentsEvent.observe(this, Observer { it?.run { showCommentsByFeed(this) } })

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

                (recyclerView.layoutManager as? LinearLayoutManager)?.run {
                    if (findLastVisibleItemPosition() == feedsAdapter.itemCount - 1)
                        if (rateLimiter.shouldFetch("global-feed-load-next-page"))
                            feedsViewModel.loadNextPage()
                }
                super.onScrolled(recyclerView, dx, dy)
            }
        })

        mBinding.pullToRefresh.setOnRefreshListener {
            feedsViewModel.refresh()
        }

        mBinding.fabNewPost.setOnClickListener { _ ->
            startActivity(Intent(activity, CreateEditFeedActivity::class.java))
        }
    }

    private fun showUserProfile(userId: String) {
        startActivity(UserProfileActivity.newIntent(activity, userId))
    }

    private fun showCommentsByFeed(feedId: String) {
        activity?.run {
            startActivity(CommentsActivity.newIntent(this, feedId))
        }
    }
}
