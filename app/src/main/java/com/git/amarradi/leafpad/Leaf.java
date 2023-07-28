package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Leaf {

    private final static String STORE_PREF = "leafstore";
    private final static String ID_KEY = "note_id_set";
    private final static String ADDDATE = "note_date_set";
    private final static String ADDTIME = "note_time_set";
    private final static String TITLE_PREFIX = "note_title_";
    private final static String BODY_PREFIX = "note_body_";


    public static ArrayList<Note> loadAll(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(STORE_PREF, Context.MODE_PRIVATE);
        ArrayList<Note> notes = new ArrayList<>();

        // Load note ids
        Set<String> noteIds = sharedPreferences.getStringSet(ID_KEY, null);

        if (noteIds != null) {
            for (String noteId : noteIds) {
                notes.add(load(context, noteId));
            }
        }
        return notes;
    }


    public static Note load(Context context, String noteId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(STORE_PREF, Context.MODE_PRIVATE);
        return load(sharedPreferences, noteId);
    }

    public static Note load(SharedPreferences sharedPreferences, String noteId) {
        String title = sharedPreferences.getString(TITLE_PREFIX + noteId, "");
        String body = sharedPreferences.getString(BODY_PREFIX + noteId, "");
        String noteDate = sharedPreferences.getString(ADDDATE+ noteId,"");
        String noteTime = sharedPreferences.getString(ADDTIME + noteId,"");
        return new Note(title, body,noteDate, noteTime,noteId);
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
        editor.putString(ADDTIME+ note.getId(), note.getTime());
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

        editor.remove(TITLE_PREFIX + note.getId());
        editor.remove(BODY_PREFIX + note.getId());
        editor.apply();
    }

   /* public static void clear(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(STORE_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }*/
}
