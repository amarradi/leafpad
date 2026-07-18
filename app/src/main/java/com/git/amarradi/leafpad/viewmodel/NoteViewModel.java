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
import com.git.amarradi.leafpad.helper.ReleaseNoteHelper;
import com.git.amarradi.leafpad.model.CategoryEntity;
import com.git.amarradi.leafpad.model.Note;
import com.git.amarradi.leafpad.model.NoteEntity;
import com.git.amarradi.leafpad.model.NoteRepository;
import com.git.amarradi.leafpad.model.ReleaseNote;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NoteViewModel extends AndroidViewModel {

    private final NoteRepository noteRepository;
    private final LiveData<List<NoteEntity>> allNoteEntities;
    private LiveData<List<CategoryEntity>> categoriesForSelectedNote;
    private final MediatorLiveData<List<Note>> notesLiveData = new MediatorLiveData<>();
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
    public LiveData<Note> getNoteById(String noteId) {
        MediatorLiveData<Note> result = new MediatorLiveData<>();

        LiveData<NoteEntity> source = noteRepository.getNoteById(noteId);

        result.addSource(source, entity -> {
            if (entity == null) return;
            result.setValue(fromEntity(entity));
        });

        return result;
    }

    private final MutableLiveData<List<Long>> originalCategoryIds = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Long>> currentCategoryIds = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<CategoryEntity>> getCategoriesForSelectedNote() {
        return categoriesForSelectedNote;
    }

    private final LiveData<java.util.Map<String, List<CategoryEntity>>> categoriesByNoteId;

    public LiveData<java.util.Map<String, List<CategoryEntity>>> getCategoriesByNoteId() {
        return categoriesByNoteId;
    }


    private Object releaseNoteHeader;

    public void setReleaseNoteHeader(Object releaseNoteHeader) {
        this.releaseNoteHeader = releaseNoteHeader;
        updateCombinedNotes(); // Liste neu aufbauen
    }

    public Object getReleaseNoteHeader() {
        return releaseNoteHeader;
    }

    private NoteEntity toEntity(Note n) {
        return new NoteEntity(
                n.getId(),
                n.getTitle(),
                n.getBody(),
                n.getDate(),
                n.getTime(),
                n.getCreateDate(),
                n.isHide()
        );
    }


    private Note fromEntity(NoteEntity e) {
        return new Note(
                e.title,
                e.body,
                e.notedate,
                e.notetime,
                e.createDate,
                e.hide,
                "",
                e.id
        );
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

        if (isNewEntry(n)) {
            noteRepository.deleteById(n.getId());
            selectedNote.setValue(null);
        } else {
            noteRepository.insert(toEntity(n));
            persistCategoriesForSelectedNote();
            markNoteSavedOnly();
        }
    }

    private void markNoteSavedOnly() {
        Note selected = selectedNote.getValue();
        if (selected != null) {
            originalNote.setValue(new Note(selected));
        }
        isNoteModified.setValue(false);
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

        if (current.isHide() != original.isHide()) {
            return true;
        }
        List<Long> origCats = originalCategoryIds.getValue();
        List<Long> currCats = currentCategoryIds.getValue();

        if (!sameIds(origCats, currCats)) {
            return true;
        }
        return false;
    }
    public NoteViewModel(@NonNull Application application) {
        super(application);

        noteRepository = new NoteRepository(application);
        categoriesByNoteId = Transformations.map(
                noteRepository.getAllActiveNoteCategoryRows(),
                rows -> {
                    java.util.Map<String, List<CategoryEntity>> map = new java.util.HashMap<>();
                    if (rows == null) return map;

                    for (com.git.amarradi.leafpad.model.NoteCategoryRow r : rows) {
                        if (r.noteId == null) continue;

                        List<CategoryEntity> list = map.get(r.noteId);
                        if (list == null) {
                            list = new ArrayList<>();
                            map.put(r.noteId, list);
                        }

                        // CategoryEntity befüllen
                        CategoryEntity c = new CategoryEntity(
                                r.name,
                                r.normalizedName,
                                r.colorHex,
                                r.sortOrder,
                                r.isArchived
                        );
                        c.id = r.categoryId;
                        list.add(c);
                    }
                    return map;
                }
        );

        categoriesForSelectedNote =
                Transformations.switchMap(currentCategoryIds, ids -> {
                    MutableLiveData<List<CategoryEntity>> empty = new MutableLiveData<>();

                    if (ids == null || ids.isEmpty()) {
                        empty.setValue(new ArrayList<>());
                        return empty;
                    }

                    return noteRepository.getCategoriesByIds(ids);
                });

        allNoteEntities = noteRepository.getAllNotes();

        notesLiveData.addSource(allNoteEntities, entities -> {
            rebuildNotesList(entities, showHiddenLiveData.getValue());
        });

        notesLiveData.addSource(showHiddenLiveData, showHidden -> {
            rebuildNotesList(allNoteEntities.getValue(), showHidden);
        });

        combinedNotes.addSource(notesLiveData, notes -> updateCombinedNotes());
        combinedNotes.addSource(releaseNoteLiveData, release -> updateCombinedNotes());

        isNoteModified.addSource(selectedNote, n -> checkModified());
        isNoteModified.addSource(originalNote, n -> checkModified());
        filteredNotes.addSource(notesLiveData, notes -> applySearchQuery());
        filteredNotes.addSource(searchQuery, q -> applySearchQuery());

        isNoteModified.addSource(currentCategoryIds, ids -> checkModified());
        isNoteModified.addSource(originalCategoryIds, ids -> checkModified());

        loadReleaseNote(getApplication().getApplicationContext());
    }

    private void rebuildNotesList(List<NoteEntity> entities, Boolean showHidden) {
        List<Note> notes = new ArrayList<>();

        boolean showOnlyHidden = showHidden != null && showHidden;

        if (entities != null) {
            for (NoteEntity e : entities) {
                if (showOnlyHidden) {
                    if (e.hide) {
                        notes.add(fromEntity(e));
                    }
                } else {
                    if (!e.hide) {
                        notes.add(fromEntity(e));
                    }
                }
            }
        }

        notesLiveData.setValue(notes);
    }

    private void checkModified() {
        Note current = selectedNote.getValue();
        Note original = originalNote.getValue();

        if (original == null || current == null) {
            isNoteModified.setValue(false);
            return;
        }

        boolean contentChanged = !current.equalsContent(original);

        List<Long> origCats = originalCategoryIds.getValue();
        List<Long> currCats = currentCategoryIds.getValue();
        boolean categoriesChanged = !sameIds(origCats, currCats);

        isNoteModified.setValue(contentChanged || categoriesChanged);
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
                    (note.getBody() != null && note.getBody().toLowerCase().contains(lowerQuery))) {
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
            noteRepository.getCategoryIdsForNote(note.getId()).observeForever(ids -> {
                List<Long> safe = ids != null ? new ArrayList<>(ids) : new ArrayList<>();
                originalCategoryIds.postValue(safe);
                currentCategoryIds.postValue(new ArrayList<>(safe));
            });

        }
    }

    public void loadNotes() {
        rebuildNotesList(allNoteEntities.getValue(), showHiddenLiveData.getValue());
    }

    public void loadNoteById(String id) {
        noteRepository.getNoteById(id).observeForever(entity -> {
            if (entity == null) return;

            Note note = fromEntity(entity);
            originalNote.setValue(new Note(note)); // Deep copy
            selectedNote.setValue(note);
        });

        noteRepository.getCategoryIdsForNote(id).observeForever(ids -> {
            List<Long> safe = ids != null ? new ArrayList<>(ids) : new ArrayList<>();
            originalCategoryIds.postValue(safe);
            currentCategoryIds.postValue(new ArrayList<>(safe));
        });
    }

    public LiveData<NoteEntity> getNoteEntityById(String id) {
        return noteRepository.getNoteById(id);
    }

    public Note toNote(NoteEntity e) {
        if (e == null) return null;

        // Passe Feldnamen an dein NoteEntity an!
        return new Note(
                e.title,
                e.body,
                e.notedate,
                e.notetime,
                e.createDate,
                e.hide,
               "",
                e.id
        );
    }



    public void selectNote(Note note) {
        setNote(note);
    }

    public void saveNote(Context context, Note note) {
        noteRepository.insert(toEntity(note));
        persistCategoriesForSelectedNote();
        markSaved();
    }

    public void deleteNote(Context context, Note note) {

        noteRepository.deleteById(note.getId());
        Leafpad.getInstance().setCollapsedNotes(note.getId(), false); // Prefs-Eintrag entfernen
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

    public LiveData<List<Long>> getSelectedCategoryIds() {
        Note note = selectedNote.getValue();
        if (note == null) {
            return new MutableLiveData<>(new ArrayList<>());
        }
        return noteRepository.getCategoryIdsForNote(note.getId());
    }


    public void markSaved() {
        Note selected = selectedNote.getValue();
        if (selected != null) {
            // originalNote ist das Vergleichsobjekt für hasUnsavedChanges
            // Deep copy!
            originalNote.setValue(new Note(selected)); // Nutze einen Copy-Konstruktor oder einen eigenen Clone
        }
        isNoteModified.setValue(false);
        List<Long> currCats = currentCategoryIds.getValue();
        if (currCats != null) {
            originalCategoryIds.setValue(new ArrayList<>(currCats));
        } else {
            originalCategoryIds.setValue(new ArrayList<>());
        }

    }

    public void setCategoriesForSelectedNote(List<Long> categoryKeys) {
        Note note = selectedNote.getValue();
        if (note == null) return;
        String noteId = note.getId();
        //noteRepository.replaceCategoriesForNote(noteId, categoryKeys);
        List<Long> safe = categoryKeys != null ? new ArrayList<>(categoryKeys) : new ArrayList<>();
        currentCategoryIds.setValue(safe);

        checkModified();
    }

    public LiveData<List<CategoryEntity>> getCategoriesForNote(String noteId) {
        return noteRepository.getCategoriesForNote(noteId);
    }

    private boolean sameIds(List<Long> a, List<Long> b) {
        if (a == null) a = new ArrayList<>();
        if (b == null) b = new ArrayList<>();

        if (a.size() != b.size()) {
            return false;
        }

        // Reihenfolge ist egal → Set-Vergleich
        return new java.util.HashSet<>(a).equals(new java.util.HashSet<>(b));
    }

    public void setSelectedNoteCategory(String newCategoryId) {
        @Nullable Note current = selectedNote.getValue();
        if (current == null) {
            return;
        }

        String oldCategoryId = current.getCategory();

        if (isSameCategory(oldCategoryId, newCategoryId)) {
            // keine echte Änderung → nichts tun
            return;
        }

        current.setCategory(newCategoryId);
        selectedNote.setValue(current);
    }

    private boolean isSameCategory(String a, String b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }


    private void persistCategoriesForSelectedNote() {
        Note note = selectedNote.getValue();
        if (note == null) return;

        String noteId = note.getId();
        if (noteId == null || noteId.trim().isEmpty()) {
            return;
        }

        List<Long> cats = currentCategoryIds.getValue();
        if (cats == null) cats = new ArrayList<>();

        noteRepository.replaceCategoriesForNote(noteId, cats);
    }

    public LiveData<List<CategoryEntity>> getCategoriesForNoteId(String noteId) {
        return noteRepository.getCategoriesForNoteId(noteId);
    }


}