package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.splashscreen.SplashScreen;
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


        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        adapter = new SimpleAdapter(this, data,
                R.layout.note_list_item,
                new String[]{"title", "date", "time"},
                new int[]{R.id.title_text, R.id.created_at, R.id.time_txt});

        updateDataset();

        listView = findViewById(R.id.note_list_view);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((adapter, v, position, id) -> {
            Intent intent = new Intent(MainActivity.this, NoteEditActivity.class);
            intent.putExtra(EXTRA_NOTE_ID, notes.get(position).getId());
            startActivity(intent);
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
        if (key.equals("theme")) {
            loadThemeFromPreference(sharedPreferences);
        }
    }
    private void loadThemeFromPreference(SharedPreferences sharedPreferences) {
        changeTheme(sharedPreferences.getString(getString(R.string.theme_key), getString(R.string.system_preference_option_value)));
    }

    private void changeTheme(String theme_value) {
        switch (theme_value) {
            case "lightmode": {
                Log.d("theme", "changeTheme: "+theme_value);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(DESIGN_MODE, theme_value);
                editor.apply();
                break;
            }
            case "darkmode": {
                Log.d("theme", "changeTheme: "+theme_value);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(DESIGN_MODE, theme_value);
                editor.apply();
                break;
            }
            case "system": {
                Log.d("theme", "changeTheme: "+theme_value);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(DESIGN_MODE, theme_value);
                editor.apply();
                break;
            }
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.item_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateListView() {
        updateDataset();
        ((SimpleAdapter) listView.getAdapter()).notifyDataSetChanged();
    }


    public void updateDataset() {
        notes = Leaf.loadAll(this);




/* I don't understand the code yet so the code is not deleted yet
        for (Note note : notes) {
            Map<String,String> datum = new HashMap<>();
            datum.put("title", note.getTitle());
            Log.d("title", "title:"+note.getTitle());
            datum.put("body", note.getBody());
            Log.d("body", "body:"+note.getBody());
            datum.put("date", note.getDate());
            Log.d("date", "date:"+note.getDate()+"Datum");
            datum.put("time", note.getTime());
            Log.d("time", "time:"+note.getTime()+"Uhr");
            datum.put("create", note.getCreateDate().stripLeading().stripTrailing());
            Log.d("create", "create:"+note.getCreateDate()+"Datum");


        }*/

       /* for (Note note : notes) {
            if (note.getTitle().isEmpty()) {
                String[] splits = note.getBody().split("");

                StringBuilder stringBuilder = new StringBuilder();
                int substringLength = 0;
                for (int i = 0; i < splits.length; i++) {
                    if (substringLength + splits[i].length() > 25) {
                        break;
                    }
                    substringLength += splits[i].length();
                    stringBuilder.append(splits[i]);
                    if (i + 1 != splits.length) {
                        if (!(substringLength + splits[i + 1].length() > 25)) {
                            stringBuilder.append("");
                        }
                    }
                }
                if (!(note.getBody().length() == stringBuilder.toString().length())) {
                    stringBuilder.append("...");
                }
                note.setTitle(stringBuilder.toString());
            }
            if (note.getDate().isEmpty() || note.getTime().isEmpty()) {
                note.setNotetime();
                note.setNotedate();
            }
        }*/

        data.clear();
        for (Note note : notes) {
            Map<String, String> datum = new HashMap<>();
            datum.put("title", note.getTitle());
            datum.put("body", note.getBody());
            datum.put("date", note.getDate());
            datum.put("time", note.getTime());
            //   datum.put("create", note.getCreateDate());
            data.add(datum);
        }

    }
/* whats that
    public String joinArray(String[] array, char delimiter) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            stringBuilder.append(array[i]);
            if (i < array.length - 1) {
                stringBuilder.append(" ");
            }
        }
        return stringBuilder.toString();
    }
 */
}
