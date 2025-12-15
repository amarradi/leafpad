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


    // ---------------------------------------------------
    // Konstruktor
    // ---------------------------------------------------
    public NoteEntity(@NonNull String id,
                      String title,
                      String body,
                      String notedate,
                      String notetime,
                      String createDate,
                      boolean hide) {

        this.id = id;
        this.title = title;
        this.body = body;
        this.notedate = notedate;
        this.notetime = notetime;
        this.createDate = createDate;
        this.hide = hide;
    }
}