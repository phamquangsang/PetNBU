package com.petnbu.petnbu.feed;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.petnbu.petnbu.BaseBindingViewHolder;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.ViewCreateFeedCameraBinding;
import com.petnbu.petnbu.databinding.ViewCreateFeedPhotosSelectedBinding;

import java.util.ArrayList;

public class PhotosAdapter extends RecyclerView.Adapter<BaseBindingViewHolder> {

    private final int VIEW_TYPE_CAMERA = 1;
    private final int VIEW_TYPE_PHOTO = 2;

    private ArrayList<String> mMediaUrls;
    private RequestManager mRequestManager;
    private final int mImageSize;
    private final ItemClickListener mItemClickListener;

    public PhotosAdapter(Context context, RequestManager requestManager, ArrayList<String> mediaUrls,
                         ItemClickListener itemClickListener, int imageSize) {
        mMediaUrls = mediaUrls;
        mRequestManager = requestManager;
        mImageSize = imageSize;
        mItemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public BaseBindingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_CAMERA) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_create_feed_camera, parent, false);
            return new CameraHolder(view);
        } else if(viewType == VIEW_TYPE_PHOTO) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_create_feed_photos_selected, parent, false);
            return new PhotoHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull BaseBindingViewHolder holder, int position) {
        if (!isCameraPos(position)) {
            holder.bindData(mMediaUrls.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return mMediaUrls != null ? mMediaUrls.size() + 1 : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return isCameraPos(position) ? VIEW_TYPE_CAMERA : VIEW_TYPE_PHOTO;
    }

    private boolean isCameraPos(int position) {
        return position == getItemCount() - 1;
    }

    protected final class PhotoHolder extends BaseBindingViewHolder<ViewCreateFeedPhotosSelectedBinding, String> {

        private PhotoHolder(View itemView) {
            super(itemView);
            ViewGroup.LayoutParams layoutParams = mBinding.imgContent.getLayoutParams();
            layoutParams.width = mImageSize;
            mBinding.imgContent.setLayoutParams(layoutParams);
            mBinding.imgContent.set(1, 1);

            mBinding.imgContent.setOnClickListener(v -> {
                if (mItemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    mItemClickListener.onPhotoClicked(mMediaUrls.get(getAdapterPosition()));
                }
            });
        }

        @Override
        public void bindData(String url) {
            mRequestManager
                    .load(url)
                    .apply(RequestOptions.centerInsideTransform())
                    .into(mBinding.imgContent);
        }
    }

    protected final class CameraHolder extends BaseBindingViewHolder<ViewCreateFeedCameraBinding, String> {

        private CameraHolder(View itemView) {
            super(itemView);
            ViewGroup.LayoutParams layoutParams = mBinding.imgContent.getLayoutParams();
            layoutParams.width = mImageSize;
            mBinding.imgContent.setLayoutParams(layoutParams);
            mBinding.imgContent.set(1, 1);

            mBinding.imgContent.setOnClickListener(v -> {
                if (mItemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    mItemClickListener.onCameraIconClicked();
                }
            });
        }

        @Override
        public void bindData(String item) {

        }
    }

    public interface ItemClickListener {

        void onCameraIconClicked();

        void onPhotoClicked(String url);
    }
}
