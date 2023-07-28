package com.git.amarradi.leafpad;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;

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
            if (intent.getAction() == "android.intent.action.SEND") {
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
            } else {
                titleEdit.setText(note.getTitle());
                bodyEdit.setText(note.getBody());
                note.setNotedate();
                note.setNotetime();
            }
        } else {
            //Build version is newer or equals than Tiramisu
            if (intent.getAction() == "android.intent.action.SEND") {
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

            } else {
                titleEdit.setText(note.getTitle());
                bodyEdit.setText(note.getBody());
                note.setNotedate();
                note.setNotetime();
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
        Log.v("SS", note.getBody());
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
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, getExportString());
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "Share Note"));
            return true;
        } else if (id == R.id.action_remove) {
            Leaf.remove(this, note);
            note = null;
            Snackbar snackbar = Snackbar.make(findViewById(id),"delete",Snackbar.LENGTH_LONG);
            snackbar.show();
            finish();
            return true;
        } else if (id == R.id.action_copy) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Note", getExportString());
            clipboard.setPrimaryClip(clip);
            Snackbar.make(findViewById(id), "Note copied", Snackbar.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public String getExportString() {
        String exportString = "";

        if (!note.getTitle().isEmpty()) {
            exportString += note.getTitle();

            if (!note.getBody().isEmpty()) {
                exportString += ": " + note.getBody();
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
