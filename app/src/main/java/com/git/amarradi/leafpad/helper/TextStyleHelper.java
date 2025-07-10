package com.git.amarradi.leafpad.helper;

import android.text.Spannable;
import android.text.style.BulletSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.text.style.URLSpan;

public class TextStyleHelper {

    public static void applyHeading(Spannable text, int start, int end) {
        text.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new RelativeSizeSpan(1.4f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public static void applyUnderline(Spannable text, int start, int end) {
        text.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public static void applyBullet(Spannable text, int start, int end) {
        text.setSpan(new BulletSpan(20), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public static void applyLink(Spannable text, int start, int end, String url) {
        text.setSpan(new URLSpan(url), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public static boolean hasStyle(Spannable text, int start, int end, Class<?> spanClass) {
        Object[] spans = text.getSpans(start, end, spanClass);
        return spans != null && spans.length > 0;
    }

    public static boolean hasUrl(Spannable text, int start, int end, String url) {
        URLSpan[] spans = text.getSpans(start, end, URLSpan.class);
        for (URLSpan span : spans) {
            if (span.getURL().equals(url)) {
                return true;
            }
        }
        return false;
    }
}
