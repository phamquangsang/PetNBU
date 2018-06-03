package com.petnbu.petnbu.notification

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
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
import com.petnbu.petnbu.GlideRequests
import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.ViewNotificationBinding
import com.petnbu.petnbu.model.Notification
import com.petnbu.petnbu.model.NotificationUI
import com.petnbu.petnbu.util.ColorUtils
import com.petnbu.petnbu.util.Objects
import java.util.*

class NotificationsRecyclerViewAdapter(context: Context)
    : ListAdapter<NotificationUI, BaseBindingViewHolder<*, *>>(NotificationDiffCallback()) {

    private val glideRequests: GlideRequests = GlideApp.with(context)
    private var addLoadMore: Boolean = false

    private val loadingMoreItemPosition: Int
        get() = if (addLoadMore) itemCount - 1 else RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<*, *> {
        return if (viewType == VIEW_TYPE_LOADING) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.view_loading, parent, false)
            ViewLoadingHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.view_notification, parent, false)
            ViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: BaseBindingViewHolder<*, *>, position: Int) {
        val item: NotificationUI? = getItem(position)
        item?.run {
            (holder as? ViewHolder)?.bindData(this)
        }
    }

    override fun getItemCount() = super.getItemCount() + if (addLoadMore) 1 else 0

    override fun getItemViewType(position: Int) = if (position < itemCount && itemCount > 0) VIEW_TYPE_NOTIFICATION else VIEW_TYPE_LOADING

    fun setAddLoadMore(addLoadMore: Boolean) {
        if (this.addLoadMore != addLoadMore) {
            if (addLoadMore) {
                this.addLoadMore = addLoadMore
                notifyItemInserted(loadingMoreItemPosition)
            } else {
                notifyItemRemoved(loadingMoreItemPosition)
                this.addLoadMore = addLoadMore
            }
        }
    }

    private inner class ViewHolder(itemView: View) : BaseBindingViewHolder<ViewNotificationBinding, NotificationUI>(itemView) {

        private lateinit var notification: NotificationUI

        override fun bindData(item: NotificationUI) {
            notification = item

            val avatarUrl = if (!notification.fromUser.avatar.thumbnailUrl.isNullOrEmpty())
                notification.fromUser.avatar.thumbnailUrl
            else
                notification.fromUser.avatar.originUrl
            glideRequests
                    .load(avatarUrl)
                    .centerInside()
                    .into(mBinding.imgProfile)
            displayMessageContent()
            mBinding.tvDate.text = DateUtils.getRelativeTimeSpanString(notification.timeCreated.time,
                    Calendar.getInstance().timeInMillis, 0L, DateUtils.FORMAT_ABBREV_RELATIVE)
        }

        override fun bindData(item: NotificationUI, payloads: List<Any>) {

        }

        private fun displayMessageContent() {
            val context = itemView.context
            val messageSpanBuilder = SpannableStringBuilder(notification.fromUser.name)
            val message = when (notification.type) {
                Notification.TYPE_LIKE_FEED -> context.getString(R.string.notification_message_like, "post")
                Notification.TYPE_LIKE_COMMENT -> context.getString(R.string.notification_message_like, "comment")
                Notification.TYPE_LIKE_REPLY -> context.getString(R.string.notification_message_like, "reply")
                Notification.TYPE_NEW_COMMENT -> context.getString(R.string.notification_message_comment)
                Notification.TYPE_NEW_REPLY -> context.getString(R.string.notification_message_reply)
                else -> ""
            }

            messageSpanBuilder.apply {
                setSpan(StyleSpan(Typeface.BOLD), 0, messageSpanBuilder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                append("  ")
                val start = length
                append(message)
                setSpan(ForegroundColorSpan(ColorUtils.modifyAlpha(Color.BLACK, 0.8f)), start, messageSpanBuilder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            mBinding.tvNotificationMessage.text = messageSpanBuilder
        }
    }

    private class ViewLoadingHolder(itemView: View) : BaseBindingViewHolder<ViewNotificationBinding, NotificationUI>(itemView) {

        override fun bindData(item: NotificationUI) {

        }

        override fun bindData(item: NotificationUI, payloads: List<Any>) {

        }
    }

    private class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationUI>() {

        override fun areContentsTheSame(oldItem: NotificationUI, newItem: NotificationUI) = oldItem.id == newItem.id

        override fun areItemsTheSame(oldItem: NotificationUI, newItem: NotificationUI) = Objects.equals(oldItem, newItem)
    }

    companion object {
        private const val VIEW_TYPE_NOTIFICATION = 1
        private const val VIEW_TYPE_LOADING = 2
    }
}
