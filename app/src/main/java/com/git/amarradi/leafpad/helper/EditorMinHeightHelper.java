package com.git.amarradi.leafpad.helper;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;

public class EditorMinHeightHelper {

    /**
     * Setzt das EditText so, dass es immer bis zum unteren Bildschirmrand reicht.
     * @param rootView z.B. das CoordinatorLayout (R.id.all)
     * @param toolbarView Toolbar (R.id.toolbar)
     * @param titleView Titelfeld/TextInputLayout (R.id.default_text_input_layout), ggf. null
     * @param bodyEdit Das eigentliche EditText (R.id.body_edit)
     */
    public static void adjustMinHeight(final View rootView, final View toolbarView, final View titleView, final EditText bodyEdit) {
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);

                // Bestimme Startpunkt für das Body-EditText
                int[] bodyPos = new int[2];
                bodyEdit.getLocationOnScreen(bodyPos);
                int topOfBody = bodyPos[1];
                int bottomVisible = r.bottom;

                int minHeight = bottomVisible - topOfBody;
                if (minHeight < 200) minHeight = 200; // Minimalhöhe als Fallback

                bodyEdit.setMinHeight(minHeight);

                // Tipp: Nur einmal setzen, damit die Schleife nicht dauernd läuft
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }
}
