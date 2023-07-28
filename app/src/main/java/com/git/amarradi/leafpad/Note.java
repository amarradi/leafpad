package com.git.amarradi.leafpad;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class Note {

    private String title;
    private String body;
    private String notedate;
    private String notetime;
    private final String id;

    private final Locale LOCALE = Locale.GERMAN;

   /*public Note(String title, String body) {
        this.title = title;
        this.body = body;
        this.id = makeId();
    }*/

    public Note(String title, String body, String notedate,String notetime, String id) {
        this.title = title;
        this.body = body;
        this.notedate = notedate;
        this.notetime = notetime;
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
        df = new SimpleDateFormat("HH:mm:ss",LOCALE);
        this.notetime = df.format(new Date());
    }
    public String getBody() {
        return body;
    }

    public String getTitle() {
        return title;
    }

    public String getDate() {
        Log.d("getDate", "getDate: "+notedate);
        return notedate;
    }
    public String getTime() {
        return notetime;
    }

    public String getId() {
        return id;
    }

   /* public void setId(String id) {
        this.id = id;
    }*/
}
