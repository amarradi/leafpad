package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class NoteEditActivity extends AppCompatActivity {

    private EditText titleEdit;
    private EditText bodyEdit;
    private Note note;
    private MaterialToolbar toolbar;
    private Resources resources;
    private MaterialSwitch visibleSwitch;

    private MaterialSwitch recipeSwitch;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_note_edit);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Java
        TextInputLayout titleLayout = findViewById(R.id.default_text_input_layout);
        TextInputLayout bodyLayout = findViewById(R.id.body_text_input_layout);
        titleLayout.setHintEnabled(false);
        bodyLayout.setHintEnabled(false);

        Intent intent = getIntent();
        String noteId = intent.getStringExtra(MainActivity.EXTRA_NOTE_ID);
        if (Objects.equals(getIntent().getAction(), "android.intent.action.VIEW")) {
            note = Leaf.load(this, Note.makeId());
        } else {
            note = Leaf.load(this, noteId);
        }

        resources = getResources();
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        titleEdit = findViewById(R.id.title_edit);
        bodyEdit = findViewById(R.id.body_edit);
        visibleSwitch = findViewById(R.id.visible_switch);

        recipeSwitch = findViewById(R.id.recipe_switch);

        note = Leaf.load(this, noteId);

        toggleView();
        toggleRecipe();

        if (isNewEntry(note, intent)) {
            note = Leaf.load(this, Note.makeId());
            note.setHide(false);
            toggleView();
            toolbar.setSubtitle(R.string.new_note);
            String action = intent.getAction();
            String type = intent.getType();
            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if (type.startsWith("text/plain")) {
                    String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                    titleEdit.setText(R.string.imported);
                    bodyEdit.setText(sharedText);
                    note.setNotedate();
                    note.setNotetime();
                }
            }
            note.setNotedate();
            note.setNotetime();

        } else {
            //existing note
            toolbar.setTitle(R.string.action_fab_note);
            toolbar.setSubtitle(note.getTitle());
            titleEdit.setText(note.getTitle());
            bodyEdit.setText(note.getBody());
        }
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
            //recipeSwitch.setText(R.string.note_is_no_recipe);
            recipeSwitch.setChecked(false);

        }
        recipeSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                //recipeSwitch.setText(R.string.note_is_recipe);
                recipeSwitch.setText(R.string.note_is_no_recipe);
                recipeSwitch.setThumbIconDrawable(getDrawable(R.drawable.togue));
                note.setCategory(resources.getStringArray(R.array.category)[0]);
            } else {
                //recipeSwitch.setText(R.string.note_is_no_recipe);
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

    private boolean isNewEntry(Note note, Intent intent) {
        Log.d("NoteEditActivity", "isNewEntry = "+ note.getDate().isEmpty() + " "+ note.getTime().isEmpty());
        return note.getDate().isEmpty() || note.getTime().isEmpty() || "android.intent.action.SEND".equals(intent.getAction());

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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note_edit, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {
            case R.id.action_share_note:
                shareNote();
                return true;
            case R.id.action_remove:
                DialogHelper.showDeleteSingleNoteDialog(NoteEditActivity.this, this::removeNote);
               /** MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(NoteEditActivity.this);
                materialAlertDialogBuilder
                        .setIcon(R.drawable.dialog_delete)
                        .setTitle(R.string.remove_dialog_title)
                        .setMessage(R.string.remove_dailog_message)
                        .setPositiveButton(R.string.action_remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        removeNote();
                        dialogInterface.dismiss();
                    }
                }).setNegativeButton(R.string.remove_dialog_abort, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                materialAlertDialogBuilder.create();
                materialAlertDialogBuilder.show(); **/
                return true;
            case R.id.action_save:
                note.setHide(visibleSwitch.isChecked());
                note.setTitle(titleEdit.getText().toString());
                note.setBody(bodyEdit.getText().toString());

                if (note.getBody().isEmpty() && note.getTitle().isEmpty()) {
                    Leaf.remove(this, note);
                } else {
                    Leaf.set(this, note);
                    toolbar.setSubtitle(note.getTitle());
                  //  Toast.makeText(this, note.getTitle() + " " + resources.getString(R.string.action_note_saved), Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void removeNote() {
        Leaf.remove(this, note);
        note = null;
        finish();
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
}
