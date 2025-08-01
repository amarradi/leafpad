package com.git.amarradi.leafpad.helper;

import android.content.Context;
import android.util.Xml;

import com.git.amarradi.leafpad.model.ReleaseNote;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;

public class ReleaseNoteHelper {

    public static ReleaseNote loadReleaseNote(Context context) {
        try (InputStream is = context.getAssets().open("releasenotes.xml")) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, null);

            String title = null, content = null, date = null, time = null;
            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tag = parser.getName();
                    if ("title".equals(tag)) {
                        title = parser.nextText().trim();
                    } else if ("content".equals(tag)) {
                        content = parser.nextText().trim();
                    } else if ("date".equals(tag)) {
                        date = parser.nextText().trim();
                    } else if ("time".equals(tag)) {
                        time = parser.nextText().trim();
                    }
                }
                eventType = parser.next();
            }
            if (title != null && content != null && date != null && time != null) {
                return new ReleaseNote(title, content, date, time);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
