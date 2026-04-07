package com.git.amarradi.leafpad.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface NoteDao {
    @Query("SELECT * FROM notes " +
            "ORDER BY " +
            "substr(notedate, 7, 4) || substr(notedate, 4, 2) || substr(notedate, 1, 2) DESC, " +
            "notetime DESC")
        // @Query("SELECT * FROM notes ORDER BY notedate DESC, notetime DESC")
    LiveData<List<NoteEntity>> getAllNotes();

    @Query("SELECT * FROM notes " +
            "WHERE hide = 0 " +
            "ORDER BY " +
            "substr(notedate, 7, 4) || substr(notedate, 4, 2) || substr(notedate, 1, 2) DESC, " +
            "notetime DESC")
        // @Query("SELECT * FROM notes WHERE hide = 0 ORDER BY notedate DESC, notetime DESC")
    LiveData<List<NoteEntity>> getVisibleNotes();

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    NoteEntity getNoteById(String noteId);

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    LiveData<NoteEntity> getById(String id);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NoteEntity note);

    @Update
    void update(NoteEntity note);

    @Delete
    void delete(NoteEntity note);

    @Query("DELETE FROM notes WHERE id = :id")
    void deleteById(String id);

    @Query("SELECT COUNT(*) FROM notes")
    int countAll();

    @Query("SELECT COUNT(*) FROM notes WHERE hide = 0")
    int countVisible();

    @Query("SELECT COUNT(*) FROM notes WHERE hide = 1")
    int countHidden();

    @Query("SELECT * FROM notes")
    List<NoteEntity> getAllNotesForBackup();

    @Query("DELETE FROM notes")
    void deleteAll();


    @Query("DELETE FROM notes")
    void deleteAllNotes();

    @Query("SELECT COUNT(*) FROM notes")
    int countAllNotes();


}