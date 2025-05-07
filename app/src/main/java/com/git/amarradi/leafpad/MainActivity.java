package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

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
    private static final String PREF_LAYOUT_MODE = "layout_mode"; // "list" oder "grid"


    public RecyclerView recyclerView;
    public NoteAdapter noteAdapter;
    public List<Note> notes = new ArrayList<>();
    private boolean showHidden = false;

    private RecyclerView.ItemDecoration gridSpacingDecoration;
    private RecyclerView.ItemDecoration listSpacingDecoration;


    private boolean isListView = true;

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
        updateDataset();
        noteAdapter = new NoteAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(noteAdapter);

        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        String savedLayout = prefs.getString(PREF_LAYOUT_MODE, "list");
        isListView = savedLayout.equals("list");

        if (isListView) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        }

        ExtendedFloatingActionButton fab = findViewById(R.id.fab_action_add);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
            intent.putExtra(EXTRA_NOTE_ID, Note.makeId());
            noteEditLauncher.launch(intent);
        });
    }
    // In MainActivity:
    private final ActivityResultLauncher<Intent> noteEditLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    updateListView();
                }
            });


    private void applyGridSpacingIfNeeded() {
        if (gridSpacingDecoration == null) {
            int spacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing); // z.B. 16dp
            gridSpacingDecoration = new GridSpacingItemDecoration(2, spacing, true);
            recyclerView.addItemDecoration(gridSpacingDecoration);
        }
    }


    private void setupSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    private void toggleLayoutManager() {
        isListView = !isListView;
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (isListView)  {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            editor.putString(PREF_LAYOUT_MODE, "list");
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            editor.putString(PREF_LAYOUT_MODE, "grid");
        }

        editor.apply();
        invalidateOptionsMenu(); // ← Menü neu zeichnen lassen
        applyLayoutMode(isListView);
        invalidateOptionsMenu();
    }



    private void applyLayoutMode(boolean isList) {
        //noteAdapter.setLayoutMode(isList);
        noteAdapter.notifyDataSetChanged();

        if (isList) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            // … List-Dekoration entfernen …
        } else {
            // 1) StaggeredManager statt GridManager
            recyclerView.setLayoutManager(
                    new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            );
            // 2) passende Masonry-Abstände
            if (gridSpacingDecoration != null) {
                recyclerView.removeItemDecoration(gridSpacingDecoration);
            }
            int vert = getResources().getDimensionPixelSize(R.dimen.masonry_vertical_spacing);
            int horz = getResources().getDimensionPixelSize(R.dimen.masonry_horizontal_spacing);
            gridSpacingDecoration = new MasonrySpacingDecoration(vert, horz);
            recyclerView.addItemDecoration(gridSpacingDecoration);
        }
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
        if (notes == null) {
            notes = new ArrayList<>();
        }

        ImageView emptyElement = findViewById(R.id.emptyElement);
        if (noteAdapter.isFilteredListEmpty()) {
            emptyElement.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
        } else {
            emptyElement.setVisibility(View.INVISIBLE);
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
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        String savedLayout = prefs.getString(PREF_LAYOUT_MODE, "list");

        if (showHidden) {
            item.setIcon(ContextCompat.getDrawable(this, R.drawable.action_eye_closed));
            item.setTitle(getString(R.string.hide_hidden));
        } else {
            item.setIcon(ContextCompat.getDrawable(this,R.drawable.action_eye_open));
            item.setTitle(getString(R.string.show_hidden));
        }

        MenuItem layoutItem = menu.findItem(R.id.item_toggle_layout);
        if ("grid".equals(savedLayout)) {
            layoutItem.setIcon(ContextCompat.getDrawable(this,R.drawable.action_gridview_off));
        } else {
            layoutItem.setIcon(ContextCompat.getDrawable(this, R.drawable.action_gridview_on));
        }
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        return switch (item.getItemId()) {
            case R.id.item_settings -> {
                startActivity(new Intent(this, SettingsActivity.class));
                yield true;
            }
            case R.id.item_show_hidden -> {
                toggleShowHidden(item);
                yield true;
            }
            case R.id.item_toggle_layout -> {
                toggleLayoutManager();
                yield true;
            }
            default -> super.onOptionsItemSelected(item);
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            updateListView();
        }
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private void toggleShowHidden(MenuItem item) {
        showHidden = !showHidden;

        if (showHidden) {
            item.setIcon(ContextCompat.getDrawable(this, R.drawable.action_eye_closed));
            item.setTitle(getString(R.string.hide_hidden));
        } else {
            item.setIcon(ContextCompat.getDrawable(this, R.drawable.action_eye_open));
            item.setTitle(getString(R.string.show_hidden));
        }
        noteAdapter.setShowOnlyHidden(showHidden);
        recyclerView.post(this::updateListView);
        invalidateOptionsMenu();
    }
}
