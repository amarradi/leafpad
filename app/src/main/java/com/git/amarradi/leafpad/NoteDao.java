package com.git.amarradi.leafpad;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NoteDao {
    @Query("SELECT * FROM Note")
    List<Note> getAll();
    @Query("SELECT * FROM Note WHERE id= :id")
    Note getById(String id);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Note note);
    @Delete
    void delete(Note note);
    @Query("DELETE FROM Note")
    void deleteAll();
}
