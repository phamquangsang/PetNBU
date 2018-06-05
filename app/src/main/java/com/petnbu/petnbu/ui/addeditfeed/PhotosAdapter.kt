package com.petnbu.petnbu.ui.addeditfeed

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.petnbu.petnbu.BaseBindingViewHolder
import com.petnbu.petnbu.GlideApp
import com.petnbu.petnbu.R
import com.petnbu.petnbu.databinding.ViewCreateFeedCameraBinding
import com.petnbu.petnbu.databinding.ViewCreateFeedPhotosSelectedBinding
import com.petnbu.petnbu.model.Photo
import java.util.*

class PhotosAdapter(context: Context,
                    val photos: ArrayList<Photo> = ArrayList(0),
                    private val itemClickListener: ItemClickListener?,
                    private val imageSize: Int)
    : RecyclerView.Adapter<BaseBindingViewHolder<*, *>>() {

    private val glideRequests = GlideApp.with(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder<*, *> {
        return if (viewType == VIEW_TYPE_CAMERA) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.view_create_feed_camera, parent, false)
            CameraHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.view_create_feed_photos_selected, parent, false)
            PhotoHolder(view)
        }
    }

    override fun onBindViewHolder(holder: BaseBindingViewHolder<*, *>, position: Int) {
        (holder as? PhotoHolder)?.bindData(photos[position])
    }

    override fun getItemCount(): Int {
        return photos.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (isCameraPos(position)) VIEW_TYPE_CAMERA else VIEW_TYPE_PHOTO
    }

    private fun isCameraPos(position: Int): Boolean {
        return position == itemCount - 1
    }

    fun removeItem(position: Int) {
        if (0 <= position && position < photos.size) {
            photos.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    inner class PhotoHolder(itemView: View) : BaseBindingViewHolder<ViewCreateFeedPhotosSelectedBinding, Photo>(itemView) {

        init {
            mBinding.imgContent.layoutParams = mBinding.imgContent.layoutParams.apply {
                width = imageSize
            }
            mBinding.imgContent.set(1, 1)
            mBinding.imgContent.setOnClickListener { _ ->
                if (itemClickListener != null && adapterPosition != RecyclerView.NO_POSITION)
                    itemClickListener.onPhotoClicked(photos[adapterPosition])
            }
            mBinding.layoutRemovePhoto.setOnClickListener { _ ->
                if (itemClickListener != null && adapterPosition != RecyclerView.NO_POSITION)
                    itemClickListener.onRemovePhotoClicked(adapterPosition)
            }
        }

        override fun bindData(photo: Photo) {
            glideRequests.load(if (TextUtils.isEmpty(photo.smallUrl)) photo.originUrl else photo.smallUrl)
                    .centerInside()
                    .into(mBinding.imgContent)
        }

        override fun bindData(item: Photo, payloads: List<Any>) {}
    }

    inner class CameraHolder(itemView: View) : BaseBindingViewHolder<ViewCreateFeedCameraBinding, String>(itemView) {

        init {
            mBinding.imgContent.layoutParams = mBinding.imgContent.layoutParams.apply {
                width = imageSize
            }
            mBinding.imgContent.set(1, 1)
            mBinding.imgContent.setOnClickListener { _ ->
                if (itemClickListener != null && adapterPosition != RecyclerView.NO_POSITION)
                    itemClickListener.onCameraIconClicked()
            }
        }

        override fun bindData(item: String) {}

        override fun bindData(item: String, payloads: List<Any>) {}
    }

    companion object {
        private const val VIEW_TYPE_CAMERA = 1
        private const val VIEW_TYPE_PHOTO = 2
    }

    interface ItemClickListener {

        fun onCameraIconClicked()

        fun onPhotoClicked(photo: Photo)

        fun onRemovePhotoClicked(position: Int)
    }
}
