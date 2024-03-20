package com.git.amarradi.leafpad;

import android.annotation.SuppressLint;

import java.util.List;

public interface LeafStore {

    List<Note> loadAll();

    Note save(Note note);

    void remove(Note note);

    Note findById(String noteId);
}
