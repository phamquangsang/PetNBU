package com.petnbu.petnbu.ui.notification

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
import com.petnbu.petnbu.GlideApp
import com.petnbu.petnbu.GlideRequests
import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.ViewNotificationBinding
import com.petnbu.petnbu.model.Notification
import com.petnbu.petnbu.model.NotificationUI
import com.petnbu.petnbu.ui.BaseBindingViewHolder
import com.petnbu.petnbu.util.ColorUtils
import java.util.*

class NotificationsRecyclerViewAdapter(context: Context)
    : ListAdapter<NotificationUI, BaseBindingViewHolder<*, *>>(NotificationDiffCallback()) {

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
            val view = LayoutInflater.from(parent.context).inflate(R.layout.view_notification, parent, false)
            ViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: BaseBindingViewHolder<*, *>, position: Int) {
        (holder as? ViewHolder)?.bindData(getItem(position))
    }

    override fun getItemCount() = dataItemCount + if (addLoadMore) 1 else 0

    override fun getItemViewType(position: Int) = if (position < dataItemCount && dataItemCount > 0) VIEW_TYPE_NOTIFICATION else VIEW_TYPE_LOADING

    private inner class ViewHolder(itemView: View) : BaseBindingViewHolder<ViewNotificationBinding, NotificationUI>(itemView) {

        private lateinit var notification: NotificationUI

        override fun bindData(item: NotificationUI) {
            notification = item

            val avatarUrl = notification.fromUser?.avatar?.thumbnailUrl ?: notification.fromUser?.avatar?.originUrl
            glideRequests
                    .load(avatarUrl)
                    .centerInside()
                    .into(mBinding.imgProfile)
            displayMessageContent()
            notification.timeCreated?.time?.run {
                mBinding.tvDate.text = DateUtils.getRelativeTimeSpanString(this,
                        Calendar.getInstance().timeInMillis, 0L, DateUtils.FORMAT_ABBREV_RELATIVE)
            }

        }

        override fun bindData(item: NotificationUI, payloads: List<Any>) {

        }

        private fun displayMessageContent() {
            val context = itemView.context
            val messageSpanBuilder = SpannableStringBuilder(notification.fromUser?.name)
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

        override fun areItemsTheSame(oldItem: NotificationUI, newItem: NotificationUI) = oldItem == newItem
    }

    companion object {
        private const val VIEW_TYPE_NOTIFICATION = 1
        private const val VIEW_TYPE_LOADING = 2
    }
}
