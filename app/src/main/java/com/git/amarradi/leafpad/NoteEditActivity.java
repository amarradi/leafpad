package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
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
    private Note note;
    private NoteViewModel noteViewModel;
    private MaterialToolbar toolbar;
    private Resources res;
    private boolean shouldPersistOnPause = true;
    private boolean isNoteDeleted = false; // <--- Flag setzen!
    private NestedScrollView bodyScroll;



    private boolean isNewNote = false;

    private boolean fromSearch = false;
    private boolean isUIConfigured = false;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_note_edit);
        View root = findViewById(R.id.body_scroll); // oder R.id.all wenn du CoordinatorLayout paddest

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

        String noteId = getIntent().getStringExtra("noteId");
        if (noteId != null) {
            List<Note> allNotes = Leaf.loadAll(this, true); // oder false, je nach ShowHidden
            for (Note note : allNotes) {
                if (note.getId().equals(noteId)) {
                    noteViewModel.setNote(note);
                    break;
                }
            }
        }
        handleIntent(getIntent());
        fromSearch = getIntent().getBooleanExtra("fromSearch", false);
        observeNote();

        View rootEdit = findViewById(R.id.all);
        View toolbar = findViewById(R.id.toolbar);
        View title = findViewById(R.id.default_text_input_layout); // oder null, wenn nicht vorhanden
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

                // Optional: Feedback zeigen oder Activity direkt schließen
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

        Note note = Leaf.load(this, noteId);
        if (note == null) {
            Log.e("NoteEditActivity", "handleIntent: Note konnte nicht geladen werden für noteId=" + noteId);
            return;
        }

        if (isNewEntry(note)) {
            isNewNote = true;
            note.setNotedate();
            note.setNotetime();
        }

        noteViewModel.selectNote(note);
    }

    private void observeNote() {
        noteViewModel.getSelectedNote().observe(this, note -> {
            if (note != null) {
                this.note = note;
                if (!isUIConfigured) {
                    configureUIFromNote(note);
                    isUIConfigured = true;
                }
                // Menü updaten:
                invalidateOptionsMenu();
            }
        });
    }

    private void configureUIFromNote(Note note) {
            if (note.getTitle() != null) {
                titleEdit.setText(note.getTitle());
            } else {
                titleEdit.setText("");
            }

            if (note.getBody() != null) {
                bodyEdit.setText(note.getBody());
            } else {
                bodyEdit.setText("");
            }
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
        if (note.getTitle() == null) {
            return true;
        }
        if (note.getTitle().isEmpty()) {
            return true;
        }
        if (note.getBody() == null) {
            return true;
        }
        if (note.getBody().isEmpty()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note_edit, menu);
        if (note != null) {
            MenuItem recipeItem = menu.findItem(R.id.action_recipe);
            boolean isRecipe = note.getCategory() != null && note.getCategory().equals(res.getStringArray(R.array.category)[0]);
            recipeItem.setChecked(isRecipe);

            if(isRecipe) {
                recipeItem.setIcon(R.drawable.btn_chefhat_active);
                invalidateOptionsMenu();
            } else {
                recipeItem.setIcon(R.drawable.btn_chefhat);
                invalidateOptionsMenu();
            }

            MenuItem hideItem = menu.findItem(R.id.action_hide);
            hideItem.setChecked(note.isHide());

            if(note.isHide()) {
                hideItem.setIcon(R.drawable.btn_hide);
                invalidateOptionsMenu();
            } else {
                hideItem.setIcon(R.drawable.btn_show);
                invalidateOptionsMenu();
            }
        }
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return switch (id) {

            case R.id.action_recipe -> {
                // Toggle Rezept-Status
                item.setChecked(!item.isChecked());
                if (item.isChecked()) {
                    note.setCategory(res.getStringArray(R.array.category)[0]);
                } else {
                    note.setCategory("");
                }
                yield true;
            }
            case R.id.action_hide -> {
                // Toggle Sichtbarkeit
                item.setChecked(!item.isChecked());
                note.setHide(item.isChecked());
                yield true;
            }

            case R.id.action_share_note -> {
                ShareHelper.shareNote(this,note);
                yield true;
            }
            case R.id.action_remove -> {
                DialogHelper.showDeleteSingleNoteDialog(NoteEditActivity.this, this::removeNote);
                yield true;
            }
            case R.id.action_save -> {
                saveNote();
                yield true;
            }
            default -> super.onOptionsItemSelected(item);
        };
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

        if (current != null) {
            current.setTitle(titleEdit.getText().toString());
            current.setBody(bodyEdit.getText().toString());
        }
        if(noteViewModel.hasUnsavedChanges() && !isNewNote) {
            if(Leafpad.isChangeNotificationEnabled(this) ){
                DialogHelper.showUnsavedChangesDialog(
                        NoteEditActivity.this,
                        ()-> {
                            shouldPersistOnPause = true;
                            noteViewModel.persist();
                            setResult(RESULT_OK);
                            exitNoteEdit();
                        },
                        () ->{
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
        if (note == null) {
            return;
        }
        updateNoteFromUI();

        if (NoteViewModel.isEmptyEntry(note)) {
            Leaf.remove(this, note);
        } else {
            Leaf.set(this, note);
            noteViewModel.saveNote(getApplication(), note);
            noteViewModel.markSaved();

        }
        setResult(RESULT_OK);
       // finish();
    }

    private void removeNote() {
        if (note != null) {
            noteViewModel.deleteNote(getApplication(), note);
            note = null;
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
        shouldPersistOnPause = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(!isNoteDeleted && note != null && !NoteViewModel.isEmptyEntry(note)) {
            updateNoteFromUI();
            if (shouldPersistOnPause && noteViewModel.hasUnsavedChanges()) {
                noteViewModel.persist();
                noteViewModel.markSaved();
                setResult(RESULT_OK);
            }
        }
   }
}