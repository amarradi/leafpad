package com.git.amarradi.leafpad;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.List;
import java.util.Objects;

public class NoteViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Note>> notesLiveData = new MutableLiveData<>();
    private static final MutableLiveData<Note> selectedNote = new MutableLiveData<>();
    private final MutableLiveData<Note> originalNote = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showHiddenLiveData = new MutableLiveData<>(false);
    private final LiveData<Boolean> isNoteEmpty = Transformations.map(
            selectedNote,
            note -> {
                if (note == null) return true;
                return (note.getTitle() == null || note.getTitle().trim().isEmpty()) &&
                        (note.getBody()  == null || note.getBody().trim().isEmpty());
            }
    );

    /** Wird aufgerufen, wenn die Activity pausiert oder der Benutzer zurückdrückt */
//    public void persist() {
//        Note n = selectedNote.getValue();
//        if (n == null) return;
//
//        if (Boolean.TRUE.equals(isNoteEmpty.getValue())) {
//            // leere Notiz -> löschen
//            Leaf.remove(getApplication(), n);
//            selectedNote.setValue(null);
//        } else {
//            // nicht leer -> speichern
//            Leaf.save(getApplication(), n);
//        }
//    }

    public void persist() {
        Note n = selectedNote.getValue();
        if (n == null) return;
        Log.d("NoteViewModel", "observeNote: "+n.getTitle()+"|"+n.getBody());
        // ** direkt prüfen, nicht über LiveData **
        String title = n.getTitle()  == null ? "" : n.getTitle().trim();
        String body  = n.getBody()   == null ? "" : n.getBody().trim();

        if (isNewEntry(n)) {
            // leere Notiz → löschen
            Leaf.remove(getApplication(), n);
            // wir clearen die Selection, damit die Activity nicht weiter mit 'n' arbeitet
            selectedNote.setValue(null);
        } else {
            // nicht-leer → speichern
            Leaf.save(getApplication(), n);
        }
        // und die Liste aktualisieren, falls ihr sofort die MainActivity updaten wollt
        loadNotes();
    }
    public boolean isNewEntry(Note note) {
        String title = note.getTitle() == null ? "" : note.getTitle().trim();
        String body  = note.getBody() == null ? "" : note.getBody().trim();
        return title.isEmpty() && body.isEmpty();
    }


    public LiveData<Boolean> isNoteModified = Transformations.map(selectedNote, current -> {
        Note original = originalNote.getValue();
        if (original == null || current == null) return false;
        return !current.equalsContent(original);
    });

    public boolean hasUnsavedChanges() {
        Note current = selectedNote.getValue();
        Note original = originalNote.getValue();

        if (current == null && original == null) return false;
        if (current == null || original == null) return true;

        return !current.getTitle().equals(original.getTitle()) ||
                !current.getBody().equals(original.getBody()) ||
                !Objects.equals(current.getCategory(), original.getCategory()) ||
                current.isHide() != original.isHide();
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

    public LiveData<Boolean> getIsNoteEmpty() {
        return isNoteEmpty;
    }

    public LiveData<Boolean> getShowHidden() {
        return showHiddenLiveData;
    }

    public void setShowHidden(boolean showHidden) {
        showHiddenLiveData.setValue(showHidden);
        loadNotes();
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
        originalNote.setValue(new Note(note));
    }

    public void saveNote(Context context, String title, String body) {
        Note note = selectedNote.getValue();
        if (note != null) {
            note.setTitle(title);
            note.setBody(body);
            if (isEmptyEntry(note)){
                deleteNote(context, note);
            } else {
                saveNote(context,note);
                // Optional: Liste neu laden oder Event triggern
            }
        }
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

    // Im ViewModel:
//    public void updateNoteVisibility(boolean hide) {
//        Note currentNote = selectedNote.getValue();
//        if (currentNote != null) {
//            currentNote.setHide(hide);
//            Leaf.save(getApplication(), currentNote);
//            selectedNote.setValue(currentNote); // damit Beobachter aktualisiert werden
//        }
//    }

    public void updateNoteVisibility(boolean hide) {
        Note currentNote = selectedNote.getValue();
        if (currentNote != null) {
            // Leere Notizen nicht speichern!
            if ((currentNote.getTitle() == null || currentNote.getTitle().trim().isEmpty()) &&
                    (currentNote.getBody() == null || currentNote.getBody().trim().isEmpty())) {
                // Nur Sichtbarkeit ändern, nicht speichern
                currentNote.setHide(hide);
                selectedNote.setValue(currentNote);
                return;
            }

            currentNote.setHide(hide);
            Leaf.save(getApplication(), currentNote);
            selectedNote.setValue(currentNote);
        }
    }


    public void updateNoteRecipe(String category) {
        Log.d("updateNoteRecipe", "updateNoteRecipe entered");
        Note currentNote = selectedNote.getValue();
        if (currentNote != null) {
            String currentCategory = currentNote.getCategory();

            if (!category.equals(currentCategory)) {
                currentNote.setCategory(category);
                Leaf.save(getApplication(), currentNote);
                Log.d("updateNoteRecipe", "Category updated to: " + category);
                selectedNote.setValue(currentNote);
            } else {
                Log.d("updateNoteRecipe", "No category update needed.");
            }
        } else {
            Log.d("updateNoteRecipe", "currentNote is null, no update performed.");

        }
    }

    public static boolean isEmptyEntry(Note note) {
        return note.getBody().isEmpty() && note.getTitle().isEmpty();
    }
}
