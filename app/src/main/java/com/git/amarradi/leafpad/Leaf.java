package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.icu.util.LocaleData;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
    //  private final static String CREATETIME = "note_time_";


    public static ArrayList<Note> loadAll(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(STORE_PREF, Context.MODE_PRIVATE);
        ArrayList<Note> notes = new ArrayList<>();
        Set<String> noteIds = sharedPreferences.getStringSet(ID_KEY, null);

        if (noteIds != null) {
            for (String noteId : noteIds) {
                notes.add(load(context, noteId));
            }
        }

       /*

       notes.sort(Comparator
                .comparing(Note::getDate)
                .thenComparing(Note::getTime));
        */

       /*
       DateTimeFormatter f = DateTimeFormatter.ofPattern("dd-MM-yyyy");
notes.sort(
  (String s1, String s2) -> LocalDate.parse(s1, f).compareTo(LocalDate.parse(s2, f)));
        */
        DateTimeFormatter f;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            f = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            notes.sort(Comparator
                        .comparing(o -> LocalDate.parse(o.getDate(), f)));
        }
        Collections.reverse(notes);
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
        String noteCreateDate = sharedPreferences.getString(CREATEDATE+ noteId,"");
     //   String noteCreateTime = sharedPreferences.getString(CREATETIME+noteId,"");
        return new Note(title, body, noteDate, noteTime, noteCreateDate, noteId);
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
        editor.putString(CREATEDATE + note.getId(), note.getCreateDate());
       // editor.putString(CREATETIME + note.getId(), note.getCreateTime());
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
        editor.remove(CREATEDATE + note.getId());
        editor.apply();
    }

   /* public static void clear(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(STORE_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }*/
}
