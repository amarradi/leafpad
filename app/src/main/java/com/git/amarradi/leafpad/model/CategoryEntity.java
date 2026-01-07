package com.git.amarradi.leafpad.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.git.amarradi.leafpad.helper.CategoryNameNormalizer;

@Entity(
        tableName = "categories",
        indices = {
                @Index(value = {"normalized_name"}, unique = true)
        }
)
public class CategoryEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name;

    @NonNull
    @ColumnInfo(name = "normalized_name")
    public String normalizedName;

    @ColumnInfo(name = "color_hex")
    public String colorHex;

    @ColumnInfo(name = "sort_order")
    public int sortOrder;

    @ColumnInfo(name = "is_archived")
    public boolean isArchived;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;


    /** ✅ DER Konstruktor, den Room benutzen soll */
    public CategoryEntity(
            @NonNull String name,
            @NonNull String normalizedName,
            @NonNull String colorHex,
            int sortOrder,
            boolean isArchived
    ) {
        this.name = name;
        this.normalizedName = normalizedName;
        // defensive default
        if (colorHex == null || colorHex.trim().isEmpty()) {
            this.colorHex = "#CCCCCC";
        } else {
            this.colorHex = colorHex;
        }
        this.sortOrder = sortOrder;
        this.isArchived = isArchived;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** ❌ Convenience-Konstruktor → NICHT für Room */
    @Ignore
    public CategoryEntity(@NonNull String name) {
        this(
                name,
                CategoryNameNormalizer.normalize(name),
                "#CCCCCC",
                0,
                false);
    }

    @Ignore
    public CategoryEntity copyForUpdate(String newName, String newColorHex) {
        String newNormalizedName = CategoryNameNormalizer.normalize(newName);
        CategoryEntity c = new CategoryEntity(
                newName,
                newNormalizedName,
                newColorHex,
                this.sortOrder,
                this.isArchived
        );
        c.id = this.id;
        c.createdAt = this.createdAt;
        c.updatedAt = System.currentTimeMillis();
        return c;
    }

}
