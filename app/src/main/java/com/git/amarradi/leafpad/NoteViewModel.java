package com.git.amarradi.leafpad;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

public class NoteViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Note>> notesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Note> selectedNote = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showHiddenLiveData = new MutableLiveData<>(false);

    public LiveData<Boolean> getShowHidden() {
        return showHiddenLiveData;
    }

    public void setShowHidden(boolean showHidden) {
        showHiddenLiveData.setValue(showHidden);
        loadNotes();
    }

    public NoteViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<Note>> getNotes() {
        return notesLiveData;
    }

    public LiveData<Note> getSelectedNote() {
        return selectedNote;
    }

//   private void loadNotes(boolean onlyHidden) {
//        List<Note> allNotes = Leaf.loadAll(getApplication(),onlyHidden);
//        Log.d("NoteViewModel", "Loaded notes: " + allNotes.size());
//        notesLiveData.postValue(allNotes);}

    public void loadNotes() {
        Boolean showHidden = showHiddenLiveData.getValue();
        if (showHidden == null) showHidden = false;
        List<Note> allNotes = Leaf.loadAll(getApplication(), showHidden);
        notesLiveData.postValue(allNotes);
    }


    public void selectNote(Note note) {
        selectedNote.setValue(note);
    }

    public void saveNote(Context context, Note note) {
        Leaf.save(getApplication(), note);
        //loadNotes(false); // Liste neu laden
        loadNotes();
    }

    public void deleteNote(Context context, Note note) {
        Leaf.remove(getApplication(), note);
        //loadNotes( false);
        loadNotes();
    }
}
