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
}
