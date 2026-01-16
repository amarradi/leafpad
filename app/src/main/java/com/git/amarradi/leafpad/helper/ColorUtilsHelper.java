package com.git.amarradi.leafpad.helper;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

public class ColorUtilsHelper {

    public static final float CATEGORY_BG_LIGHTEN_FACTOR = 0.35f;

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

    @ColorInt
    public static int parseCategoryColor(@Nullable String colorHex) {
        if (colorHex == null || colorHex.trim().isEmpty()) {
            return Color.parseColor("#CCCCCC");
        }
        try {
            return Color.parseColor(colorHex);
        } catch (Exception e) {
            return Color.parseColor("#CCCCCC");
        }
    }

    @ColorInt
    public static int getCategoryBackgroundColor(@ColorInt int baseColor) {
        return lightenColor(baseColor, 0.35f);
    }




}
