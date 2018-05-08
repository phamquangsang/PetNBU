package com.petnbu.petnbu.userprofile;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.petnbu.petnbu.R;
import com.petnbu.petnbu.databinding.FragmentFeedProfileItemBinding;
import com.petnbu.petnbu.model.FeedUIModel;
import com.petnbu.petnbu.ui.common.DataBoundListAdapter;
import com.petnbu.petnbu.userprofile.dummy.DummyContent.DummyItem;
import com.petnbu.petnbu.util.Objects;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DummyItem} and makes a call to the
 * specified {@link UserProfileFragment.OnProfileFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class ProfileFeedAdapter extends DataBoundListAdapter<FeedUIModel, FragmentFeedProfileItemBinding> {

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
    protected void bind(FragmentFeedProfileItemBinding binding, FeedUIModel item) {
        binding.setFeed(item);
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    protected boolean areItemsTheSame(FeedUIModel oldItem, FeedUIModel newItem) {
        return Objects.equals(oldItem, newItem);
    }

    @Override
    protected boolean areContentsTheSame(FeedUIModel oldItem, FeedUIModel newItem) {
        return oldItem.getFeedId().equals(newItem.getFeedId());
    }

}
