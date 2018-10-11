package com.petnbu.petnbu.ui.comment

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
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
import com.petnbu.petnbu.GlideApp
import com.petnbu.petnbu.GlideRequests
import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.ViewCommentBinding
import com.petnbu.petnbu.databinding.ViewLoadingBinding
import com.petnbu.petnbu.model.CommentUI
import com.petnbu.petnbu.model.isUploading
import com.petnbu.petnbu.ui.BaseBindingViewHolder
import com.petnbu.petnbu.util.ColorUtils
import java.util.*

class CommentsRecyclerViewAdapter(context: Context,
                                  private val feedId: String,
                                  private val commentsViewModel: CommentsViewModel)
    : ListAdapter<CommentUI, BaseBindingViewHolder<*, *>>(CommentDiffCallback()) {

    private val glideRequests: GlideRequests = GlideApp.with(context)

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
        if (payloads.isNotEmpty())
            (holder as? ViewHolder)?.bindData(getItem(position), payloads)
        else
            super.onBindViewHolder(holder, position, payloads)

    }

    override fun getItemCount() = dataItemCount + if (addLoadMore) 1 else 0

    override fun getItemViewType(position: Int) = if (position < dataItemCount && dataItemCount > 0) VIEW_TYPE_COMMENT else VIEW_TYPE_LOADING

    inner class ViewHolder(itemView: View) : BaseBindingViewHolder<ViewCommentBinding, CommentUI>(itemView) {

        private lateinit var comment: CommentUI
        private val openRepliesClickListener = { _: View ->
            if (adapterPosition != RecyclerView.NO_POSITION) {
                getItem(adapterPosition).run {
                    if (!isUploading())
                        commentsViewModel.openRepliesForComment(id)
                }
            }
        }

        init {
            mBinding.tvReply.setOnClickListener(openRepliesClickListener)
            mBinding.tvLatestComment.setOnClickListener(openRepliesClickListener)
            mBinding.tvPreviousReplies.setOnClickListener(openRepliesClickListener)
            mBinding.imgProfile.setOnClickListener { _: View ->
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    getItem(adapterPosition).run {
                        if (!isUploading())
                            commentsViewModel.openUserProfile(owner.userId)
                    }
                }
            }

            mBinding.imgLike.setOnClickListener { _: View ->
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    getItem(adapterPosition).run {
                        if (!isUploading())
                            commentsViewModel.likeCommentClicked(id)
                    }
                }
            }
        }

        override fun bindData(item: CommentUI) {
            comment = item
            displayUserInfo()
            displayContent()
            displayInfo()
            displayReplies()
        }

        override fun bindData(item: CommentUI, payloads: List<Any>) {
            comment = item

            (payloads[0] as? Bundle)?.run {
                if (getBoolean("like_status"))
                    displayLikeInfo()
                if (getBoolean("latest_comment"))
                    displayReplies()
            }
        }

        private fun displayUserInfo() {
            comment.owner.run {
                val avatarUrl = if (!avatar.thumbnailUrl.isNullOrEmpty()) avatar.thumbnailUrl else avatar.originUrl
                glideRequests.load(avatarUrl)
                        .centerInside()
                        .into(mBinding.imgProfile)
            }
        }

        private fun displayContent() {
            comment.owner.run {
                mBinding.tvContent.text = SpannableStringBuilder()
                        .bold {
                            color(color = Color.BLACK) {
                                append(name)
                            }
                        }
                        .append("  ")
                        .color(color = ColorUtils.modifyAlpha(Color.BLACK, 0.8f)) {
                            append(comment.content)
                        }
            }

            comment.photo?.run {
                mBinding.imgPhoto.isVisible = true

                val ratio = width / height.toFloat()
                if (width > height)
                    mBinding.imgPhoto.set(1.0f, 1 / ratio)
                else
                    mBinding.imgPhoto.set(1.0f / 2, 1 / (2 * ratio))


                val photoUrl = if (!smallUrl.isNullOrEmpty()) smallUrl else originUrl
                glideRequests.load(photoUrl)
                        .centerInside()
                        .into(mBinding.imgPhoto)
            } ?: kotlin.run {
                mBinding.imgPhoto.isVisible = false
            }
        }

        private fun displayInfo() {
            mBinding.tvDate.text = DateUtils.getRelativeTimeSpanString(comment.timeCreated.time,
                    Calendar.getInstance().timeInMillis, 0L, DateUtils.FORMAT_ABBREV_RELATIVE)

            if (comment.id != feedId) {
                mBinding.divider.isInvisible = true
                mBinding.tvLikesCount.isVisible = true
                mBinding.tvReply.isVisible = true
                mBinding.imgLike.isVisible = true
                displayLikeInfo()
            } else {
                mBinding.tvLikesCount.isVisible = false
                mBinding.tvReply.isVisible = false
                mBinding.imgLike.isVisible = false
                mBinding.divider.isVisible = true
                mBinding.progressBar.isVisible = false
            }
        }

        private fun displayLikeInfo() {
            if (comment.isUploading() || comment.likeInProgress) {
                mBinding.imgLike.isInvisible = true
                mBinding.progressBar.isVisible = true
            } else {
                mBinding.imgLike.isVisible = true
                mBinding.progressBar.isVisible = false
                mBinding.imgLike.setImageResource(if (comment.isLiked) R.drawable.ic_favorite_red_24dp else R.drawable.ic_favorite_border_black_24dp)
            }
            if (comment.likeCount > 0) {
                mBinding.tvLikesCount.isVisible = true
                mBinding.tvLikesCount.text = "${comment.likeCount} ${if (comment.likeCount > 1) "likes" else "like"}"
            } else {
                mBinding.tvLikesCount.isVisible = false
            }
        }

        private fun displayReplies() {
            if (comment.latestCommentId != null) {
                mBinding.imgLatestCommentOwnerProfile.isVisible = true
                mBinding.tvLatestComment.isVisible = true
                mBinding.tvPreviousReplies.isVisible = true

                mBinding.tvLatestComment.text = SpannableStringBuilder()
                        .bold {
                            color(color = Color.BLACK) {
                                append(comment.latestCommentOwnerName)
                            }
                        }
                        .append("  ")
                        .color(color = ColorUtils.modifyAlpha(Color.BLACK, 0.8f)) {
                            append("${if (comment.latestCommentPhoto != null) "replied" else comment.latestCommentContent}")
                        }

                if (comment.commentCount > 1) {
                    mBinding.tvPreviousReplies.isVisible = true
                    mBinding.tvPreviousReplies.text = itemView.context.getString(R.string.feed_view_previous_replies, comment.commentCount - 1)
                } else {
                    mBinding.tvPreviousReplies.isVisible = false
                }

                comment.latestCommentOwnerAvatar?.run {
                    val latestCommentOwnerAvatarUrl = if (!thumbnailUrl.isNullOrEmpty()) thumbnailUrl else originUrl
                    glideRequests
                            .load(latestCommentOwnerAvatarUrl)
                            .centerInside()
                            .into(mBinding.imgLatestCommentOwnerProfile)
                }
            } else {
                mBinding.imgLatestCommentOwnerProfile.isVisible = false
                mBinding.tvLatestComment.isVisible = false
                mBinding.tvPreviousReplies.isVisible = false
            }
        }
    }

    inner class ViewLoadingHolder(itemView: View) : BaseBindingViewHolder<ViewLoadingBinding, Void?>(itemView) {

        override fun bindData(item: Void?) {}

        override fun bindData(item: Void?, payloads: List<Any>) {}
    }

    private class CommentDiffCallback : DiffUtil.ItemCallback<CommentUI>() {

        override fun areItemsTheSame(oldItem: CommentUI, newItem: CommentUI) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: CommentUI, newItem: CommentUI) = oldItem == newItem

        override fun getChangePayload(oldItem: CommentUI, newItem: CommentUI): Any? {
            val bundle = Bundle().apply {
                if (oldItem.likeInProgress != newItem.likeInProgress || oldItem.isLiked != newItem.isLiked)
                    putBoolean("like_status", true)
                if (oldItem.latestCommentId != newItem.latestCommentId)
                    putBoolean("latest_comment", true)
            }
            return if (!bundle.isEmpty) bundle else super.getChangePayload(oldItem, newItem)
        }
    }

    companion object {
        private const val VIEW_TYPE_COMMENT = 1
        private const val VIEW_TYPE_LOADING = 2
    }
}
