package com.git.amarradi.leafpad.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.git.amarradi.leafpad.R;
import com.git.amarradi.leafpad.helper.ColorUtilsHelper;
import com.git.amarradi.leafpad.model.CategoryEntity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<CategoryEntity> categories = new ArrayList<>();

    private final List<Long> selectedCategoryIds = new ArrayList<>();

    private boolean selectionEnabled = true;

    private MaterialCardView card;


    public interface Listener {
        void onEditCategory(CategoryEntity category);

        void onDeleteCategory(CategoryEntity category);
    }

    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }


    public void setSelectionEnabled(boolean enabled) {
        selectionEnabled = enabled;
        notifyDataSetChanged();
    }
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
        final CategoryEntity category = categories.get(position);
        holder.nameText.setText(category.name);
        applyCategoryColors(
                holder.card,
                holder.nameText,
                holder.editButton,
                holder.deleteButton,
                category
        );
        holder.materialCheckBox.setOnCheckedChangeListener(null);
        boolean checked = selectedCategoryIds.contains(category.id);
        holder.materialCheckBox.setChecked(checked);
        if (selectionEnabled) {
            holder.materialCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedCategoryIds.contains(category.id)) {
                        selectedCategoryIds.add(category.id);
                    }
                } else {
                    selectedCategoryIds.remove(category.id);
                }
            });
        } else {

            // Manage-Mode: Checkbox komplett ausblenden
            holder.materialCheckBox.setVisibility(View.GONE);

            // sicherheitshalber kein Haken "hängen lassen" (recycling)
            holder.materialCheckBox.setChecked(false);
            holder.materialCheckBox.setOnCheckedChangeListener(null);
        }


        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditCategory(category);
            }
        });
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteCategory(category);
            }
        });

    }

    private void applyCategoryColors(
            MaterialCardView card,
            MaterialTextView nameText,
            ImageButton btnEdit,
            ImageButton btnDelete,
            CategoryEntity category
    ) {
        if (category == null) return;

        int baseColor = ColorUtilsHelper.parseCategoryColor(category.colorHex);

        int bgColor = ColorUtilsHelper.getCategoryBackgroundColor(baseColor);

        card.setStrokeWidth(ColorUtilsHelper.dpToPx(card.getContext(), 2));
        card.setStrokeColor(baseColor);
        card.setCardBackgroundColor(bgColor);

        int primaryColor = MaterialColors.getColor(
                card,
                com.google.android.material.R.attr.colorPrimary
        );

        nameText.setTextColor(primaryColor);
        btnEdit.setColorFilter(primaryColor);
        btnDelete.setColorFilter(primaryColor);
    }


    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {

        MaterialTextView nameText;
        MaterialCheckBox materialCheckBox;
        ImageButton editButton;
        ImageButton deleteButton;
        MaterialCardView card;



        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.category_name);
            materialCheckBox = itemView.findViewById(R.id.category_checkbox);
            editButton = itemView.findViewById(R.id.btnEdit);
            deleteButton = itemView.findViewById(R.id.btnDelete);
            card = itemView.findViewById(R.id.category_card);

        }
    }
}
