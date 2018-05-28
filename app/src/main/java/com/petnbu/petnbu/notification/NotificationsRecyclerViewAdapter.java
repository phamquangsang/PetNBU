package com.petnbu.petnbu.notification;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.internal.Preconditions;
import com.petnbu.petnbu.BaseBindingViewHolder;
import com.petnbu.petnbu.GlideApp;
import com.petnbu.petnbu.GlideRequests;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.ViewNotificationBinding;
import com.petnbu.petnbu.model.Notification;
import com.petnbu.petnbu.util.ColorUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class NotificationsRecyclerViewAdapter extends RecyclerView.Adapter<BaseBindingViewHolder> {

    private final int VIEW_TYPE_NOTIFICATION = 1;
    private final int VIEW_TYPE_LOADING = 2;

    private GlideRequests mGlideRequests;
    private List<Notification> mNotifications;
    private boolean mAddLoadMore;

    public NotificationsRecyclerViewAdapter(Context context, List<Notification> notifications) {
        Preconditions.checkNotNull(context);
        mGlideRequests = GlideApp.with(context);
        mNotifications = notifications != null ? notifications : new ArrayList<>(0);
    }

    @NonNull
    @Override
    public BaseBindingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_loading, parent, false);
            return new ViewLoadingHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_notification, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull BaseBindingViewHolder holder, int position) {
        holder.bindData(getItem(position));
    }

    @Override
    public int getItemCount() {
        return getDataItemCount() + (mAddLoadMore ? 1 : 0);
    }

    private int getDataItemCount() {
        return mNotifications.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position < getDataItemCount() && getDataItemCount() > 0) {
            return VIEW_TYPE_NOTIFICATION;
        }
        return VIEW_TYPE_LOADING;
    }

    private Notification getItem(int position) {
        if (position < 0 || position >= mNotifications.size())
            return null;
        return mNotifications.get(position);
    }

    public void setAddLoadMore(boolean addLoadMore) {
        if (mAddLoadMore != addLoadMore) {
            if (addLoadMore) {
                mAddLoadMore = addLoadMore;
                notifyItemInserted(getLoadingMoreItemPosition());
            } else {
                notifyItemRemoved(getLoadingMoreItemPosition());
                mAddLoadMore = addLoadMore;
            }
        }
    }

    private int getLoadingMoreItemPosition() {
        return mAddLoadMore ? getItemCount() - 1 : RecyclerView.NO_POSITION;
    }

    protected class ViewHolder extends BaseBindingViewHolder<ViewNotificationBinding, Notification> {

        private Notification mNotification;

        public ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void bindData(Notification item) {
            mNotification = item;

            String avatarUrl = TextUtils.isEmpty(mNotification.getFromUser().getAvatar().getThumbnailUrl()) ?
                    mNotification.getFromUser().getAvatar().getOriginUrl() : mNotification.getFromUser().getAvatar().getThumbnailUrl();
            mGlideRequests
                    .load(avatarUrl)
                    .centerInside()
                    .into(mBinding.imgProfile);
            displayMessageContent();
            mBinding.tvDate.setText(DateUtils.getRelativeTimeSpanString(mNotification.getTimeCreated(),
                    Calendar.getInstance().getTimeInMillis(), 0L, DateUtils.FORMAT_ABBREV_RELATIVE));
        }

        @Override
        public void bindData(Notification item, List<Object> payloads) {

        }

        private void displayMessageContent() {
            SpannableStringBuilder messageSpanBuilder = new SpannableStringBuilder(mNotification.getFromUser().getName());
            messageSpanBuilder.setSpan(new StyleSpan(Typeface.BOLD), 0, messageSpanBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            messageSpanBuilder.append("  ");

            String message;
            Context context = itemView.getContext();
            switch (mNotification.getType()) {
                case Notification.TYPE_LIKE_FEED :
                    message = context.getString(R.string.notification_message_like, "post");
                    break;
                case Notification.TYPE_LIKE_COMMENT :
                    message = context.getString(R.string.notification_message_like, "comment");
                    break;
                case Notification.TYPE_LIKE_REPLY :
                    message =  context.getString(R.string.notification_message_like, "reply");
                    break;
                case Notification.TYPE_NEW_COMMENT :
                    message = context.getString(R.string.notification_message_comment);
                    break;
                case Notification.TYPE_NEW_REPLY :
                    message = context.getString(R.string.notification_message_reply);
                    break;
                default: message = "";
            }
            int start = messageSpanBuilder.length();
            messageSpanBuilder.append(message);
            messageSpanBuilder.setSpan(new ForegroundColorSpan(ColorUtils.modifyAlpha(Color.BLACK, 0.8f)), start, messageSpanBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mBinding.tvNotificationMessage.setText(messageSpanBuilder);
        }
    }

    protected class ViewLoadingHolder extends BaseBindingViewHolder<ViewNotificationBinding, Notification> {

        public ViewLoadingHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void bindData(Notification item) {

        }

        @Override
        public void bindData(Notification item, List<Object> payloads) {

        }
    }
}
