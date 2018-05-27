package com.petnbu.petnbu.feed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.util.ArrayMap;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.google.android.gms.common.internal.Preconditions;
import com.petnbu.petnbu.GlideApp;
import com.petnbu.petnbu.GlideRequests;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.util.TraceUtils;
import com.petnbu.petnbu.util.Utils;
import com.petnbu.petnbu.databinding.ViewFeedBinding;
import com.petnbu.petnbu.model.FeedUI;
import com.petnbu.petnbu.model.Photo;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

import static com.petnbu.petnbu.model.LocalStatus.STATUS_ERROR;
import static com.petnbu.petnbu.model.LocalStatus.STATUS_UPLOADING;

public class FeedsRecyclerViewAdapter extends RecyclerView.Adapter<FeedsRecyclerViewAdapter.ViewHolder> {

    private final GlideRequests mGlideRequests;
    private List<FeedUI> mFeeds;
    private final ArrayMap<String, Integer> lastSelectedPhotoPositions = new ArrayMap<>();
    private final FeedsViewModel mFeedsViewModel;
    private final RecyclerView.RecycledViewPool mFeedPhotosViewPool;

    private int maxPhotoHeight;
    private final int deviceWidth;
    private final int minPhotoHeight;
    private final OnItemClickListener mOnItemClickListener;
    private int mDataVersion;

