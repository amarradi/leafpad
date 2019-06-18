package com.schmidtdominik.leafpad;

import java.util.UUID;

public class Note {

    private String title;
    private String body;
    private String id;

    public Note(String title, String body) {
        this.title = title;
        this.body = body;
        this.id = makeId();
    }

    public Note(String title, String body, String id) {
        this.title = title;
        this.body = body;
        this.id = id;
    }

    public static String makeId() {
        return UUID.randomUUID().toString();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
