package com.petnbu.petnbu.notification

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.AsyncTask
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
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

import java.util.ArrayList
import java.util.Calendar

class NotificationsRecyclerViewAdapter(context: Context?, notifications: List<NotificationUI>?) : RecyclerView.Adapter<BaseBindingViewHolder<*, *>>() {

    private val VIEW_TYPE_NOTIFICATION = 1
    private val VIEW_TYPE_LOADING = 2

    private val glideRequests: GlideRequests
    private var notifications: List<NotificationUI>
    private var addLoadMore: Boolean = false
    private var dataVersion = 0

    private val dataItemCount: Int
        get() = notifications.size

    private val loadingMoreItemPosition: Int
        get() = if (addLoadMore) itemCount - 1 else RecyclerView.NO_POSITION

    init {
        context!!
        glideRequests = GlideApp.with(context)
        this.notifications = notifications ?: ArrayList(0)
    }

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
        if(item != null) {
            (holder as? ViewHolder)?.bindData(item)
        }
    }

    override fun getItemCount(): Int {
        return dataItemCount + if (addLoadMore) 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < dataItemCount && dataItemCount > 0) {
            VIEW_TYPE_NOTIFICATION
        } else VIEW_TYPE_LOADING
    }

    private fun getItem(position: Int): NotificationUI? {
        return if (position < 0 || position >= notifications.size) null else notifications[position]
    }

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

    inner class ViewHolder(itemView: View) : BaseBindingViewHolder<ViewNotificationBinding, NotificationUI>(itemView) {

        private lateinit var notification: NotificationUI

        override fun bindData(item: NotificationUI) {
            notification = item

            val avatarUrl = if (TextUtils.isEmpty(notification.fromUser.avatar.thumbnailUrl))
                notification.fromUser.avatar.originUrl
            else
                notification.fromUser.avatar.thumbnailUrl
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
            val messageSpanBuilder = SpannableStringBuilder(notification!!.fromUser.name)
            messageSpanBuilder.setSpan(StyleSpan(Typeface.BOLD), 0, messageSpanBuilder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            messageSpanBuilder.append("  ")

            val context = itemView.context
            val message = when (notification.type) {
                Notification.TYPE_LIKE_FEED -> context.getString(R.string.notification_message_like, "post")
                Notification.TYPE_LIKE_COMMENT -> context.getString(R.string.notification_message_like, "comment")
                Notification.TYPE_LIKE_REPLY -> context.getString(R.string.notification_message_like, "reply")
                Notification.TYPE_NEW_COMMENT -> context.getString(R.string.notification_message_comment)
                Notification.TYPE_NEW_REPLY -> context.getString(R.string.notification_message_reply)
                else -> ""
            }
            val start = messageSpanBuilder.length
            messageSpanBuilder.append(message)
            messageSpanBuilder.setSpan(ForegroundColorSpan(ColorUtils.modifyAlpha(Color.BLACK, 0.8f)), start, messageSpanBuilder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            mBinding.tvNotificationMessage.text = messageSpanBuilder
        }
    }

    inner class ViewLoadingHolder(itemView: View) : BaseBindingViewHolder<ViewNotificationBinding, NotificationUI>(itemView) {

        override fun bindData(item: NotificationUI) {

        }

        override fun bindData(item: NotificationUI, payloads: List<Any>) {

        }
    }

    class NotificationDiffCallback(private val oldData: List<NotificationUI>, private val newData: List<NotificationUI>) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldData.size
        }

        override fun getNewListSize(): Int {
            return newData.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldData[oldItemPosition].id == newData[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return Objects.equals(oldData[oldItemPosition], newData[newItemPosition])
        }
    }

    @SuppressLint("StaticFieldLeak")
    fun setData(notifications: List<NotificationUI>) {
        dataVersion++
        val startVersion = dataVersion
        object : AsyncTask<Void, Void, DiffUtil.DiffResult>() {
            override fun doInBackground(vararg voids: Void): DiffUtil.DiffResult {
                return DiffUtil.calculateDiff(NotificationDiffCallback(this@NotificationsRecyclerViewAdapter.notifications, notifications))
            }

            override fun onPostExecute(diffResult: DiffUtil.DiffResult) {
                if (startVersion != dataVersion) {
                    // ignore update
                    return
                }
                this@NotificationsRecyclerViewAdapter.notifications = notifications
                diffResult.dispatchUpdatesTo(this@NotificationsRecyclerViewAdapter)
            }
        }.execute()
    }
}
