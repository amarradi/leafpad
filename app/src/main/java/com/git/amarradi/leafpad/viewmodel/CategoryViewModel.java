package com.git.amarradi.leafpad.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.git.amarradi.leafpad.model.CategoryEntity;
import com.git.amarradi.leafpad.model.CategoryRepository;

import java.util.List;

public class CategoryViewModel extends AndroidViewModel {

    private final CategoryRepository repository;
    private final LiveData<List<CategoryEntity>> categories;

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        repository = new CategoryRepository(application);
        categories = repository.getActiveCategories();
    }

    public LiveData<List<CategoryEntity>> getCategories() {
        return categories;
    }

    public void deleteCategory(CategoryEntity category) {
        repository.delete(category);
    }

    public void createCategory(String name, String colorHex) {
        if (name == null || name.trim().isEmpty()) return;

        CategoryEntity category = new CategoryEntity(
                name.trim(),
                colorHex,
                0,
                false
        );

        repository.insert(category);
    }

    public void updateCategory(CategoryEntity category) {
        repository.update(category);

    }




}
