package com.petnbu.petnbu.ui.common;

import android.databinding.BindingAdapter;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class BindingAdapters {

    @BindingAdapter("imageUrl")
    public static void bindImage(ImageView imageView, String url) {
        Glide.with(imageView).load(url).into(imageView);
    }

    @BindingAdapter("visibleGone")
    public static void showHide(View view, boolean show) {
        view.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}