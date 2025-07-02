package com.git.amarradi.leafpad.helper;

import android.content.Context;
import android.content.Intent;

import com.git.amarradi.leafpad.R;
import com.git.amarradi.leafpad.model.Note;

public class ShareHelper {

    public static void shareNote(Context context, Note note) {
        if (note == null) return;

        String content = context.getString(R.string.title_hint) + ": " + note.getTitle()
                + "\n\n" + context.getString(R.string.body_hint) + ": " + note.getBody();

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, content);
        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_note)));
    }
}
