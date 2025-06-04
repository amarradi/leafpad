package com.git.amarradi.leafpad.viewmodel;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.git.amarradi.leafpad.model.Leaf;
import com.git.amarradi.leafpad.model.Note;

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

    public void persist() {
        Note n = selectedNote.getValue();
        if (n == null) return;

        String title = n.getTitle()  == null ? "" : n.getTitle().trim();
        String body  = n.getBody()   == null ? "" : n.getBody().trim();

        if (isNewEntry(n)) {
            Leaf.remove(getApplication(), n);
            selectedNote.setValue(null);
        } else {
            Leaf.save(getApplication(), n);
        }
        loadNotes();
    }
    public boolean isNewEntry(Note note) {

        String title = "";
        Log.d("NoteViewModel", "isNewEntry: noteTitle"+note.getTitle());
        if (note.getTitle().isEmpty()) {
            Log.d("NoteViewModel", "isNewEntry: title"+ title);

            title = "";
        } else {
            title = note.getTitle().trim();
        }

        String body;
        if (note.getBody() == null) {
            body = "";
        } else {
            body = note.getBody().trim();
        }
        Log.d("NoteViewModel","isNewEntry" +title.isEmpty()+"|"+body.isEmpty());
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

//        Log.d("NoteViewModel", "current: "+current.getTitle()+"|"+current.getBody());

//        Log.d("NoteViewModel", "original: "+original.getTitle()+"|"+original.getBody());

        if (original == null) {
            return false;
        }

        if(current == null) {
            return false;
        }

//        String currentTitle = current.getTitle() == null ? "" : current.getTitle();
//        String currentBody = current.getBody() == null ? "" : current.getBody();
//        String originalTitle = original.getTitle() == null ? "" : original.getTitle();
//        String originalBody = original.getBody() == null ? "" : original.getBody();

        String currentTitle;
        if (current.getTitle() == null) {
            currentTitle = "";
        } else {
            currentTitle = current.getTitle();
        }

        String currentBody;
        if (current.getBody() == null) {
            currentBody = "";
        } else {
            currentBody = current.getBody();
        }

        String originalTitle;
        if (original.getTitle() == null) {
            originalTitle = "";
        } else {
            originalTitle = original.getTitle();
        }

        String originalBody;
        if (original.getBody() == null) {
            originalBody = "";
        } else {
            originalBody = original.getBody();
        }


//        boolean changed =
//                !currentTitle.equals(originalTitle) ||
//                        !currentBody.equals(originalBody) ||
//                        !Objects.equals(current.getCategory(), original.getCategory()) ||
//                        current.isHide() != original.isHide();

        boolean changed = false;

        if (!currentTitle.equals(originalTitle)) {
            changed = true;
        } else if (!currentBody.equals(originalBody)) {
            changed = true;
        } else if (!Objects.equals(current.getCategory(), original.getCategory())) {
            changed = true;
        } else if (current.isHide() != original.isHide()) {
            changed = true;
        }


//        Log.d("NoteViewModel", "hasUnsavedChanges = " + changed);
        return changed;
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

    public void setNote(Note note) {
        if (note == null) {
            originalNote.setValue(null);
            selectedNote.setValue(null);
        } else {
            originalNote.setValue(note);
            selectedNote.setValue(new Note(note)); // Klon!
        }
    }


    public void loadNotes() {
        Boolean showHidden = showHiddenLiveData.getValue();
        if (showHidden == null) showHidden = false;
        List<Note> allNotes = Leaf.loadAll(getApplication(), showHidden);
        notesLiveData.postValue(allNotes);
    }

    public void selectNote(Note note) {
        selectedNote.setValue(note);
        originalNote.setValue(new Note(
                note.getTitle(),
                note.getBody(),
                note.getDate(),
                note.getTime(),
                note.getCreateDate(),
                note.isHide(),
                note.getCategory(),
                note.getId()
        ));
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
            }
        }
    }

    public void saveNote(Context context, Note note) {
        Leaf.save(getApplication(), note);
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

//    public void updateNoteVisibility(boolean hide) {
//        Note currentNote = selectedNote.getValue();
//        if (currentNote != null) {
//
//            if ((currentNote.getTitle() == null || currentNote.getTitle().trim().isEmpty()) &&
//                    (currentNote.getBody() == null || currentNote.getBody().trim().isEmpty())) {
//
//                currentNote.setHide(hide);
//                selectedNote.setValue(currentNote);
//                return;
//            }
//            currentNote.setHide(hide);
//            Leaf.save(getApplication(), currentNote);
//            selectedNote.setValue(currentNote);
//        }
//    }


    public void updateNoteRecipe(String category) {
//        Log.d("updateNoteRecipe", "updateNoteRecipe entered");
        Note currentNote = selectedNote.getValue();
        if (currentNote != null) {
            String currentCategory = currentNote.getCategory();

            if (!category.equals(currentCategory)) {
                currentNote.setCategory(category);
               // Leaf.save(getApplication(), currentNote);
//                Log.d("updateNoteRecipe", "Category updated to: " + category);
                selectedNote.setValue(currentNote);
            }
//            else {
//                Log.d("updateNoteRecipe", "No category update needed.");
//            }
        }
//        else {
////            Log.d("updateNoteRecipe", "currentNote is null, no update performed.");
//
//        }
    }

    public static boolean isEmptyEntry(Note note) {
        return note.getBody().isEmpty() && note.getTitle().isEmpty();
    }
}
