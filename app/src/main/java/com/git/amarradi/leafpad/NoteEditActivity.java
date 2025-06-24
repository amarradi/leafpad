package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.git.amarradi.leafpad.helper.DialogHelper;
import com.git.amarradi.leafpad.helper.ShareHelper;
import com.git.amarradi.leafpad.model.Leaf;
import com.git.amarradi.leafpad.model.Note;
import com.git.amarradi.leafpad.viewmodel.NoteViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
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
    private MaterialSwitch visibleSwitch;
    private MaterialSwitch recipeSwitch;
    private boolean shouldPersistOnPause = true;
    private boolean isNoteDeleted = false; // <--- Flag setzen!

    private boolean isNewNote = false;

    private boolean fromSearch = false;
    private boolean isUIConfigured = false;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_note_edit);
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

        // Fall 2: Normale Bearbeitung einer bestehenden oder neuen Note
//        String noteId = intent.getStringExtra(Leafpad.EXTRA_NOTE_ID);
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

                setupRecipeSwitch(note);
                setupVisibilitySwitch(note);
            }
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void setupVisibilitySwitch(Note note) {
        visibleSwitch.setOnCheckedChangeListener(null);
        boolean isHidden = note.isHide();
        visibleSwitch.setChecked(isHidden);
        updateVisibilityUI(isHidden);
        visibleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            note.setHide(isChecked);
            updateVisibilityUI(isChecked);
        });

        if (isHidden) {
            visibleSwitch.setChecked(true);
            visibleSwitch.setText(getString(R.string.show_note));
            visibleSwitch.setThumbIconDrawable(getDrawable(R.drawable.action_eye_closed));
        } else {
            visibleSwitch.setChecked(false);
            visibleSwitch.setText(getString(R.string.hide_note));
            visibleSwitch.setThumbIconDrawable(getDrawable(R.drawable.action_eye_open));
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void updateVisibilityUI(boolean isHidden) {
        if (isHidden) {
            visibleSwitch.setText(R.string.show_note);
            visibleSwitch.setThumbIconDrawable(getDrawable(R.drawable.action_eye_closed));
        } else {
            visibleSwitch.setText(R.string.hide_note);
            visibleSwitch.setThumbIconDrawable(getDrawable(R.drawable.action_eye_open));
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void setupRecipeSwitch(Note note) {
        String cat = res.getStringArray(R.array.category)[0];
        recipeSwitch.setOnCheckedChangeListener(null);
        boolean isRecipe = cat.equals(note.getCategory());
        recipeSwitch.setChecked(isRecipe);

        if (isRecipe) {
            recipeSwitch.setText(R.string.note_is_no_recipe);
            recipeSwitch.setThumbIconDrawable(getDrawable(R.drawable.togue));
        } else {
            recipeSwitch.setText(R.string.note_is_recipe);
            recipeSwitch.setThumbIconDrawable(getDrawable(R.drawable.togue_strikethrough));
        }

        recipeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                noteViewModel.updateNoteRecipe(cat);
            } else {
                noteViewModel.updateNoteRecipe("");
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
        bodyEdit.post(new Runnable() {
            @Override
            public void run() {
                Rect visibleBounds = new Rect();
                getWindow().getDecorView().getWindowVisibleDisplayFrame(visibleBounds);

                int availableHeight = visibleBounds.height();

                // Beispiel: 40% der tatsächlich sichtbaren Fensterhöhe als Mindesthöhe
                int minHeight = (int) (availableHeight * 0.68);

                bodyEdit.setMinHeight(minHeight);
            }
        });
        visibleSwitch = findViewById(R.id.visible_switch);
        recipeSwitch = findViewById(R.id.recipe_switch);
        titleLayout.setHintEnabled(false);
        bodyLayout.setHintEnabled(false);
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
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return switch (id) {

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
        if (recipeSwitch.isChecked()) {
            current.setCategory(res.getStringArray(R.array.category)[0]);
        } else {
            current.setCategory("");
        }
        current.setHide(visibleSwitch.isChecked());
    }

    private void exitNoteEdit() {
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
        }
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
        if(!isNoteDeleted && !isNewEntry(note)) {
            updateNoteFromUI();
            if (shouldPersistOnPause && noteViewModel.hasUnsavedChanges()) {
                noteViewModel.persist();
            }
        }
   }
}