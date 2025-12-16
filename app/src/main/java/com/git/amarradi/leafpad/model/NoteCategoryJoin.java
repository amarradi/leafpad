package com.git.amarradi.leafpad.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(
        tableName = "note_category_join",
        primaryKeys = {"note_id", "category_id"}
)
public class NoteCategoryJoin {

    @NonNull
    @ColumnInfo(name = "note_id")
    public String noteId;

    @ColumnInfo(name = "category_id")
    public long categoryId;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    public NoteCategoryJoin(@NonNull String noteId,
                            long categoryId,
                            long createdAt)
    {
        this.noteId = noteId;
        this.categoryId = categoryId;
        this.createdAt = createdAt;
    }
}
