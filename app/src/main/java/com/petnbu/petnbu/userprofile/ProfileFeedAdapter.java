package com.petnbu.petnbu.userprofile;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.FragmentFeedProfileItemBinding;
import com.petnbu.petnbu.model.Feed;
import com.petnbu.petnbu.model.FeedUI;
import com.petnbu.petnbu.ui.common.DataBoundListAdapter;
import com.petnbu.petnbu.util.Objects;

public class ProfileFeedAdapter extends DataBoundListAdapter<FeedUI, FragmentFeedProfileItemBinding> {

    private final UserProfileFragment.OnProfileFragmentInteractionListener mListener;

    public ProfileFeedAdapter(UserProfileFragment.OnProfileFragmentInteractionListener listener) {
        mListener = listener;
    }

    @Override
    protected FragmentFeedProfileItemBinding createBinding(ViewGroup parent) {
        FragmentFeedProfileItemBinding binding =
                DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.fragment_feed_profile_item, parent, false);

        binding.getRoot().setOnClickListener(v -> {
            if (null != mListener) {
                // Notify the active callbacks interface (the activity, if the
                // fragment is attached to one) that an item has been selected.
                mListener.onListFragmentInteractionListener(binding.getFeed());
            }
        });
        return binding;
    }

    @Override
    protected void bind(FragmentFeedProfileItemBinding binding, FeedUI item) {
        binding.setFeed(item);
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    protected boolean areItemsTheSame(FeedUI oldItem, FeedUI newItem) {
        return Objects.equals(oldItem, newItem);
    }

    @Override
    protected boolean areContentsTheSame(FeedUI oldItem, FeedUI newItem) {
        return oldItem.getFeedId().equals(newItem.getFeedId());
    }

}
