package com.petnbu.petnbu.feed;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.internal.Preconditions;
import com.petnbu.petnbu.BaseBindingViewHolder;
import com.petnbu.petnbu.GlideRequests;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.ViewFeedPhotosBinding;
import com.petnbu.petnbu.model.FeedUI;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.util.ImageUtils;

public class FeedPhotosAdapter extends RecyclerView.Adapter<FeedPhotosAdapter.PhotoHolder> {

    private FeedUI mFeed;
    private GlideRequests mGlideRequests;
    private PhotosPagerAdapter.OnItemClickListener mOnItemClickListener;
    private int mImageWidth;

    public FeedPhotosAdapter(FeedUI feed, GlideRequests glideRequests,
                             PhotosPagerAdapter.OnItemClickListener onItemClickListener, int imageWidth) {
        Preconditions.checkNotNull(feed);
        Preconditions.checkNotNull(glideRequests);

        mFeed = feed;
        mGlideRequests = glideRequests;
        mOnItemClickListener = onItemClickListener;
        mImageWidth = imageWidth;
    }

    @NonNull
    @Override
    public PhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_feed_photos, parent, false);
        return new PhotoHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoHolder holder, int position) {
        holder.bindData(mFeed.getPhotos().get(position));
    }

    @Override
    public int getItemCount() {
        return mFeed.getPhotos() != null ? mFeed.getPhotos().size() : 0;
    }

    public void setData(FeedUI feed) {
        mFeed = feed;
        notifyDataSetChanged();
    }

    protected final class PhotoHolder extends BaseBindingViewHolder<ViewFeedPhotosBinding, Photo> {

        private PhotoHolder(View itemView) {
            super(itemView);
            ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
            layoutParams.width = mImageWidth;
            itemView.setLayoutParams(layoutParams);
        }

        @Override
        public void bindData(Photo photo) {
            String photoUrl = ImageUtils.getPhotoUrl(photo, mImageWidth);
            mGlideRequests
                    .load(TextUtils.isEmpty(photoUrl) ? photo.getOriginUrl() : photoUrl)
                    .centerInside()
                    .into(mBinding.imgContent);
        }
    }
}
