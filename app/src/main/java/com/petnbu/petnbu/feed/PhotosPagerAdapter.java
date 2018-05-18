package com.petnbu.petnbu.feed;

import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.common.internal.Preconditions;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.ViewFeedPhotosBinding;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.util.ImageUtils;

public class PhotosPagerAdapter extends PagerAdapter {

    private Feed mFeed;
    private RequestManager mRequestManager;
    private OnItemClickListener mOnItemClickListener;

    public PhotosPagerAdapter(Feed feed, RequestManager requestManager, OnItemClickListener onItemClickListener) {
        Preconditions.checkNotNull(feed);
        Preconditions.checkNotNull(requestManager);

        mFeed = feed;
        mRequestManager = requestManager;
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
            mRequestManager
                    .load(!TextUtils.isEmpty(photoUrl) ? photoUrl : mFeed.getPhotos().get(position).getOriginUrl())
                    .apply(RequestOptions.centerInsideTransform())
                    .into(viewFeedPhotosBinding.imgContent);
        });
        viewFeedPhotosBinding.imgContent.setOnClickListener(onPhotoClickedListener);
        container.addView(viewFeedPhotosBinding.getRoot());
        return viewFeedPhotosBinding.getRoot();
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
        mRequestManager.clear((View) object);
    }

    public void setData(Feed feed) {
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
