package com.git.amarradi.leafpad;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

public class NoteViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Note>> notesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Note> selectedNote = new MutableLiveData<>();



    public NoteViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<Note>> getNotes() {
        return notesLiveData;
    }

    public LiveData<Note> getSelectedNote() {
        return selectedNote;
    }

    public void loadNotes(boolean includeHidden) {
        Log.d("NoteViewModel", "showHidden: " + includeHidden);  // Log-Ausgabe
        List<Note> allNotes = Leaf.loadAll(getApplication(),includeHidden);
        Log.d("NoteViewModel", "Loaded Notes: " + allNotes.size());  // Log-Ausgabe
        //notesLiveData.setValue(notes);
        /**List<Note> filteredNotes = new ArrayList<>();
        for (Note note : allNotes) {
            Log.d("NoteViewModel", "Checking note: " + note.getTitle() + ", hide: " + note.isHide());
            if(includeHidden || !note.isHide()) {
                filteredNotes.add(note);
            }
        }

        Log.d("NoteViewModel", "Filtered Notes: " + filteredNotes.size());**/
        notesLiveData.setValue(allNotes);
    }

    public void selectNote(Note note) {
        selectedNote.setValue(note);
    }

    public void saveNote(Context context, Note note) {
        Leaf.save(getApplication(), note);
        loadNotes(false); // Liste neu laden
    }

    public void deleteNote(Context context, Note note) {
        Leaf.remove(getApplication(), note);
        loadNotes( false);
    }
}
