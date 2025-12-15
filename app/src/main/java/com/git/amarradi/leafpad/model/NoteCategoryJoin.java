package com.git.amarradi.leafpad.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(
        tableName = "note_categories",
        primaryKeys = {"note_id", "category_key"}
)
public class NoteCategoryJoin {

    @NonNull
    @ColumnInfo(name = "note_id")
    public String noteId;

    @NonNull
    @ColumnInfo(name = "category_key")
    public String categoryKey;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    public NoteCategoryJoin(@NonNull String noteId,
                            @NonNull String categoryKey,
                            long createdAt) {
        this.noteId = noteId;
        this.categoryKey = categoryKey;
        this.createdAt = createdAt;
    }
}
