package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;

import com.git.amarradi.leafpad.fragment.CategoryFragment;
import com.git.amarradi.leafpad.helper.DialogHelper;
import com.git.amarradi.leafpad.helper.EditorMinHeightHelper;
import com.git.amarradi.leafpad.helper.ShareHelper;
import com.git.amarradi.leafpad.model.CategoryEntity;
import com.git.amarradi.leafpad.model.Leaf;
import com.git.amarradi.leafpad.model.Note;
import com.git.amarradi.leafpad.viewmodel.NoteViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

import java.util.Objects;

public class NoteEditActivity extends AppCompatActivity implements ColorPickerDialogListener {
    @Override
    public void onColorSelected(int dialogId, int color) {
        DialogHelper.onColorSelectedForCategoryDialog(dialogId, color);
    }

    @Override
    public void onDialogDismissed(int dialogId) {

    }

    private EditText titleEdit;
    private EditText bodyEdit;
    private NoteViewModel noteViewModel;
    private MaterialToolbar toolbar;
    public Resources res;

    private boolean shouldPersistOnPause = true;
    private boolean isNoteDeleted = false;
    private NestedScrollView bodyScroll;
    private boolean isNewNote = false;
    private boolean fromSearch = false;
    //private boolean isUIConfigured = false;
    private String configuredNoteId = null;
    private MenuItem saveMenuItem;
    private TextWatcher modificationWatcher;

