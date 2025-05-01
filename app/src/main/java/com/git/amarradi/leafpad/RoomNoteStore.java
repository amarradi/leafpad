package com.git.amarradi.leafpad;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class RoomNoteStore implements NoteStore {
    private static NoteDao DAO = null;

    private void checkDAO(Context context) {
        if (DAO==null) {
            DAO = AppDatabase.getInstance(context).noteDao();
        }
    }
    public ArrayList<Note> loadAll(Context context, boolean includeHidden) {
        checkDAO(context);
        ArrayList<Note> result = new ArrayList();
        List<Note> notes = DAO.getAll();
        for (Note note : notes) {
            if (!note.isHide() || includeHidden) {
                result.add(note);
            }
        }
        return result;
    }

    public Note load(Context context, String noteId) {
        checkDAO(context);
        Note note = DAO.getById(noteId);
        if (note==null) {
            return new Note("","","","","",false,"", noteId);
        }
        else {
            return note;
        }
    }

    public void set(Context context, Note note) {
        checkDAO(context);
        DAO.insert(note);
    }

    public void remove(Context context, Note note) {
        checkDAO(context);
        DAO.delete(note);
    }

    public void deleteAll(Context context) {
        DAO.deleteAll();
    }
}
