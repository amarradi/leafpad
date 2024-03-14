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
    private String create_time;
    private final String id;

    private final Locale LOCALE = Locale.GERMAN;

   /*public Note(String title, String body) {
        this.title = title;
        this.body = body;
        this.id = makeId();
    }*/

    public Note(String title, String body, String notedate, String notetime, String create_date, String create_time, String id) {
        this.title = title;
        this.body = body;
        this.notedate = notedate;
        this.notetime = notetime;
        this.create_date = create_date;
        this.create_time = create_time;
        this.id = id;

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

    public void setCreateTime() {
        SimpleDateFormat df;
        df = new SimpleDateFormat("HH:mm", LOCALE);
        this.create_time = df.format(new Date());
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

    public String getCreateTime() {
        return create_time;
    }


    public String getId() {
        return id;
    }

   /* public void setId(String id) {
        this.id = id;
    }*/
}
