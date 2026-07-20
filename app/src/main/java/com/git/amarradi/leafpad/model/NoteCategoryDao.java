package com.git.amarradi.leafpad.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

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

    @Query("DELETE FROM note_category_join WHERE category_id = :categoryId")
    void deleteAllForCategory(long categoryId);


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

    @Query(
            "SELECT c.* " +
                    "FROM categories c " +
                    "INNER JOIN note_category_join j ON c.id = j.category_id " +
                    "WHERE j.note_id = :noteId " +
                    "ORDER BY c.sort_order, c.name"
    )
    LiveData<List<CategoryEntity>> getCategoriesForNote(String noteId);

    @Transaction
    default void replaceForNote(String noteId, List<Long> categoryIds) {
        deleteAllForNote(noteId);
        int count = (categoryIds == null) ? 0 : categoryIds.size();
        android.util.Log.d("JOIN", "replaceForNote noteId=" + noteId + " count=" + count);
        if (categoryIds == null) return;

        for (Long cid : categoryIds) {
            if (cid == null) continue;
            insert(new NoteCategoryJoin(noteId, cid));
        }
    }


    @Query("SELECT * FROM categories WHERE id IN (:ids) AND is_archived = 0 ORDER BY sort_order, name")
    LiveData<List<CategoryEntity>> getCategoriesByIds(List<Long> ids);

    @Query("SELECT * FROM note_category_join")
    List<NoteCategoryJoin> getAllJoinsForBackup();
}
