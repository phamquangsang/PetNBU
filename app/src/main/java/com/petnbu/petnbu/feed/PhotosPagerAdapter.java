package com.petnbu.petnbu.feed;

import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.internal.Preconditions;
import com.petnbu.petnbu.GlideRequests;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.ViewFeedPhotosBinding;
import com.petnbu.petnbu.model.FeedUI;
import com.petnbu.petnbu.util.ImageUtils;

public class PhotosPagerAdapter extends PagerAdapter {

    private FeedUI mFeed;
    private GlideRequests mGlideRequests;
    private OnItemClickListener mOnItemClickListener;

    public PhotosPagerAdapter(FeedUI feed, GlideRequests glideRequests, OnItemClickListener onItemClickListener) {
        Preconditions.checkNotNull(feed);
        Preconditions.checkNotNull(glideRequests);

        mFeed = feed;
        mGlideRequests = glideRequests;
        mOnItemClickListener = onItemClickListener;
    }

    @Override
    public int getCount() {
        return mFeed.getPhotos() != null ? mFeed.getPhotos().size() : 0;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        ViewFeedPhotosBinding viewFeedPhotosBinding = DataBindingUtil.bind(View.inflate(container.getContext(),
                R.layout.view_feed_photos, null));
        ImageUtils.SizeDeterminer sizeDeterminer = new ImageUtils.SizeDeterminer(viewFeedPhotosBinding.imgContent);
        sizeDeterminer.getSize((width, height) -> {
            String photoUrl = ImageUtils.getPhotoUrl(mFeed.getPhotos().get(position), width);
            mGlideRequests
                    .load(!TextUtils.isEmpty(photoUrl) ? photoUrl : mFeed.getPhotos().get(position).getOriginUrl())
                    .centerInside()
                    .into(viewFeedPhotosBinding.imgContent);
        });
        viewFeedPhotosBinding.imgContent.setOnClickListener(onPhotoClickedListener);
        container.addView(viewFeedPhotosBinding.getRoot());
        return viewFeedPhotosBinding.getRoot();
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
        mGlideRequests.clear((View) object);
    }

    public void setData(FeedUI feed) {
        mFeed = feed;
        notifyDataSetChanged();
    }

    private View.OnClickListener onPhotoClickedListener = v -> {
        if(mOnItemClickListener != null) {
            mOnItemClickListener.onPhotoClicked();
        }
    };

    public interface OnItemClickListener {

        void onPhotoClicked();
    }
}
