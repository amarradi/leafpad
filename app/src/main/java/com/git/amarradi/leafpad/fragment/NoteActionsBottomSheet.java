package com.git.amarradi.leafpad.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.git.amarradi.leafpad.R;
import com.git.amarradi.leafpad.model.Note;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textview.MaterialTextView;

public class NoteActionsBottomSheet extends BottomSheetDialogFragment {

    @Override
    public int getTheme() {
        return R.style.AppBottomSheetDialogTheme;
    }

    public interface OnNoteActionListener {
        void onHideToggle(Note note);

        void onShare(Note note);

        void onRemove(Note note);
    }

    private Note note;
    private OnNoteActionListener listener;

    public static NoteActionsBottomSheet newInstance(Note note, OnNoteActionListener listener) {
        NoteActionsBottomSheet sheet = new NoteActionsBottomSheet();
        sheet.note = note;
        sheet.listener = listener;
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_note_actions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Edge-to-Edge: unteren Insets-Abstand (Navigationsleiste) als Padding einrechnen
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int bottom = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom);
            return insets;
        });
        if (note == null) {
            dismiss();
            return;
        }

        // ------------------------------------------------------------
        // Verstecken/Zeigen – Icon & Text je nach aktuellem Status
        // ------------------------------------------------------------
        View hideRow = view.findViewById(R.id.action_hide_note);
        ImageView hideIcon = view.findViewById(R.id.action_hide_note_icon);
        MaterialTextView hideText = view.findViewById(R.id.action_hide_note_text);

        if (note.isHide()) {
            hideText.setText(getString(R.string.show_note));
            hideIcon.setImageResource(R.drawable.btn_show);
        } else {
            hideText.setText(getString(R.string.hide_note));
            hideIcon.setImageResource(R.drawable.btn_hide);
        }

        hideRow.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHideToggle(note);
            }
            dismiss();
        });

        // ------------------------------------------------------------
        // Teilen
        // ------------------------------------------------------------
        view.findViewById(R.id.action_share_note).setOnClickListener(v -> {
            if (listener != null) {
                listener.onShare(note);
            }
            dismiss();
        });

        // ------------------------------------------------------------
        // Löschen
        // ------------------------------------------------------------
        view.findViewById(R.id.action_remove).setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemove(note);
            }
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                (com.google.android.material.bottomsheet.BottomSheetDialog) getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior =
                        com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }
}