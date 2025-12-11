package com.git.amarradi.leafpad.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class NoteEntity {

    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "body")
    public String body;

    @ColumnInfo(name = "notedate")
    public String notedate;

    @ColumnInfo(name = "notetime")
    public String notetime;

    @ColumnInfo(name = "create_date")
    public String createDate;

    @ColumnInfo(name = "hide")
    public boolean hide;

    @ColumnInfo(name = "category_key")
    public String categoryKey;

    // ---------------------------------------------------
    // Konstruktor
    // ---------------------------------------------------
    public NoteEntity(@NonNull String id,
                      String title,
                      String body,
                      String notedate,
                      String notetime,
                      String createDate,
                      boolean hide,
                      String categoryKey) {

        this.id = id;
        this.title = title;
        this.body = body;
        this.notedate = notedate;
        this.notetime = notetime;
        this.createDate = createDate;
        this.hide = hide;
        this.categoryKey = categoryKey;
    }

    // ---------------------------------------------------
    // Getter / Setter
    // ---------------------------------------------------

    @NonNull
    public String getId() { return id; }

    public void setId(@NonNull String id) { this.id = id; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }

    public void setBody(String body) { this.body = body; }

    public String getNotedate() { return notedate; }

    public void setNotedate(String notedate) { this.notedate = notedate; }

    public String getNotetime() { return notetime; }

    public void setNotetime(String notetime) { this.notetime = notetime; }

    public String getCreateDate() { return createDate; }

    public void setCreateDate(String createDate) { this.createDate = createDate; }

    public boolean isHide() { return hide; }

    public void setHide(boolean hide) { this.hide = hide; }

    public String getCategoryKey() { return categoryKey; }

    public void setCategoryKey(String categoryKey) { this.categoryKey = categoryKey; }
}
