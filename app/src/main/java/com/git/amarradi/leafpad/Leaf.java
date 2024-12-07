package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class Leaf {

    private final static String STORE_PREF = "leafstore";
    private final static String ID_KEY = "note_id_set";
    private final static String ADDDATE = "note_date_set";
    private final static String ADDTIME = "note_time_set";
    private final static String CREATEDATE = "note_date_";
    private final static String TITLE_PREFIX = "note_title_";
    private final static String BODY_PREFIX = "note_body_";
    private final static boolean HIDE = false;

    public static ArrayList<Note> loadAll(Context context, boolean includeHidden) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(STORE_PREF, Context.MODE_PRIVATE);
        ArrayList<Note> notes = new ArrayList<>();
        Set<String> noteIds = sharedPreferences.getStringSet(ID_KEY, null);
        Log.d("LoadAll", "Note IDs loaded: " + noteIds);



       /** if (noteIds != null) {
            for (String noteId : noteIds) {
                notes.add(load(context, noteId));
            }
        }**/

        for (String noteId : noteIds) {
            Note note = load(context, noteId);
            if (!note.isHide() || includeHidden) { // Parameter `includeHidden` hinzufügen
                notes.add(note);
            }
        }


        DateTimeFormatter d;
        DateTimeFormatter t;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                d = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY);
                t = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY);
                notes.sort(Comparator
                        .comparing((Note o) -> LocalDate.parse(o.getDate(), d))
                        .thenComparing(o -> LocalTime.parse(o.getTime(), t)));
            } catch (DateTimeParseException dateTimeParseException) {
                Log.d("dateTimeParseException", "loadAll: "+dateTimeParseException.getClass());
            }
        }

        Collections.reverse(notes);
        return notes;
    }

    public static Note load(Context context, String noteId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(STORE_PREF, Context.MODE_PRIVATE);
        boolean noteHide;
        if (sharedPreferences.contains(HIDE + noteId)) {
            // Migrate old data if found
            noteHide = sharedPreferences.getBoolean(HIDE + noteId, false);
            sharedPreferences.edit().remove(HIDE + noteId).putBoolean(HIDE + "_" + noteId, noteHide).apply();
        } else {
            noteHide = sharedPreferences.getBoolean(HIDE + "_" + noteId, false);
        }

        return load(sharedPreferences, noteId);
    }

    public static Note load(SharedPreferences sharedPreferences, String noteId) {
        String title = sharedPreferences.getString(TITLE_PREFIX + noteId, "");
        String body = sharedPreferences.getString(BODY_PREFIX + noteId, "");
        String noteDate = sharedPreferences.getString(ADDDATE+ noteId,"");
        String noteTime = sharedPreferences.getString(ADDTIME + noteId,"");
        String noteCreateDate = sharedPreferences.getString(CREATEDATE+ noteId,"");
        //boolean noteHide = sharedPreferences.getBoolean(HIDE + noteId,false); //der alte Schlüssel bis version 1.14
        boolean noteHide = sharedPreferences.getBoolean(HIDE + "_" + noteId, false);
        return new Note(title, body, noteDate, noteTime, noteCreateDate, noteHide, noteId);
    }

    @SuppressLint("MutatingSharedPrefs")
    public static void set(Context context, Note note) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(STORE_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> ids = sharedPreferences.getStringSet(ID_KEY, null);

        if (ids == null) {
            ids = new HashSet<>();
            ids.add(note.getId());
            editor.putStringSet(ID_KEY, ids);
        } else if (!ids.contains(note.getId())) {
            ids.add(note.getId());
            editor.putStringSet(ID_KEY, ids);
        }



        editor.putString(TITLE_PREFIX + note.getId(), note.getTitle());
        editor.putString(BODY_PREFIX + note.getId(), note.getBody());
        editor.putString(ADDDATE + note.getId(), note.getDate());
        editor.putString(ADDTIME + note.getId(), note.getTime());
        //editor.putBoolean(HIDE + note.getId(), note.isHide());

        // Migriere alten Schlüssel, falls vorhanden
        if (sharedPreferences.contains(HIDE + note.getId())) {
            boolean oldHide = sharedPreferences.getBoolean(HIDE + note.getId(), false);
            editor.remove(HIDE + note.getId()); // Alten Schlüssel entfernen
            editor.putBoolean(HIDE + "_" + note.getId(), oldHide); // Neuen Schlüssel setzen
        } else {
            editor.putBoolean(HIDE + "_" + note.getId(), note.isHide());
        }

        editor.apply();
    }

    @SuppressLint("MutatingSharedPrefs")
    public static void remove(Context context, Note note) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(STORE_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> ids = sharedPreferences.getStringSet(ID_KEY, null);

        if (ids == null) {
            return;
        }

        ids.remove(note.getId());
        editor.remove(ID_KEY + note.getId());
        editor.remove(TITLE_PREFIX + note.getId());
        editor.remove(BODY_PREFIX + note.getId());
        editor.remove(ADDDATE + note.getId());
        editor.remove(ADDTIME + note.getId());
        editor.remove(HIDE + note.getId());
        editor.apply();
    }
}
