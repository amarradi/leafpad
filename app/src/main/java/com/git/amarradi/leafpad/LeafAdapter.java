package com.git.amarradi.leafpad;

import android.content.Context;

import java.util.ArrayList;

public class LeafAdapter implements NoteStore {
    public ArrayList<Note> loadAll(Context context, boolean includeHidden) {
        return Leaf.loadAll(context,includeHidden);
    }
    public Note load(Context context, String noteId){
        return Leaf.load(context,noteId);
    }
    public void set(Context context, Note note) {
        Leaf.set(context,note);
    }
    public void remove(Context context, Note note) {
        Leaf.remove(context,note);
    }
    public void deleteAll(Context context) {
        Leaf.deleteAll(context);
    }
}
