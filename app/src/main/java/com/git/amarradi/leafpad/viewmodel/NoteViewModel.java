package com.git.amarradi.leafpad.viewmodel;

import static com.git.amarradi.leafpad.Leafpad.getCurrentAppVersion;
import static com.git.amarradi.leafpad.Leafpad.getReleaseNoteSeenVersion;
import static com.git.amarradi.leafpad.Leafpad.isReleaseNoteDismissed;
import static com.git.amarradi.leafpad.Leafpad.setReleaseNoteDismissed;
import static com.git.amarradi.leafpad.Leafpad.setReleaseNoteSeenVersion;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.git.amarradi.leafpad.model.Leaf;
import com.git.amarradi.leafpad.model.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NoteViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Note>> notesLiveData = new MutableLiveData<>();
    private static final MutableLiveData<Note> selectedNote = new MutableLiveData<>();
    private final MutableLiveData<Note> originalNote = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showHiddenLiveData = new MutableLiveData<>(false);

    private final MutableLiveData<Boolean> showReleaseNotes = new MutableLiveData<>(false);

    public LiveData<Boolean> getShowReleaseNotes() {
        return showReleaseNotes;
    }

    // Diese Methode prüft, ob die ReleaseNotes angezeigt werden sollen:
    public void checkReleaseNoteStatus(Context context) {
        int currentVersion = getCurrentAppVersion(context);
        int seenVersion = getReleaseNoteSeenVersion(context);

        boolean dismissed = isReleaseNoteDismissed(context); // z.B. aus SharedPreferences
        if (!dismissed && currentVersion > seenVersion) {
            showReleaseNotes.postValue(true);
        } else {
            showReleaseNotes.postValue(false);
        }
    }

    // Beim Schließen der ReleaseNotes
    public void dismissReleaseNotes(Context context) {
        setReleaseNoteDismissed(context, true); // in SharedPrefs
        setReleaseNoteSeenVersion(context, getCurrentAppVersion(context));
        showReleaseNotes.postValue(false);
    }

    // Beim manuellen Wiederherstellen
    public void showReleaseNotesAgain(Context context) {
        setReleaseNoteDismissed(context, false);
        showReleaseNotes.postValue(true);
    }


    private final LiveData<Boolean> isNoteEmpty = Transformations.map(
            selectedNote,
            note -> {
                if (note == null) return true;
                return (note.getTitle() == null || note.getTitle().trim().isEmpty()) &&
                        (note.getBody()  == null || note.getBody().trim().isEmpty());
            }
    );

    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MediatorLiveData<List<Note>> filteredNotes = new MediatorLiveData<>();

    public void persist() {
        Note n = selectedNote.getValue();
        if (n == null) return;

        String title;
        if (n.getTitle() == null) {
            title = "";
        } else {
            title = n.getTitle().trim();
        }

        String body;
        if (n.getBody() == null) {
            body = "";
        } else {
            body = n.getBody().trim();
        }

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
        if (note.getTitle().isEmpty()) {
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

        if (original == null) {
            return false;
        }

        if(current == null) {
            return false;
        }

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
        return changed;
    }
    public NoteViewModel(@NonNull Application application) {
        super(application);

        filteredNotes.addSource(notesLiveData, notes -> applySearchQuery());
        filteredNotes.addSource(searchQuery, q -> applySearchQuery());
    }
    private void applySearchQuery() {
        List<Note> allNotes = notesLiveData.getValue();
        String query = searchQuery.getValue();

        if (allNotes == null || query == null || query.isEmpty()) {
            filteredNotes.setValue(new ArrayList<>());
            return;
        }

        List<Note> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (Note note : allNotes) {
            if ((note.getTitle() != null && note.getTitle().toLowerCase().contains(lowerQuery)) ||
                    (note.getBody() != null && note.getBody().toLowerCase().contains(lowerQuery)) ||
                    (note.getCategory() != null && note.getCategory().toLowerCase().contains(lowerQuery))) {
                filtered.add(note);
            }
        }
        filteredNotes.setValue(filtered);
    }
    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public LiveData<List<Note>> getSearchResults() {
        return filteredNotes;
    }

    public LiveData<List<Note>> getNotes() {
        return notesLiveData;
    }

    public LiveData<Note> getSelectedNote() {
        return selectedNote;
    }

    public LiveData<Boolean> getShowHidden() {
        return showHiddenLiveData;
    }

    public void setShowHidden(boolean showHidden) {
        showHiddenLiveData.setValue(showHidden);
        loadNotes();
    }

    public void setNote(Note note) {
        if (note == null) {
            originalNote.setValue(null);
            selectedNote.setValue(null);
        } else {
            originalNote.setValue(note);
            // Copy to compare
            selectedNote.setValue(new Note(note));
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
    public void saveNote(Context context, Note note) {
        Leaf.save(getApplication(), note);
        loadNotes();
    }
    public void deleteNote(Context context, Note note) {
        Leaf.remove(getApplication(), note);
        loadNotes();
    }
    public void updateNoteRecipe(String category) {
        Note currentNote = selectedNote.getValue();
        if (currentNote != null) {
            String currentCategory = currentNote.getCategory();

            if (!category.equals(currentCategory)) {
                currentNote.setCategory(category);
                selectedNote.setValue(currentNote);
            }
        }
    }
    public static boolean isEmptyEntry(Note note) {
        return note.getBody().isEmpty() && note.getTitle().isEmpty();
    }
    public LiveData<List<Note>> searchNotes(String query) {
        MutableLiveData<List<Note>> result = new MutableLiveData<>();
        List<Note> allNotes = notesLiveData.getValue();

        if (allNotes == null) {
            result.setValue(new ArrayList<>());
            return result;
        }

        List<Note> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (Note note : allNotes) {
            if ((note.getTitle() != null && note.getTitle().toLowerCase().contains(lowerQuery)) ||
                    (note.getBody() != null && note.getBody().toLowerCase().contains(lowerQuery))) {
                filtered.add(note);
            }
        }

        result.setValue(filtered);
        return result;
    }
}