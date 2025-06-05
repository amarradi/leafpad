package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.git.amarradi.leafpad.helper.DialogHelper;
import com.git.amarradi.leafpad.model.Leaf;
import com.git.amarradi.leafpad.model.Note;
import com.git.amarradi.leafpad.viewmodel.NoteViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputLayout;

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
        handleIntent(getIntent());
        observeNote();
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    private void handleIntent(Intent intent) {
        String noteId = intent.getStringExtra(Leafpad.EXTRA_NOTE_ID);
        Note note = Leaf.load(this, noteId);

        if (Intent.ACTION_SEND.equals(intent.getAction())&&"text/plain".equals(intent.getType())) {
            String shareText = intent.getStringExtra(Intent.EXTRA_TEXT);
            note.setTitle(getString(R.string.imported));
            note.setBody(shareText);
        }
        if (isNewEntry(note)) {
            note.setNotedate();
            note.setNotetime();
        }

        noteViewModel.selectNote(note);
//        Log.d("NoteEditActivity", "Selected note: " + noteViewModel.getSelectedNote().getValue());
    }

    private void observeNote() {
        noteViewModel.getSelectedNote().observe(this, note -> {
            if (note != null) {
                this.note = note;
//                Log.d("NoteEditActivity", "observeNote: 1 " + note.getTitle() + "|" + note.getBody());
                configureUIFromNote(note);
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
            Log.d("NoteEditActivity", "Recipe switch toggled: " + isChecked);
            if (isChecked) {
                noteViewModel.updateNoteRecipe(cat);
            } else {
                noteViewModel.updateNoteRecipe("");
            }
        });
    }

    private void configureUIFromNote(Note note) {
        toolbar.setTitle(R.string.action_edit_note);
        if (titleEdit.getText().toString().isEmpty()) {
            titleEdit.setText(note.getTitle());
        }
        if (bodyEdit.getText().toString().isEmpty()) {
            bodyEdit.setText(note.getBody());
        }

        if (isNewEntry(note)) {
            setupToolbarSubtitle(getString(R.string.new_note));
        } else {
            setupToolbarSubtitle(note.getTitle());
        }
    }

    private void setupToolbarSubtitle(String subtitle) {
        toolbar.setSubtitle(subtitle);
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
    }
//    private boolean isNewEntry(Note note) {
//        return note.getTitle() == null ||
//                note.getTitle().isEmpty() ||
//                note.getBody() == null ||
//                note.getBody().isEmpty();
//    }
    private boolean isNewEntry(Note note) {
        if (note.getTitle() == null) {
            Log.d("NoteEditActivity", "isNewEntry: title "+note.getTitle());
            return true;
        }
        if (note.getTitle().isEmpty()) {
            Log.d("NoteEditActivity", "isNewEntry: title "+note.getTitle());
            return true;
        }
        if (note.getBody() == null) {
            Log.d("NoteEditActivity", "isNewEntry: body "+note.getBody());
            return true;
        }
        if (note.getBody().isEmpty()) {
            Log.d("NoteEditActivity", "isNewEntry: body "+note.getBody());
            return true;
        }
        return false;
    }


    public boolean isEmptyEntry(Note note) {
        return note.getBody().isEmpty() && note.getTitle().isEmpty();
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
                shareNote();
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
//        Log.d("NoteEditActivity", "updateNoteFromUI: "+noteViewModel.getSelectedNote().getValue().getBody()
//                + "|"+noteViewModel.getSelectedNote().getValue().getBody()
//                + "|"+noteViewModel.getSelectedNote().getValue().getTitle()
//                + "|"+noteViewModel.getSelectedNote().getValue().getCategory()
//                + "|"+noteViewModel.getSelectedNote().getValue().isHide());
        if (current == null) return;

        current.setTitle(titleEdit.getText().toString());
        current.setBody(bodyEdit.getText().toString());
        if (recipeSwitch.isChecked()) {
//            Log.d("NoteEditActivity", "updateNoteFromUI: recipeSwitch = "+recipeSwitch.isChecked());
            current.setCategory(res.getStringArray(R.array.category)[0]);
        } else {
//            Log.d("NoteEditActivity", "updateNoteFromUI: recipeSwitch = "+recipeSwitch.isChecked());
            current.setCategory("");
        }
        current.setHide(visibleSwitch.isChecked());
//        Log.d("NoteEditActivity", "updateNoteFromUI: visibleSwitch = "+visibleSwitch.isChecked());
    }


    private void checkForUnsavedChanges() {
        updateNoteFromUI();
        Note current = noteViewModel.getSelectedNote().getValue();

        if (current != null) {
            current.setTitle(titleEdit.getText().toString());
            current.setBody(bodyEdit.getText().toString());
        }

        if(noteViewModel.hasUnsavedChanges()) {
            if(Leafpad.isChangeNotificationEnabled(this) ){
                DialogHelper.showUnsavedChangesDialog(
                        NoteEditActivity.this,
                        ()-> {
                            shouldPersistOnPause = true;
                            noteViewModel.persist();
                            NoteEditActivity.this.finish();
                        },
                        () ->{
                            shouldPersistOnPause = false;
                            NoteEditActivity.this.finish();
                        }
                );
            } else {
                NoteEditActivity.this.finish();
            }
        } else {
            NoteEditActivity.this.finish();
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
            toolbar.setSubtitle(note.getTitle());
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
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> checkForUnsavedChanges());
    }

    private void shareNote() {
        saveNote();
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, getExportString());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_note)));
    }

    public String getExportString() {
        StringBuilder exportString = new StringBuilder();
        if (!note.getTitle().isEmpty()) {
            exportString.append(getString(R.string.action_share_title)).append(": ").append(note.getTitle()).append("\n");
        }
        if (!note.getBody().isEmpty()) {
            exportString.append(getString(R.string.action_share_body)).append(": ").append(note.getBody());
        }
        return exportString.toString().trim();
    }
    @Override
    protected void onResume() {
        super.onResume();
        shouldPersistOnPause = true;
    }

    @Override
    protected void onPause() {
        super.onPause();

//
//        if (!shouldPersistOnPause) {
//            return;
//        }

        if(!isNoteDeleted) {
            updateNoteFromUI();
            if (shouldPersistOnPause && noteViewModel.hasUnsavedChanges()) {
                noteViewModel.persist();
            }
        }
        //noteViewModel.persist();
   }
}
