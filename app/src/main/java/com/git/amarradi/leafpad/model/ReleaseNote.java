package com.git.amarradi.leafpad.model;

public class ReleaseNote {
    private final String title;
    private final String content;
    private final String date;
    private final String time;

    public ReleaseNote(String title, String content, String date, String time) {
        this.title = title;
        this.content = content;
        this.date = date;
        this.time = time;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getDate() { return date; }
    public String getTime() { return time; }
}
