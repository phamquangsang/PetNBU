package com.petnbu.petnbu.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.Fragment;

public final class NavigationUtils {

    private NavigationUtils() {
    }

    public static final void openPhotoGallery(Activity activity, boolean allowMultiPick, int requestCode) {
        if (Build.VERSION.SDK_INT < 19) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            activity.startActivityForResult(intent, requestCode);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiPick);
            intent.setType("image/*");
            activity.startActivityForResult(intent, requestCode);
        }
    }

    public static final void openPhotoGallery(Fragment fragment, boolean allowMultiPick, int requestCode) {
        if (Build.VERSION.SDK_INT < 19) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            fragment.startActivityForResult(intent, requestCode);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiPick);
            intent.setType("image/*");
            fragment.startActivityForResult(intent, requestCode);
        }
    }
}
