package com.git.amarradi.leafpad.model;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.HashSet;
import java.util.Set;

@Database(
        entities = {
                Category.class,
                NoteEntity.class   // 🔥 Notes jetzt Teil der DB
        },
        version = 2,                // 🔥 Version erhöht (Migration aktiv)
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    // 🔥 Globaler Context für Migrationen
    private static Context appContext;

    public abstract CategoryDao categoryDao();
    public abstract NoteDao noteDao();

    // ---------------------------------------------------------------------
    // PREPOPULATE – Standardkategorie "recipe"
    // ---------------------------------------------------------------------
    private static final RoomDatabase.Callback PREPOPULATE_CALLBACK =
            new RoomDatabase.Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    super.onCreate(db);

                    long now = System.currentTimeMillis();

                    db.execSQL(
                            "INSERT INTO categories " +
                                    "(name, color_hex, sort_order, is_archived, created_at, updated_at) " +
                                    "VALUES (" +
                                    "'Rezept'," +          // UI-Name (wird später übersetzt angezeigt)
                                    "'#000080'," +         // Farbe
                                    "0," +
                                    "0," +
                                    now + "," +
                                    now +
                                    ")"
                    );
                }
            };

    // ---------------------------------------------------------------------
    // MIGRATION 1 → 2 (SharedPrefs → Room)
    // ---------------------------------------------------------------------
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {

            // 1) Notes-Tabelle anlegen
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS notes (" +
                            "id TEXT NOT NULL PRIMARY KEY, " +
                            "title TEXT, " +
                            "body TEXT, " +
                            "notedate TEXT, " +
                            "notetime TEXT, " +
                            "create_date TEXT, " +
                            "hide INTEGER NOT NULL, " +
                            "category_key TEXT)"
            );

            // 2) Übernehme existierende Notizen
            migrateNotesFromSharedPrefs(appContext, db);
        }
    };

    // ---------------------------------------------------------------------
    // HELPER – liest SharedPrefs und schreibt Notes in die Datenbank
    // ---------------------------------------------------------------------
    private static void migrateNotesFromSharedPrefs(Context context, SupportSQLiteDatabase db) {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences("leafstore", Context.MODE_PRIVATE);

        Set<String> ids = prefs.getStringSet("note_id_set", new HashSet<>());

        for (String id : ids) {

            String title = prefs.getString("note_title_" + id, "");
            String body = prefs.getString("note_body_" + id, "");
            String notedate = prefs.getString("note_date_set" + id, "");
            String notetime = prefs.getString("note_time_set" + id, "");
            String createDate = prefs.getString("note_date_" + id, "");

            boolean hide = prefs.getBoolean("false_" + id, false);

            String categoryKey = prefs.getString("note_category_" + id, "");

            // Deutsch → interner Key
            if ("Rezept".equals(categoryKey)) {
                categoryKey = "recipe";
            }

            db.execSQL(
                    "INSERT OR REPLACE INTO notes " +
                            "(id, title, body, notedate, notetime, create_date, hide, category_key) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    new Object[]{
                            id,
                            title,
                            body,
                            notedate,
                            notetime,
                            createDate,
                            hide ? 1 : 0,
                            categoryKey
                    }
            );
        }
    }

    // ---------------------------------------------------------------------
    // INSTANCE + Kontext setzen
    // ---------------------------------------------------------------------
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {

                    // 🔥 globaler Context für Migrationen
                    appContext = context.getApplicationContext();

                    INSTANCE = Room.databaseBuilder(
                                    appContext,
                                    AppDatabase.class,
                                    "leafpad.db"
                            )
                            .addCallback(PREPOPULATE_CALLBACK)
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigrationOnDowngrade()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
