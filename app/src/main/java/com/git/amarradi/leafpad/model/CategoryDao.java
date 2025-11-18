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
    long insert(Category category);

    @Update
    int update(Category category);

    @Delete
    int delete(Category category);

    @Query("SELECT * FROM categories WHERE is_archived = 0 ORDER BY sort_order, name")
    LiveData<List<Category>> getActiveCategories();

    @Query("SELECT * FROM categories ORDER BY sort_order, name")
    LiveData<List<Category>> getAllCategories();

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    LiveData<Category> getById(long id);
}
