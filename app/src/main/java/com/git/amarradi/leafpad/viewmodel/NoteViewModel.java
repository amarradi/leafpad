package com.git.amarradi.leafpad.viewmodel;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.git.amarradi.leafpad.Leafpad;
import com.git.amarradi.leafpad.helper.ReleaseNoteHelper;
import com.git.amarradi.leafpad.model.Leaf;
import com.git.amarradi.leafpad.model.Note;
import com.git.amarradi.leafpad.model.ReleaseNote;

import java.util.ArrayList;
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

    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MediatorLiveData<List<Note>> filteredNotes = new MediatorLiveData<>();
    private final MutableLiveData<ReleaseNote> releaseNoteLiveData = new MutableLiveData<>();
    public LiveData<ReleaseNote> getReleaseNote() {
        return releaseNoteLiveData;
    }
    private final MediatorLiveData<List<Object>> combinedNotes = new MediatorLiveData<>();
    public LiveData<List<Object>> getCombinedNotes() { return combinedNotes; }
    private final MediatorLiveData<Boolean> isNoteModified = new MediatorLiveData<>(false);

    private Object releaseNoteHeader;

    public void setReleaseNoteHeader(Object releaseNoteHeader) {
        this.releaseNoteHeader = releaseNoteHeader;
        updateCombinedNotes(); // Liste neu aufbauen
    }

    public Object getReleaseNoteHeader() {
        return releaseNoteHeader;
    }



    public void updateSingleNote(Note updatedNote) {
        List<Note> currentNotes = notesLiveData.getValue();
        if (currentNotes == null) {
            currentNotes = new ArrayList<>();
        } else {
            currentNotes = new ArrayList<>(currentNotes); // kopieren, um LiveData nicht direkt zu verändern
        }

        boolean replaced = false;

        for (int i = 0; i < currentNotes.size(); i++) {
            if (currentNotes.get(i).getId().equals(updatedNote.getId())) {
                currentNotes.set(i, updatedNote); // ersetze vorhandene Notiz
                replaced = true;
                break;
            }
        }

        if (!replaced) {
            currentNotes.add(0, updatedNote); // neue Notiz ganz oben einfügen
        }

        notesLiveData.setValue(currentNotes);

        // Wenn du auch combinedNotesLiveData nutzt (z. B. für ReleaseNotes), hier ebenfalls setzen:
        updateCombinedNotes();
    }

    private void updateCombinedNotes() {
        List<Note> visibleNotes = notesLiveData.getValue();
        List<Object> combined = new ArrayList<>();
        if (releaseNoteHeader != null) {
            combined.add(releaseNoteHeader);
        }
        if (visibleNotes != null) {
            combined.addAll(visibleNotes);
        }
        combinedNotes.setValue(combined);
    }



    public void checkAndLoadReleaseNote(Context context) {
        int savedVersion = Leafpad.getCurrentLeafpadVersionCode(context); // default = 0
        int currentVersion = Leafpad.getCurrentVersionCode(context);

        if (savedVersion <= currentVersion) {
            Log.d("checkAndLoadReleaseNote","saved: "+savedVersion+" current:"+currentVersion);
                ReleaseNote note = ReleaseNoteHelper.loadReleaseNote(context);
                releaseNoteLiveData.setValue(note);
        } else {
            releaseNoteLiveData.setValue(null);
        }
    }



    public void setNoteHide() {
        // 1. Hole die aktuell ausgewählte Notiz.
        Note note = selectedNote.getValue();
        if (note == null) {
            // Falls keine Notiz ausgewählt ist, abbrechen.
            return;
        }

        // 2. Toggle den Versteckt-Status der Notiz.
        note.setHide(!note.isHide());

        // 3. Hole die aktuelle Notizenliste aus dem LiveData.
        List<Note> oldList = notesLiveData.getValue();
        if (oldList == null) {
            // Falls die Liste leer oder nicht initialisiert ist, abbrechen.
            return;
        }
        // 4. Erstelle eine neue Kopie der Liste (damit LiveData die Änderung erkennt).
        List<Note> newList = new ArrayList<>(oldList);

        // 5. Finde die Position der geänderten Notiz in der Liste.
        int index = -1;
        for (int i = 0; i < newList.size(); i++) {
            Note n = newList.get(i);
            // Hier solltest du vergleichen, ob es wirklich dieselbe Notiz ist.
            // Am besten anhand einer eindeutigen ID (z. B. note.getId()).
            if (n.getId().equals(note.getId())) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            // 6. Ersetze die alte Notiz durch die geänderte Version.
            newList.set(index, note);

            // 7. Setze die aktualisierte Liste als neuen Wert im LiveData.
            notesLiveData.setValue(newList);

            // (Optional: setze das geänderte Note-Objekt auch erneut im selectedNote-LiveData, falls nötig)
            selectedNote.setValue(note);
        }
    }

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
    public void updateNoteFromUI(String title, String body) {
        Note current = selectedNote.getValue();
        if (current == null) return;

        boolean changed = false;

        if (!Objects.equals(current.getTitle(), title)) {
            current.setTitle(title);
            changed = true;
        }

        if (!Objects.equals(current.getBody(), body)) {
            current.setBody(body);
            changed = true;
        }

        if (changed) {
            selectedNote.setValue(current); // nur wenn sich wirklich was geändert hat!
        }
    }
    public void updateModificationState() {
        isNoteModified.setValue(hasUnsavedChanges());
    }

    public boolean hasUnsavedChanges() {
        Note current = selectedNote.getValue();
        Note original = originalNote.getValue();

        if (original == null || current == null) {
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

        if (!currentTitle.equals(originalTitle)) {
            return true;
        }
        if (!currentBody.equals(originalBody)) {
            return true;
        }
        if (!Objects.equals(current.getCategory(), original.getCategory())) {
            return true;
        }
        if (current.isHide() != original.isHide()) {
            return true;
        }
        return false;
    }
    public NoteViewModel(@NonNull Application application) {
        super(application);

        isNoteModified.addSource(selectedNote, n -> checkModified());
        isNoteModified.addSource(originalNote, n -> checkModified());
        filteredNotes.addSource(notesLiveData, notes -> applySearchQuery());
        filteredNotes.addSource(searchQuery, q -> applySearchQuery());

        loadReleaseNote(getApplication().getApplicationContext());
    }

    private void checkModified() {
        Note current = selectedNote.getValue();
        Note original = originalNote.getValue();
        if (original == null || current == null) {
            isNoteModified.setValue(false);
        } else {
            isNoteModified.setValue(!current.equalsContent(original));
        }
    }
    public LiveData<Boolean> getIsNoteModified() {
        return isNoteModified;
    }

    public void setNoteModified(boolean modified) {
        isNoteModified.setValue(modified);
    }

    public void loadReleaseNote(Context context) {
        ReleaseNote note = ReleaseNoteHelper.loadReleaseNote(context);
        releaseNoteLiveData.setValue(note);
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

    public void markSaved() {
        Note selected = selectedNote.getValue();
        if (selected != null) {
            // originalNote ist das Vergleichsobjekt für hasUnsavedChanges
            // Deep copy!
            originalNote.setValue(new Note(selected)); // Nutze einen Copy-Konstruktor oder einen eigenen Clone
        }
        isNoteModified.setValue(false);
    }

}