package com.petnbu.petnbu.ui.comment

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.petnbu.petnbu.BaseBindingViewHolder
import com.petnbu.petnbu.GlideApp
import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.ViewCommentBinding
import com.petnbu.petnbu.databinding.ViewLoadingBinding
import com.petnbu.petnbu.model.CommentUI
import com.petnbu.petnbu.model.LocalStatus
import com.petnbu.petnbu.model.Photo
import com.petnbu.petnbu.util.ColorUtils
import java.util.*

class RepliesRecyclerViewAdapter(context: Context,
                                 private val commentId: String,
                                 private val onItemClickListener: OnItemClickListener,
                                 private val commentsViewModel: CommentsViewModel)
    : ListAdapter<CommentUI, BaseBindingViewHolder<*, *>>(CommentDiffCallback()) {

    private val glideRequests = GlideApp.with(context)

    var addLoadMore: Boolean = false
        set(value) {
            if (field != value) {
                if (value) {
                    field = value
                    notifyItemInserted(loadingMoreItemPosition)
                } else {
                    notifyItemRemoved(loadingMoreItemPosition)
                    field = value
                }
            }
        }

    private val dataItemCount: Int
        get() = super.getItemCount()

    private val loadingMoreItemPosition: Int
        get() = if (addLoadMore) itemCount - 1 else RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<*, *> {
        return if (viewType == VIEW_TYPE_LOADING) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.view_loading, parent, false)
            ViewLoadingHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.view_comment, parent, false)
            ViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: BaseBindingViewHolder<*, *>, position: Int) {
        (holder as? ViewHolder)?.bindData(getItem(position))
    }

    override fun onBindViewHolder(holder: BaseBindingViewHolder<*, *>, position: Int, payloads: List<Any>) {
        if (!payloads.isEmpty())
            (holder as? ViewHolder)?.bindData(getItem(position), payloads)
        else
            super.onBindViewHolder(holder, position, payloads)
    }

    override fun getItemCount() = dataItemCount + if (addLoadMore) 1 else 0

    override fun getItemViewType(position: Int) = if (position < dataItemCount && dataItemCount > 0) VIEW_TYPE_COMMENT else VIEW_TYPE_LOADING

    private inner class ViewHolder(itemView: View) : BaseBindingViewHolder<ViewCommentBinding, CommentUI>(itemView) {

        private lateinit var comment: CommentUI

        init {
            mBinding.imgProfile.setOnClickListener { v ->
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    getItem(adapterPosition).run {
                        owner?.run {
                            if (localStatus != LocalStatus.STATUS_UPLOADING)
                                commentsViewModel.openUserProfile(userId)
                        }
                    }
                }
            }

            mBinding.imgLike.setOnClickListener { v ->
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    getItem(adapterPosition).run {
                        if (localStatus != LocalStatus.STATUS_UPLOADING)
                            commentsViewModel.likeSubCommentClicked(id)
                    }
                }
            }
        }

        override fun bindData(item: CommentUI) {
            comment = item
            displayUserInfo()
            displayContent()
            displayInfo()
        }

        override fun bindData(item: CommentUI, payloads: List<Any>) {
            comment = item

            (payloads[0] as? Bundle)?.run {
                if (getBoolean("like_status"))
                    displayLikeInfo()
            }
        }

        private fun displayUserInfo() {
            comment.owner?.run {
                val avatarUrl = if (!avatar.thumbnailUrl.isNullOrEmpty()) avatar.thumbnailUrl else avatar.originUrl
                glideRequests.load(avatarUrl)
                        .centerInside()
                        .into(mBinding.imgProfile)
            }
        }

        private fun displayContent() {
            comment.owner?.run {
                val contentBuilder = SpannableStringBuilder()
                contentBuilder.apply {
                    var start = 0
                    append(name)
                    setSpan(StyleSpan(Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(ForegroundColorSpan(Color.BLACK), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    append("  ")

                    start = length
                    append(comment.content)
                    setSpan(ForegroundColorSpan(ColorUtils.modifyAlpha(Color.BLACK, 0.8f)), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                mBinding.tvContent.text = contentBuilder
            }

            comment.photo?.run {
                mBinding.imgPhoto.visibility = View.VISIBLE

                val ratio = width / height.toFloat()
                if (width > height) {
                    mBinding.imgPhoto.set(1.0f, 1 / ratio)
                } else {
                    mBinding.imgPhoto.set(1.0f / 2, 1 / (2 * ratio))
                }

                val photoUrl = if (!smallUrl.isNullOrEmpty()) smallUrl else originUrl
                glideRequests.load(photoUrl)
                        .centerInside()
                        .into(mBinding.imgPhoto)
            } ?: kotlin.run {
                mBinding.imgPhoto.visibility = View.GONE
            }
        }

        private fun displayInfo() {
            comment.timeCreated?.run {
                mBinding.tvDate.text = DateUtils.getRelativeTimeSpanString(time,
                        Calendar.getInstance().timeInMillis, 0L, DateUtils.FORMAT_ABBREV_RELATIVE)
            }

            if (comment.id != commentId) {
                mBinding.divider.visibility = View.INVISIBLE
                displayLikeInfo()
            } else {
                mBinding.divider.visibility = View.VISIBLE
                mBinding.progressBar.visibility = View.GONE
            }
        }

        private fun displayLikeInfo() {
            if (comment.localStatus == LocalStatus.STATUS_UPLOADING || comment.likeInProgress) {
                mBinding.imgLike.visibility = View.INVISIBLE
                mBinding.progressBar.visibility = View.VISIBLE
            } else {
                mBinding.imgLike.visibility = View.VISIBLE
                mBinding.progressBar.visibility = View.GONE
                mBinding.imgLike.setImageResource(if (comment.isLiked) R.drawable.ic_favorite_red_24dp else R.drawable.ic_favorite_border_black_24dp)
            }
            if (comment.likeCount > 0) {
                mBinding.tvLikesCount.visibility = View.VISIBLE
                mBinding.tvLikesCount.text = "${comment.likeCount} ${if (comment.likeCount > 1) "likes" else "like"}"
            } else {
                mBinding.tvLikesCount.visibility = View.GONE
            }
        }
    }

    private class ViewLoadingHolder(itemView: View) : BaseBindingViewHolder<ViewLoadingBinding, Void>(itemView) {

        override fun bindData(item: Void) {}

        override fun bindData(item: Void, payloads: List<Any>) {}
    }

    private class CommentDiffCallback : DiffUtil.ItemCallback<CommentUI>() {

        override fun areItemsTheSame(oldItem: CommentUI, newItem: CommentUI) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: CommentUI, newItem: CommentUI) = oldItem == newItem

        override fun getChangePayload(oldItem: CommentUI, newItem: CommentUI): Any? {
            val bundle = Bundle()
            bundle.apply {
                if (oldItem.likeInProgress != newItem.likeInProgress || oldItem.isLiked != newItem.isLiked) {
                    putBoolean("like_status", true)
                }
                if(oldItem.latestCommentId != newItem.latestCommentId) {
                    putBoolean("latest_comment", true)
                }
            }
            return if(!bundle.isEmpty) bundle else super.getChangePayload(oldItem, newItem)
        }
    }

    interface OnItemClickListener {

        fun onPhotoClicked(photo: Photo)

        fun onLikeClicked(commentId: String)
    }

    companion object {
        private const val VIEW_TYPE_COMMENT = 1
        private const val VIEW_TYPE_LOADING = 2
    }
}
