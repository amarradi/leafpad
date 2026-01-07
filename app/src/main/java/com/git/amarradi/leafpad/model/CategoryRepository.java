package com.git.amarradi.leafpad.model;

import android.app.Application;
import android.database.sqlite.SQLiteConstraintException;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.git.amarradi.leafpad.helper.CategoryNameNormalizer;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryRepository {

    public enum WriteResult {
        OK,
        EMPTY_NAME,
        DUPLICATE,
        DB_ERROR
    }

    private final CategoryDao categoryDao;
    private final LiveData<List<CategoryEntity>> activeCategories;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CategoryRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        categoryDao = db.categoryDao();
        activeCategories = categoryDao.getActiveCategories();
    }

    public LiveData<List<CategoryEntity>> getActiveCategories() {
        return activeCategories;
    }

    public LiveData<WriteResult> insertSafe(final String rawName,
                                            final String colorHex,
                                            final int sortOrder,
                                            final boolean isArchived) {

        final MutableLiveData<WriteResult> result = new MutableLiveData<>();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                String name = rawName == null ? "" : rawName.trim();
                if (name.isEmpty()) {
                    result.postValue(WriteResult.EMPTY_NAME);
                    return;
                }

                String normalized = CategoryNameNormalizer.normalize(name);

                // UX-Check
                if (categoryDao.countByNormalized(normalized) > 0) {
                    result.postValue(WriteResult.DUPLICATE);
                    return;
                }

                try {
                    CategoryEntity category = new CategoryEntity(
                            name,
                            normalized,
                            colorHex,
                            sortOrder,
                            isArchived
                    );
                    categoryDao.insert(category);
                    result.postValue(WriteResult.OK);
                } catch (SQLiteConstraintException e) {
                    // DB bleibt letzte Instanz (Race-Condition etc.)
                    result.postValue(WriteResult.DUPLICATE);
                } catch (Exception e) {
                    result.postValue(WriteResult.DB_ERROR);
                }
            }
        });

        return result;
    }

    public LiveData<WriteResult> updateSafe(final CategoryEntity original,
                                            final String rawNewName,
                                            final String newColorHex) {

        final MutableLiveData<WriteResult> result = new MutableLiveData<>();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (original == null) {
                    result.postValue(WriteResult.DB_ERROR);
                    return;
                }

                String newName = rawNewName == null ? "" : rawNewName.trim();
                if (newName.isEmpty()) {
                    result.postValue(WriteResult.EMPTY_NAME);
                    return;
                }

                String normalized = CategoryNameNormalizer.normalize(newName);

                Long existingId = categoryDao.findIdByNormalized(normalized);
                if (existingId != null && existingId.longValue() != original.id) {
                    result.postValue(WriteResult.DUPLICATE);
                    return;
                }

                try {
                    // WICHTIG: copyForUpdate muss normalizedName setzen (wie wir besprochen haben)
                    CategoryEntity updated = original.copyForUpdate(newName, newColorHex);
                    categoryDao.update(updated);
                    result.postValue(WriteResult.OK);
                } catch (SQLiteConstraintException e) {
                    result.postValue(WriteResult.DUPLICATE);
                } catch (Exception e) {
                    result.postValue(WriteResult.DB_ERROR);
                }
            }
        });

        return result;
    }

    public void delete(final CategoryEntity category) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                categoryDao.delete(category);
            }
        });
    }
}
