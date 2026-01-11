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
public interface CategoryDao {

    @Query("SELECT COUNT(*) FROM categories WHERE normalized_name = :normalized LIMIT 1")
    int countByNormalized(String normalized);

    @Query("SELECT id FROM categories WHERE normalized_name = :normalized LIMIT 1")
    Long findIdByNormalized(String normalized);


    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(CategoryEntity category);

    @Update
    int update(CategoryEntity category);

    @Delete
    int delete(CategoryEntity category);

    @Query("SELECT * FROM categories WHERE is_archived = 0 ORDER BY sort_order, name")
    LiveData<List<CategoryEntity>> getActiveCategories();

    @Query("SELECT * FROM categories ORDER BY sort_order, name")
    LiveData<List<CategoryEntity>> getAllCategories();

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    LiveData<CategoryEntity> getById(long id);

    @Query("SELECT * FROM categories WHERE name = :name COLLATE NOCASE LIMIT 1")
    LiveData<CategoryEntity> getByName(String name);

    @Query("""
    SELECT c.*
    FROM categories c
    INNER JOIN note_category_join nc
        ON c.id = nc.category_id
    WHERE nc.note_id = :noteId
    ORDER BY c.sort_order, c.name
""")
    LiveData<List<CategoryEntity>> getCategoriesForNote(String noteId);

    @Query("SELECT * FROM categories WHERE normalized_name = :normalized LIMIT 1")
    LiveData<CategoryEntity> getByNormalized(String normalized);

    @Query("""
                SELECT DISTINCT
                    nc.note_id AS noteId,
                    c.id AS categoryId,
                    c.name AS name,
                    c.normalized_name AS normalizedName,
                    c.color_hex AS colorHex,
                    c.sort_order AS sortOrder,
                    c.is_archived AS isArchived
                FROM note_category_join nc
                INNER JOIN categories c ON c.id = nc.category_id
                WHERE c.is_archived = 0
                ORDER BY nc.note_id, c.sort_order, c.name
            """)
    LiveData<List<NoteCategoryRow>> getAllActiveNoteCategoryRows();




}
