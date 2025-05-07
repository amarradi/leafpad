package com.git.amarradi.leafpad;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class NoteViewModel extends ViewModel {

    private final MutableLiveData<List<Note>> notesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Note> selectedNote = new MutableLiveData<>();

    public LiveData<List<Note>> getNotes() {
        return notesLiveData;
    }

    public LiveData<Note> getSelectedNote() {
        return selectedNote;
    }

    public void loadNotes(Context context, boolean includeHidden) {
        List<Note> notes = Leaf.loadAll(context, includeHidden);
        notesLiveData.setValue(notes);
    }

    public void selectNote(Note note) {
        selectedNote.setValue(note);
    }

    public void saveNote(Context context, Note note) {
        Leaf.save(context, note);
        loadNotes(context, false); // Liste neu laden
    }

    public void deleteNote(Context context, Note note) {
        Leaf.remove(context, note);
        loadNotes(context, false);
    }
}
