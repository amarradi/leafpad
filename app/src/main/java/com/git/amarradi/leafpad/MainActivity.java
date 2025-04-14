package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public final static String EXTRA_NOTE_ID = "com.git.amarradi.leafpad";
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String DESIGN_MODE = "system";

    public List<Note> notes;
    public SimpleAdapter adapter;
    public ListView listView;

    private boolean showHidden = false;
    private final Integer PREVIEW = 25;

    List<Map<String, String>> data = new ArrayList<>();


    @SuppressLint("RestrictedApi")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupSharedPreferences();
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        String themes = sharedPreferences.getString(DESIGN_MODE, "");
        changeTheme(themes);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDefaultDisplayHomeAsUpEnabled(true);

        adapter = new SimpleAdapter(this, data,
                R.layout.note_list_item,
                new String[]{"title", "body","date", "time", "category"},
                new int[]{R.id.title_text, R.id.note_preview, R.id.created_at, R.id.time_txt, R.id.category_txt});

        adapter.setViewBinder((view, data, textRepresentation) -> {
            if (view.getId() == R.id.category_txt) {
                if (data instanceof String) {
                    String category = (String) data;
                    Log.d("setViewBinder", "category: "+data.toString());
                    // Setze den Text explizit (da der Default-Binding unterdrückt wird)
                    ((TextView) view).setText(category);

                    // Bestimme, ob der Dunkelmodus aktiv ist
                    int currentNightMode = view.getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    boolean isDarkMode;
                    if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                        isDarkMode = true;
                    } else {
                        isDarkMode = false;
                    }

                    // Finde das übergeordnete MaterialCardView
                    View parent = (View) view.getParent();
                    while (parent != null && !(parent instanceof com.google.android.material.card.MaterialCardView)) {
                        parent = (View) parent.getParent();
                    }
                    if (parent != null) {
                        com.google.android.material.card.MaterialCardView cardView = (com.google.android.material.card.MaterialCardView) parent;
                      //  int highlightColor = ContextCompat.getColor(view.getContext(), R.color.md_theme_light_recipe);

                        // Farben abhängig vom Modus setzen
                        int highlightColor;
                        if (isDarkMode) {
                            highlightColor = ContextCompat.getColor(view.getContext(), R.color.md_theme_dark_recipe);
                        } else {
                            highlightColor = ContextCompat.getColor(view.getContext(), R.color.md_theme_light_recipe);
                        }

                        int iconColor;
                        if (isDarkMode) {
                           // iconColor = ContextCompat.getColor(view.getContext(), android.R.color.white);
                            iconColor = ContextCompat.getColor(view.getContext(), R.color.md_theme_dark_recipe);
                        } else {
                            iconColor = ContextCompat.getColor(view.getContext(), R.color.md_theme_light_recipe);
                        }

                       int defaultStrokeColor = ContextCompat.getColor(view.getContext(), R.color.md_theme_light_primaryContainer);

                        if (!android.text.TextUtils.isEmpty(category)) {
                            // Kategorie vorhanden: Rahmen auf Highlight-Farbe setzen
                            cardView.setStrokeColor(highlightColor);
                            // Zeige das Kategorie-Textfeld
                            view.setVisibility(View.VISIBLE);
                            // Icon sichtbar machen
                            View icon = ((View) view.getParent()).findViewById(R.id.category_icon);
                            if (icon != null) {
                                icon.setVisibility(View.VISIBLE);
                            }
                        } else {
                            // Keine Kategorie: Rahmen zurücksetzen
                            cardView.setStrokeColor(defaultStrokeColor);
                            cardView.invalidate(); // Erzwingt Neuzeichnen
                            // Verstecke das Kategorie-Textfeld
                            view.setVisibility(View.GONE);
                            // Verstecke auch das Icon
                            View icon = ((View) view.getParent()).findViewById(R.id.category_icon);
                            if (icon != null) {
                                icon.setVisibility(View.GONE);
                            }
                        }
                    }
                }
                return true;
            }
            return false;
        });

        //updateDataset();

        listView = findViewById(R.id.note_list_view);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener((adapter, v, position, id) -> {

            // Hole die ID aus der angezeigten Datenliste
            String noteId = data.get(position).get("id");

            // Finde die Notiz in der "notes"-Liste anhand der ID
            Note selectedNote = null;
            for (Note note : notes) {
                if (note.getId().equals(noteId)) {

                    selectedNote = note;
                    break;
                }
            }

            if (selectedNote != null) {
                Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
                intent.putExtra(EXTRA_NOTE_ID, selectedNote.getId());
                // Log.d("MainActivity", "Selected Note: ID=" + selectedNote.getId() + ", Title=" + selectedNote.getTitle());
                startActivity(intent);
            } //else {
              //  Log.e("MainActivity", "Error: Note not found for ID=" + noteId);
            //}
        });

        ExtendedFloatingActionButton extendedFloatingActionButton = findViewById(R.id.fab_action_add);
        extendedFloatingActionButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
            intent.putExtra(EXTRA_NOTE_ID, Note.makeId());
            startActivity(intent);
        });
        listView.setEmptyView(findViewById(R.id.emptyElement));

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
        if("lightmode".equals(themeValue)) {
                return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if("darkmode".equals(themeValue)) {
                return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateListView();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        MenuItem item = menu.findItem(R.id.item_show_hidden);
        if (showHidden) {
            item.setIcon(getDrawable(R.drawable.action_eye_closed));
            item.setTitle(getString(R.string.hide_hidden));
        } else {
            item.setIcon(getDrawable(R.drawable.action_eye_open));
            item.setTitle(getString(R.string.show_hidden));
        }
        //invalidateOptionsMenu();
        return true;
    }

    @SuppressLint({"NonConstantResourceId"})
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.item_show_hidden:
                toggleShowHidden(item);
                return true;
            case R.id.action_open_search:
                Intent searchIntent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(searchIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void toggleShowHidden(MenuItem item) {
        showHidden = !showHidden;
        if (!showHidden) {
            item.setIcon(getDrawable(R.drawable.action_eye_closed));
            item.setTitle(getString(R.string.hide_hidden));
        } else {
            item.setIcon(getDrawable(R.drawable.action_eye_open));
            item.setTitle(getString(R.string.show_hidden));
        }
        updateListView();
        invalidateOptionsMenu();
    }


    public void updateListView() {
        updateDataset();
        ((SimpleAdapter) listView.getAdapter()).notifyDataSetChanged();
    }


    public void updateDataset() {
        //Log.d("MainActivity", "updateDataset() called");
        notes = Leaf.loadAll(this, showHidden);
        data.clear();

        for (Note note : notes) {
            if (showHidden && note.isHide()) {
                Map<String, String> datum = new HashMap<>();
                datum.put("id", note.getId());
                datum.put("title", note.getTitle());
                // Kürze den Notiztext auf maximal 50 Zeichen
                String previewText = note.getBody();
                if (previewText.length() > PREVIEW) {
                    previewText = previewText.substring(0, PREVIEW) + "...";
                }
                datum.put("body", previewText);
                datum.put("date", note.getDate());
                datum.put("time", note.getTime());
                datum.put("category", note.getCategory());
                data.add(datum);
            } else if(!showHidden && !note.isHide()){
                Map<String, String> datum = new HashMap<>();
                datum.put("id", note.getId());
                datum.put("title", note.getTitle());
                // Kürze den Notiztext auf maximal 50 Zeichen
                String previewText = note.getBody();
                if (previewText.length() > PREVIEW) {
                    previewText = previewText.substring(0, PREVIEW) + "...";
                }
                datum.put("body", previewText);
                datum.put("date", note.getDate());
                datum.put("time", note.getTime());
                datum.put("category", note.getCategory());
                data.add(datum);
            }
        }
        // Debugging: Ausgabe der geladenen Daten
        //for (Map<String, String> datum : data) {
        //    Log.d("MainActivity", "Datum: " + datum);  // Hier wird jedes Element der Liste geloggt
        //}

        //Log.d("MainActivity", "Displayed Notes: " + data.size());
    }

}