    public FeedsRecyclerViewAdapter(Context context, List<FeedUI> feeds, OnItemClickListener onItemClickListener,
                                    FeedsViewModel feedsViewModel) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(feedsViewModel);
        mFeeds = feeds;
        minPhotoHeight = Utils.goldenRatio(Utils.getDeviceWidth(context), true);
        mGlideRequests = GlideApp.with(context);
        mOnItemClickListener = onItemClickListener;
        mFeedsViewModel = feedsViewModel;
        deviceWidth = Utils.getDeviceWidth(context);
        mFeedPhotosViewPool = new RecyclerView.RecycledViewPool();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TraceUtils.begin("create feed view holder");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_feed, parent, false);
        TraceUtils.end();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bindData(mFeeds.get(position));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if(!payloads.isEmpty()) {
            Bundle bundle = (Bundle) payloads.get(0);
            if(bundle.getBoolean("like_status")) {
                FeedUI feed = mFeeds.get(position);
                if (feed.likeInProgress) {
                    holder.mBinding.imgLike.setVisibility(View.INVISIBLE);
                    holder.mBinding.imgLikeInProgress.setVisibility(View.VISIBLE);
                } else {
                    holder.mBinding.imgLike.setVisibility(View.VISIBLE);
                    holder.mBinding.imgLikeInProgress.setVisibility(View.GONE);

                    if (feed.isLiked) {
                        holder.mBinding.imgLike.setImageResource(R.drawable.ic_favorite_red_24dp);
                    } else {
                        holder.mBinding.imgLike.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                    }
                }
                if(feed.getLikeCount() > 0) {
                    holder.mBinding.tvLikesCount.setVisibility(View.VISIBLE);
                    holder.mBinding.tvLikesCount.setText(String.format("%d %s", feed.getLikeCount(), feed.getLikeCount() > 1 ? "likes" : "like"));
                } else {
                    holder.mBinding.tvLikesCount.setVisibility(View.GONE);
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public int getItemCount() {
        return mFeeds != null ? mFeeds.size() : 0;
    }

    public void setMaxPhotoHeight(int maxPhotoHeight) {
        this.maxPhotoHeight = maxPhotoHeight;
    }

    @SuppressLint("StaticFieldLeak")
    public void setFeeds(List<FeedUI> feeds) {
        mDataVersion++;
        final int startVersion = mDataVersion;
        new AsyncTask<Void, Void, DiffUtil.DiffResult>() {
            @Override
            protected DiffUtil.DiffResult doInBackground(Void... voids) {
                return DiffUtil.calculateDiff(new FeedsDiffCallback(mFeeds, feeds));
            }

            @Override
            protected void onPostExecute(DiffUtil.DiffResult diffResult) {
                if (startVersion != mDataVersion) {
                    // ignore update
                    return;
                }
                mFeeds = feeds;
                diffResult.dispatchUpdatesTo(FeedsRecyclerViewAdapter.this);
            }
        }.execute();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        private ViewFeedBinding mBinding;
        private FeedUI mFeed;
        private final View.OnClickListener mOpenProfileClickListener = v -> {
            if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                mFeedsViewModel.openUserProfile(mFeeds.get(getAdapterPosition()).ownerId);
            }
        };
        private final View.OnClickListener mOpenCommentsClickListener = v -> {
            if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                mFeedsViewModel.openComments(mFeeds.get(getAdapterPosition()).feedId);
            }
        };

        public ViewHolder(View itemView) {
            super(itemView);
            mBinding = DataBindingUtil.bind(itemView);

            mBinding.imgProfile.setOnClickListener(mOpenProfileClickListener);
            mBinding.tvName.setOnClickListener(mOpenProfileClickListener);
            mBinding.imgLike.setOnClickListener(v -> {
                if (mOnItemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    mOnItemClickListener.onLikeClicked(mFeeds.get(getAdapterPosition()).feedId);
                }
            });
            mBinding.imgComment.setOnClickListener(mOpenCommentsClickListener);
            mBinding.tvViewComments.setOnClickListener(mOpenCommentsClickListener);
            mBinding.tvContent.setOnClickListener(mOpenCommentsClickListener);

            mBinding.imgOptions.setOnClickListener(v -> {
                if (mOnItemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    mOnItemClickListener.onOptionClicked(v, mFeeds.get(getAdapterPosition()));
                }
            });

            mBinding.rvPhotos.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            SnapHelper snapHelper = new PagerSnapHelper();
            snapHelper.attachToRecyclerView(mBinding.rvPhotos);
            mBinding.rvPhotos.setRecycledViewPool(mFeedPhotosViewPool);
        }

        public void bindData(FeedUI feed) {
//            Timber.i("bind feed: %s", feed.toString());
            TraceUtils.begin("bind Feed");
            mFeed = feed;

            if (feed.status == STATUS_UPLOADING) {
                mBinding.layoutRoot.setShouldInterceptEvents(true);
                mBinding.layoutDisable.setVisibility(View.VISIBLE);
                mBinding.viewLoading.progressBar.setVisibility(View.VISIBLE);

                mBinding.layoutError.setVisibility(View.GONE);
                mBinding.imgLike.setVisibility(View.GONE);
                mBinding.imgComment.setVisibility(View.GONE);
                mBinding.tvLikesCount.setVisibility(View.GONE);
                mBinding.tvViewComments.setVisibility(View.GONE);
                mBinding.imgOptions.setVisibility(View.GONE);
                mBinding.imgLikeInProgress.setVisibility(View.GONE);
            } else {
                mBinding.layoutRoot.setShouldInterceptEvents(false);

                if (feed.status == STATUS_ERROR) {
                    mBinding.layoutError.setVisibility(View.VISIBLE);
                    mBinding.viewLoading.progressBar.setVisibility(View.GONE);
                    mBinding.imgLike.setVisibility(View.GONE);
                    mBinding.imgComment.setVisibility(View.GONE);
                    mBinding.tvLikesCount.setVisibility(View.GONE);
                    mBinding.tvViewComments.setVisibility(View.GONE);
                    mBinding.imgOptions.setVisibility(View.GONE);
                    mBinding.imgLikeInProgress.setVisibility(View.GONE);
                } else {
                    mBinding.layoutDisable.setVisibility(View.GONE);
                    mBinding.imgLike.setVisibility(View.VISIBLE);
                    mBinding.imgComment.setVisibility(View.VISIBLE);
                    mBinding.tvLikesCount.setVisibility(View.VISIBLE);
                    mBinding.tvViewComments.setVisibility(View.VISIBLE);
                    mBinding.imgOptions.setVisibility(View.VISIBLE);
                    mBinding.imgLikeInProgress.setVisibility(View.GONE);
                }
            }
            displayUserInfo();
            displayTime();
            displayPhotos();
            displayLikeInfo();
            displayContent();
            displayCommentCount();
            TraceUtils.end();
        }

        private void displayUserInfo() {
            mBinding.tvName.setText(mFeed.name);
            if (mFeed.avatar != null) {
                String avatarUrl = !TextUtils.isEmpty(mFeed.avatar.getThumbnailUrl())
                        ? mFeed.avatar.getThumbnailUrl() : mFeed.avatar.getOriginUrl();
                mGlideRequests.load(avatarUrl)
                        .centerInside()
                        .into(mBinding.imgProfile);
            }
        }

        private void displayTime() {
            if (mFeed.timeCreated != null) {
                mBinding.tvDate.setText(DateUtils.getRelativeTimeSpanString(mFeed.timeCreated.getTime(),
                        Calendar.getInstance().getTimeInMillis(), 0L, DateUtils.FORMAT_ABBREV_ALL));
            }
        }

        private void displayPhotos() {
            if (mFeed.photos != null && !mFeed.photos.isEmpty()) {
                constraintHeightForPhoto(mFeed.photos.get(0).getWidth(), mFeed.photos.get(0).getHeight());

                mBinding.rvPhotos.setAdapter(new FeedPhotosAdapter(mFeed, mGlideRequests, () -> {
                }, deviceWidth));

                int currentPos = 0;
                Integer value = lastSelectedPhotoPositions.get(mFeed.feedId);
                if (value != null) {
                    currentPos = value.intValue();
                }
                mBinding.rvPhotos.scrollToPosition(currentPos);

                if (mFeed.photos.size() > 1) {
                    mBinding.tvPhotosCount.setVisibility(View.VISIBLE);
                    mBinding.tvPhotosCount.setText(String.format(Locale.getDefault(), "%d/%d", currentPos + 1, mFeed.photos.size()));
                } else {
                    mBinding.tvPhotosCount.setVisibility(View.GONE);
                }

                mBinding.rvPhotos.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                        super.onScrollStateChanged(recyclerView, newState);

                        if(newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                            int position = linearLayoutManager.findFirstVisibleItemPosition();
                            mBinding.tvPhotosCount.setText(String.format(Locale.getDefault(), "%d/%d", position + 1, mFeed.photos.size()));
                            lastSelectedPhotoPositions.put(mFeed.feedId, position);
                        }
                    }
                });
            }
        }

        private void constraintHeightForPhoto(int photoWidth, int photoHeight) {
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) mBinding.layoutMedia.getLayoutParams();
            int height = (int) ((photoHeight / (float) (photoWidth)) * Utils.getDeviceWidth(itemView.getContext()));

            layoutParams.height = height > maxPhotoHeight ? maxPhotoHeight : height < minPhotoHeight ? minPhotoHeight : height;
            mBinding.layoutMedia.setLayoutParams(layoutParams);
        }

        private void displayContent() {
            SpannableStringBuilder builder = new SpannableStringBuilder();
            if(!TextUtils.isEmpty(mFeed.feedContent)) {
                builder.append(mFeed.name);
                builder.setSpan(new StyleSpan(Typeface.BOLD), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new ForegroundColorSpan(Color.BLACK), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.append("  ");
                builder.append(mFeed.feedContent);
            }
            if(!TextUtils.isEmpty(mFeed.commentContent) || mFeed.commentPhoto != null) {
                if(!TextUtils.isEmpty(builder)) {
                    builder.append("\n");
                }
                String commentUserName = mFeed.commentOwnerName;
                builder.append(commentUserName);
                builder.setSpan(new StyleSpan(Typeface.BOLD), builder.length() - commentUserName.length()
                        , builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.append("  ");
                builder.append(TextUtils.isEmpty(mFeed.commentContent) ? "replied" : mFeed.commentContent);
            }
            mBinding.tvContent.setVisibility(TextUtils.isEmpty(builder) ? View.GONE : View.VISIBLE);
            mBinding.tvContent.setText(builder);
        }

        private void displayLikeInfo() {
            if (mFeed.likeInProgress) {
                mBinding.imgLike.setVisibility(View.INVISIBLE);
                mBinding.imgLikeInProgress.setVisibility(View.VISIBLE);
            } else {
                mBinding.imgLike.setVisibility(View.VISIBLE);
                mBinding.imgLikeInProgress.setVisibility(View.GONE);

                if (mFeed.isLiked) {
                    mBinding.imgLike.setImageResource(R.drawable.ic_favorite_red_24dp);
                } else {
                    mBinding.imgLike.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                }
            }
            if(mFeed.getLikeCount() > 0) {
                mBinding.tvLikesCount.setText(String.format("%d %s", mFeed.getLikeCount(), mFeed.getLikeCount() > 1 ? "likes" : "like"));
                mBinding.tvLikesCount.setVisibility(View.VISIBLE);
            } else {
                mBinding.tvLikesCount.setVisibility(View.GONE);
            }
        }

        private void displayCommentCount() {
            if(mFeed.getCommentCount() > 1) {
                mBinding.tvViewComments.setVisibility(View.VISIBLE);
                mBinding.tvViewComments.setText(String.format("View all %d comments", mFeed.getCommentCount()));
            } else {
                mBinding.tvViewComments.setVisibility(View.GONE);
            }
        }
    }

    private static class FeedsDiffCallback extends DiffUtil.Callback {

        private final List<FeedUI> oldData;
        private final List<FeedUI> newData;

        private FeedsDiffCallback(List<FeedUI> oldData, List<FeedUI> newData) {
            this.oldData = oldData;
            this.newData = newData;
        }

        @Override
        public int getOldListSize() {
            return oldData.size();
        }

        @Override
        public int getNewListSize() {
            return newData.size();
        }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldData.get(oldPos).feedId.equals(newData.get(newPos).feedId);
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            return oldData.get(oldPos).equals(newData.get(newPos));
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            Bundle bundle = new Bundle();
            FeedUI oldFeed = oldData.get(oldItemPosition);
            FeedUI newFeed = newData.get(newItemPosition);
            if(oldFeed.likeInProgress != newFeed.likeInProgress || oldFeed.isLiked != newFeed.isLiked) {
                bundle.putBoolean("like_status", true);
                return bundle;
            }
            return super.getChangePayload(oldItemPosition, newItemPosition);
        }
    }

    public interface OnItemClickListener {

        void onPhotoClicked(Photo photo);

        void onLikeClicked(String feedId);

        void onOptionClicked(View view, FeedUI feed);
    }
}
