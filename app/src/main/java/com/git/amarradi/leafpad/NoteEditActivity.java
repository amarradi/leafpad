package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;

import com.git.amarradi.leafpad.helper.DialogHelper;
import com.git.amarradi.leafpad.helper.EditorMinHeightHelper;
import com.git.amarradi.leafpad.helper.ShareHelper;
import com.git.amarradi.leafpad.model.Leaf;
import com.git.amarradi.leafpad.model.Note;
import com.git.amarradi.leafpad.viewmodel.NoteViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.Objects;

public class NoteEditActivity extends AppCompatActivity {

    private EditText titleEdit;
    private EditText bodyEdit;
    private NoteViewModel noteViewModel;
    private MaterialToolbar toolbar;
    private Resources res;
    private boolean shouldPersistOnPause = true;
    private boolean isNoteDeleted = false;
    private NestedScrollView bodyScroll;
    private boolean isNewNote = false;
    private boolean fromSearch = false;
    private boolean isUIConfigured = false;
    private MenuItem saveMenuItem;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_note_edit);
        View root = findViewById(R.id.body_scroll);

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

        String noteId = getIntent().getStringExtra("noteId");
        if (noteId != null) {
            List<Note> allNotes = Leaf.loadAll(this, true); // oder false, je nach ShowHidden
            for (Note n : allNotes) {
                if (n.getId().equals(noteId)) {
                    noteViewModel.setNote(n);
                    break;
                }
            }
        }
        handleIntent(getIntent());
        fromSearch = getIntent().getBooleanExtra("fromSearch", false);
        observeNote();

        View rootEdit = findViewById(R.id.all);
        View toolbar = findViewById(R.id.toolbar);
        View title = findViewById(R.id.default_text_input_layout);
        EditText bodyEdit = findViewById(R.id.body_edit);

        EditorMinHeightHelper.adjustMinHeight(rootEdit, toolbar, title, bodyEdit);
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            String shareText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shareText != null && !shareText.isEmpty()) {
                Note newNote = new Note("", "", "", "", "", false, "", Note.makeId());
                newNote.setTitle(getString(R.string.imported));
                newNote.setBody(shareText);
                newNote.setNotedate();
                newNote.setNotetime();
                newNote.setCreateDate();

                Leaf.set(this, newNote);
                noteViewModel.loadNotes();

                setResult(RESULT_OK);
                finish();
                return;
            }
        }
        String noteId = getIntent().getStringExtra(Leafpad.EXTRA_NOTE_ID);

        if (noteId == null) {
            Log.e("NoteEditActivity", "handleIntent: Keine noteId vorhanden, neue leere Notiz wird erzeugt");

            Note newNote = new Note("", "", "", "", "", false, "", Note.makeId());
            newNote.setNotedate();
            newNote.setNotetime();
            newNote.setCreateDate();

            noteViewModel.selectNote(newNote);
            isNewNote = true;
            return;
        }

        Note loaded = Leaf.load(this, noteId);
        if (loaded == null) {
            Log.e("NoteEditActivity", "handleIntent: Note konnte nicht geladen werden für noteId=" + noteId);
            return;
        }

        if (isNewEntry(loaded)) {
            isNewNote = true;
            loaded.setNotedate();
            loaded.setNotetime();
        }

        noteViewModel.selectNote(loaded);
    }

    private void observeNote() {
        noteViewModel.getSelectedNote().observe(this, note -> {
            if (note != null && !isUIConfigured) {
                configureUIFromNote(note);
                isUIConfigured = true;
            }
            // Menü immer updaten!
            invalidateOptionsMenu();
        });
    }

    private void configureUIFromNote(Note note) {
        titleEdit.setText(note.getTitle() != null ? note.getTitle() : "");
        bodyEdit.setText(note.getBody() != null ? note.getBody() : "");
    }

    private void initViews() {
        TextInputLayout titleLayout = findViewById(R.id.default_text_input_layout);
        TextInputLayout bodyLayout = findViewById(R.id.body_text_input_layout);
        titleEdit = findViewById(R.id.title_edit);
        bodyEdit = findViewById(R.id.body_edit);
        bodyScroll = findViewById(R.id.body_scroll);

        bodyEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                bodyEdit.post(() -> scrollToCursor());
            }
        });

        bodyEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                bodyEdit.postDelayed(this::scrollToCursor, 250);
            }
        });

        bodyEdit.setOnClickListener(v -> {
            bodyEdit.postDelayed(this::scrollToCursor, 250);
        });

        titleLayout.setHintEnabled(false);
        bodyLayout.setHintEnabled(false);
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

    private boolean isNewEntry(Note note) {
        return (note.getTitle() == null || note.getTitle().isEmpty() ||
                note.getBody() == null || note.getBody().isEmpty());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note_edit, menu);
        saveMenuItem = menu.findItem(R.id.action_save);
        if (saveMenuItem != null) {
            saveMenuItem.setEnabled(false); // Initial disabled
        }
        Note current = noteViewModel.getSelectedNote().getValue();
        if (current != null) {
            MenuItem recipeItem = menu.findItem(R.id.action_recipe);
            boolean isRecipe = current.getCategory() != null &&
                    current.getCategory().equals(res.getStringArray(R.array.category)[0]);
            recipeItem.setChecked(isRecipe);
            recipeItem.setIcon(isRecipe ? R.drawable.btn_chefhat_active : R.drawable.btn_chefhat);

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
            case R.id.action_recipe: {
                Note current = noteViewModel.getSelectedNote().getValue();
                if (current != null) {
                    if (item.isChecked()) {
                        current.setCategory("");
                    } else {
                        current.setCategory(res.getStringArray(R.array.category)[0]);
                    }
                }
                invalidateOptionsMenu();
                return true;
            }
            case R.id.action_hide: {
                Note current = noteViewModel.getSelectedNote().getValue();
                if (current != null) {
                    current.setHide(!item.isChecked());
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
        setResult(RESULT_OK);
        if (fromSearch) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else {
            NoteEditActivity.this.finish();
        }
    }

    private void checkForUnsavedChanges() {
        updateNoteFromUI();
        Note current = noteViewModel.getSelectedNote().getValue();

        if (current != null && (current.getTitle() == null || current.getTitle().trim().isEmpty())) {
            DialogHelper.showTitleRequiredDialog(this);
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
                            setResult(RESULT_OK);
                            exitNoteEdit();
                        },
                        () -> {
                            shouldPersistOnPause = false;
                            exitNoteEdit();
                        }
                );
            } else {
                exitNoteEdit();
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
            DialogHelper.showTitleRequiredDialog(this);
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
            Leaf.set(this, current);
            noteViewModel.saveNote(getApplication(), current);
            noteViewModel.markSaved();
        }
        Intent resultIntent = new Intent();
        resultIntent.putExtra("updated_note", current);
        resultIntent.putExtra("is_new_note", noteViewModel.isNewEntry(current));
        setResult(RESULT_OK, resultIntent);
        finish(); // optional hier direkt beenden, wenn nicht schon an anderer Stelle

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

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> checkForUnsavedChanges());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Leafpad.isKeepScreenOnEnabled(this)) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Note current = noteViewModel.getSelectedNote().getValue();
        if (!isNoteDeleted && current != null && !NoteViewModel.isEmptyEntry(current)) {
            updateNoteFromUI();
            if (shouldPersistOnPause && noteViewModel.hasUnsavedChanges()) {
                noteViewModel.persist();
                noteViewModel.markSaved();
                setResult(RESULT_OK);
            }
        }
    }
}
