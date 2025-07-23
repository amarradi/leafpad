package com.git.amarradi.leafpad.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class Note implements Parcelable {

    private String title;
    private String body;
    private String notedate;
    private String notetime;
    private String create_date;
    private String id;
    private boolean hide;
    private String category;

    private final Locale LOCALE = Locale.GERMAN;

    public Note(Note other) {
        this.title = other.title;
        this.body = other.body;
        this.notedate = other.notedate;
        this.notetime = other.notetime;
        this.create_date = other.create_date;
        this.id = other.id;
        this.hide = other.hide;
        this.category = other.category;
    }

    public Note(String title, String body, String notedate, String notetime, String create_date, boolean hide,String category, String id) {
        this.title = title;
        this.body = body;
        this.notedate = notedate;
        this.notetime = notetime;
        this.create_date = create_date;
        this.hide = hide;
        this.category = category;
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isHide() {
        return hide;
    }

    public void setHide(boolean hide) {
        this.hide = hide;
    }

    public static String makeId() {
        return UUID.randomUUID().toString();
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setNotedate() {
        SimpleDateFormat df;
        df = new SimpleDateFormat("dd.MM.yyyy", LOCALE);
        this.notedate = df.format(new Date());
    }

    public void setNotetime() {
        SimpleDateFormat df;
        df = new SimpleDateFormat("HH:mm",LOCALE);
        this.notetime = df.format(new Date());
    }

    public void setNotedate(String notedate) {
        this.notedate = notedate;
    }

    public void setNotetime(String notetime) {
        this.notetime = notetime;
    }

    public void setCreateDate(){
        SimpleDateFormat df;
        df = new SimpleDateFormat("dd.MM.yyyy", LOCALE);
        this.create_date = df.format(new Date());
    }

    public String getBody() {
        return body;
    }

    public String getTitle() {
        return title;
    }

    public String getDate() {
        return notedate;
    }

    public String getTime() {
        return notetime;
    }

    public String getCreateDate() {
        return create_date;
    }

    public String getId() {
        return id;
    }

    public boolean equalsContent(Note other) {
        if (other == null) return false;
        return safeEquals(title, other.title) &&
                safeEquals(body, other.body) &&
                safeEquals(category, other.category) &&
                hide == other.hide;
    }

    private boolean safeEquals(String a, String b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    protected Note(android.os.Parcel in) {
        title = in.readString();
        body = in.readString();
        notedate = in.readString();
        notetime = in.readString();
        create_date = in.readString();
        id = in.readString();
        hide = in.readByte() != 0;
        category = in.readString();
    }

    public static final Creator<Note> CREATOR = new Creator<Note>() {
        @Override
        public Note createFromParcel(android.os.Parcel in) {
            return new Note(in);
        }

        @Override
        public Note[] newArray(int size) {
            return new Note[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(android.os.Parcel parcel, int flags) {
        parcel.writeString(title);
        parcel.writeString(body);
        parcel.writeString(notedate);
        parcel.writeString(notetime);
        parcel.writeString(create_date);
        parcel.writeString(id);
        parcel.writeByte((byte) (hide ? 1 : 0));
        parcel.writeString(category);
    }

}
