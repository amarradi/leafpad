package com.git.amarradi.leafpad.model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NoteCategoryDao {

    // ------------------------------------
    // Zuweisung
    // ------------------------------------
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(NoteCategoryJoin join);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<NoteCategoryJoin> joins);

    // ------------------------------------
    // Löschen
    // ------------------------------------
    @Query("DELETE FROM note_categories WHERE note_id = :noteId")
    void deleteAllForNote(String noteId);

    @Query("""
        DELETE FROM note_categories 
        WHERE note_id = :noteId AND category_key = :categoryKey
    """)
    void deleteSingle(String noteId, String categoryKey);

    // ------------------------------------
    // Abfragen
    // ------------------------------------
    @Query("""
        SELECT category_key 
        FROM note_categories 
        WHERE note_id = :noteId
    """)
    List<String> getCategoryKeysForNote(String noteId);

    @Query("""
        SELECT note_id 
        FROM note_categories 
        WHERE category_key = :categoryKey
    """)
    List<String> getNoteIdsForCategory(String categoryKey);
}
