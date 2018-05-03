package com.petnbu.petnbu.userprofile;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.petnbu.petnbu.R;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.userprofile.dummy.DummyContent.DummyItem;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DummyItem} and makes a call to the
 * specified {@link UserProfileFragment.OnProfileFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class ProfileFeedAdapter extends RecyclerView.Adapter<ProfileFeedAdapter.ViewHolder> {

    private final List<Feed> mValues;
    private final UserProfileFragment.OnProfileFragmentInteractionListener mListener;

    public ProfileFeedAdapter(List<Feed> items, UserProfileFragment.OnProfileFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_feed_profile_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        List<Photo> photos = holder.mItem.getPhotos();
        Glide.with(holder.mContentView).load(photos.get(0).getOriginUrl()).into(holder.mContentView);
        if(photos.size() > 1){
            holder.mImgMultiPhoto.setVisibility(View.VISIBLE);
        }else{
            holder.mImgMultiPhoto.setVisibility(View.GONE);
        }


        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteractionListener(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final ImageView mContentView;
        private final ImageView mImgMultiPhoto;
        public Feed mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mContentView = view.findViewById(R.id.imgPhoto);
            mImgMultiPhoto = view.findViewById(R.id.img_multiple_photos);
        }

    }
}
