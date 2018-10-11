package com.petnbu.petnbu.ui.feed

import android.annotation.SuppressLint
import android.arch.paging.PagedListAdapter
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.util.ArrayMap
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
import com.petnbu.petnbu.GlideRequests
import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.ViewFeedBinding
import com.petnbu.petnbu.databinding.ViewFeedPhotosBinding
import com.petnbu.petnbu.extensions.beginSysTrace
import com.petnbu.petnbu.model.*
import com.petnbu.petnbu.ui.BaseBindingViewHolder
import com.petnbu.petnbu.util.ImageUtils
import com.petnbu.petnbu.util.Utils
import java.util.*

class FeedsRecyclerViewAdapter(context: Context,
                               private val onItemClickListener: OnItemClickListener?,
                               private val feedsViewModel: FeedsViewModel)
    : PagedListAdapter<FeedUI, BaseBindingViewHolder<*, *>>(FeedDiffCallback()) {

    private val glideRequests = GlideApp.with(context)
    private val lastSelectedPhotoPositions = ArrayMap<String, Int>()
    private val feedPhotosViewPool = RecyclerView.RecycledViewPool()

    private var maxPhotoHeight = 0
    private val deviceWidth = Utils.getDeviceWidth(context)
    private val minPhotoHeight = Utils.goldenRatio(Utils.getDeviceWidth(context), true)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<*, *> {
        val view = beginSysTrace("create feed view holder") {
            LayoutInflater.from(parent.context).inflate(R.layout.view_feed, parent, false)
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: BaseBindingViewHolder<*, *>, position: Int) {
        beginSysTrace("bind Feed") {
            (holder as? ViewHolder)?.bindData(getItem(position))
        }
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

    private inner class ViewHolder(itemView: View) : BaseBindingViewHolder<ViewFeedBinding, FeedUI?>(itemView) {

        private lateinit var feed: FeedUI
        private val openProfileClickListener = { _: View ->
            if (adapterPosition != RecyclerView.NO_POSITION) {
                getItem(adapterPosition)?.let { feed ->
                    feedsViewModel.openUserProfile(feed.ownerId)
                }
            }
        }
        private val openCommentsClickListener = { _: View ->
            if (adapterPosition != RecyclerView.NO_POSITION) {
                getItem(adapterPosition)?.let { feed ->
                    feedsViewModel.openComments(feed.feedId)
                }
            }
        }

        init {
            mBinding.imgProfile.setOnClickListener(openProfileClickListener)
            mBinding.tvName.setOnClickListener(openProfileClickListener)
            mBinding.imgLike.setOnClickListener {
                if (onItemClickListener != null && adapterPosition != RecyclerView.NO_POSITION) {
                    getItem(adapterPosition)?.let { feed ->
                        onItemClickListener.onLikeClicked(feed.feedId)
                    }
                }
            }
            mBinding.imgComment.setOnClickListener(openCommentsClickListener)
            mBinding.tvViewComments.setOnClickListener(openCommentsClickListener)
            mBinding.tvContent.setOnClickListener(openCommentsClickListener)

            mBinding.imgOptions.setOnClickListener { view ->
                if (onItemClickListener != null && adapterPosition != RecyclerView.NO_POSITION)
                    onItemClickListener.onOptionClicked(view, getItem(adapterPosition)!!)
            }

            mBinding.rvPhotos.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(mBinding.rvPhotos)
            mBinding.rvPhotos.recycledViewPool = feedPhotosViewPool
            mBinding.rvPhotos.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                @SuppressLint("SetTextI18n")
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    val feed: FeedUI = getItem(adapterPosition)!!

                    feed.photos?.run {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            val layoutManager = recyclerView.layoutManager
                            if (layoutManager is LinearLayoutManager) {
                                val position = layoutManager.getPosition(snapHelper.findSnapView(layoutManager))
                                mBinding.tvPhotosCount.text = "${(position ?: 0) + 1}/$size"
                                lastSelectedPhotoPositions[feed.feedId] = position
                            }
                        }
                    }
                }
            })
        }

        override fun bindData(item: FeedUI?) {
            if(item == null) return

            feed = item

            if (feed.isUploading()) {
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

                if (feed.isUploadingError()) {
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
        }

        override fun bindData(item: FeedUI?, payloads: List<Any>) {
            if(item == null) return

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
            feed.isPhotosAvailable()?.run {
                constraintHeightForPhoto(this[0].width, this[0].height)

                mBinding.rvPhotos.adapter = FeedPhotosAdapter(this, glideRequests, object : FeedPhotosAdapter.OnItemClickListener {
                    override fun onPhotoClicked() {}
                }, deviceWidth)

                val currentPos = lastSelectedPhotoPositions[feed.feedId] ?: 0
                mBinding.rvPhotos.scrollToPosition(currentPos)
                mBinding.tvPhotosCount.text = "${currentPos + 1}/$size"
                mBinding.tvPhotosCount.isVisible = size > 1
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

                    feed.commentOwnerName?.run commentOwnerName@{
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

        override fun areItemsTheSame(oldItem: FeedUI, newItem: FeedUI) = oldItem.feedId == newItem.feedId

        override fun areContentsTheSame(oldItem: FeedUI, newItem: FeedUI) = oldItem == newItem

        override fun getChangePayload(oldItem: FeedUI, newItem: FeedUI): Any? {
            val bundle = Bundle().apply {
                if (oldItem.likeInProgress != newItem.likeInProgress || oldItem.isLiked != newItem.isLiked)
                    putBoolean("like_status", true)
                if (!oldItem.latestCommentId.isNullOrEmpty() && oldItem.latestCommentId != newItem.latestCommentId)
                    putBoolean("latest_comment", true)
            }
            return if (!bundle.isEmpty) bundle else super.getChangePayload(oldItem, newItem)
        }
    }

    private class FeedPhotosAdapter(private val photos: List<Photo>,
                                    private val glideRequests: GlideRequests,
                                    private val onItemClickListener: OnItemClickListener,
                                    private val imageWidth: Int)
        : RecyclerView.Adapter<FeedPhotosAdapter.PhotoHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.view_feed_photos, parent, false)
            return PhotoHolder(view)
        }

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            holder.bindData(photos[position])
        }

        override fun getItemCount() = photos.size

        inner class PhotoHolder constructor(itemView: View) : BaseBindingViewHolder<ViewFeedPhotosBinding, Photo>(itemView) {

            init {
                itemView.updateLayoutParams { width = imageWidth }
            }

            override fun bindData(photo: Photo) {
                val photoUrl = ImageUtils.getPhotoUrl(photo, imageWidth)
                glideRequests
                        .load(if (!photoUrl.isNullOrBlank()) photoUrl else photo.originUrl)
                        .centerInside()
                        .into(mBinding.imgContent)
            }

            override fun bindData(item: Photo, payloads: List<Any>) {}
        }

        interface OnItemClickListener {

            fun onPhotoClicked()
        }
    }

    interface OnItemClickListener {

        fun onPhotoClicked(photo: Photo)

        fun onLikeClicked(feedId: String)

        fun onOptionClicked(view: View, feed: FeedUI)
    }
}
