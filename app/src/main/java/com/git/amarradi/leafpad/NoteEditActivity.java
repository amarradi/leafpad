// NoteEditActivity.java - Vollständig MVVM-konform
package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.git.amarradi.leafpad.helper.DialogHelper;
import com.git.amarradi.leafpad.helper.ShareHelper;
import com.git.amarradi.leafpad.model.Note;
import com.git.amarradi.leafpad.viewmodel.NoteViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class NoteEditActivity extends AppCompatActivity {

    private EditText titleEdit;
    private EditText bodyEdit;
    private NoteViewModel noteViewModel;
    private MaterialToolbar toolbar;
    private MaterialSwitch visibleSwitch;
    private MaterialSwitch recipeSwitch;
    private boolean shouldPersistOnPause = true;
    private boolean isNoteDeleted = false;
    private boolean fromSearch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_note_edit);

        initViews();
        setupToolbar();

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        handleIntent(getIntent());
        fromSearch = getIntent().getBooleanExtra("fromSearch", false);

        observeNote();
    }

    private void initViews() {
        TextInputLayout titleLayout = findViewById(R.id.default_text_input_layout);
        TextInputLayout bodyLayout = findViewById(R.id.body_text_input_layout);
        titleEdit = findViewById(R.id.title_edit);
        bodyEdit = findViewById(R.id.body_edit);
        visibleSwitch = findViewById(R.id.visible_switch);
        recipeSwitch = findViewById(R.id.recipe_switch);

        titleLayout.setHintEnabled(false);
        bodyLayout.setHintEnabled(false);

        titleEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                noteViewModel.updateTitle(titleEdit.getText().toString());
            }
        });

        bodyEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                noteViewModel.updateBody(bodyEdit.getText().toString());
            }
        });

        visibleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            noteViewModel.setHidden(isChecked);
        });

        recipeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            noteViewModel.setRecipe(isChecked);
        });
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> checkForUnsavedChanges());
    }

    private void handleIntent(Intent intent) {
        String noteId = intent.getStringExtra(Leafpad.EXTRA_NOTE_ID);

        if (noteId == null) {
            noteViewModel.createNewNote(this);
        } else {
            noteViewModel.loadNoteById(this, noteId);
        }
    }

    private void observeNote() {
        noteViewModel.getSelectedNote().observe(this, note -> {
            if (note == null) return;

            titleEdit.setText(note.getTitle());
            bodyEdit.setText(note.getBody());
            visibleSwitch.setChecked(note.isHide());

            boolean isRecipe = noteViewModel.isRecipeNote(note);
            recipeSwitch.setChecked(isRecipe);
        });
    }

    private void checkForUnsavedChanges() {
        if (noteViewModel.hasUnsavedChanges() && !noteViewModel.isNewEntry()) {
            if (Leafpad.isChangeNotificationEnabled(this)) {
                DialogHelper.showUnsavedChangesDialog(
                        this,
                        () -> {
                            shouldPersistOnPause = true;
                            noteViewModel.persist();
                            exitNoteEdit();
                        },
                        this::exitNoteEdit
                );
            } else {
                exitNoteEdit();
            }
        } else {
            exitNoteEdit();
        }
    }

    private void exitNoteEdit() {
        if (fromSearch) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share_note) {
            ShareHelper.shareNote(this, noteViewModel.getSelectedNote().getValue());
            return true;
        } else if (id == R.id.action_remove) {
            DialogHelper.showDeleteSingleNoteDialog(this, () -> {
                noteViewModel.deleteNote(this);
                isNoteDeleted = true;
                finish();
            });
            return true;
        } else if (id == R.id.action_save) {
            noteViewModel.persist();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isNoteDeleted && !noteViewModel.isNewEntry()) {
            if (shouldPersistOnPause && noteViewModel.hasUnsavedChanges()) {
                noteViewModel.persist();
            }
        }
    }
}
