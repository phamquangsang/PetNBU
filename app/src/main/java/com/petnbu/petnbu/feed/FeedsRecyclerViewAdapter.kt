package com.petnbu.petnbu.feed

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.util.ArrayMap
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PagerSnapHelper
import android.support.v7.widget.RecyclerView
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import com.petnbu.petnbu.BaseBindingViewHolder
import com.petnbu.petnbu.GlideApp
import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.ViewFeedBinding
import com.petnbu.petnbu.model.FeedUI
import com.petnbu.petnbu.model.LocalStatus.STATUS_ERROR
import com.petnbu.petnbu.model.LocalStatus.STATUS_UPLOADING
import com.petnbu.petnbu.model.Photo
import com.petnbu.petnbu.util.TraceUtils
import com.petnbu.petnbu.util.Utils
import java.util.*

class FeedsRecyclerViewAdapter(context: Context,
                               private val onItemClickListener: OnItemClickListener?,
                               private val feedsViewModel: FeedsViewModel)
    : ListAdapter<FeedUI, BaseBindingViewHolder<*, *>>(FeedDiffCallback()) {

    private val glideRequests = GlideApp.with(context)
    private val lastSelectedPhotoPositions = ArrayMap<String, Int>()
    private val feedPhotosViewPool = RecyclerView.RecycledViewPool()

    private var maxPhotoHeight: Int = 0
    private val deviceWidth = Utils.getDeviceWidth(context)
    private val minPhotoHeight = Utils.goldenRatio(Utils.getDeviceWidth(context), true)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<*, *> {
        TraceUtils.begin("create feed view holder")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_feed, parent, false)
        TraceUtils.end()
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: BaseBindingViewHolder<*, *>, position: Int) {
        (holder as? ViewHolder)?.bindData(getItem(position))
    }

    override fun onBindViewHolder(holder: BaseBindingViewHolder<*, *>, position: Int, payloads: List<Any>) {
        if (payloads.isNotEmpty()) {
            (holder as? ViewHolder)?.bindData(getItem(position), payloads)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun setMaxPhotoHeight(maxPhotoHeight: Int) {
        this.maxPhotoHeight = maxPhotoHeight
    }

    private inner class ViewHolder(itemView: View) : BaseBindingViewHolder<ViewFeedBinding, FeedUI>(itemView) {

        private lateinit var feed: FeedUI
        private val openProfileClickListener = { _: View ->
            if (adapterPosition != RecyclerView.NO_POSITION)
                feedsViewModel.openUserProfile(getItem(adapterPosition).ownerId)
        }
        private val openCommentsClickListener = { _: View ->
            if (adapterPosition != RecyclerView.NO_POSITION)
                feedsViewModel.openComments(getItem(adapterPosition).feedId)
        }

        init {
            mBinding.imgProfile.setOnClickListener(openProfileClickListener)
            mBinding.tvName.setOnClickListener(openProfileClickListener)
            mBinding.imgLike.setOnClickListener {
                if (onItemClickListener != null && adapterPosition != RecyclerView.NO_POSITION)
                    onItemClickListener.onLikeClicked(getItem(adapterPosition).feedId)
            }
            mBinding.imgComment.setOnClickListener(openCommentsClickListener)
            mBinding.tvViewComments.setOnClickListener(openCommentsClickListener)
            mBinding.tvContent.setOnClickListener(openCommentsClickListener)

            mBinding.imgOptions.setOnClickListener {
                if (onItemClickListener != null && adapterPosition != RecyclerView.NO_POSITION)
                    onItemClickListener.onOptionClicked(it, getItem(adapterPosition))
            }

            mBinding.rvPhotos.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(mBinding.rvPhotos)
            mBinding.rvPhotos.recycledViewPool = feedPhotosViewPool
        }

        override fun bindData(item: FeedUI) {
            TraceUtils.begin("bind Feed")
            feed = item

            if (feed.status == STATUS_UPLOADING) {
                mBinding.layoutRoot.setShouldInterceptEvents(true)
                mBinding.layoutDisable.visibility = View.VISIBLE
                mBinding.viewLoading.progressBar.visibility = View.VISIBLE

                mBinding.layoutError.visibility = View.GONE
                mBinding.imgLike.visibility = View.GONE
                mBinding.imgComment.visibility = View.GONE
                mBinding.tvLikesCount.visibility = View.GONE
                mBinding.tvViewComments.visibility = View.GONE
                mBinding.imgOptions.visibility = View.GONE
                mBinding.imgLikeInProgress.visibility = View.GONE
            } else {
                mBinding.layoutRoot.setShouldInterceptEvents(false)

                if (feed.status == STATUS_ERROR) {
                    mBinding.layoutError.visibility = View.VISIBLE
                    mBinding.viewLoading.progressBar.visibility = View.GONE
                    mBinding.imgLike.visibility = View.GONE
                    mBinding.imgComment.visibility = View.GONE
                    mBinding.tvLikesCount.visibility = View.GONE
                    mBinding.tvViewComments.visibility = View.GONE
                    mBinding.imgOptions.visibility = View.GONE
                    mBinding.imgLikeInProgress.visibility = View.GONE
                } else {
                    mBinding.layoutDisable.visibility = View.GONE
                    mBinding.imgLike.visibility = View.VISIBLE
                    mBinding.imgComment.visibility = View.VISIBLE
                    mBinding.tvLikesCount.visibility = View.VISIBLE
                    mBinding.tvViewComments.visibility = View.VISIBLE
                    mBinding.imgOptions.visibility = View.VISIBLE
                    mBinding.imgLikeInProgress.visibility = View.GONE
                }
            }
            displayUserInfo()
            displayTime()
            displayPhotos()
            displayLikeInfo()
            displayContent()
            displayCommentCount()
            TraceUtils.end()
        }

        override fun bindData(item: FeedUI, payloads: List<Any>) {
            feed = item

            (payloads[0] as? Bundle)?.run {
                if (getBoolean("like_status")) {
                    displayLikeInfo()
                }
                if (getBoolean("latest_comment")) {
                    displayContent()
                    displayCommentCount()
                }
            }
        }

        private fun displayUserInfo() {
            mBinding.tvName.text = feed.name
            feed.avatar?.run {
                val avatarUrl = if (!thumbnailUrl.isNullOrEmpty()) thumbnailUrl else originUrl
                glideRequests.load(avatarUrl)
                        .centerInside()
                        .into(mBinding.imgProfile)
            }
        }

        private fun displayTime() {
            feed.timeCreated?.run {
                mBinding.tvDate.text = DateUtils.getRelativeTimeSpanString(time,
                        Calendar.getInstance().timeInMillis, 0L, DateUtils.FORMAT_ABBREV_ALL)
            }
        }

        private fun displayPhotos() {
            if (feed.photos != null && !feed.photos.isEmpty()) {
                constraintHeightForPhoto(feed.photos[0].width, feed.photos[0].height)

                mBinding.rvPhotos.adapter = FeedPhotosAdapter(feed, glideRequests, { }, deviceWidth)

                var currentPos = 0
                val value = lastSelectedPhotoPositions[feed.feedId]
                if (value != null) {
                    currentPos = value.toInt()
                }
                mBinding.rvPhotos.scrollToPosition(currentPos)

                if (feed.photos.size > 1) {
                    mBinding.tvPhotosCount.visibility = View.VISIBLE
                    mBinding.tvPhotosCount.text = String.format(Locale.getDefault(), "%d/%d", currentPos + 1, feed.photos.size)
                } else {
                    mBinding.tvPhotosCount.visibility = View.GONE
                }

                mBinding.rvPhotos.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)

                        if (newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                            val linearLayoutManager = recyclerView!!.layoutManager as LinearLayoutManager
                            val position = linearLayoutManager.findFirstVisibleItemPosition()
                            mBinding.tvPhotosCount.text = String.format(Locale.getDefault(), "%d/%d", position + 1, feed.photos.size)
                            lastSelectedPhotoPositions[feed.feedId] = position
                        }
                    }
                })
            }
        }

        private fun constraintHeightForPhoto(photoWidth: Int, photoHeight: Int) {
            val layoutParams = mBinding.layoutMedia.layoutParams as ConstraintLayout.LayoutParams
            val height = (photoHeight / photoWidth.toFloat() * deviceWidth).toInt()

            layoutParams.height = when {
                height > maxPhotoHeight -> maxPhotoHeight
                height < minPhotoHeight -> minPhotoHeight
                else -> height
            }
            mBinding.layoutMedia.layoutParams = layoutParams
        }

        private fun displayContent() {
            val contentBuilder = SpannableStringBuilder()
            contentBuilder.apply {
                if (!feed.feedContent.isNullOrEmpty()) {
                    append(feed.name)
                    setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(ForegroundColorSpan(Color.BLACK), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    append("  ")
                    append(feed.feedContent)
                }
                if (!feed.commentContent.isNullOrEmpty() || feed.commentPhoto != null) {
                    if (!isNullOrEmpty())
                        append("\n")

                    val commentUserName = feed.commentOwnerName
                    append(commentUserName)
                    setSpan(StyleSpan(Typeface.BOLD), length - commentUserName.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    append("  ")
                    append(if (feed.commentContent.isNullOrEmpty()) "replied" else feed.commentContent)
                }
            }
            mBinding.tvContent.visibility = if (contentBuilder.isNullOrEmpty()) View.GONE else View.VISIBLE
            mBinding.tvContent.text = contentBuilder
        }

        private fun displayLikeInfo() {
            if (feed.likeInProgress) {
                mBinding.imgLike.visibility = View.INVISIBLE
                mBinding.imgLikeInProgress.visibility = View.VISIBLE
            } else {
                mBinding.imgLike.visibility = View.VISIBLE
                mBinding.imgLikeInProgress.visibility = View.GONE
                mBinding.imgLike.setImageResource(if (feed.isLiked) R.drawable.ic_favorite_red_24dp else R.drawable.ic_favorite_border_black_24dp)
            }
            if (feed.getLikeCount() > 0) {
                mBinding.tvLikesCount.text = String.format("%d %s", feed.getLikeCount(), if (feed.getLikeCount() > 1) "likes" else "like")
                mBinding.tvLikesCount.visibility = View.VISIBLE
            } else {
                mBinding.tvLikesCount.visibility = View.GONE
            }
        }

        private fun displayCommentCount() {
            if (feed.getCommentCount() > 1) {
                mBinding.tvViewComments.visibility = View.VISIBLE
                mBinding.tvViewComments.text = String.format("View all %d comments", feed.getCommentCount())
            } else {
                mBinding.tvViewComments.visibility = View.GONE
            }
        }
    }

    private class FeedDiffCallback : DiffUtil.ItemCallback<FeedUI>() {

        override fun areItemsTheSame(oldItem: FeedUI, newItem: FeedUI) = oldItem.feedId == newItem.feedId

        override fun areContentsTheSame(oldItem: FeedUI, newItem: FeedUI) = oldItem == newItem

        override fun getChangePayload(oldItem: FeedUI, newItem: FeedUI): Any? {
            val bundle = Bundle()
            if (oldItem.likeInProgress != newItem.likeInProgress || oldItem.isLiked != newItem.isLiked) {
                bundle.putBoolean("like_status", true)
            }
            if (!oldItem.latestCommentId.isNullOrEmpty() && oldItem.latestCommentId != newItem.latestCommentId) {
                bundle.putBoolean("latest_comment", true)
            }
            return if (!bundle.isEmpty) bundle else super.getChangePayload(oldItem, newItem)
        }
    }

    interface OnItemClickListener {

        fun onPhotoClicked(photo: Photo)

        fun onLikeClicked(feedId: String)

        fun onOptionClicked(view: View, feed: FeedUI)
    }
}
