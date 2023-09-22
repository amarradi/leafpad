package com.git.amarradi.leafpad;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class NoteReadActivity extends AppCompatActivity {

    public final static String EXTRA_NOTE_ID = "com.git.amarradi.leafpad";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        TextView title = findViewById(R.id.readTitle);
        TextView content = findViewById(R.id.readNote);

        String noteId = intent.getStringExtra(NoteReadActivity.EXTRA_NOTE_ID);
        Note note = Leaf.load(this, noteId);

        title.setText(note.getTitle());
        content.setKeyListener(null);
        content.setText(note.getBody());

    }
}