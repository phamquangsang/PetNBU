package com.petnbu.petnbu.ui

import android.databinding.BindingAdapter
import android.view.View
import android.widget.ImageView

import com.bumptech.glide.Glide
import com.petnbu.petnbu.model.Photo
import com.petnbu.petnbu.util.ImageUtils

object BindingAdapters {

    @JvmStatic
    @BindingAdapter("imageUrl")
    fun bindImage(imageView: ImageView, url: String) {
        Glide.with(imageView).load(url).into(imageView)
    }

    @JvmStatic
    @BindingAdapter("bindPhoto")
    fun bindImage(imageView: ImageView, photo: Photo) {
        ImageUtils.SizeDeterminer(imageView).getSize { width, _ ->
            val url = ImageUtils.getPhotoUrl(photo, ImageUtils.getResolutionType(width))
            Glide.with(imageView).load(url).into(imageView)
        }
    }

    @JvmStatic
    @BindingAdapter("visibleGone")
    fun showHide(view: View, show: Boolean) {
        view.visibility = if (show) View.VISIBLE else View.GONE
    }
}
