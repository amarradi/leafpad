package com.git.amarradi;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Leaf {

    private final static String STORE_PREF = "leafstore";
    private final static String ID_KEY = "note_id_set";
    private final static String TITLE_PREFIX = "note_title_";
    private final static String BODY_PREFIX = "note_body_";


    public static ArrayList<Note> loadAll(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(STORE_PREF, Context.MODE_PRIVATE);
        ArrayList<Note> notes = new ArrayList<Note>();

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
        return new Note(title, body, noteId);
    }

    public static void set(Context context, Note note) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(STORE_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> ids = sharedPreferences.getStringSet(ID_KEY, null);

        if (ids == null) {
            ids = new HashSet<String>();
            ids.add(note.getId());
            editor.putStringSet(ID_KEY, ids);
        } else if (!ids.contains(note.getId())) {
            ids.add(note.getId());
            editor.putStringSet(ID_KEY, ids);
        }

        editor.putString(TITLE_PREFIX + note.getId(), note.getTitle());
        editor.putString(BODY_PREFIX + note.getId(), note.getBody());
        editor.apply();
    }

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

    public static void clear(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(STORE_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}
