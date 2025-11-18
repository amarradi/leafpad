package com.git.amarradi.leafpad.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "categories",
        indices = {
                @Index(value = {"name"}, unique = true)
        }
)
public class Category {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @NonNull
    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "color_hex")
    public String colorHex;

    @ColumnInfo(name = "sort_order")
    public int sortOrder;

    @ColumnInfo(name = "is_archived")
    public boolean isArchived;

    @ColumnInfo(name = "created_at")
    public Long createdAt;

    @ColumnInfo(name = "updated_at")
    public Long updatedAt;

    public Category(@NonNull String name) {
        this.name = name;
        this.colorHex = null;
        this.sortOrder = 0;
        this.isArchived = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }
}
