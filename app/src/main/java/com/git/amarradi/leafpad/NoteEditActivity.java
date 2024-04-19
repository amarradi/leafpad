package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

public class NoteEditActivity extends AppCompatActivity {

    private EditText titleEdit;
    private EditText bodyEdit;
    private Note note;

    private MaterialToolbar toolbar;

    private Resources resources;

    private LeafStore leafStore;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        leafStore = new Leaf(this);

        setContentView(R.layout.activity_note_edit);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (Objects.equals(getIntent().getAction(), "android.intent.action.VIEW")) {
            note = leafStore.findById(Note.makeId());
        }
        resources = getResources();
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();

        titleEdit = findViewById(R.id.title_edit);

        bodyEdit = findViewById(R.id.body_edit);

        //TextView text = findViewById(R.id.created_at);

        String noteId = intent.getStringExtra(MainActivity.EXTRA_NOTE_ID);

        note = leafStore.findById(noteId);
        if (isNewEntry(note, intent)) {
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

    private boolean isNewEntry(Note note, Intent intent) {
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
            leafStore.remove(note);
            return;
        }
        leafStore.save(note);
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_share_note:
                shareNote();
                return true;
            case R.id.action_remove:
                MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(NoteEditActivity.this);
                materialAlertDialogBuilder.setIcon(R.drawable.dialog_delete).setTitle(R.string.remove_dialog_title).setMessage(R.string.remove_dailog_message).setPositiveButton(R.string.action_remove, (dialogInterface, i) -> {
                removeNote();
                dialogInterface.dismiss();
                }).setNegativeButton(R.string.remove_dialog_abort, (dialogInterface, i) -> dialogInterface.dismiss());
                materialAlertDialogBuilder.create();
                materialAlertDialogBuilder.show();
                return true;
            case R.id.action_save:
                note.setTitle(titleEdit.getText().toString());
                note.setBody(bodyEdit.getText().toString());
                if (note.getBody().isEmpty() && note.getTitle().isEmpty()) {
                    //don't save empty notes
                    leafStore.remove(note);
                } else {
                    leafStore.save (note);
                    toolbar.setSubtitle(note.getTitle());
                    Toast.makeText(this, note.getTitle() + " " + resources.getString(R.string.action_note_saved), Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void removeNote() {
        leafStore.remove(note);
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
        String exportString = "";
        if (!note.getTitle().isEmpty()) {
            exportString += getString(R.string.action_share_title) + ": " + note.getTitle() + "\n";
            if (!note.getBody().isEmpty()) {
                exportString += getString(R.string.action_share_body) + ": " + note.getBody();
            }
        } else if (!note.getBody().isEmpty()) {
            exportString += note.getBody();
        }
        return exportString;
    }

}
