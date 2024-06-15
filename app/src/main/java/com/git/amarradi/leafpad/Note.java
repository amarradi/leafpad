package com.git.amarradi.leafpad;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class Note {

    private String title;
    private String body;
    private String notedate;
    private String notetime;
    private String create_date;
    private final String id;
    private boolean hide;

    private final Locale LOCALE = Locale.GERMAN;


    public Note(String title, String body, String notedate, String notetime, String create_date, boolean hide, String id) {
        this.title = title;
        this.body = body;
        this.notedate = notedate;
        this.notetime = notetime;
        this.create_date = create_date;
        this.hide = hide;
        this.id = id;

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

}
