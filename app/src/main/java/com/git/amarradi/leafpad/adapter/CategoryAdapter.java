package com.git.amarradi.leafpad.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.git.amarradi.leafpad.R;
import com.git.amarradi.leafpad.model.CategoryEntity;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<CategoryEntity> categories = new ArrayList<>();

    private final List<Long> selectedCategoryIds = new ArrayList<>();



    public void setCategories(List<CategoryEntity> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    public void setSelectedCategoryIds(List<Long> ids) {
        selectedCategoryIds.clear();
        if (ids != null) {
            selectedCategoryIds.addAll(ids);
        }
        notifyDataSetChanged();
    }


    public List<Long> getSelectedCategoryIds() {
        return new ArrayList<>(selectedCategoryIds);
    }


    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.note_category_list_item, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryEntity category = categories.get(position);
        holder.nameText.setText(category.name);

        boolean checked = selectedCategoryIds.contains(category.id);
        holder.materialCheckBox.setChecked(checked);

        holder.materialCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedCategoryIds.contains(category.id)) {
                    selectedCategoryIds.add(category.id);
                }
            } else {
                selectedCategoryIds.remove(category.id);
            }
        });
    }


    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {

        TextView nameText;
        MaterialCheckBox materialCheckBox;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.category_name);
            materialCheckBox = itemView.findViewById(R.id.category_checkbox);
        }
    }
}
