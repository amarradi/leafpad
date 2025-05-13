package com.git.amarradi.leafpad.helper;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.git.amarradi.leafpad.Leafpad;
import com.git.amarradi.leafpad.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DialogHelper {

    public interface OnDialogConfirmedListener {
        void onConfirmed();
    }

    public static void showConfirmDialog(Context context, String title, String message,
                                         String positiveText, String negativeText,
                                         OnDialogConfirmedListener listener) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, (dialog, which) -> {
                    if (listener != null) {
                        listener.onConfirmed();
                    }
                })
                .setNegativeButton(negativeText, null)
                .show();
    }

    public static void showWarningDialog(Context context, String title, String message, String buttonText) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(buttonText, null)
                .show();
    }

    public static void showDeleteSingleNoteDialog(Context context, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(context)
                .setIcon(R.drawable.dialog_delete)
                .setTitle(R.string.remove_dialog_title)
                .setMessage(R.string.remove_dailog_message)
                .setPositiveButton(R.string.action_remove, (dialog, which) -> {
                    onConfirm.run();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.remove_dialog_abort, (dialog, which) -> dialog.dismiss())
                .show();
    }


    public static void showDeleteConfirmation(Context context, OnDialogConfirmedListener listener) {
        showConfirmDialog(
                context,
                context.getString(R.string.showDeleteConfirmation),
                context.getString(R.string.showDeleteConfirmationMessage),
                context.getString(R.string.delete),
                context.getString(R.string.cancel),
                listener
        );
    }

    public static void showRestoreConfirmation(Context context, OnDialogConfirmedListener listener) {
        showConfirmDialog(
                context,
                context.getString(R.string.showRestoreConfirmation),
                context.getString(R.string.showRestoreConfirmationMessage),
                context.getString(R.string.yes),
                context.getString(R.string.cancel),
                listener
        );
    }

    public static void showThemeSelectionDialog(Context context, Runnable onThemeChanged) {
        String[] themeLabels = context.getResources().getStringArray(R.array.design_mode_preference_key);
        String[] themeValues = context.getResources().getStringArray(R.array.design_mode_preference_value);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String currentValue = prefs.getString("theme", "system");
        int currentIndex = java.util.Arrays.asList(themeValues).indexOf(currentValue);

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.theme_preference)
                .setSingleChoiceItems(themeLabels, currentIndex, (dialog, which) -> {
                    prefs.edit().putString("theme", themeValues[which]).apply();
                    Leafpad.getInstance().saveTheme(themeValues[which]);
                    if (onThemeChanged != null) onThemeChanged.run(); // z. B. recreate()
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }


}
