package com.petnbu.petnbu.feed;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.RequestManager;
import com.petnbu.petnbu.BaseBindingViewHolder;
import com.petnbu.petnbu.GlideApp;
import com.petnbu.petnbu.GlideRequests;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.ViewCreateFeedCameraBinding;
import com.petnbu.petnbu.databinding.ViewCreateFeedPhotosSelectedBinding;
import com.petnbu.petnbu.model.Photo;

import java.util.ArrayList;

public class PhotosAdapter extends RecyclerView.Adapter<BaseBindingViewHolder> {

    private final int VIEW_TYPE_CAMERA = 1;
    private final int VIEW_TYPE_PHOTO = 2;

    private ArrayList<Photo> mPhotos;
    private GlideRequests mGlideRequests;
    private final int mImageSize;
    private final ItemClickListener mItemClickListener;

    public PhotosAdapter(Context context, ArrayList<Photo> photos,
                         ItemClickListener itemClickListener, int imageSize) {
        mPhotos = photos;
        mGlideRequests = GlideApp.with(context);
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
            holder.bindData(mPhotos.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return mPhotos != null ? mPhotos.size() + 1 : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return isCameraPos(position) ? VIEW_TYPE_CAMERA : VIEW_TYPE_PHOTO;
    }

    private boolean isCameraPos(int position) {
        return position == getItemCount() - 1;
    }

    public void removeItem(int position) {
        if(mPhotos != null && 0 <= position && position < mPhotos.size()) {
            mPhotos.remove(position);
            notifyItemRemoved(position);
        }
    }

    public ArrayList<Photo> getPhotos() {
        return mPhotos;
    }

    protected final class PhotoHolder extends BaseBindingViewHolder<ViewCreateFeedPhotosSelectedBinding, Photo> {

        private PhotoHolder(View itemView) {
            super(itemView);
            ViewGroup.LayoutParams layoutParams = mBinding.imgContent.getLayoutParams();
            layoutParams.width = mImageSize;
            mBinding.imgContent.setLayoutParams(layoutParams);
            mBinding.imgContent.set(1, 1);

            mBinding.imgContent.setOnClickListener(v -> {
                if (mItemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    mItemClickListener.onPhotoClicked(mPhotos.get(getAdapterPosition()));
                }
            });
            mBinding.layoutRemovePhoto.setOnClickListener(v -> {
                if (mItemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    mItemClickListener.onRemovePhotoClicked(getAdapterPosition());
                }
            });
        }

        @Override
        public void bindData(Photo photo) {
            mGlideRequests.load(TextUtils.isEmpty(photo.getSmallUrl()) ? photo.getOriginUrl() : photo.getSmallUrl())
                    .centerInside()
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

        void onPhotoClicked(Photo photo);

        void onRemovePhotoClicked(int position);
    }
}
