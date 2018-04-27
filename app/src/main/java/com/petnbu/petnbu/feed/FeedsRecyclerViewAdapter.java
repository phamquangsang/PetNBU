package com.petnbu.petnbu.feed;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.common.internal.Preconditions;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.Utils;
import com.petnbu.petnbu.databinding.ViewFeedBinding;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedUser;

import java.util.List;
import java.util.Locale;
import java.util.Random;

public class FeedsRecyclerViewAdapter extends RecyclerView.Adapter<FeedsRecyclerViewAdapter.ViewHolder> {

    public static final int IMAGE_PROFILE_RADIUS_CORNER = 10;
    private List<Feed> mFeeds;
    private ArrayMap<String, Integer> lastSelectedPhotoPositions = new ArrayMap<>();

    private int maxPhotoHeight;
    private final int minPhotoHeight;
    private RequestManager mRequestManager;

    public FeedsRecyclerViewAdapter(Context context, List<Feed> feeds) {
        Preconditions.checkNotNull(feeds);
        mFeeds = feeds;
        minPhotoHeight = Utils.goldenRatio(Utils.getDeviceWidth(context), true);
        mRequestManager = Glide.with(context);
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
        return mFeeds.size();
    }

    public void setMaxPhotoHeight(int maxPhotoHeight) {
        this.maxPhotoHeight = maxPhotoHeight;
    }

    public void setFeeds(List<Feed> feeds) {
        mFeeds = feeds;
        notifyDataSetChanged();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        private ViewFeedBinding mBinding;
        private Feed mFeed;

        public ViewHolder(View itemView) {
            super(itemView);
            mBinding = DataBindingUtil.bind(itemView);
        }

        public void bindData(Feed feed) {
            mFeed = feed;
            displayUserInfo();
            displayPhotos();
            displayText();
        }

        private void displayText() {
            SpannableStringBuilder builder = new SpannableStringBuilder(mFeed.getFeedUser().getDisplayName() + "");
            builder.setSpan(new StyleSpan(Typeface.BOLD), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new ForegroundColorSpan(Color.BLACK), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append(" ");
            builder.append(mFeed.getContent());
            mBinding.tvContent.setText(builder);
        }

        private void displayUserInfo() {
            FeedUser feedUser = mFeed.getFeedUser();
            mBinding.tvName.setText(feedUser.getDisplayName());
            mRequestManager.load(feedUser.getPhotoUrl())
                    .apply(RequestOptions.centerCropTransform())
                    .into(mBinding.imgProfile);
        }

        private void displayPhotos() {
            if(mFeed.getPhotos() != null && !mFeed.getPhotos().isEmpty()) {
                constraintHeightForPhoto(mFeed.getPhotos().get(0).getWidth(), mFeed.getPhotos().get(0).getHeight());

                mBinding.vpPhotos.setAdapter(new PhotosPagerAdapter(mFeed, mRequestManager));
                int currentPos = 0;
                Integer value = lastSelectedPhotoPositions.get(mFeed.getFeedId());
                if(value != null) {
                    currentPos = value.intValue();
                }
                mBinding.vpPhotos.setCurrentItem(currentPos);

                if(mFeed.getPhotos().size() > 1) {
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
            int height = (int) ((photoHeight / (float)(photoWidth)) * Utils.getDeviceWidth(itemView.getContext()));

            layoutParams.height = height > maxPhotoHeight ? maxPhotoHeight : height < minPhotoHeight ? minPhotoHeight : height;
            mBinding.layoutMedia.setLayoutParams(layoutParams);
        }
    }
}
