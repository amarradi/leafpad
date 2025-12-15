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
}
