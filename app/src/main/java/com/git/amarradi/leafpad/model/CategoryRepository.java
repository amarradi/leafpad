package com.git.amarradi.leafpad.model;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryRepository {

    private final CategoryDao categoryDao;
    private final LiveData<List<Category>> activeCategories;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CategoryRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        categoryDao = db.categoryDao();
        activeCategories = categoryDao.getActiveCategories();
    }

    public LiveData<List<Category>> getActiveCategories() {
        return activeCategories;
    }

    public void insert(final Category category) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                categoryDao.insert(category);
            }
        });
    }

    public void update(final Category category) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                categoryDao.update(category);
            }
        });
    }

    public void delete(final Category category) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                categoryDao.delete(category);
            }
        });
    }
}
