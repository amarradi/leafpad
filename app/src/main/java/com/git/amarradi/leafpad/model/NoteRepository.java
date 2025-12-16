package com.git.amarradi.leafpad.model;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteRepository {

    private final NoteDao noteDao;

    private final NoteCategoryDao noteCategoryDao;
    private final LiveData<List<NoteEntity>> allNotes;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public NoteRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        noteDao = db.noteDao();
        noteCategoryDao = db.noteCategoryDao();
        allNotes = noteDao.getAllNotes();
    }

    public void addCategoryToNote(String noteId, long categoryId) {
        Executors.newSingleThreadExecutor().execute(() -> {
        noteCategoryDao.insert(
                new NoteCategoryJoin(
                        noteId,
                        categoryId,
                        System.currentTimeMillis()));
        });

    }

    public void removeCategoryFromNote(String noteId, long categoryId) {
        Executors.newSingleThreadExecutor().execute(() ->
                noteCategoryDao.deleteSingle(noteId, categoryId)
        );
    }


    public LiveData<List<NoteEntity>> getAllNotes() {
        return allNotes;
    }

    public LiveData<NoteEntity> getNoteById(String id) {
        return noteDao.getById(id);
    }


    public void insert(NoteEntity note) {
        dbExecutor.execute(() -> noteDao.insert(note));
    }

    public void update(NoteEntity note) {
        dbExecutor.execute(() -> noteDao.update(note));
    }

    public void delete(NoteEntity note) {
        dbExecutor.execute(() -> noteDao.delete(note));
    }
    public void deleteById(String id) {
        dbExecutor.execute(() -> noteDao.deleteById(id));
    }

    public void replaceCategoriesForNote(String noteId, List<Long> categoryIds) {
        dbExecutor.execute(()->{
            noteCategoryDao.deleteAllForNote(noteId);

            long now = System.currentTimeMillis();
            for (long categoryId : categoryIds) {
                noteCategoryDao.insert(
                        new NoteCategoryJoin(
                                noteId,
                                categoryId,
                                now
                        )
                );
            }

        });
    }
}
