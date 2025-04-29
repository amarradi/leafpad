package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.git.amarradi.leafpad.adapter.NoteAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String EXTRA_NOTE_ID = "com.git.amarradi.leafpad";
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String DESIGN_MODE = "system";

    public RecyclerView recyclerView;
    public NoteAdapter noteAdapter;
    public List<Note> notes = new ArrayList<>();
    private boolean showHidden = false;

    @SuppressLint("RestrictedApi")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        String themes = sharedPreferences.getString(DESIGN_MODE, "");
        changeTheme(themes);

        setupSharedPreferences();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDefaultDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.note_list_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        noteAdapter = new NoteAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(noteAdapter);

        ExtendedFloatingActionButton fab = findViewById(R.id.fab_action_add);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
            intent.putExtra(EXTRA_NOTE_ID, Note.makeId());
            startActivity(intent);
        });
    }

    private void setupSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("theme".equals(key)) {
            loadThemeFromPreference(sharedPreferences);
        }
    }

    private void loadThemeFromPreference(SharedPreferences sharedPreferences) {
        changeTheme(sharedPreferences.getString(getString(R.string.theme_key), getString(R.string.system_preference_option_value)));
    }

    private void changeTheme(String themeValue) {
        AppCompatDelegate.setDefaultNightMode(toNightMode(themeValue));
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(DESIGN_MODE, themeValue);
        editor.apply();
    }

    private int toNightMode(String themeValue) {
        if ("lightmode".equals(themeValue)) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if ("darkmode".equals(themeValue)) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateListView();
    }

    public void updateListView() {
        updateDataset();
        noteAdapter.updateNotes(notes);

        ImageView emptyElement = findViewById(R.id.emptyElement);
        if (notes.isEmpty()) {
            emptyElement.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyElement.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    public void updateDataset() {
        notes = Leaf.loadAll(this, showHidden);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public boolean onCreateOptionsMenu(@NonNull android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        android.view.MenuItem item = menu.findItem(R.id.item_show_hidden);
        if (showHidden) {
            item.setIcon(getDrawable(R.drawable.action_eye_closed));
            item.setTitle(getString(R.string.hide_hidden));
        } else {
            item.setIcon(getDrawable(R.drawable.action_eye_open));
            item.setTitle(getString(R.string.show_hidden));
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
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void toggleShowHidden(android.view.MenuItem item) {
        showHidden = !showHidden;
        if (showHidden) {
            item.setIcon(getDrawable(R.drawable.action_eye_closed));
            item.setTitle(getString(R.string.hide_hidden));
        } else {
            item.setIcon(getDrawable(R.drawable.action_eye_open));
            item.setTitle(getString(R.string.show_hidden));
        }
        updateListView();
        invalidateOptionsMenu();
    }
}
