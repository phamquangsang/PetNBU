package com.petnbu.petnbu.notification;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.internal.Preconditions;
import com.petnbu.petnbu.BaseBindingViewHolder;
import com.petnbu.petnbu.GlideApp;
import com.petnbu.petnbu.GlideRequests;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.ViewNotificationBinding;

import java.util.ArrayList;
import java.util.List;

public class NotificationsRecyclerViewAdapter extends RecyclerView.Adapter<BaseBindingViewHolder> {

    private final int VIEW_TYPE_NOTIFICATION = 1;
    private final int VIEW_TYPE_LOADING = 2;

    private GlideRequests mGlideRequests;
    private List<String> mNotifications;
    private boolean mAddLoadMore;

    public NotificationsRecyclerViewAdapter(Context context, List<String> notifications) {
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

    private String getItem(int position) {
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

    protected class ViewHolder extends BaseBindingViewHolder<ViewNotificationBinding, String> {

        public ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void bindData(String item) {

        }

        @Override
        public void bindData(String item, List<Object> payloads) {

        }
    }

    protected class ViewLoadingHolder extends BaseBindingViewHolder<ViewNotificationBinding, String> {

        public ViewLoadingHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void bindData(String item) {

        }

        @Override
        public void bindData(String item, List<Object> payloads) {

        }
    }
}
