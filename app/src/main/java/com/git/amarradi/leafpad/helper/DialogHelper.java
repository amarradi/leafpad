package com.git.amarradi.leafpad.helper;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

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

    public static void showThemeSelectionDialog(Context context, SharedPreferences prefs, @Nullable Runnable onThemeChanged) {
        String[] entries = {
                context.getString(R.string.system_preference_key),
                context.getString(R.string.lightmode_preference_key),
                context.getString(R.string.darkmode_preference_key)
        };

        String[] entryValues = {
                context.getString(R.string.system_preference_option_value),
                context.getString(R.string.lightmode_preference_option_value),
                context.getString(R.string.darkmode_preference_option_value)
        };

        String currentValue = prefs.getString("theme", context.getString(R.string.system_preference_option_value));
        int selectedIndex = 0;

        for (int i = 0; i < entryValues.length; i++) {
            if (entryValues[i].equals(currentValue)) {
                selectedIndex = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.theme_dialog))
                .setSingleChoiceItems(entries, selectedIndex, (dialog, which) -> {
                    prefs.edit().putString("theme", entryValues[which]).apply();

                    if (onThemeChanged != null) {
                        onThemeChanged.run();
                    }

                    if (context instanceof Activity) {
                        ((Activity) context).recreate();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }


}
