package com.git.amarradi.leafpad.model;

import static androidx.room.ForeignKey.CASCADE;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
        tableName = "note_category_join",
        primaryKeys = {"note_id", "category_id"},
        foreignKeys = {
                @ForeignKey(
                        entity = NoteEntity.class,
                        parentColumns = "id",
                        childColumns = "note_id",
                        onDelete = CASCADE
                ),
                @ForeignKey(
                        entity = CategoryEntity.class,
                        parentColumns = "id",
                        childColumns = "category_id",
                        onDelete = CASCADE
                )
        },
        indices = {
                @Index("note_id"),
                @Index("category_id")
        }
)
public class NoteCategoryJoin {

    @NonNull
    @ColumnInfo(name = "note_id")
    public String noteId;

    @ColumnInfo(name = "category_id")
    public long categoryId;

    public NoteCategoryJoin(@NonNull String noteId,
                            long categoryId)
    {
        this.noteId = noteId;
        this.categoryId = categoryId;
    }
}
