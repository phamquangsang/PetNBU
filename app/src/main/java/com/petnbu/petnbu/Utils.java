package com.petnbu.petnbu;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;

public final class Utils {

    private static final float GOLDEN_RATIO = 1.6180f;

    /***
     * @return return calculated smaller size if the isLonger is true otherwise calculated longer size
     */
    public static int goldenRatio(int size, boolean isLonger) {
        return isLonger ? (int) (size / GOLDEN_RATIO) : (int) (size * GOLDEN_RATIO);
    }


    public static int getDeviceWidth(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(Context context, float dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }
}
