package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.git.amarradi.leafpad.helper.DialogHelper;
import com.git.amarradi.leafpad.helper.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class NoteEditActivity extends AppCompatActivity {

    private EditText titleEdit;
    private EditText bodyEdit;
    private Note note;
    private NoteViewModel noteViewModel;
    private MaterialToolbar toolbar;
    private Resources resources;
    private MaterialSwitch visibleSwitch;

    private MaterialSwitch recipeSwitch;

    private boolean sharedJustNow = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_note_edit);

        Intent intent = getIntent();

        if (Intent.ACTION_SEND.equals(intent.getAction())&&"text/plain".equals(intent.getType())) {
            setupNewNoteFromIntent(intent);
        } else {

            // ViewModel und Observer
            noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
            noteViewModel.getSelectedNote().observe(this, note -> {
                if (note != null) {
                    this.note = note;
                    configureUIForNote();
                }
            });
        }

        setupToolbar();
        initViews();

        loadNote(intent);
        resources = getResources();

        configureUIForNote();

    }

    private void configureUIForNote(){
        if (note != null) {
            if(isNewEntry(note)){
                setupNewNoteFromIntent(getIntent());
            } else {
                setupExistingNote();
            }
            toggleView();
            toggleRecipe();
        }
    }


    private void setupExistingNote() {
        toolbar.setTitle(R.string.action_fab_note);
        toolbar.setSubtitle(note.getTitle());
        titleEdit.setText(note.getTitle());
        bodyEdit.setText(note.getBody());
    }

    private void setupNewNoteFromIntent(Intent intent) {
        note = Leaf.load(this, Note.makeId());
        note.setHide(false);
        note.setNotedate();
        note.setNotetime();

        noteViewModel.selectNote(note);

        toolbar.setSubtitle(R.string.new_note);
        titleEdit.setText("");
        bodyEdit.setText("");

        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())){
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            titleEdit.setText(R.string.imported);
            bodyEdit.setText(sharedText);
        }

    }

    private void loadNote(Intent intent) {
        String noteId = intent.getStringExtra(MainActivity.EXTRA_NOTE_ID);
        if (noteId != null) {
            noteViewModel.selectNote(Leaf.load(this, noteId)); // Setze die Notiz im ViewModel
        } else {
            noteViewModel.selectNote(Leaf.load(this, Note.makeId()));
        }
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

    @SuppressLint("UseCompatLoadingForDrawables")
    private void toggleRecipe() {
        recipeSwitch.setOnCheckedChangeListener(null);
        if(note.getCategory().equals(resources.getStringArray(R.array.category)[0])){
            recipeSwitch.setThumbIconDrawable(getDrawable(R.drawable.togue));
            Log.d("toggleCheckBox", "toggleCheckBox: "+note.getCategory()+" "+recipeSwitch.isChecked());
            recipeSwitch.setText(R.string.note_is_no_recipe);
            recipeSwitch.setChecked(true);
        } else {
            recipeSwitch.setThumbIconDrawable(getDrawable(R.drawable.togue_strikethrough));
            recipeSwitch.setText(R.string.note_is_recipe);
            recipeSwitch.setChecked(false);

        }
        recipeSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                recipeSwitch.setText(R.string.note_is_no_recipe);
                recipeSwitch.setThumbIconDrawable(getDrawable(R.drawable.togue));
                note.setCategory(resources.getStringArray(R.array.category)[0]);
            } else {
                recipeSwitch.setText(R.string.note_is_recipe);
                recipeSwitch.setThumbIconDrawable(getDrawable(R.drawable.togue_strikethrough));
                note.setCategory("");
            }
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void toggleView() {
        visibleSwitch.setOnCheckedChangeListener(null);
        if(note.isHide()) {
            visibleSwitch.setThumbIconDrawable(getDrawable(R.drawable.action_eye_closed));
            visibleSwitch.setText(getString(R.string.show_note));
            visibleSwitch.setChecked(true);
        } else {
            visibleSwitch.setThumbIconDrawable(getDrawable(R.drawable.action_eye_open));
            visibleSwitch.setText(getString(R.string.hide_note));
            visibleSwitch.setChecked(false);
        }
        Log.d("NoteEditActivity", "Note visibility toggled: Hide = " + note.isHide());
        visibleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            note.setHide(isChecked);
            if (isChecked) {
                visibleSwitch.setText(getString(R.string.show_note));
                visibleSwitch.setThumbIconDrawable(getDrawable(R.drawable.action_eye_closed));
            } else {
                visibleSwitch.setText(getString(R.string.hide_note));
                visibleSwitch.setThumbIconDrawable(getDrawable(R.drawable.action_eye_open));
            }
        });

    }

    private boolean isNewEntry(Note note) {
        return note.getDate().isEmpty() || note.getTime().isEmpty();
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (note == null) {
            return;
        }
        note.setTitle(titleEdit.getText().toString());
        note.setBody(bodyEdit.getText().toString());

        if (note.getBody().isEmpty() && note.getTitle().isEmpty()) {
            //don't save empty notes
            Leaf.remove(this, note);
            note = null;
            finish();
        } else {
            Leaf.set(this, note);
        }

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

    private void saveNote() {
        if (note == null) {
            return;
        }
        updateNoteFromUI();

        if (note.getBody().isEmpty() && note.getTitle().isEmpty()) {
            Leaf.remove(this, note);
        } else {
            Leaf.set(this, note);
            noteViewModel.saveNote(getApplication(), note);
            //noteViewModel.selectNote(note); // Die Notiz im ViewModel aktualisieren
            toolbar.setSubtitle(note.getTitle());
        }
    }


    private void updateNoteFromUI() {
        note.setHide(visibleSwitch.isChecked());
        note.setTitle(titleEdit.getText().toString());
        note.setBody(bodyEdit.getText().toString());
    }

    private void removeNote() {
        if (note != null) {
            //Leaf.remove(this, note);
            noteViewModel.deleteNote(getApplication(), note);
            note = null;


        }
        setResult(RESULT_OK);
        finish();
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    private void shareNote() {
        if (note.getBody().isEmpty() && note.getTitle().isEmpty()) {
            Toast.makeText(this, getResources().getString(R.string.note_will_be_saved_first), Toast.LENGTH_SHORT).show();
        }
        note.setTitle(titleEdit.getText().toString());
        note.setBody(bodyEdit.getText().toString());
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, getExportString());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_note)));
        sharedJustNow = true;
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

        if (sharedJustNow) {
            sharedJustNow = false;
            View rootView = findViewById(R.id.all);
            NotificationHelper.showSnackbar(rootView, getString(R.string.note_shared),Snackbar.LENGTH_SHORT, findViewById(R.id.snackbar_anchor));
        }
    }

}
