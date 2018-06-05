package com.petnbu.petnbu.ui;

import android.databinding.BindingAdapter;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.petnbu.petnbu.model.Photo;
import com.petnbu.petnbu.util.ImageUtils;

public class BindingAdapters {

    @BindingAdapter("imageUrl")
    public static void bindImage(ImageView imageView, String url) {
        Glide.with(imageView).load(url).into(imageView);
    }

    @BindingAdapter("bindPhoto")
    public static void bindImage(ImageView imageView, Photo photo) {
        new ImageUtils.SizeDeterminer(imageView).getSize((width, height) -> {
            String url = ImageUtils.getPhotoUrl(photo, ImageUtils.getResolutionType(width));
            Glide.with(imageView).load(url).into(imageView);
        });
    }

    @BindingAdapter("visibleGone")
    public static void showHide(View view, boolean show) {
        view.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
