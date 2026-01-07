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

    /**
     * UI kann das Result beobachten und z.B. Toast/Snackbar zeigen.
     */
    public LiveData<CategoryRepository.WriteResult> createCategory(String name, String colorHex) {
        return repository.insertSafe(name, colorHex, 0, false);
    }

    /**
     * Update mit separaten Feldern ist sicherer als "updateCategory(CategoryEntity)".
     * So erzwingen wir Normalisierung + Dublettencheck zentral.
     */
    public LiveData<CategoryRepository.WriteResult> updateCategory(CategoryEntity original,
                                                                   String newName,
                                                                   String newColorHex) {
        return repository.updateSafe(original, newName, newColorHex);
    }
}
