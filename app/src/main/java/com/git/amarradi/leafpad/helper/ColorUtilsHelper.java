package com.git.amarradi.leafpad.helper;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;

public class ColorUtilsHelper {
    public static int lightenColor(@ColorInt int color, float amount) {
        float safeAmount = Math.max(0f, Math.min(1f, amount));

        float[] hsl = new float[3];
        ColorUtils.colorToHSL(color, hsl);

        hsl[2] = Math.max(0f, Math.min(1f, hsl[2] + safeAmount));

        return ColorUtils.HSLToColor(hsl);
    }


    public static int dpToPx(Context context, float dp) {
        return Math.round(
                android.util.TypedValue.applyDimension(
                        android.util.TypedValue.COMPLEX_UNIT_DIP,
                        dp,
                        context.getResources().getDisplayMetrics()
                )
        );
    }


}
