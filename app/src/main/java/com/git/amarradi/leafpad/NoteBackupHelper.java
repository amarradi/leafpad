package com.git.amarradi.leafpad;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class NoteBackupHelper {

    public static final String BASE_NAME = "leafpad_";

    public static String generateTimestamp() {

        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("dd.MM.yyyy_HH:mm", java.util.Locale.getDefault());
        return formatter.format(new java.util.Date());
    }


    public static void backupNotesToStream(Context context, OutputStream outputStream) throws Exception {
        List<Note> notes = Leaf.loadAll(context, true);
        XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
        StringWriter writer = new StringWriter();

        serializer.setOutput(writer);
        serializer.startDocument("UTF-8", true);
        serializer.startTag("", "notes");

        for (Note note : notes) {
            serializer.startTag("", "note");

            serializer.startTag("", "id").text(note.getId()).endTag("", "id");
            serializer.startTag("", "title").text(note.getTitle()).endTag("", "title");
            serializer.startTag("", "body").text(note.getBody()).endTag("", "body");
            serializer.startTag("", "date").text(note.getDate()).endTag("", "date");
            serializer.startTag("", "time").text(note.getTime()).endTag("", "time");
            serializer.startTag("", "created").text(note.getCreateDate()).endTag("", "created");
            serializer.startTag("", "hide").text(Boolean.toString(note.isHide())).endTag("", "hide");
            serializer.startTag("", "category").text(note.getCategory()).endTag("", "category");
            serializer.endTag("", "note");
        }

        serializer.endTag("", "notes");
        serializer.endDocument();

        outputStream.write(writer.toString().getBytes());
    }

    public static boolean isValidLeafpadBackup(InputStream inputStream) {
        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(inputStream, "UTF-8");

            boolean foundNotesRoot = false;
            boolean validStructure = false;
            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();

                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("notes".equals(tagName)) {
                            foundNotesRoot = true;
                        }
                        if ("note".equals(tagName)) {
                            boolean hasId = false;
                            boolean hasTitle = false;
                            boolean hasBody = false;
                            boolean hasDate = false;
                            boolean hasTime = false;
                            boolean hasHide = false;
                            boolean hasCategory = false;

                            int depth = parser.getDepth();
                            while (!(eventType == XmlPullParser.END_TAG && parser.getDepth() == depth && "note".equals(parser.getName()))) {
                                eventType = parser.next();
                                if (eventType == XmlPullParser.START_TAG) {
                                    switch (parser.getName()) {
                                        case "id" -> hasId = true;
                                        case "title" -> hasTitle = true;
                                        case "body" -> hasBody = true;
                                        case "date" -> hasDate = true;
                                        case "time" -> hasTime = true;
                                        case "hide" -> hasHide = true;
                                        case "category" -> hasCategory = true;
                                    }
                                }
                            }

                            if (hasId && hasTitle && hasBody && hasDate && hasTime && hasHide && hasCategory) {
                                validStructure = true;
                            } else {
                                return false;
                            }
                        }
                        break;
                }

                eventType = parser.next();
            }

            return foundNotesRoot && validStructure;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public static List<Note> restoreNotesFromStream(Context context, InputStream inputStream) throws Exception {
        List<Note> notes = new ArrayList<>();
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setInput(inputStream, "UTF-8");

        int eventType = parser.getEventType();
        Note note = null;
        String text = "";

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = parser.getName();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if ("note".equals(tagName)) {
                        note = new Note("", "", "", "", "", false, "", "");
                    }
                    break;
                case XmlPullParser.TEXT:
                    text = parser.getText();
                    break;
                case XmlPullParser.END_TAG:
                    if (note != null) {
                        switch (tagName) {
                            case "id" -> note.setId(text);
                            case "title" -> note.setTitle(text);
                            case "body" -> note.setBody(text);
                            case "date" -> note.setNotedate(text);
                            case "time" -> note.setNotetime(text);
                            case "created" -> note.setCreateDate();
                            case "hide" -> note.setHide(Boolean.parseBoolean(text));
                            //case "category" -> note.setCategory((text != null && !text.trim().equalsIgnoreCase("false")) ? text : "");
                            case "category" -> note.setCategory(
                                    text != null && !text.equalsIgnoreCase("true") && !text.equalsIgnoreCase("false") ? text : ""
                            );

                            case "note" -> notes.add(note);
                        }
                    }
                    break;
            }
            eventType = parser.next();
        }
        for (Note n : notes) {
            Leaf.set(context, n);
        }
        return notes;
    }
}
