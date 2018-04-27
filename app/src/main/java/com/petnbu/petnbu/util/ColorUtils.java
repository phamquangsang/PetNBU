package com.petnbu.petnbu.util;

/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Palette.Swatch;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Iterator;

public class ColorUtils {
    public static final int IS_LIGHT = 0;
    public static final int IS_DARK = 1;
    public static final int LIGHTNESS_UNKNOWN = 2;

    private ColorUtils() {
    }

    @CheckResult
    @ColorInt
    public static int modifyAlpha(@ColorInt int color, @IntRange(from = 0L, to = 255L) int alpha) {
        return color & 16777215 | alpha << 24;
    }

    @CheckResult
    @ColorInt
    public static int modifyAlpha(@ColorInt int color, @FloatRange(from = 0.0D, to = 1.0D) float alpha) {
        return modifyAlpha(color, (int) (255.0F * alpha));
    }

    @CheckResult
    @ColorInt
    public static int blendColors(@ColorInt int color1, @ColorInt int color2, @FloatRange(from = 0.0D, to = 1.0D) float ratio) {
        float inverseRatio = 1.0F - ratio;
        float a = (float) Color.alpha(color1) * inverseRatio + (float) Color.alpha(color2) * ratio;
        float r = (float) Color.red(color1) * inverseRatio + (float) Color.red(color2) * ratio;
        float g = (float) Color.green(color1) * inverseRatio + (float) Color.green(color2) * ratio;
        float b = (float) Color.blue(color1) * inverseRatio + (float) Color.blue(color2) * ratio;
        return Color.argb((int) a, (int) r, (int) g, (int) b);
    }

    public static int isDark(Palette palette) {
        Swatch mostPopulous = getMostPopulousSwatch(palette);
        if (mostPopulous == null) {
            return 2;
        } else {
            return isDark(mostPopulous.getHsl()) ? 1 : 0;
        }
    }

    @Nullable
    public static Palette.Swatch getMostPopulousSwatch(Palette palette) {
        Swatch mostPopulous = null;
        if (palette != null) {
            Iterator var2 = palette.getSwatches().iterator();

            while (true) {
                Swatch swatch;
                do {
                    if (!var2.hasNext()) {
                        return mostPopulous;
                    }

                    swatch = (Swatch) var2.next();
                }
                while (mostPopulous != null && swatch.getPopulation() <= mostPopulous.getPopulation());

                mostPopulous = swatch;
            }
        } else {
            return mostPopulous;
        }
    }

    public static boolean isDark(@NonNull Bitmap bitmap) {
        return isDark(bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
    }

    public static boolean isDark(@NonNull Bitmap bitmap, int backupPixelX, int backupPixelY) {
        Palette palette = Palette.from(bitmap).maximumColorCount(3).generate();
        if (palette != null && palette.getSwatches().size() > 0) {
            return isDark(palette) == 1;
        } else {
            return isDark(bitmap.getPixel(backupPixelX, backupPixelY));
        }
    }

    public static boolean isDark(float[] hsl) {
        return hsl[2] < 0.5F;
    }

    public static boolean isDark(@ColorInt int color) {
        float[] hsl = new float[3];
        android.support.v4.graphics.ColorUtils.colorToHSL(color, hsl);
        return isDark(hsl);
    }

    @ColorInt
    public static int scrimify(@ColorInt int color, boolean isDark, @FloatRange(from = 0.0D, to = 1.0D) float lightnessMultiplier) {
        float[] hsl = new float[3];
        android.support.v4.graphics.ColorUtils.colorToHSL(color, hsl);
        if (!isDark) {
            ++lightnessMultiplier;
        } else {
            lightnessMultiplier = 1.0F - lightnessMultiplier;
        }

        hsl[2] = MathUtils.constrain(0.0F, 1.0F, hsl[2] * lightnessMultiplier);
        return android.support.v4.graphics.ColorUtils.HSLToColor(hsl);
    }

    @ColorInt
    public static int scrimify(@ColorInt int color, @FloatRange(from = 0.0D, to = 1.0D) float lightnessMultiplier) {
        return scrimify(color, isDark(color), lightnessMultiplier);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Lightness {
    }
}