    private void logNav(String msg) {
        if (!BuildConfig.DEBUG) return;
        Log.d("NAV_NOTE", msg
                + " | backStack=" + getSupportFragmentManager().getBackStackEntryCount()
                + " | fc=" + (findViewById(R.id.fragment_container) != null ? findViewById(R.id.fragment_container).getVisibility() : -1)
                + " | bs=" + (findViewById(R.id.body_scroll) != null ? findViewById(R.id.body_scroll).getVisibility() : -1)
        );
    }


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Leafpad.isKeepScreenOnEnabled(this)) {
            if (BuildConfig.DEBUG) {
                Log.d("KeepScreenOn", "Preference says: " + Leafpad.isKeepScreenOnEnabled(this));
            }
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (BuildConfig.DEBUG) {
                Log.d("KeepScreenOn", "FLAG_KEEP_SCREEN_ON gesetzt");
            }
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_note_edit);
        View root = findViewById(R.id.body_scroll);
        ChipGroup categoryChipGroup = findViewById(R.id.category_chip_group);
        View categoryChipScroll = findViewById(R.id.category_chip_scroll);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int nav = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            int bottom = Math.max(ime, nav);
            view.setPadding(0, 0, 0, bottom);
            return insets;
        });

        res = getResources();
        initViews();
        setupToolbar();
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        noteViewModel.getIsNoteModified().observe(this, isModified -> {
            if (saveMenuItem != null) {
                saveMenuItem.setEnabled(Boolean.TRUE.equals(isModified));
            }
        });

        Intent intent = getIntent();

        if (Intent.ACTION_SEND.equals(intent.getAction())
                && "text/plain".equals(intent.getType())) {
            handleShareIntent(intent);
            return;
        }

        boolean isNewNoteIntent = intent.getBooleanExtra(Leafpad.EXTRA_IS_NEW_NOTE, false);

        String noteId = getIntent().getStringExtra(Leafpad.EXTRA_NOTE_ID);
        if (!isNewNoteIntent && noteId == null && !Intent.ACTION_SEND.equals(intent.getAction())) {
            isNewNoteIntent = true;
            noteId = Note.makeId();
        }
        if (isNewNoteIntent) {
            Note newNote = new Note(
                    "", "", "", "", "",
                    false, "", noteId
            );
            newNote.setNotedate();
            newNote.setNotetime();
            newNote.setCreateDate();

            isNewNote = true;
            noteViewModel.setNote(newNote);

        } else if (noteId != null) {

            noteViewModel.getNoteById(noteId).observe(this, note -> {
                if (note == null) return;

                isNewNote = false;
                noteViewModel.setNote(note);
            });

        }

        fromSearch = getIntent().getBooleanExtra("fromSearch", false);
        observeNote();

        View rootEdit = findViewById(R.id.all);
        logNav("toolbar instance=" + toolbar);
        View toolbar = findViewById(R.id.toolbar);
        if (BuildConfig.DEBUG) {

            Log.d("NAV_NOTE", "setting_toolbar in NoteEditActivity=" + toolbar);
        }
        View title = findViewById(R.id.default_text_input_layout);
        EditText bodyEdit = findViewById(R.id.body_edit);

        EditorMinHeightHelper.adjustMinHeight(rootEdit, toolbar, title, bodyEdit);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                {
                    logNav("OnBackPressedCallback fired");

                    if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                        logNav("BackStack > 0 -> popBackStackImmediate");
                        getSupportFragmentManager().popBackStackImmediate();
                        View fc = findViewById(R.id.fragment_container);
                        View bs = findViewById(R.id.body_scroll);
                        if (fc != null) fc.setVisibility(View.GONE);
                        if (bs != null) bs.setVisibility(View.VISIBLE);

                        restoreEditorToolbar();
                        logNav("After pop+UI restore");
                        return;
                    }
                    logNav("No backstack -> checkForUnsavedChanges()");
                    checkForUnsavedChanges();
                }
            }
        });
        noteViewModel.getCategoriesForSelectedNote()
                .observe(this, categories -> {

                    categoryChipGroup.removeAllViews();

                    if (categories == null || categories.isEmpty()) {
                        categoryChipGroup.setVisibility(View.GONE);
                        return;
                    }

                    categoryChipScroll.setVisibility(View.VISIBLE);

                    categoryChipGroup.setVisibility(View.VISIBLE);

                    for (CategoryEntity c : categories) {
                        Chip chip = (Chip) getLayoutInflater().inflate(
                                R.layout.item_category_chip,
                                categoryChipGroup,
                                false
                        );

                        chip.setText(c.name);
                        applyCategoryChipStyle(chip, c.colorHex);

                        chip.setClickable(false);
                        chip.setCheckable(false);
                        chip.setCloseIconVisible(false);
                        chip.setEnsureMinTouchTargetSize(false);

                        categoryChipGroup.addView(chip);
                    }
                });

    }

    private void applyCategoryChipStyle(Chip chip, String colorHex) {
        int baseColor;

        try {
            baseColor = Color.parseColor(colorHex);
        } catch (Exception e) {
            baseColor = Color.GRAY;
        }

        int bgColor = com.git.amarradi.leafpad.helper.ColorUtilsHelper.lightenColor(baseColor, 0.35f);

        chip.setChipStrokeWidth(
                com.git.amarradi.leafpad.helper.ColorUtilsHelper.dpToPx(chip.getContext(), 1)
        );
        chip.setChipStrokeColor(ColorStateList.valueOf(baseColor));
        chip.setChipBackgroundColor(ColorStateList.valueOf(bgColor));

        boolean darkBg = androidx.core.graphics.ColorUtils.calculateLuminance(bgColor) < 0.5;
        int textColor = darkBg ? Color.WHITE : Color.BLACK;
        chip.setTextColor(textColor);
    }

