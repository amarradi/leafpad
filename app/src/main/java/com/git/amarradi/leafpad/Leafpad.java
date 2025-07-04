package com.git.amarradi.leafpad;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.git.amarradi.leafpad.adapter.NoteAdapter;
import com.git.amarradi.leafpad.helper.LayoutModeHelper;

import java.io.IOException;
import java.util.Objects;

public class Leafpad extends Application {

    public static final String SHARED_PREFS = "shared_prefs";
    public static final String PREF_SHOW_HIDDEN = "pref_show_hidden";
    public static final String DESIGN_MODE = "design_mode";
    public static final String OLD_DESIGN_MODE = "system"; // Alter Schlüssel
    public static final String PREF_LAYOUT_MODE = "layout_mode";// "list" oder "grid"
    public static final String EXTRA_NOTE_ID = "com.git.amarradi.leafpad.extra.NOTE_ID";
    public static final String PREF_NOTIFY_ON_CHANGE = "change";
    public static final String KEY_RELEASE_NOTE_CLOSED = "release_note_closed";
    public static final String CURRENT_LEAFPAD_VERSION_CODE = "current_leafpad_version_code";

    private static Leafpad instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        migrateOldDesignMode();
        applyTheme();
        saveShowHidden(false);

    }

    public static Leafpad getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Leafpad not initialized yet!");
        }
        return instance;
    }

    public static boolean isReleaseNoteClosed(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(KEY_RELEASE_NOTE_CLOSED, false);
    }
    public static void setReleaseNoteClosed(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(KEY_RELEASE_NOTE_CLOSED, true).apply();
    }
    public static void resetReleaseNoteClosed(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(KEY_RELEASE_NOTE_CLOSED, false).apply();
    }
    public static void setCurrentLeafpadVersionCode(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putInt(CURRENT_LEAFPAD_VERSION_CODE,Leafpad.getCurrentVersionCode(context)).apply();
    }
    public static int getCurrentLeafpadVersionCode(Context context) {
        int versionCode;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        versionCode = sharedPreferences.getInt(CURRENT_LEAFPAD_VERSION_CODE,0);
        return versionCode;
    }
    public static boolean isChangeNotificationEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_NOTIFY_ON_CHANGE, false);
    }

    public static void setChangeNotificationEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(PREF_NOTIFY_ON_CHANGE, enabled).apply();
    }

    public static SharedPreferences getPrefs() {
        return getInstance().getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
    }

    public boolean isListLayout() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String layout = sharedPreferences.getString(PREF_LAYOUT_MODE, "list");
        return "list".equals(layout);
    }

    // Speichert den gewünschten Layout-Modus
    public void saveLayoutMode(boolean isList) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (isList) {
            editor.putString(PREF_LAYOUT_MODE, "list");
        } else {
            editor.putString(PREF_LAYOUT_MODE, "grid");
        }
        editor.apply();
    }


    public void toggleLayoutMode(RecyclerView recyclerView, NoteAdapter adapter) {
        boolean isList = !LayoutModeHelper.isListMode(this);
        LayoutModeHelper.saveMode(this, isList); // <- wichtig!
        LayoutModeHelper.applyLayout(this, recyclerView, adapter, isList);
    }

    public void applyCurrentLayoutMode(RecyclerView recyclerView, NoteAdapter adapter) {
        boolean isList = LayoutModeHelper.isListMode(this);
        LayoutModeHelper.applyLayout(this, recyclerView, adapter, isList);
    }

//    public boolean isListLayoutMode() {
//        return LayoutModeHelper.isListMode(this);
//    }


    // Schaltet den Modus um und gibt den neuen Zustand zurück
//    public boolean toggleLayoutMode() {
//        boolean newMode = !isListLayout();
//        saveLayoutMode(newMode);
//        return newMode;
//    }

    public boolean getSavedShowHidden() {
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        return prefs.getBoolean(PREF_SHOW_HIDDEN, false);
    }

    public void saveShowHidden(boolean showHidden) {
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_SHOW_HIDDEN, showHidden).apply();
    }

    // Speichern des aktuellen Status für den Theme
    public void saveTheme(String themeValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(DESIGN_MODE, themeValue);
        editor.apply();
        applyTheme();  // Direkt nach dem Speichern das Theme anwenden
    }

    // Anwenden des gespeicherten Themes
    public void applyTheme() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String themeValue = sharedPreferences.getString(DESIGN_MODE, "system");
        AppCompatDelegate.setDefaultNightMode(toNightMode(themeValue));
    }

    // Umwandeln der gespeicherten Theme-Einstellung in den entsprechenden NightMode
    private int toNightMode(String themeValue) {
        if ("lightmode".equals(themeValue)) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if ("darkmode".equals(themeValue)) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    // Überprüfe, ob der alte Schlüssel vorhanden ist und migriere ihn
    private void migrateOldDesignMode() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        String oldThemeValue = sharedPreferences.getString(OLD_DESIGN_MODE, null);

        // Falls der alte Schlüssel existiert, migriere den Wert
        if (oldThemeValue != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            // Kopiere den Wert zum neuen Schlüssel
            editor.putString(DESIGN_MODE, oldThemeValue);
            // Lösche den alten Schlüssel
            editor.remove(OLD_DESIGN_MODE);
            editor.apply();
        }
    }

    public static int getCurrentVersionCode(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.getLocalizedMessage();
            return 0;
        }
    }

    public void close() throws IOException {
        // Ressourcen freigeben – z. B. Datenbank, Caches etc.
    }
}
