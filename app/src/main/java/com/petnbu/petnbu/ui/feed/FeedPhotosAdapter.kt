package com.petnbu.petnbu.ui.feed

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.petnbu.petnbu.ui.BaseBindingViewHolder
import com.petnbu.petnbu.GlideRequests
import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.ViewFeedPhotosBinding
import com.petnbu.petnbu.model.FeedUI
import com.petnbu.petnbu.model.Photo
import com.petnbu.petnbu.util.ImageUtils

class FeedPhotosAdapter(private var feed: FeedUI,
                        private val glideRequests: GlideRequests,
                        private val onItemClickListener: OnItemClickListener,
                        private val imageWidth: Int)
    : RecyclerView.Adapter<FeedPhotosAdapter.PhotoHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_feed_photos, parent, false)
        return PhotoHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
        holder.bindData(feed.getPhotos()[position])
    }

    override fun getItemCount() = feed.getPhotos()?.size ?: 0

    inner class PhotoHolder constructor(itemView: View) : BaseBindingViewHolder<ViewFeedPhotosBinding, Photo>(itemView) {

        init {
            itemView.layoutParams = itemView.layoutParams.apply {
                width = imageWidth
            }
        }

        override fun bindData(photo: Photo) {
            val photoUrl = ImageUtils.getPhotoUrl(photo, imageWidth)
            glideRequests
                    .load(if (!photoUrl.isNullOrBlank()) photoUrl else photo.originUrl)
                    .centerInside()
                    .into(mBinding.imgContent)
        }

        override fun bindData(item: Photo, payloads: List<Any>) {}
    }

    interface OnItemClickListener {

        fun onPhotoClicked()
    }
}
