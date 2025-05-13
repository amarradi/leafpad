package com.git.amarradi.leafpad.helper;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.git.amarradi.leafpad.Leafpad;
import com.git.amarradi.leafpad.R;
import com.git.amarradi.leafpad.adapter.NoteAdapter;
import com.git.amarradi.leafpad.util.MasonrySpacingDecoration;

public class LayoutModeHelper {

    private static final String PREF_LAYOUT_MODE = Leafpad.PREF_LAYOUT_MODE;
    private static RecyclerView.ItemDecoration gridSpacingDecoration;

    public static boolean isListMode(Context context) {
        SharedPreferences prefs = Leafpad.getPrefs();
        String mode = prefs.getString(PREF_LAYOUT_MODE, "list");
        return "list".equals(mode);
    }

    public static void toggleAndApply(Context context, RecyclerView recyclerView, NoteAdapter adapter) {
        boolean isList = !isListMode(context);
        saveMode(context, isList);
        applyLayout(context, recyclerView, adapter, isList);
    }

    public static void saveMode(Context context, boolean isList) {
        SharedPreferences prefs = Leafpad.getPrefs();
        prefs.edit().putString(PREF_LAYOUT_MODE, isList ? "list" : "grid").apply();
    }

    public static void applyLayout(Context context, RecyclerView recyclerView, NoteAdapter adapter, boolean isList) {
        adapter.setLayoutMode(isList);


        adapter.notifyDataSetChanged();

        if (isList) {
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            if (gridSpacingDecoration != null) {
                recyclerView.removeItemDecoration(gridSpacingDecoration);
            }
        } else {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
            int vert = context.getResources().getDimensionPixelSize(R.dimen.masonry_vertical_spacing);
            int horz = context.getResources().getDimensionPixelSize(R.dimen.masonry_horizontal_spacing);
            gridSpacingDecoration = new MasonrySpacingDecoration(vert, horz);
            recyclerView.addItemDecoration(gridSpacingDecoration);
        }
    }
}
