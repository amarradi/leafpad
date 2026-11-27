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

    // Kategorien, die oben stehen (Auswahl beim Öffnen + neu angelegte). Wird bewusst
    // nicht bei jedem An-/Abhaken aktualisiert, damit Zeilen nicht unter dem Finger springen.
    private final java.util.Set<Long> topIds = new java.util.HashSet<>();
    private List<CategoryEntity> sourceCategories = new ArrayList<>();

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
        this.sourceCategories = categories != null ? categories : new ArrayList<>();
        applyOrder();
    }

    /** Diese Kategorien oben anzeigen (Reihenfolge innerhalb der Gruppen bleibt). */
    public void setTopIds(java.util.Collection<Long> ids) {
        topIds.clear();
        if (ids != null) topIds.addAll(ids);
        applyOrder();
    }

    /** Kategorie anhaken und nach oben stellen, z. B. direkt nach dem Anlegen. */
    public void selectAndPin(long id) {
        if (!selectedCategoryIds.contains(id)) selectedCategoryIds.add(id);
        topIds.add(id);
        applyOrder();
    }

    private void applyOrder() {
        List<CategoryEntity> top = new ArrayList<>();
        List<CategoryEntity> rest = new ArrayList<>();
        for (CategoryEntity c : sourceCategories) {
            (topIds.contains(c.id) ? top : rest).add(c);
        }
        top.addAll(rest);
        this.categories = top;
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
            holder.materialCheckBox.setVisibility(View.VISIBLE);

            // Ganze Zeile antippbar: schaltet die Checkbox um (vorher reagierte nur
            // die kleine Checkbox). Bearbeiten/Löschen behalten ihre eigenen Klicks.
            holder.card.setClickable(true);
            holder.card.setFocusable(true);
            holder.card.setOnClickListener(v ->
                    holder.materialCheckBox.setChecked(!holder.materialCheckBox.isChecked()));

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

            // Manage-Mode: Checkbox komplett ausblenden, Zeile selbst ohne Funktion
            holder.materialCheckBox.setVisibility(View.GONE);
            holder.card.setOnClickListener(null);
            holder.card.setClickable(false);
            holder.card.setFocusable(false);

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
                androidx.appcompat.R.attr.colorPrimary
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
