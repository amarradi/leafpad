package com.git.amarradi.leafpad;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Objects;

public class NoteEditActivity extends AppCompatActivity {

    private EditText titleEdit;
    private EditText bodyEdit;

    private Note note;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_edit);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();

        titleEdit = findViewById(R.id.title_edit);
        bodyEdit = findViewById(R.id.body_edit);

        String noteId = intent.getStringExtra(MainActivity.EXTRA_NOTE_ID);
        note = Leaf.load(this, noteId);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
           //Build version is older than Tiramisu
            if ("android.intent.action.SEND".equals(intent.getAction())) {
                String action = intent.getAction();
                String type = intent.getType();
                if (Intent.ACTION_SEND.equals(action) && type != null) {
                    if (type.startsWith("text/plain")) {
                        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                        titleEdit.setText(R.string.imported);
                        bodyEdit.setText(sharedText);
                        note.setNotedate();
                        note.setNotetime();
                        if(note.getCreateDate().isEmpty()) {
                            note.setCreateDate();
                        }
                    }
                }
            } else {
                titleEdit.setText(note.getTitle());
                bodyEdit.setText(note.getBody());
                note.setNotedate();
                note.setNotetime();
                if(note.getCreateDate().isEmpty()) {
                    note.setCreateDate();
                }
            }
        } else {
            //Build version is newer or equals than Tiramisu
            if ("android.intent.action.SEND".equals(intent.getAction())) {
                String action = intent.getAction();
                String type = intent.getType();
                if (Intent.ACTION_SEND.equals(action) && type != null) {
                    if (type.startsWith("text/plain")) {
                        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                        titleEdit.setText(R.string.imported);
                        bodyEdit.setText(sharedText);
                        note.setNotedate();
                        note.setNotetime();
                        if(note.getCreateDate().isEmpty()) {
                            note.setCreateDate();
                        }
                    }
                }

            } else {
                titleEdit.setText(note.getTitle());
                bodyEdit.setText(note.getBody());
                note.setNotedate();
                note.setNotetime();
                if(note.getCreateDate().isEmpty()) {
                    note.setCreateDate();
                }
            }
        }
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
            Leaf.remove(this, note);
            return;
        }
        Leaf.set(this, note);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_share_note) {
            shareNote();
            return true;
        } else if (id == R.id.action_remove) {
            removeNote();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void removeNote() {
        Leaf.remove(this, note);
        note = null;
        finish();
    }

    private void shareNote() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, getExportString());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share Note"));
    }

    public String getExportString() {
        String exportString = "";
        if (!note.getTitle().isEmpty()) {
            exportString += getString(R.string.action_share_title) + ": " + note.getTitle()+"\n";
            if (!note.getBody().isEmpty()) {
                exportString += getString(R.string.action_share_body) + ": " + note.getBody();
            }
        } else if (!note.getBody().isEmpty()) {
            exportString += note.getBody();
        }
        return exportString;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }
}
