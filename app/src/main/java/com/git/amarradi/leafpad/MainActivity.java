package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.git.amarradi.leafpad.adapter.NoteAdapter;
import com.git.amarradi.leafpad.helper.DialogHelper;
import com.git.amarradi.leafpad.helper.LayoutModeHelper;
import com.git.amarradi.leafpad.model.Note;
import com.git.amarradi.leafpad.viewmodel.NoteViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.Objects;


public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public RecyclerView recyclerView;
    public NoteAdapter noteAdapter;
    private NoteViewModel noteViewModel;


    @SuppressLint("RestrictedApi")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        noteViewModel = new ViewModelProvider(
                this,
                new ViewModelProvider.AndroidViewModelFactory(getApplication())
        ).get(NoteViewModel.class);
        boolean savedShowHidden = Leafpad.getInstance().getSavedShowHidden();
        noteViewModel.setShowHidden(savedShowHidden);

        noteViewModel.loadNotes();
        noteViewModel.getNotes().observe(this, notes -> {
            //Log.d("MainActivity", "Observed notes: " + notes.size());
            noteAdapter.updateNotes(notes);
            recyclerView.post(()->recyclerView.scrollToPosition(0));

            ImageView emptyElement = findViewById(R.id.emptyElement);
            if (noteAdapter.isFilteredListEmpty()) {
                emptyElement.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.INVISIBLE);
            } else {
                emptyElement.setVisibility(View.INVISIBLE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });

        noteViewModel.getShowHidden().observe(this, showHidden -> {
            noteAdapter.setShowOnlyHidden(showHidden);
        });

        setupSharedPreferences();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDefaultDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.note_list_view);

        noteAdapter = new NoteAdapter(this, new ArrayList<>(), new NoteClickListener() {
            @Override
            public void onNoteClicked(Note note) {
                noteViewModel.selectNote(note);
                Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
                intent.putExtra(Leafpad.EXTRA_NOTE_ID, note.getId());
                startActivity(intent);
            }

            @Override
            public void onNoteIconClicked(Note note, View anchor) {
                showPopupMenu(note, anchor);
            }
        });

        recyclerView.setAdapter(noteAdapter);

        Leafpad.getInstance().applyCurrentLayoutMode(recyclerView, noteAdapter);

        ExtendedFloatingActionButton fab = findViewById(R.id.fab_action_add);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
            intent.putExtra(Leafpad.EXTRA_NOTE_ID, Note.makeId());
            startActivity(intent);
        });
    }

    private void setupSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    private void showPopupMenu(Note note, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_popup, popup.getMenu());

        LayoutModeHelper.forcePopupMenuIcons(popup);
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_share_note) {
                shareNote(note);
                return true;
            } else if (id == R.id.action_remove) {
                DialogHelper.showDeleteSingleNoteDialog(this, () -> noteViewModel.deleteNote(this,note));
                return true;
            }
            return false;
        });

        popup.show();
    }
    private void shareNote(Note note) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String content = note.getTitle() + "\n\n" + note.getBody();
        shareIntent.putExtra(Intent.EXTRA_TEXT, content);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_note)));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("theme".equals(key)) {
            String newValue = sharedPreferences.getString("theme", "system");
            Leafpad.getInstance().saveTheme(newValue);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Leafpad.getInstance().applyCurrentLayoutMode(recyclerView, noteAdapter);
        noteViewModel.loadNotes();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public boolean onCreateOptionsMenu(@NonNull android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        android.view.MenuItem item = menu.findItem(R.id.item_show_hidden);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String savedLayout = sharedPreferences.getString(Leafpad.PREF_LAYOUT_MODE, "list");
        Boolean showHidden = noteViewModel.getShowHidden().getValue();
        if (showHidden == null) {
            showHidden = false;
        }
        if (showHidden) {
            item.setIcon(getDrawable(R.drawable.eye_invisible));
            item.setTitle(getString(R.string.hide_hidden));
        } else {
            item.setIcon(getDrawable(R.drawable.eye_visible));
            item.setTitle(getString(R.string.show_hidden));
        }
        MenuItem layoutItem = menu.findItem(R.id.item_toggle_layout);
        if ("grid".equals(savedLayout)) {
            layoutItem.setIcon(R.drawable.listview);
        } else {
            layoutItem.setIcon(R.drawable.gridview);
        }
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.item_show_hidden:
                toggleShowHidden(item);
                return true;
            case R.id.item_toggle_layout:
                Leafpad.getInstance().toggleLayoutMode(recyclerView, noteAdapter);
                invalidateOptionsMenu();
                return true;
            case R.id.action_search:
                Intent searchIntent = new Intent(this, SearchActivity.class);
                startActivity(searchIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void toggleShowHidden(MenuItem item) {

        Boolean current = noteViewModel.getShowHidden().getValue();
        if (current == null) {
            current = false;
        }

        boolean newValue = !current;
        noteViewModel.setShowHidden(newValue);

        if (newValue) {
            item.setIcon(getDrawable(R.drawable.eye_invisible));
            item.setTitle(getString(R.string.hide_hidden));
        } else {
            item.setIcon(getDrawable(R.drawable.eye_visible));
            item.setTitle(getString(R.string.show_hidden));
        }
        Leafpad.getInstance().saveShowHidden(newValue);
    }
    public interface NoteClickListener {
        void onNoteClicked(Note note);
        void onNoteIconClicked(Note note, View anchor);
    }
}
