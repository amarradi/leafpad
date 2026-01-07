package com.git.amarradi.leafpad.helper;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import com.git.amarradi.leafpad.Leafpad;
import com.git.amarradi.leafpad.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class DialogHelper {

    public static void showInfoDialog(@NonNull Context context,
                                      @NonNull String title,
                                      @NonNull String message) {

        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    public interface OnDialogConfirmedListener {
        void onConfirmed();
    }

    public interface OnCategorySavedListener {
        void onSaved(String name, String colorHex);
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

    public static void showUnsavedChangesDialog(Context context, Runnable onSave, Runnable onDiscard) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.unsaved_changes_title)
                .setMessage(R.string.unsaved_changes_message)
                .setPositiveButton(R.string.save_changes, (dialog, which) -> onSave.run())
                .setNegativeButton(R.string.discard, (dialog, which) -> onDiscard.run())
                .setNeutralButton(R.string.cancel, null)
                .show();
    }

    public static void showDeleteSingleNoteDialog(Context context, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(context)
                .setIcon(R.drawable.ic_delete)
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
                listener);
    }

    public static void showRestoreConfirmation(Context context, OnDialogConfirmedListener listener) {
        showConfirmDialog(
                context,
                context.getString(R.string.showRestoreConfirmation),
                context.getString(R.string.showRestoreConfirmationMessage),
                context.getString(R.string.yes),
                context.getString(R.string.cancel),
                listener);
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
                    if (onThemeChanged != null)
                        onThemeChanged.run();
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static void showTitleRequiredDialog(Context context,  DialogInterface.OnClickListener discardListener) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.title_required)
                .setMessage(R.string.please_enter_a_title)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(R.string.discard, discardListener)
                .show();
    }

    public static void showKeepScreenOnWarningDialog(Context context, OnDialogConfirmedListener listener) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.keep_screen_on_dialog_title)
                .setMessage(R.string.keep_screen_on_dialog_message)
                .setPositiveButton(R.string.keep_screen_on_dialog_yes, (dialog, which) -> {
                    if (listener != null)
                        listener.onConfirmed();
                })
                .setNegativeButton(R.string.keep_screen_on_dialog_no, null)
                .show();
    }

    public static void showColorPicker(
            FragmentActivity activity,
            int initialColor,
            int dialogId
    ) {
        ColorPickerDialog.newBuilder()
                .setColor(initialColor)
                .setDialogId(dialogId)
                .setShowAlphaSlider(false)
                .show(activity);
    }


    private static String colorToHex(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }


//    public interface OnCategorySavedListener {
//        void onSaved(String name, String colorHex);
//    }

    // interner Dialog-State pro dialogId
    private static final Map<Integer, CategoryDialogState> CATEGORY_DIALOG_STATES = new HashMap<>();

    private static class CategoryDialogState {
        WeakReference<View> colorPreviewRef;
        int selectedColor;
    }

    public static void showCategoryAddOrEditDialog(
            Fragment fragment,
            String title,
            @Nullable String initialName,
            @Nullable String initialColorHex,
            int colorPickerDialogId,
            OnCategorySavedListener onSaved
    ) {
        Context context = fragment.requireContext();

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_category_edit, null);

        TextInputEditText nameInput = view.findViewById(R.id.category_name_edit);
        View colorPreview = view.findViewById(R.id.colorPickerView);

        // Initialwerte
        String startName = initialName != null ? initialName : "";
        nameInput.setText(startName);

        int tmpColor = Color.parseColor("#CCCCCC");
        if (initialColorHex != null) {
            try {
                tmpColor = Color.parseColor(initialColorHex);
            } catch (IllegalArgumentException ignored) {
            }
        }
        final int startColor = tmpColor;

        // State erstellen/merken
        CategoryDialogState state = new CategoryDialogState();
        state.selectedColor = startColor;
        state.colorPreviewRef = new WeakReference<>(colorPreview);
        CATEGORY_DIALOG_STATES.put(colorPickerDialogId, state);

        // Preview setzen
        colorPreview.setBackgroundColor(startColor);

        // Picker öffnen
        colorPreview.setOnClickListener(v -> {
            ColorPickerDialog.newBuilder()
                    .setDialogId(colorPickerDialogId)
                    .setColor(state.selectedColor)
                    .setShowAlphaSlider(false)
                    .show(fragment.requireActivity());
        });

        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setView(view)
                .setPositiveButton(R.string.save_category, (d, w) -> {

                    String name = "";
                    if (nameInput.getText() != null) {
                        name = nameInput.getText().toString().trim();
                    }

                    if (name.isEmpty()) {
                        return;
                    }

                    CategoryDialogState current = CATEGORY_DIALOG_STATES.get(colorPickerDialogId);
                    int picked = startColor;
                    if (current != null) {
                        picked = current.selectedColor;
                    }

                    String hex = colorToHex(picked);

                    if (onSaved != null) {
                        onSaved.onSaved(name, hex);
                    }

                    // Cleanup nur bei Save
                    CATEGORY_DIALOG_STATES.remove(colorPickerDialogId);
                })
                .setNegativeButton(R.string.cancel, (d, w) -> {
                    // Cancel = keine Änderung
                    CATEGORY_DIALOG_STATES.remove(colorPickerDialogId);
                })
                .show();
    }


    /**
     * Muss aus Fragment/Activity onColorSelected(...) aufgerufen werden.
     */
    public static void onColorSelectedForCategoryDialog(int dialogId, int color) {
        CategoryDialogState state = CATEGORY_DIALOG_STATES.get(dialogId);
        if (state == null) {
            return;
        }

        state.selectedColor = color;

        View preview = state.colorPreviewRef != null ? state.colorPreviewRef.get() : null;
        if (preview != null) {
            preview.setBackgroundColor(color);
        }
    }

    public static void showDeleteCategoryDialog(
            Context context,
            String categoryName,
            OnDialogConfirmedListener listener
    ) {
        new MaterialAlertDialogBuilder(context)
                .setIcon(R.drawable.ic_delete)
                .setTitle(R.string.delete_category)
                .setMessage(context.getString(R.string.delete_category_confirm, categoryName)
                        + "\n\n" + context.getString(R.string.delete_category_hint))

                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (listener != null) {
                        listener.onConfirmed();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

}
