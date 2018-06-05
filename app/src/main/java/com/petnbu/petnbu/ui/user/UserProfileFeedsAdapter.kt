package com.petnbu.petnbu.ui.user

import android.databinding.DataBindingUtil
import android.support.v7.util.DiffUtil
import android.view.LayoutInflater
import android.view.ViewGroup
import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.ViewFeedProfileItemBinding
import com.petnbu.petnbu.model.FeedUI
import com.petnbu.petnbu.ui.DataBoundListAdapter

class UserProfileFeedsAdapter : DataBoundListAdapter<FeedUI, ViewFeedProfileItemBinding>(FeedDiffCallback()) {

    override fun createBinding(parent: ViewGroup): ViewFeedProfileItemBinding {
        return DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.view_feed_profile_item, parent, false)
    }

    override fun bind(binding: ViewFeedProfileItemBinding, item: FeedUI) {
        binding.feed = item
    }

    private class FeedDiffCallback : DiffUtil.ItemCallback<FeedUI>() {

        override fun areItemsTheSame(oldItem: FeedUI, newItem: FeedUI) = oldItem.feedId == newItem.feedId

        override fun areContentsTheSame(oldItem: FeedUI, newItem: FeedUI) = oldItem == newItem
    }
}
