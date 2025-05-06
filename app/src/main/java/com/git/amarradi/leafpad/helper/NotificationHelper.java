package com.git.amarradi.leafpad.helper;

import android.view.View;

import com.google.android.material.snackbar.Snackbar;

public class NotificationHelper {
    public static void showSnackbar(View view, String message) {
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
                    .show();
        }
    }

    public static void showSnackbar(View view, String message,int duration, View anchorView) {
        if (view != null) {
            Snackbar.make(view, message, duration)
                    .setAnchorView(anchorView)
                    .show();
        }
    }

    public static void showSnackbar(View view, String message, int duration) {
        if (view != null) {
            Snackbar.make(view, message, duration).show();
        }
    }

    public static void showSnackbar(View view, String message, int duration, String actionText, View.OnClickListener actionListener) {
        if (view != null) {
            Snackbar.make(view, message, duration)
                    .setAction(actionText, actionListener)
                    .show();
        }
    }
}
