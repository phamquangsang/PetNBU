package com.petnbu.petnbu.feed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.ViewPager;
import android.support.v7.util.DiffUtil;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.common.internal.Preconditions;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.Utils;
import com.petnbu.petnbu.databinding.ViewFeedBinding;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedEntity;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.util.ColorUtils;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FeedsRecyclerViewAdapter extends RecyclerView.Adapter<FeedsRecyclerViewAdapter.ViewHolder> {

    private RequestManager mRequestManager;
    private List<Feed> mFeeds;
    private ArrayMap<String, Integer> lastSelectedPhotoPositions = new ArrayMap<>();

    private int maxPhotoHeight;
    private final int minPhotoHeight;
    private final OnItemClickListener mOnItemClickListener;
    private int mDataVersion;

    public FeedsRecyclerViewAdapter(Context context, List<Feed> feeds, OnItemClickListener onItemClickListener) {
        Preconditions.checkNotNull(context);
        mFeeds = feeds;
        minPhotoHeight = Utils.goldenRatio(Utils.getDeviceWidth(context), true);
        mRequestManager = Glide.with(context);
        mOnItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_feed, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bindData(mFeeds.get(position));
    }

    @Override
    public int getItemCount() {
        return mFeeds != null ? mFeeds.size() : 0;
    }

    public void setMaxPhotoHeight(int maxPhotoHeight) {
        this.maxPhotoHeight = maxPhotoHeight;
    }

    @SuppressLint("StaticFieldLeak")
    public void setFeeds(List<Feed> feeds) {
        mDataVersion++;
        final int startVersion = mDataVersion;
        new AsyncTask<Void, Void, DiffUtil.DiffResult>() {
            @Override
            protected DiffUtil.DiffResult  doInBackground(Void... voids) {
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
        private Feed mFeed;
        private final View.OnClickListener profileClickListener = v -> {
            if (mOnItemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                mOnItemClickListener.onProfileClicked(mFeeds.get(getAdapterPosition()).getUserId());
            }
        };

        public ViewHolder(View itemView) {
            super(itemView);
            mBinding = DataBindingUtil.bind(itemView);

            mBinding.imgProfile.setOnClickListener(profileClickListener);
            mBinding.tvName.setOnClickListener(profileClickListener);
            mBinding.imgLike.setOnClickListener(v -> {
                if (mOnItemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    mOnItemClickListener.onLikeClicked(mFeeds.get(getAdapterPosition()).getFeedId());
                }
            });
            mBinding.imgComment.setOnClickListener(v -> {
                if (mOnItemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    mOnItemClickListener.onCommentClicked(mFeeds.get(getAdapterPosition()).getFeedId());
                }
            });
            mBinding.tvViewComments.setOnClickListener(v -> {
                if (mOnItemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    mOnItemClickListener.onCommentClicked(mFeeds.get(getAdapterPosition()).getFeedId());
                }
            });
            mBinding.imgOptions.setOnClickListener(v -> {
                if(mOnItemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    mOnItemClickListener.onOptionClicked(v, mFeeds.get(getAdapterPosition()));
                }
            });
        }

        public void bindData(Feed feed) {
            mFeed = feed;
            displayUserInfo();
            displayTime();
            displayPhotos();
            displayText();

            if (feed.getStatus() == FeedEntity.STATUS_UPLOADING) {
                mBinding.layoutRoot.setShouldInterceptEvents(true);
                mBinding.layoutDisable.setVisibility(View.VISIBLE);
                mBinding.spinKit.setVisibility(View.VISIBLE);
                mBinding.layoutError.setVisibility(View.GONE);

                mBinding.imgLike.setVisibility(View.GONE);
                mBinding.imgComment.setVisibility(View.GONE);
                mBinding.tvLikesCount.setVisibility(View.GONE);
                if(TextUtils.isEmpty(feed.getContent())) {
                    mBinding.tvContent.setVisibility(View.GONE);
                } else {
                    mBinding.tvContent.setVisibility(View.VISIBLE);
                }
                mBinding.tvViewComments.setVisibility(View.GONE);
            } else {
                mBinding.layoutRoot.setShouldInterceptEvents(false);

                if (feed.getStatus() == FeedEntity.STATUS_ERROR) {
                    mBinding.spinKit.setVisibility(View.GONE);
                    mBinding.layoutError.setVisibility(View.VISIBLE);
                    mBinding.imgLike.setVisibility(View.GONE);
                    mBinding.imgComment.setVisibility(View.GONE);
                    mBinding.tvLikesCount.setVisibility(View.GONE);
                    mBinding.tvViewComments.setVisibility(View.GONE);
                } else {
                    mBinding.layoutDisable.setVisibility(View.GONE);
                    mBinding.imgLike.setVisibility(View.VISIBLE);
                    mBinding.imgComment.setVisibility(View.VISIBLE);
                    mBinding.tvLikesCount.setVisibility(View.VISIBLE);
                    mBinding.tvContent.setVisibility(View.VISIBLE);
                    mBinding.tvViewComments.setVisibility(View.VISIBLE);
                }
                if(TextUtils.isEmpty(feed.getContent())) {
                    mBinding.tvContent.setVisibility(View.GONE);
                } else {
                    mBinding.tvContent.setVisibility(View.VISIBLE);
                }
            }
        }

        private void displayUserInfo() {
            mBinding.tvName.setText(mFeed.getName());
            mRequestManager.asBitmap()
                    .load(mFeed.getAvatar().getOriginUrl())
                    .apply(RequestOptions.centerCropTransform())
                    .into(new BitmapImageViewTarget(mBinding.imgProfile) {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            Context context = mBinding.imgProfile.getContext();
                            if(ColorUtils.isDark(resource)) {
                                mBinding.imgProfile.setBorderWidth(0);
                            } else {
                                mBinding.imgProfile.setBorderColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                                mBinding.imgProfile.setBorderWidth(1);
                            }
                            getView().setImageBitmap(resource);
                        }
                    });
        }

        private void displayTime() {
            mBinding.tvDate.setText(DateUtils.getRelativeTimeSpanString(mFeed.getTimeCreated().getTime(),
                    Calendar.getInstance().getTimeInMillis(), 0L, DateUtils.FORMAT_ABBREV_ALL));
        }

        private void displayPhotos() {
            if (mFeed.getPhotos() != null && !mFeed.getPhotos().isEmpty()) {
                constraintHeightForPhoto(mFeed.getPhotos().get(0).getWidth(), mFeed.getPhotos().get(0).getHeight());

                if(mBinding.vpPhotos.getAdapter() != null) {
                    PhotosPagerAdapter pagerAdapter = (PhotosPagerAdapter) mBinding.vpPhotos.getAdapter();
                    pagerAdapter.setData(mFeed);

                } else {
                    mBinding.vpPhotos.setAdapter(new PhotosPagerAdapter(mFeed, mRequestManager, () -> {
                        if (mOnItemClickListener != null && mFeed.getStatus() == FeedEntity.STATUS_DONE) {
                            mOnItemClickListener.onPhotoClicked(mFeed.getPhotos().get(mBinding.vpPhotos.getCurrentItem()));
                        }
                    }));
                }
                int currentPos = 0;
                Integer value = lastSelectedPhotoPositions.get(mFeed.getFeedId());
                if (value != null) {
                    currentPos = value.intValue();
                }
                mBinding.vpPhotos.setCurrentItem(currentPos);

                if (mFeed.getPhotos().size() > 1) {
                    mBinding.tvPhotosCount.setVisibility(View.VISIBLE);
                    mBinding.tvPhotosCount.setText(String.format(Locale.getDefault(), "%d/%d", currentPos + 1, mFeed.getPhotos().size()));
                } else {
                    mBinding.tvPhotosCount.setVisibility(View.GONE);
                }
                mBinding.vpPhotos.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                    }

                    @Override
                    public void onPageSelected(int position) {
                        mBinding.tvPhotosCount.setText(String.format(Locale.getDefault(), "%d/%d", position + 1, mFeed.getPhotos().size()));
                        lastSelectedPhotoPositions.put(mFeed.getFeedId(), position);
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {

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

        private void displayText() {
            SpannableStringBuilder builder = new SpannableStringBuilder(mFeed.getName() + "");
            builder.setSpan(new StyleSpan(Typeface.BOLD), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new ForegroundColorSpan(Color.BLACK), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append("  ");
            builder.append(mFeed.getContent());
            mBinding.tvContent.setText(builder);
        }
    }

    private static class FeedsDiffCallback extends DiffUtil.Callback {

        private final List<Feed> oldData;
        private final List<Feed> newData;

        private FeedsDiffCallback(List<Feed> oldData, List<Feed> newData) {
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
            return oldData.get(oldPos).getFeedId().equals(newData.get(newPos).getFeedId());
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            return oldData.get(oldPos).equals(newData.get(newPos));
        }
    }

    public interface OnItemClickListener {

        void onProfileClicked(String userId);

        void onPhotoClicked(Photo photo);

        void onLikeClicked(String feedId);

        void onCommentClicked(String feedId);

        void onOptionClicked(View view, Feed feed);
    }
}
