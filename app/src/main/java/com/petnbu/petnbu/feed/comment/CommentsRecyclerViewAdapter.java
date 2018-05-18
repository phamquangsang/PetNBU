package com.petnbu.petnbu.feed.comment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.petnbu.petnbu.BaseBindingViewHolder;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.Utils;
import com.petnbu.petnbu.databinding.ViewCommentBinding;
import com.petnbu.petnbu.model.Comment;
import com.petnbu.petnbu.model.CommentUI;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.util.ColorUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CommentsRecyclerViewAdapter extends RecyclerView.Adapter<CommentsRecyclerViewAdapter.ViewHolder> {

    private List<CommentUI> mComments;
    private RequestManager mRequestManager;
    private OnItemClickListener mOnItemClickListener;
    private String mFeedId;

    private int mDataVersion;

    public CommentsRecyclerViewAdapter(List<CommentUI> comments, String feedId, RequestManager requestManager,
                                       OnItemClickListener onItemClickListener) {
        mComments = comments != null ? comments : new ArrayList<>();
        mFeedId = feedId;
        mRequestManager = requestManager;
        mOnItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bindData(mComments.get(position));
    }

    @Override
    public int getItemCount() {
        return mComments.size();
    }

    public void setComments(List<CommentUI> comments) {
        mDataVersion++;
        final int startVersion = mDataVersion;
        new AsyncTask<Void, Void, DiffUtil.DiffResult>() {
            @Override
            protected DiffUtil.DiffResult  doInBackground(Void... voids) {
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

        public ViewHolder(View itemView) {
            super(itemView);
            mBinding.tvReply.setOnClickListener(v -> {
                if(mOnItemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    mOnItemClickListener.onReplyClicked(mComments.get(getAdapterPosition()).getId());
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
            mRequestManager.asBitmap()
                    .load(mComment.getOwner().getAvatar().getOriginUrl())
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
        }

        private void displayInfo() {
            mBinding.tvDate.setText(DateUtils.getRelativeTimeSpanString(mComment.getTimeCreated().getTime(),
                    Calendar.getInstance().getTimeInMillis(), 0L, DateUtils.FORMAT_ABBREV_RELATIVE));
            if(!mComment.getId().equals(mFeedId)) {
                mBinding.divider.setVisibility(View.INVISIBLE);
                mBinding.tvLikesCount.setVisibility(View.VISIBLE);
                mBinding.tvReply.setVisibility(View.VISIBLE);
                mBinding.layoutLike.setVisibility(View.VISIBLE);

                if (mComment.getLikeCount() > 0) {
                    mBinding.tvLikesCount.setVisibility(View.VISIBLE);
                    mBinding.tvLikesCount.setText(String.format("%d %s", mComment.getLikeCount(), mComment.getLikeCount() > 1 ? "likes" : "like"));
                } else {
                    mBinding.tvLikesCount.setVisibility(View.GONE);
                }
            } else {
                mBinding.tvLikesCount.setVisibility(View.GONE);
                mBinding.tvReply.setVisibility(View.GONE);
                mBinding.layoutLike.setVisibility(View.GONE);
                mBinding.divider.setVisibility(View.VISIBLE);
            }
        }

        private void displayReplies() {
            if(mComment.getLatestCommentId() != null) {
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
                builder.append(mComment.getLatestCommentContent());
                builder.setSpan(new ForegroundColorSpan(ColorUtils.modifyAlpha(Color.BLACK, 0.8f)), start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                mBinding.tvLatestComment.setText(builder);
                mBinding.tvPreviousReplies.setText(context.getString(R.string.feed_view_previous_replies, mComment.getCommentCount() - 1));

                mRequestManager.asBitmap().load(mComment.getLatestCommentOwnerAvatar().getOriginUrl())
                        .apply(RequestOptions.overrideOf(imageSize, imageSize))
                        .listener(new RequestListener<Bitmap>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                RoundedBitmapDrawable roundedBitmapDrawable= RoundedBitmapDrawableFactory.create(context.getResources(), resource);
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

    public interface OnItemClickListener {

        void onProfileClicked(String userId);

        void onPhotoClicked(Photo photo);

        void onLikeClicked(String commentId);

        void onReplyClicked(String commentId);
    }
}
