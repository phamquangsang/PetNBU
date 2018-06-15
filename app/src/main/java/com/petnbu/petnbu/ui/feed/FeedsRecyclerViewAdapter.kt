package com.petnbu.petnbu.ui.feed

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.util.ArrayMap
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PagerSnapHelper
import android.support.v7.widget.RecyclerView
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.petnbu.petnbu.GlideApp
import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.ViewFeedBinding
import com.petnbu.petnbu.model.FeedUI
import com.petnbu.petnbu.model.LocalStatus.STATUS_ERROR
import com.petnbu.petnbu.model.LocalStatus.STATUS_UPLOADING
import com.petnbu.petnbu.model.Photo
import com.petnbu.petnbu.ui.BaseBindingViewHolder
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
        if (payloads.isNotEmpty())
            (holder as? ViewHolder)?.bindData(getItem(position), payloads)
        else
            super.onBindViewHolder(holder, position, payloads)
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
            mBinding.rvPhotos.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                @SuppressLint("SetTextI18n")
                override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                    val feed: FeedUI = getItem(adapterPosition)

                    feed.photos?.run {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            (recyclerView!!.layoutManager as? LinearLayoutManager)?.run {
                                val position = getPosition(snapHelper.findSnapView(this))
                                mBinding.tvPhotosCount.text = "${position + 1}/$size"
                                lastSelectedPhotoPositions[feed.feedId] = position
                            }
                        }
                    }
                }
            })
        }

        override fun bindData(item: FeedUI) {
            TraceUtils.begin("bind Feed")
            feed = item

            if (feed.status == STATUS_UPLOADING) {
                mBinding.layoutRoot.setShouldInterceptEvents(true)
                mBinding.layoutDisable.isVisible = true
                mBinding.viewLoading.progressBar.isVisible = true

                mBinding.layoutError.isVisible = false
                mBinding.imgLike.isVisible = false
                mBinding.imgComment.isVisible = false
                mBinding.tvLikesCount.isVisible = false
                mBinding.tvViewComments.isVisible = false
                mBinding.imgOptions.isVisible = false
                mBinding.imgLikeInProgress.isVisible = false
            } else {
                mBinding.layoutRoot.setShouldInterceptEvents(false)

                if (feed.status == STATUS_ERROR) {
                    mBinding.layoutError.isVisible = true
                    mBinding.viewLoading.progressBar.isVisible = false
                    mBinding.imgLike.isVisible = false
                    mBinding.imgComment.isVisible = false
                    mBinding.tvLikesCount.isVisible = false
                    mBinding.tvViewComments.isVisible = false
                    mBinding.imgOptions.isVisible = false
                    mBinding.imgLikeInProgress.isVisible = false
                } else {
                    mBinding.layoutDisable.isVisible = false
                    mBinding.imgLike.isVisible = true
                    mBinding.imgComment.isVisible = true
                    mBinding.tvLikesCount.isVisible = true
                    mBinding.tvViewComments.isVisible = true
                    mBinding.imgOptions.isVisible = true
                    mBinding.imgLikeInProgress.isVisible = false
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

        @SuppressLint("SetTextI18n")
        private fun displayPhotos() {
            feed.photos?.run {
                if (isNotEmpty()) {
                    constraintHeightForPhoto(this[0].width, this[0].height)

                    mBinding.rvPhotos.adapter = FeedPhotosAdapter(feed, glideRequests, object : FeedPhotosAdapter.OnItemClickListener {
                        override fun onPhotoClicked() {}
                    }, deviceWidth)

                    val currentPos = lastSelectedPhotoPositions[feed.feedId] ?: 0
                    mBinding.rvPhotos.scrollToPosition(currentPos)
                    mBinding.tvPhotosCount.text = "${currentPos + 1}/$size"
                    mBinding.tvPhotosCount.isVisible = size > 1
                }
            }
        }

        private fun constraintHeightForPhoto(photoWidth: Int, photoHeight: Int) {
            val targetHeight = (photoHeight / photoWidth.toFloat() * deviceWidth).toInt()
            mBinding.layoutMedia.updateLayoutParams<ConstraintLayout.LayoutParams> {
                height = when {
                    targetHeight > maxPhotoHeight -> maxPhotoHeight
                    targetHeight < minPhotoHeight -> minPhotoHeight
                    else -> targetHeight
                }
            }
        }

        private fun displayContent() {
            val contentBuilder = SpannableStringBuilder().apply {
                if (!feed.feedContent.isNullOrEmpty()) {
                    bold {
                        color(color = Color.BLACK) {
                            append(feed.name)
                        }
                    }
                    append("  ")
                    append(feed.feedContent)
                }
                if (!feed.commentContent.isNullOrEmpty() || feed.commentPhoto != null) {
                    if (!isEmpty())
                        append("\n")

                    feed.commentOwnerName?.run commentOwnerName@ {
                        bold {
                            append(this@commentOwnerName)
                        }
                    }
                    append("  ")
                    append(if (feed.commentContent.isNullOrEmpty()) "replied" else feed.commentContent)
                }
            }
            mBinding.tvContent.isVisible = !contentBuilder.isEmpty()
            mBinding.tvContent.text = contentBuilder
        }

        private fun displayLikeInfo() {
            mBinding.imgLike.isInvisible = feed.likeInProgress
            mBinding.imgLikeInProgress.isVisible = feed.likeInProgress
            mBinding.imgLike.setImageResource(if (feed.isLiked) R.drawable.ic_favorite_red_24dp else R.drawable.ic_favorite_border_black_24dp)

            mBinding.tvLikesCount.text = "${feed.likeCount} ${if (feed.likeCount > 1) "likes" else "like"}"
            mBinding.tvLikesCount.isVisible = feed.likeCount > 0
        }

        private fun displayCommentCount() {
            mBinding.tvViewComments.isVisible = feed.commentCount > 1
            mBinding.tvViewComments.text = "View all ${feed.commentCount} comments"
        }
    }

    private class FeedDiffCallback : DiffUtil.ItemCallback<FeedUI>() {

        override fun areItemsTheSame(oldItem: FeedUI, newItem: FeedUI) =
                oldItem.feedId == newItem.feedId

        override fun areContentsTheSame(oldItem: FeedUI, newItem: FeedUI) =
                oldItem == newItem

        override fun getChangePayload(oldItem: FeedUI, newItem: FeedUI): Any? {
            val bundle = Bundle()
            if (oldItem.likeInProgress != newItem.likeInProgress
                    || oldItem.isLiked != newItem.isLiked) {
                bundle.putBoolean("like_status", true)
            }
            if (!oldItem.latestCommentId.isNullOrEmpty()
                    && oldItem.latestCommentId != newItem.latestCommentId) {
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