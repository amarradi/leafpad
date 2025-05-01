package com.git.amarradi.leafpad;

import android.content.Context;

import java.util.ArrayList;

public interface NoteStore {
    public ArrayList<Note> loadAll(Context context, boolean includeHidden);
    public Note load(Context context, String noteId);
    public void set(Context context, Note note);
    public void remove(Context context, Note note);
    public void deleteAll(Context context);
}
