package com.git.amarradi.leafpad.helper;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.git.amarradi.leafpad.model.AppDatabase;

import java.util.HashSet;
import java.util.Set;

public final class LeafstoreImporter {

    // Source prefs
    private static final String LEAFSTORE_PREF = "leafstore";
    private static final String KEY_ID_SET = "note_id_set";

    private static final String TITLE_PREFIX = "note_title_";
    private static final String BODY_PREFIX = "note_body_";
    private static final String ADDDATE_PREFIX = "note_date_set";   // + id
    private static final String ADDTIME_PREFIX = "note_time_set";   // + id
    private static final String CREATEDATE_PREFIX = "note_date_";   // + id
    private static final String CATEGORY_PREFIX = "note_category_"; // + id

    // hide keys (alt + neu)
    private static final String HIDE_BASE = "false"; // alt: "false<id>", neu: "false_<id>"

    // One-time flag
    private static final String PREF_IMPORTED_FLAG = "pref_leafstore_imported_v1";

    private LeafstoreImporter() {
    }

    public static void importIfNeeded(Context context) {
        SharedPreferences def = PreferenceManager.getDefaultSharedPreferences(context);
        if (def.getBoolean(PREF_IMPORTED_FLAG, false)) {
            return;
        }

        SharedPreferences leafstore = context.getSharedPreferences(LEAFSTORE_PREF, Context.MODE_PRIVATE);
        Set<String> ids = leafstore.getStringSet(KEY_ID_SET, null);

        if (ids == null || ids.isEmpty()) {
            // nothing to do, but don't check again every start
            def.edit().putBoolean(PREF_IMPORTED_FLAG, true).apply();
            return;
        }

        // Use a stable copy (SharedPreferences returns a mutable backing set sometimes)
        Set<String> idsCopy = new HashSet<>(ids);

        SupportSQLiteDatabase db = AppDatabase.getInstance(context)
                .getOpenHelper()
                .getWritableDatabase();

        db.beginTransaction();
        boolean success = false;

        try {
            for (String id : idsCopy) {
                importOne(db, leafstore, id);
            }
            db.setTransactionSuccessful();
            success = true;
        } finally {
            db.endTransaction();
        }

        if (success) {
            def.edit().putBoolean(PREF_IMPORTED_FLAG, true).apply();
            cleanupLeafstore(leafstore, idsCopy);
        }
    }

    private static void importOne(SupportSQLiteDatabase db, SharedPreferences leafstore, String id) {

        String title = leafstore.getString(TITLE_PREFIX + id, "");
        String body = leafstore.getString(BODY_PREFIX + id, "");
        String notedate = leafstore.getString(ADDDATE_PREFIX + id, "");
        String notetime = leafstore.getString(ADDTIME_PREFIX + id, "");
        String createDate = leafstore.getString(CREATEDATE_PREFIX + id, "");

        // prefer new key false_<id>, fallback old key false<id>
        boolean hide = leafstore.getBoolean(HIDE_BASE + "_" + id, false);
        if (!hide) {
            hide = leafstore.getBoolean(HIDE_BASE + id, false);
        }

        db.execSQL(
                "INSERT OR REPLACE INTO notes (id, title, body, notedate, notetime, create_date, hide) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                new Object[]{id, title, body, notedate, notetime, createDate, hide ? 1 : 0}
        );

        // Category: old model had exactly one category string per note
        String rawCategory = leafstore.getString(CATEGORY_PREFIX + id, "");
        if (rawCategory == null) rawCategory = "";
        rawCategory = rawCategory.trim();

        if (!rawCategory.isEmpty()) {
            // map legacy "recipe" to your default category "Rezept"
            // (because DB prepopulates "Rezept"/"rezept")
            String displayName = rawCategory;
            if ("recipe".equalsIgnoreCase(rawCategory)) {
                displayName = "Rezept";
            }

            long categoryId = ensureCategory(db, displayName);
            db.execSQL(
                    "INSERT OR IGNORE INTO note_category_join (note_id, category_id) VALUES (?, ?)",
                    new Object[]{id, categoryId}
            );
        }
    }

    private static long ensureCategory(SupportSQLiteDatabase db, String displayName) {
        String normalized = displayName.trim().toLowerCase();

        // 1) try find by normalized_name (unique)
        try (android.database.Cursor c = db.query(
                "SELECT id FROM categories WHERE normalized_name = ? LIMIT 1",
                new Object[]{normalized}
        )) {
            if (c.moveToFirst()) {
                return c.getLong(0);
            }
        }

        // 2) insert (match your schema exactly)
        long now = System.currentTimeMillis();
        db.execSQL(
                "INSERT INTO categories (name, normalized_name, color_hex, sort_order, is_archived, created_at, updated_at) " +
                        "VALUES (?, ?, NULL, 0, 0, ?, ?)",
                new Object[]{displayName, normalized, now, now}
        );

        // 3) fetch last row id
        try (android.database.Cursor c = db.query("SELECT last_insert_rowid()")) {
            if (c.moveToFirst()) {
                return c.getLong(0);
            }
        }

        return -1;
    }

    private static void cleanupLeafstore(SharedPreferences leafstore, Set<String> ids) {
        SharedPreferences.Editor e = leafstore.edit();

        for (String id : ids) {
            e.remove(TITLE_PREFIX + id);
            e.remove(BODY_PREFIX + id);
            e.remove(ADDDATE_PREFIX + id);
            e.remove(ADDTIME_PREFIX + id);
            e.remove(CREATEDATE_PREFIX + id);
            e.remove(CATEGORY_PREFIX + id);

            // remove both hide keys
            e.remove(HIDE_BASE + id);
            e.remove(HIDE_BASE + "_" + id);
        }

        e.remove(KEY_ID_SET);
        e.apply();
    }
}
