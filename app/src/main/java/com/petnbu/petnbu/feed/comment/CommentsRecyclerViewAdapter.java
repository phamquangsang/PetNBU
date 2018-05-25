package com.petnbu.petnbu.feed.comment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
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

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.common.internal.Preconditions;
import com.petnbu.petnbu.BaseBindingViewHolder;
import com.petnbu.petnbu.GlideApp;
import com.petnbu.petnbu.GlideRequests;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.util.Utils;
import com.petnbu.petnbu.databinding.ViewCommentBinding;
import com.petnbu.petnbu.databinding.ViewLoadingBinding;
import com.petnbu.petnbu.model.CommentUI;
import com.petnbu.petnbu.model.LocalStatus;
import com.petnbu.petnbu.util.ColorUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CommentsRecyclerViewAdapter extends RecyclerView.Adapter<BaseBindingViewHolder> {

    private static final int VIEW_TYPE_COMMENT = 1;
    private static final int VIEW_TYPE_LOADING = 2;

    private List<CommentUI> mComments;
    private CommentsViewModel mCommentsViewModel;
    private String mFeedId;
    private GlideRequests mGlideRequests;

    private int mDataVersion;
    private boolean mAddLoadMore;

    public CommentsRecyclerViewAdapter(Context context, List<CommentUI> comments, String feedId,
                                       CommentsViewModel commentsViewModel) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(commentsViewModel);

        mComments = comments != null ? comments : new ArrayList<>();
        mCommentsViewModel = commentsViewModel;
        mFeedId = feedId;
        mGlideRequests = GlideApp.with(context);
    }

    @NonNull
    @Override
    public BaseBindingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_loading, parent, false);
            return new ViewLoadingHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_comment, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull BaseBindingViewHolder holder, int position) {
        if (getItemViewType(position) != VIEW_TYPE_LOADING) {
            holder.bindData(mComments.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return getDataItemCount() + (mAddLoadMore ? 1 : 0);
    }

    private int getDataItemCount() {
        return mComments.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position < getDataItemCount() && getDataItemCount() > 0) {
            return VIEW_TYPE_COMMENT;
        }
        return VIEW_TYPE_LOADING;
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

    public void setComments(List<CommentUI> comments) {
        mDataVersion++;
        final int startVersion = mDataVersion;
        new AsyncTask<Void, Void, DiffUtil.DiffResult>() {
            @Override
            protected DiffUtil.DiffResult doInBackground(Void... voids) {
                return DiffUtil.calculateDiff(new CommentsDiffCallback(mComments, comments));
            }

            @Override
            protected void onPostExecute(DiffUtil.DiffResult diffResult) {
                if (startVersion != mDataVersion) {
                    // ignore update
                    return;
                }
                mComments = comments;
                diffResult.dispatchUpdatesTo(CommentsRecyclerViewAdapter.this);
            }
        }.execute();
    }

    protected class ViewHolder extends BaseBindingViewHolder<ViewCommentBinding, CommentUI> {

        private CommentUI mComment;
        private final View.OnClickListener mOpenRepliesClickListener = (v) -> {
            if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                CommentUI commentUI = mComments.get(getAdapterPosition());
                if (commentUI.getLocalStatus() != LocalStatus.STATUS_UPLOADING)
                    mCommentsViewModel.openRepliesForComment(commentUI.getId());
            }
        };

        public ViewHolder(View itemView) {
            super(itemView);
            mBinding.tvReply.setOnClickListener(mOpenRepliesClickListener);
            mBinding.tvLatestComment.setOnClickListener(mOpenRepliesClickListener);
            mBinding.tvPreviousReplies.setOnClickListener(mOpenRepliesClickListener);

            mBinding.imgProfile.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    CommentUI commentUI = mComments.get(getAdapterPosition());
                    if (commentUI.getLocalStatus() != LocalStatus.STATUS_UPLOADING)
                        mCommentsViewModel.openUserProfile(commentUI.getOwner().getUserId());
                }
            });

            mBinding.imgLike.setOnClickListener(view ->{
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    CommentUI commentUI = mComments.get(getAdapterPosition());
                    if (commentUI.getLocalStatus() != LocalStatus.STATUS_UPLOADING)
                        mCommentsViewModel.likeClicked(commentUI.getId());
                }
            });
        }

        @Override
        public void bindData(CommentUI item) {
            mComment = item;
            displayUserInfo();
            displayContent();
            displayInfo();
            displayReplies();
        }

        private void displayUserInfo() {
            String avatarUrl = TextUtils.isEmpty(mComment.getOwner().getAvatar().getThumbnailUrl()) ?
                    mComment.getOwner().getAvatar().getOriginUrl() : mComment.getOwner().getAvatar().getThumbnailUrl();
            mGlideRequests.load(avatarUrl)
                    .centerInside()
                    .into(mBinding.imgProfile);
        }

        private void displayContent() {
            SpannableStringBuilder builder = new SpannableStringBuilder(mComment.getOwner().getName() + "");
            int start = 0;
            builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new ForegroundColorSpan(Color.BLACK), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append("  ");

            start = builder.length();
            builder.append(mComment.getContent());
            builder.setSpan(new ForegroundColorSpan(ColorUtils.modifyAlpha(Color.BLACK, 0.8f)), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mBinding.tvContent.setText(builder);

            if (mComment.getPhoto() != null) {
                mBinding.imgPhoto.setVisibility(View.VISIBLE);
                Photo photo = mComment.getPhoto();

                float ratio = photo.getWidth() / (float) photo.getHeight();
                if (photo.getWidth() > photo.getHeight()) {
                    mBinding.imgPhoto.set(1.0f, 1/ratio);
                } else {
                    mBinding.imgPhoto.set(1.0f/2, 1/(2*ratio));
                }
                String photoUrl = TextUtils.isEmpty(mComment.getPhoto().getSmallUrl()) ?
                        mComment.getPhoto().getOriginUrl() : mComment.getPhoto().getSmallUrl();

                mGlideRequests.load(photoUrl)
                        .centerInside()
                        .into(mBinding.imgPhoto);
            } else {
                mBinding.imgPhoto.setVisibility(View.GONE);
            }
        }

        private void displayInfo() {
            mBinding.tvDate.setText(DateUtils.getRelativeTimeSpanString(mComment.getTimeCreated().getTime(),
                    Calendar.getInstance().getTimeInMillis(), 0L, DateUtils.FORMAT_ABBREV_RELATIVE));
            if (!mComment.getId().equals(mFeedId)) {
                mBinding.divider.setVisibility(View.INVISIBLE);
                mBinding.tvLikesCount.setVisibility(View.VISIBLE);
                mBinding.tvReply.setVisibility(View.VISIBLE);
                mBinding.imgLike.setVisibility(View.VISIBLE);

                if (mComment.getLikeCount() > 0) {
                    mBinding.tvLikesCount.setVisibility(View.VISIBLE);
                    mBinding.tvLikesCount.setText(String.format("%d %s", mComment.getLikeCount(), mComment.getLikeCount() > 1 ? "likes" : "like"));
                } else {
                    mBinding.tvLikesCount.setVisibility(View.GONE);
                }

                if (mComment.getLocalStatus() == LocalStatus.STATUS_UPLOADING || mComment.isLikeInProgress()) {
                    mBinding.imgLike.setVisibility(View.INVISIBLE);
                    mBinding.progressBar.setVisibility(View.VISIBLE);
                } else {
                    mBinding.imgLike.setVisibility(View.VISIBLE);
                    mBinding.progressBar.setVisibility(View.GONE);
                    if(mComment.isLiked){
                        mBinding.imgLike.setImageResource(R.drawable.ic_favorite_red_24dp);
                    }else{
                        mBinding.imgLike.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                    }
                }
            } else {
                mBinding.tvLikesCount.setVisibility(View.GONE);
                mBinding.tvReply.setVisibility(View.GONE);
                mBinding.imgLike.setVisibility(View.GONE);
                mBinding.divider.setVisibility(View.VISIBLE);
                mBinding.progressBar.setVisibility(View.GONE);
            }
        }

        private void displayReplies() {
            if (mComment.getLatestCommentId() != null) {
                mBinding.tvLatestComment.setVisibility(View.VISIBLE);
                mBinding.tvPreviousReplies.setVisibility(View.VISIBLE);

                Context context = itemView.getContext();
                int imageSize = (int) Utils.convertDpToPixel(context, 36);
                Drawable leftDrawable = ContextCompat.getDrawable(context, R.drawable.logo);
                leftDrawable.setBounds(0, 0, imageSize, imageSize);

                mBinding.tvLatestComment.setCompoundDrawables(leftDrawable, null, null, null);
                mBinding.tvLatestComment.setCompoundDrawablePadding((int) Utils.convertDpToPixel(context, 8));

                SpannableStringBuilder builder = new SpannableStringBuilder(mComment.getLatestCommentOwnerName() + "");
                int start = 0;
                builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new ForegroundColorSpan(Color.BLACK), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.append("  ");

                start = builder.length();
                builder.append(mComment.getLatestCommentPhoto() != null ? "replied" : mComment.getLatestCommentContent());
                builder.setSpan(new ForegroundColorSpan(ColorUtils.modifyAlpha(Color.BLACK, 0.8f)), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                mBinding.tvLatestComment.setText(builder);

                if (mComment.getCommentCount() > 1) {
                    mBinding.tvPreviousReplies.setVisibility(View.VISIBLE);
                    mBinding.tvPreviousReplies.setText(context.getString(R.string.feed_view_previous_replies, mComment.getCommentCount() - 1));
                } else {
                    mBinding.tvPreviousReplies.setVisibility(View.GONE);
                }

                mGlideRequests.asBitmap()
                        .load(mComment.getLatestCommentOwnerAvatar().getOriginUrl())
                        .apply(RequestOptions.overrideOf(imageSize, imageSize))
                        .listener(new RequestListener<Bitmap>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(context.getResources(), resource);
                                roundedBitmapDrawable.setCircular(true);
                                roundedBitmapDrawable.setAntiAlias(true);
                                roundedBitmapDrawable.setBounds(0, 0, imageSize, imageSize);
                                mBinding.tvLatestComment.setCompoundDrawables(roundedBitmapDrawable, null, null, null);
                                return false;
                            }
                        }).submit();
            } else {
                mBinding.tvLatestComment.setVisibility(View.GONE);
                mBinding.tvPreviousReplies.setVisibility(View.GONE);
            }
        }
    }

    protected class ViewLoadingHolder extends BaseBindingViewHolder<ViewLoadingBinding, Void> {

        public ViewLoadingHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void bindData(Void item) {
        }
    }

    private static class CommentsDiffCallback extends DiffUtil.Callback {

        private final List<CommentUI> oldData;
        private final List<CommentUI> newData;

        private CommentsDiffCallback(List<CommentUI> oldData, List<CommentUI> newData) {
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
            return oldData.get(oldPos).getId().equals(newData.get(newPos).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            return oldData.get(oldPos).equals(newData.get(newPos));
        }
    }
}
