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
import android.text.TextUtils;
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
import com.google.android.gms.common.internal.Preconditions;
import com.petnbu.petnbu.BaseBindingViewHolder;
import com.petnbu.petnbu.GlideApp;
import com.petnbu.petnbu.GlideRequests;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.model.LocalStatus;
import com.petnbu.petnbu.util.Utils;
import com.petnbu.petnbu.databinding.ViewCommentBinding;
import com.petnbu.petnbu.model.CommentUI;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.util.ColorUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RepliesRecyclerViewAdapter extends RecyclerView.Adapter<RepliesRecyclerViewAdapter.ViewHolder> {

    private List<CommentUI> mComments;
    private RepliesRecyclerViewAdapter.OnItemClickListener mOnItemClickListener;
    private CommentsViewModel mCommentsViewModel;
    private GlideRequests mGlideRequests;

    private String mCommentId;
    private int mDataVersion;

    public RepliesRecyclerViewAdapter(Context context, List<CommentUI> comments, String commentId,
                                      OnItemClickListener onItemClickListener, CommentsViewModel commentsViewModel) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(commentsViewModel);

        mComments = comments != null ? comments : new ArrayList<>();
        mGlideRequests = GlideApp.with(context);
        mCommentsViewModel = commentsViewModel;
        mOnItemClickListener = onItemClickListener;
        mCommentId = commentId;
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
                return DiffUtil.calculateDiff(new RepliesRecyclerViewAdapter.CommentsDiffCallback(mComments, comments));
            }
            @Override
            protected void onPostExecute(DiffUtil.DiffResult diffResult) {
                if (startVersion != mDataVersion) {
                    // ignore update
                    return;
                }
                mComments = comments;
                diffResult.dispatchUpdatesTo(RepliesRecyclerViewAdapter.this);
            }
        }.execute();
    }

    protected class ViewHolder extends BaseBindingViewHolder<ViewCommentBinding, CommentUI> {

        private CommentUI mComment;

        public ViewHolder(View itemView) {
            super(itemView);

            mBinding.imgProfile.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                    CommentUI commentUI = mComments.get(getAdapterPosition());
                    if (commentUI.getLocalStatus() != LocalStatus.STATUS_UPLOADING)
                        mCommentsViewModel.openUserProfile(commentUI.getOwner().getUserId());
                }
            });
        }

        @Override
        public void bindData(CommentUI item) {
            mComment = item;
            displayUserInfo();
            displayContent();
            displayInfo();
        }

        private void displayUserInfo() {
            mGlideRequests.asBitmap()
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
            if (!mComment.getId().equals(mCommentId)) {
                mBinding.divider.setVisibility(View.INVISIBLE);

                if (mComment.getLikeCount() > 0) {
                    mBinding.tvLikesCount.setVisibility(View.VISIBLE);
                    mBinding.tvLikesCount.setText(String.format("%d %s", mComment.getLikeCount(), mComment.getLikeCount() > 1 ? "likes" : "like"));
                } else {
                    mBinding.tvLikesCount.setVisibility(View.GONE);
                }

                if (mComment.getLocalStatus() == LocalStatus.STATUS_UPLOADING) {
                    mBinding.imgLike.setVisibility(View.INVISIBLE);
                    mBinding.progressBar.setVisibility(View.VISIBLE);
                } else {
                    mBinding.imgLike.setVisibility(View.VISIBLE);
                    mBinding.progressBar.setVisibility(View.GONE);
                }
            } else {
                mBinding.divider.setVisibility(View.VISIBLE);
                mBinding.progressBar.setVisibility(View.GONE);
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

        void onPhotoClicked(Photo photo);

        void onLikeClicked(String commentId);
    }
}
