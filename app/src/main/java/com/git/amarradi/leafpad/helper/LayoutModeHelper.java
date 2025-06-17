package com.git.amarradi.leafpad.helper;

import static com.git.amarradi.leafpad.adapter.NoteAdapter.LayoutMode;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.PopupMenu;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.git.amarradi.leafpad.Leafpad;
import com.git.amarradi.leafpad.R;
import com.git.amarradi.leafpad.adapter.NoteAdapter;
import com.git.amarradi.leafpad.util.MasonrySpacingDecoration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class LayoutModeHelper {
    private static final String PREF_LAYOUT_MODE = Leafpad.PREF_LAYOUT_MODE;
    private static RecyclerView.ItemDecoration gridSpacingDecoration;

    public static boolean isListMode(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String mode = sharedPreferences.getString(PREF_LAYOUT_MODE, "list");
        return "list".equals(mode);
    }

    public static void saveMode(Context context, boolean isList) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (isList) {
            editor.putString(PREF_LAYOUT_MODE,"list");
        } else {
            editor.putString(PREF_LAYOUT_MODE,"grid");
        }
        editor.apply();
    }

    public static void applyLayout(Context context, RecyclerView recyclerView, NoteAdapter adapter, boolean isList) {

        if (isList) {
            adapter.setLayoutMode(LayoutMode.LIST);
        } else {
            adapter.setLayoutMode(LayoutMode.GRID);
        }

        if (isList) {
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            if (gridSpacingDecoration != null) {
                recyclerView.removeItemDecoration(gridSpacingDecoration);
            }
        } else {
            recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
            int vert = context.getResources().getDimensionPixelSize(R.dimen.masonry_vertical_spacing);
            int horiz = context.getResources().getDimensionPixelSize(R.dimen.masonry_horizontal_spacing);
            gridSpacingDecoration = new MasonrySpacingDecoration(vert, horiz);
            recyclerView.addItemDecoration(gridSpacingDecoration);
        }
    }

    public static void forcePopupMenuIcons(PopupMenu popup) {
        try {
            Field[] fields = popup.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popup);
                    assert menuPopupHelper != null;
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
