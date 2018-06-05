package com.petnbu.petnbu.userprofile

import android.databinding.DataBindingUtil
import android.support.v7.util.DiffUtil
import android.view.LayoutInflater
import android.view.ViewGroup
import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.FragmentFeedProfileItemBinding
import com.petnbu.petnbu.model.FeedUI
import com.petnbu.petnbu.ui.DataBoundListAdapter

class ProfileFeedAdapter : DataBoundListAdapter<FeedUI, FragmentFeedProfileItemBinding>(FeedDiffCallback()) {

    override fun createBinding(parent: ViewGroup): FragmentFeedProfileItemBinding {
        return DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.fragment_feed_profile_item, parent, false)
    }

    override fun bind(binding: FragmentFeedProfileItemBinding, item: FeedUI) {
        binding.feed = item
    }

    private class FeedDiffCallback : DiffUtil.ItemCallback<FeedUI>() {

        override fun areItemsTheSame(oldItem: FeedUI, newItem: FeedUI) = oldItem.feedId == newItem.feedId

        override fun areContentsTheSame(oldItem: FeedUI, newItem: FeedUI) = oldItem == newItem
    }
}
