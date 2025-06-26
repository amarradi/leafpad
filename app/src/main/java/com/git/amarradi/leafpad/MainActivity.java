package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.git.amarradi.leafpad.adapter.NoteAdapter;
import com.git.amarradi.leafpad.model.Note;
import com.git.amarradi.leafpad.viewmodel.NoteViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NoteAdapter noteAdapter;
    private NoteViewModel noteViewModel;

    private ImageView emptyElement;

    @SuppressLint("RestrictedApi")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.note_list_view);
        emptyElement = findViewById(R.id.emptyElement);

        noteViewModel = new ViewModelProvider(
                this,
                new ViewModelProvider.AndroidViewModelFactory(getApplication())
        ).get(NoteViewModel.class);

        noteAdapter = new NoteAdapter(this, new NoteAdapter.NoteClickListener() {
            @Override
            public void onNoteClicked(Note note) {
                noteViewModel.selectNote(note);
                Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
                intent.putExtra(Leafpad.EXTRA_NOTE_ID, note.getId());
                startActivity(intent);
            }

            @Override
            public void onNoteIconClicked(Note note, View anchor) {
                // Deine Logik für Menü o.ä.
            }
        });

        // Listener für den ReleaseNote-Close-Button
        noteAdapter.setOnReleaseNoteCloseListener(new NoteAdapter.OnReleaseNoteCloseListener() {
            @Override
            public void onReleaseNoteClose() {
                // Update: Merke im ViewModel oder Leafpad, dass ReleaseNote geschlossen ist
                Leafpad.getInstance().setReleaseNoteDismissed(true);
                noteViewModel.reloadReleaseNoteState();
                // UI sofort updaten: Header entfernen, ggf. EmptyImage einblenden
                updateReleaseNoteHeader();
                updateEmptyImage();
            }
        });

        recyclerView.setAdapter(noteAdapter);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDefaultDisplayHomeAsUpEnabled(true);

        ExtendedFloatingActionButton fab = findViewById(R.id.fab_action_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
                intent.putExtra(Leafpad.EXTRA_NOTE_ID, Note.makeId());
                startActivity(intent);
            }
        });

        observeNotes();
        updateReleaseNoteHeader();
    }

    private void observeNotes() {
        noteViewModel.getNotes().observe(this, notes -> {
            noteAdapter.setNotes(notes);
            updateReleaseNoteHeader();
            updateEmptyImage();
        });
        noteViewModel.getShowHidden().observe(this, showHidden -> {
            // Wenn dein Adapter das unterstützt:
            // noteAdapter.setShowOnlyHidden(showHidden);
            noteViewModel.loadNotes(); // oder andere Logik für hidden
        });
    }

    private void updateReleaseNoteHeader() {
        boolean shouldShowRelease = noteViewModel.updateShowReleaseNoteState(get);
        if (shouldShowRelease) {
            // ReleaseNotes laden, z. B. aus dem ReleaseNoteHelper oder ViewModel
            NoteViewModel.ReleaseNoteData data = noteViewModel.getReleaseNoteData();
            noteAdapter.setReleaseNoteHeader(
                    data.title,
                    data.content,
                    data.date,
                    data.time,
                    true
            );
        } else {
            noteAdapter.setReleaseNoteHeader("", "", "", "", false);
        }
    }

    private void updateEmptyImage() {
        boolean isNoteListEmpty = noteAdapter.getItemCount() == 0 ||
                (noteAdapter.getItemCount() == 1 && noteAdapter.getItemViewType(0) == NoteAdapter.VIEW_TYPE_HEADER);
        boolean showReleaseHeader = noteViewModel.shouldShowReleaseNote();

        if (!showReleaseHeader && isNoteListEmpty) {
            emptyElement.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
        } else {
            emptyElement.setVisibility(View.INVISIBLE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
