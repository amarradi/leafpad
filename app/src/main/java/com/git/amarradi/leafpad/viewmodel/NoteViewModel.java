package com.git.amarradi.leafpad.viewmodel;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.git.amarradi.leafpad.Leafpad;
import com.git.amarradi.leafpad.model.Leaf;
import com.git.amarradi.leafpad.model.Note;
import com.git.amarradi.leafpad.model.ReleaseNoteHeader;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NoteViewModel extends AndroidViewModel {

    private final MutableLiveData<List<Note>> notesLiveData = new MutableLiveData<>();
    private static final MutableLiveData<Note> selectedNote = new MutableLiveData<>();
    private final MutableLiveData<Note> originalNote = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showHiddenLiveData = new MutableLiveData<>(false);
    private final MediatorLiveData<List<Object>> combinedNotes = new MediatorLiveData<>();
    public LiveData<List<Object>> getCombinedNotes() { return combinedNotes; }
    // ---- ReleaseNotes-Handling ----
    private final MutableLiveData<Boolean> showReleaseNote = new MutableLiveData<>(false);

    private final MutableLiveData<Boolean> releaseNoteVisibleLiveData = new MutableLiveData<>();

    private final MutableLiveData<Boolean> showReleaseNoteLiveData = new MutableLiveData<>(false);

    // Getter für die Activity/Fragment:
    public LiveData<Boolean> getShowReleaseNoteLiveData() {
        return showReleaseNoteLiveData;
    }
    public LiveData<Boolean> getReleaseNoteVisible() {
        return releaseNoteVisibleLiveData;
    }

    public void updateShowReleaseNoteState(Context context) {
        boolean dismissed = Leafpad.isReleaseNoteDismissed(context);
        int currentVersion = Leafpad.getCurrentAppVersion(context);
        int seenVersion = Leafpad.getReleaseNoteSeenVersion(context);

        if (!dismissed && currentVersion > seenVersion) {
            showReleaseNoteLiveData.setValue(true);
        } else {
            showReleaseNoteLiveData.setValue(false);
        }
    }


    public NoteViewModel(@NonNull Application application) {
        super(application);
        // Beobachte Notes & Suchanfrage für Filter-Logik
        filteredNotes.addSource(notesLiveData, notes -> applySearchQuery());
        filteredNotes.addSource(searchQuery, q -> applySearchQuery());

        // Reagiere auf Notizen-Änderungen
        combinedNotes.addSource(notesLiveData, notes -> updateCombinedNotes());
        // Reagiere auf Header-Änderungen
        combinedNotes.addSource(showReleaseNote, show -> updateCombinedNotes());

        // ReleaseNotes-Anzeige initial setzen
        updateReleaseNotesVisibility();
    }

    // ---- ReleaseNotes MVVM API ----

    private void updateCombinedNotes() {
        List<Object> items = new ArrayList<>();
        if (showReleaseNote.getValue() != null && showReleaseNote.getValue()) {
            items.add(new ReleaseNoteHeader());
        }
        List<Note> notes = notesLiveData.getValue();
        if (notes != null) items.addAll(notes);
        combinedNotes.postValue(items);
    }

    public void checkAndShowReleaseNotes(Context context) {
        // Prüfe, ob ReleaseNotes angezeigt werden müssen
        boolean show = Leafpad.shouldShowReleaseNotes(context);
        showReleaseNote.postValue(show);
        updateCombinedNotes();
    }

    public void reloadReleaseNoteState() {
        boolean visible = !Leafpad.isReleaseNoteDismissed(getApplication());
        releaseNoteVisibleLiveData.postValue(visible);
    }

    // Wird beim Schließen vom Nutzer aufgerufen:
    public void dismissReleaseNotes(Context context) {
        Leafpad.isReleaseNoteDismissed(context);
        showReleaseNote.postValue(false);
        updateCombinedNotes();
    }
    public LiveData<Boolean> getShowReleaseNote() {
        return showReleaseNote;
    }

    // Prüfe, ob ReleaseNotes angezeigt werden sollen (z.B. nach Update/Neuinstallation)
    public void updateReleaseNotesVisibility() {
        Application app = getApplication();
        boolean dismissed = Leafpad.isReleaseNoteDismissed(app);
        int currentVersion = Leafpad.getCurrentAppVersion(app);
        int seenVersion = Leafpad.getReleaseNoteSeenVersion(app);
        // Zeige ReleaseNotes, wenn...
        // - Sie nicht explizit dismissed wurden ODER
        // - ...der VersionCode seitdem erhöht wurde
        if (!dismissed || currentVersion > seenVersion) {
            showReleaseNote.setValue(true);
        } else {
            showReleaseNote.setValue(false);
        }
    }

    // Vom UI aufrufen, wenn ReleaseNotes geschlossen wurden (über das X)
//    public void dismissReleaseNotes(Context context) {
//        int currentVersion = Leafpad.getCurrentAppVersion(context);
//        Leafpad.setReleaseNoteSeenVersion(context, currentVersion); // Merke, welche Version der Nutzer gesehen hat
//        Leafpad.setReleaseNoteDismissed(context, true);             // Merke, dass sie weggeklickt wurden
//        showReleaseNotes.setValue(false);                           // LiveData für UI
//    }

    // Optional: Vom UI/Settings aufrufen, um ReleaseNotes wieder sichtbar zu machen
    public void resetReleaseNotes(Context context) {
        Leafpad.setReleaseNoteDismissed(context, false);
        showReleaseNote.setValue(true);
    }

    // ---- Standard Notiz-Logik ----

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

        String title = n.getTitle() == null ? "" : n.getTitle().trim();
        String body = n.getBody() == null ? "" : n.getBody().trim();

        if (isNewEntry(n)) {
            Leaf.remove(getApplication(), n);
            selectedNote.setValue(null);
        } else {
            Leaf.save(getApplication(), n);
        }
        loadNotes();
    }

    public boolean isNewEntry(Note note) {
        String title = note.getTitle() == null ? "" : note.getTitle().trim();
        String body = note.getBody() == null ? "" : note.getBody().trim();
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
        if (original == null || current == null) return false;

        boolean changed = false;
        if (!Objects.equals(current.getTitle(), original.getTitle())) changed = true;
        else if (!Objects.equals(current.getBody(), original.getBody())) changed = true;
        else if (!Objects.equals(current.getCategory(), original.getCategory())) changed = true;
        else if (current.isHide() != original.isHide()) changed = true;
        return changed;
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
            selectedNote.setValue(new Note(note));
        }
    }
    public void loadNotes() {
        Boolean showHidden = showHiddenLiveData.getValue();
        if (showHidden == null) showHidden = false;
        List<Note> allNotes = Leaf.loadAll(getApplication(), showHidden);
        notesLiveData.postValue(allNotes);
        updateCombinedNotes();
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
