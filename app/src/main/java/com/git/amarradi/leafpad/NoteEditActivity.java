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
        setupVisibilitySwitch();
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
        Log.d("NoteEditActivity", "Selected note: " + noteViewModel.getSelectedNote().getValue());
    }

    private void observeNote() {
        noteViewModel.getSelectedNote().observe(this, note -> {
            if (note != null) {
                this.note = note;
                if (!isNewEntry(note)) {
                    Log.d("NoteEditActivity", "observeNote: "+note.getTitle()+"|"+note.getBody());
                    configureUIFromNote(note);
                }
                setupRecipeSwitch(note);
                setupVisibilitySwitch(note.isHide());
            }
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void setupVisibilitySwitch(boolean isHidden) {

        visibleSwitch.setOnCheckedChangeListener(null);

        if (isHidden) {
            visibleSwitch.setChecked(true);
            visibleSwitch.setText(getString(R.string.show_note));
            visibleSwitch.setThumbIconDrawable(getDrawable(R.drawable.action_eye_closed));
        } else {
            visibleSwitch.setChecked(false);
            visibleSwitch.setText(getString(R.string.hide_note));
            visibleSwitch.setThumbIconDrawable(getDrawable(R.drawable.action_eye_open));
        }

        visibleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            noteViewModel.updateNoteVisibility(isChecked);
        });
    }

    private void setupVisibilitySwitch() {
        // immer nur den Listener setzen, aber NICHT configureUIFromNote neu aufrufen
        visibleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // bevor wir ins VM schießen, unbedingt die UI‐Werte übernehmen
            note.setTitle(titleEdit.getText().toString());
            note.setBody(bodyEdit.getText().toString());
            noteViewModel.updateNoteVisibility(isChecked);
            // und dann rein nur die Switch‐Optik anpassen:
            updateVisibilityUI(isChecked);
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void updateVisibilityUI(boolean isHidden) {
        if (isHidden) {
            visibleSwitch.setChecked(true);
            visibleSwitch.setText(R.string.show_note);
            visibleSwitch.setThumbIconDrawable(getDrawable(R.drawable.action_eye_closed));
        } else {
            visibleSwitch.setChecked(false);
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
        recipeSwitch.setText(isRecipe ? R.string.note_is_no_recipe : R.string.note_is_recipe);
        recipeSwitch.setThumbIconDrawable(getDrawable(isRecipe ? R.drawable.togue : R.drawable.togue_strikethrough));

        recipeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d("NoteEditActivity", "Recipe switch toggled: " + isChecked);
            noteViewModel.updateNoteRecipe(isChecked ? cat : "");
        });
    }

    private void configureUIFromNote(Note note) {
        toolbar.setTitle(R.string.action_edit_note);

        titleEdit.setText(note.getTitle());
        bodyEdit.setText(note.getBody());

        if (isNewEntry(note)) {
            setupToolbarSubtitle(R.string.new_note);
        } else {
            setupToolbarSubtitle(note.getTitle());
        }
    }

    private void setupToolbarSubtitle(int stringResId) {
        toolbar.setSubtitle(getString(stringResId));
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

    private boolean isNewEntry(Note note) {
        return note.getTitle() == null ||
                note.getTitle().isEmpty() ||
                note.getBody() == null ||
                note.getBody().isEmpty();
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
        Log.d("NoteEditActivity", "removeNote entered");
        if (note != null) {
            //Leaf.remove(this, note);
            noteViewModel.deleteNote(getApplication(), note);
            note = null;

            isNoteDeleted = true; // <--- Flag setzen!
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

        saveNote();
       // note.setTitle(titleEdit.getText().toString());
       // note.setBody(bodyEdit.getText().toString());

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
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("NoteEditActivity", "onPause entered");

        if (isNoteDeleted) return;
        // alle UI-Felder in das Note-Objekt übernehmen:
        Note n = noteViewModel.getSelectedNote().getValue();
        if (n != null) {
            n.setTitle(titleEdit.getText().toString());
            n.setBody (bodyEdit .getText().toString());
        }
        // jetzt ViewModel persist() aufrufen – löscht oder speichert
        noteViewModel.persist();
    }
}
