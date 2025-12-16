package com.git.amarradi.leafpad.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NoteCategoryDao {

    // -------------------------------------------------
    // INSERT
    // -------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(NoteCategoryJoin join);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<NoteCategoryJoin> joins);

    // -------------------------------------------------
    // DELETE
    // -------------------------------------------------

    @Query("DELETE FROM note_category_join WHERE note_id = :noteId")
    void deleteAllForNote(String noteId);

    @Query("""
        DELETE FROM note_category_join
        WHERE note_id = :noteId
          AND category_id = :categoryId
    """)
    void deleteSingle(String noteId, long categoryId);

    // -------------------------------------------------
    // QUERY
    // -------------------------------------------------

    @Query("""
        SELECT category_id
        FROM note_category_join
        WHERE note_id = :noteId
    """)
    LiveData<List<Long>> getCategoryIdsForNote(String noteId);

    @Query("""
        SELECT note_id
        FROM note_category_join
        WHERE category_id = :categoryId
    """)
    List<String> getNoteIdsForCategory(long categoryId);
}