//    private void applyCategoryColor(Chip chip, String colorHex) {
//        if (colorHex == null || colorHex.isEmpty()) {
//            return;
//        }
//
//        try {
//            int color = android.graphics.Color.parseColor(colorHex);
//            chip.setTextColor(color);
//            int bgColor = androidx.core.graphics.ColorUtils.setAlphaComponent(color, 40);
//            chip.setChipBackgroundColor(
//                    android.content.res.ColorStateList.valueOf(bgColor)
//            );
//
//        } catch (IllegalArgumentException e) {
//
//        }
//    }


    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    private void handleShareIntent(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            String shareText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shareText != null && !shareText.isEmpty()) {

                Note newNote = new Note("", "", "", "", "", false, "", Note.makeId());
                newNote.setTitle(getString(R.string.imported));
                newNote.setBody(shareText);
                newNote.setNotedate();
                newNote.setNotetime();
                newNote.setCreateDate();
                noteViewModel.saveNote(getApplicationContext(), newNote);

                setResult(RESULT_OK);
                finish();
            }
        }
    }


    private void observeNote() {
        noteViewModel.getSelectedNote().observe(this, note -> {
            if (note != null) {
                if (!note.getId().equals(configuredNoteId)) {
                    configureUIFromNote(note);
                    configuredNoteId = note.getId();
                }
            }
            // Menü immer updaten!
            invalidateOptionsMenu();
        });
    }

    private void configureUIFromNote(Note note) {
        titleEdit.removeTextChangedListener(modificationWatcher);
        bodyEdit.removeTextChangedListener(modificationWatcher);

        titleEdit.setText(note.getTitle() != null ? note.getTitle() : "");
        bodyEdit.setText(note.getBody() != null ? note.getBody() : "");

        titleEdit.addTextChangedListener(modificationWatcher);
        bodyEdit.addTextChangedListener(modificationWatcher);

        bodyEdit.postDelayed(() -> {
            bodyEdit.getText().length();
            scrollToCursor();
        },150);
    }

    private void initViews() {
        TextInputLayout titleLayout = findViewById(R.id.default_text_input_layout);
        TextInputLayout bodyLayout = findViewById(R.id.body_text_input_layout);
        titleEdit = findViewById(R.id.title_edit);
        bodyEdit = findViewById(R.id.body_edit);
        bodyScroll = findViewById(R.id.body_scroll);

        titleLayout.setHintEnabled(false);
        bodyLayout.setHintEnabled(false);

        modificationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                noteViewModel.updateNoteFromUI(
                        titleEdit.getText().toString(),
                        bodyEdit.getText().toString()
                );
            }
        };

        bodyEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                bodyEdit.postDelayed(this::scrollToCursor, 250);
            }
        });

        bodyEdit.setOnClickListener(v -> {
            bodyEdit.postDelayed(this::scrollToCursor, 250);
        });
    }

    private void scrollToCursor() {
        int selection = bodyEdit.getSelectionStart();
        Layout layout = bodyEdit.getLayout();
        if (layout != null && selection > 0) {
            int line = layout.getLineForOffset(selection);
            int y = layout.getLineBottom(line);
            bodyScroll.smoothScrollTo(0, y);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note_edit, menu);
        saveMenuItem = menu.findItem(R.id.action_save);
        Boolean modified = noteViewModel.getIsNoteModified().getValue();
        if (saveMenuItem != null) {
            saveMenuItem.setEnabled(modified != null && modified);
        }
        Note current = noteViewModel.getSelectedNote().getValue();
        if (current != null) {
            MenuItem hideItem = menu.findItem(R.id.action_hide);
            hideItem.setChecked(current.isHide());
            hideItem.setIcon(current.isHide() ? R.drawable.btn_hide : R.drawable.btn_show);
        }
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {

            case R.id.action_hide: {
                Note current = noteViewModel.getSelectedNote().getValue();
                if (current != null) {
                    current.setHide(!item.isChecked());
                    noteViewModel.updateModificationState();

                }
                invalidateOptionsMenu();
                return true;
            }
            case R.id.action_share_note: {
                Note current = noteViewModel.getSelectedNote().getValue();
                if (current != null) {
                    ShareHelper.shareNote(this, current);
                }
                return true;
            }
            case R.id.action_remove: {
                DialogHelper.showDeleteSingleNoteDialog(NoteEditActivity.this, this::removeNote);
                return true;
            }
            case R.id.action_save: {
                saveNote();
                return true;
            }
            case R.id.action_setCategory: {
                findViewById(R.id.body_scroll).setVisibility(View.GONE);
                findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayShowTitleEnabled(true);
                    getSupportActionBar().setTitle(R.string.manage_categories);
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

                }
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(
                                    R.id.fragment_container,
                                    CategoryFragment.newInstance(CategoryFragment.MODE_PICK_FOR_NOTE)
                            )
                            .addToBackStack("category")
                            .commit();


                return true;
            }
            case R.id.home: {
                logNav("onOptionsItemSelected: HOME");
                getOnBackPressedDispatcher().onBackPressed();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void updateNoteFromUI() {
        Note current = noteViewModel.getSelectedNote().getValue();
        if (current == null) return;

        current.setTitle(titleEdit.getText().toString());
        current.setBody(bodyEdit.getText().toString());
    }

    private void exitNoteEdit() {
        setResultWithCurrentNote(isNewNote);
        if (fromSearch) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else {
            finish();
        }
    }

    private void checkForUnsavedChanges() {
        updateNoteFromUI();
        Note current = noteViewModel.getSelectedNote().getValue();

        if (current != null && (current.getTitle() == null || current.getTitle().trim().isEmpty())) {
            DialogHelper.showTitleRequiredDialog(this,  (dialog, which) -> {
                finish();
                    });
            titleEdit.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(titleEdit, InputMethodManager.SHOW_IMPLICIT);
            }
            return;
        }

        if (current != null) {
            current.setTitle(titleEdit.getText().toString());
            current.setBody(bodyEdit.getText().toString());
        }
        if (noteViewModel.hasUnsavedChanges() && !isNewNote) {
            if (Leafpad.isChangeNotificationEnabled(this)) {
                DialogHelper.showUnsavedChangesDialog(
                        NoteEditActivity.this,
                        () -> {
                            shouldPersistOnPause = true;
                            noteViewModel.persist();
                            returnResultAndFinish();
                        },
                        () -> {
                            shouldPersistOnPause = false;
                            exitNoteEdit();
                        }
                );
            } else {
                noteViewModel.persist();
                returnResultAndFinish();
            }
        } else {
            exitNoteEdit();
        }
    }

    private void saveNote() {
        Note current = noteViewModel.getSelectedNote().getValue();
        if (current == null) {
            return;
        }
        updateNoteFromUI();

        if(current.getTitle() == null || current.getTitle().trim().isEmpty()) {
            DialogHelper.showTitleRequiredDialog(this, (dialog, which) -> {
                finish();
            });
            titleEdit.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(titleEdit, InputMethodManager.SHOW_IMPLICIT);
            }
            return;
        }

        if (NoteViewModel.isEmptyEntry(current)) {
            Leaf.remove(this, current);
        } else {
            noteViewModel.saveNote(getApplication(), current);
            noteViewModel.markSaved();
        }
        Intent resultIntent = new Intent();
        resultIntent.putExtra("updated_note", current);
        resultIntent.putExtra("is_new_note", noteViewModel.isNewEntry(current));
        setResult(RESULT_OK, resultIntent);
        finish();

    }

    private void removeNote() {
        Note current = noteViewModel.getSelectedNote().getValue();
        if (current != null) {
            noteViewModel.deleteNote(getApplication(), current);
            isNoteDeleted = true;
        }
        setResult(RESULT_OK);
        finish();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //  logNav("Toolbar NAV click");
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

        toolbar.setOnTouchListener((v, event) -> {
            return false;
        });
    }

    private void returnResultAndFinish() {
        Note current = noteViewModel.getSelectedNote().getValue();
        if (current != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("updated_note", current);
            resultIntent.putExtra("is_new_note", isNewNote);
            setResult(RESULT_OK, resultIntent);
        } else {
            setResult(RESULT_OK);
        }
        finish();
    }


    @Override
    protected void onResume() {
        super.onResume();
        Leafpad.applyKeepScreenOnFlag(this);
        if (Leafpad.isKeepScreenOnEnabled(this)) {
            Leafpad.enableWakeLock(this);
        }
    }

    private void setResultWithCurrentNote(boolean isNew) {
        Note current = noteViewModel.getSelectedNote().getValue();
        if (current == null) {
            setResult(RESULT_CANCELED);
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("updated_note", current);
        resultIntent.putExtra("is_new_note", isNew);
        setResult(RESULT_OK, resultIntent);
    }


    @Override
    protected void onPause() {
        super.onPause();
        Leafpad.clearKeepScreenOnFlag(this);
        Leafpad.disableWakeLock();
       // getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Note current = noteViewModel.getSelectedNote().getValue();

       // ❗ Abbrechen, wenn Titel leer ist (egal ob Body gefüllt oder nicht)
       assert current != null;
       if (current.getTitle() == null || current.getTitle().trim().isEmpty()) {
           return;
       }
        // UI -> Note übernehmen
        updateNoteFromUI();
        // Wenn gelöscht wurde: nichts mehr persistieren
        if (isNoteDeleted) {
            return;
        }
        // Leere Notiz (nach deiner Logik) nicht speichern
        if (NoteViewModel.isEmptyEntry(current)) {
            return;
        }

        if (shouldPersistOnPause) {
            if (noteViewModel.hasUnsavedChanges()) {
                noteViewModel.persist();
                noteViewModel.markSaved();
                setResultWithCurrentNote(isNewNote);
            }
        }
    }

    public void restoreEditorToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        invalidateOptionsMenu();
    }
}
