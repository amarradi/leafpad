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
                CategoryEntity.class,
                NoteEntity.class,
                NoteCategoryJoin.class
        },
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    /** globaler Context für Migration */
    private static Context appContext;

    public abstract CategoryDao categoryDao();
    public abstract NoteDao noteDao();
    public abstract NoteCategoryDao noteCategoryDao();

    // ---------------------------------------------------------------------
    // PREPOPULATE – Standardkategorie "Rezept"
    // ---------------------------------------------------------------------
    private static final RoomDatabase.Callback PREPOPULATE_CALLBACK =
            new RoomDatabase.Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    super.onCreate(db);

                    long now = System.currentTimeMillis();

                    db.execSQL(
                            "INSERT INTO categories " +
                                    "(name,normalized_name, color_hex, sort_order, is_archived, created_at, updated_at) " +
                                    "VALUES (" +
                                    "'Rezept', " +
                                    "'rezept', " +
                                    "'#000080', " +
                                    "0, " +
                                    "0, " +
                                    now + ", " +
                                    now +
                                    ")"
                    );
                }
            };

    // ---------------------------------------------------------------------
    // MIGRATION 1 → 2  (SharedPreferences → Room)
    // ---------------------------------------------------------------------
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {

            // Notes
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS notes (" +
                            "id TEXT NOT NULL PRIMARY KEY, " +
                            "title TEXT, " +
                            "body TEXT, " +
                            "notedate TEXT, " +
                            "notetime TEXT, " +
                            "create_date TEXT, " +
                            "hide INTEGER NOT NULL" +
                            ")"
            );

            // Categories
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS categories (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "color_hex TEXT, " +
                            "sort_order INTEGER NOT NULL DEFAULT 0, " +
                            "is_archived INTEGER NOT NULL DEFAULT 0, " +
                            "created_at INTEGER NOT NULL, " +
                            "updated_at INTEGER NOT NULL" +
                            ")"
            );

            // Join table (N:M)
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS note_category_join (" +
                            "note_id TEXT NOT NULL, " +
                            "category_id INTEGER NOT NULL, " +
                            "PRIMARY KEY(note_id, category_id)" +
                            ")"
            );

            migrateNotesFromSharedPrefs(appContext, db);
        }
    };
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {

            // 1) neue Tabelle mit NOT NULL normalized_name erstellen
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS categories_new (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "normalized_name TEXT NOT NULL, " +
                            "color_hex TEXT, " +
                            "sort_order INTEGER NOT NULL DEFAULT 0, " +
                            "is_archived INTEGER NOT NULL DEFAULT 0, " +
                            "created_at INTEGER NOT NULL, " +
                            "updated_at INTEGER NOT NULL" +
                            ")"
            );

            // 2) Daten rüberkopieren + normalized_name initial setzen
            db.execSQL(
                    "INSERT INTO categories_new " +
                            "(id, name, normalized_name, color_hex, sort_order, is_archived, created_at, updated_at) " +
                            "SELECT " +
                            "id, name, lower(trim(name)), color_hex, sort_order, is_archived, created_at, updated_at " +
                            "FROM categories"
            );

            // 3) Dubletten entschärfen (alles außer MIN(id) bekommt Suffix)
            db.execSQL(
                    "UPDATE categories_new " +
                            "SET normalized_name = normalized_name || '__' || id " +
                            "WHERE id IN (" +
                            "  SELECT c.id FROM categories_new c " +
                            "  JOIN (" +
                            "    SELECT normalized_name, MIN(id) AS keep_id " +
                            "    FROM categories_new " +
                            "    GROUP BY normalized_name " +
                            "    HAVING COUNT(*) > 1" +
                            "  ) d ON d.normalized_name = c.normalized_name " +
                            "  WHERE c.id <> d.keep_id" +
                            ")"
            );

            // 4) UNIQUE Index erstellen (Name muss zu Room passen)
            db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_categories_normalized_name " +
                            "ON categories_new(normalized_name)"
            );

            // 5) alte Tabelle ersetzen
            db.execSQL("DROP TABLE categories");
            db.execSQL("ALTER TABLE categories_new RENAME TO categories");
        }
    };


    // ---------------------------------------------------------------------
    // SharedPrefs → Notes Migration
    // ---------------------------------------------------------------------
    private static void migrateNotesFromSharedPrefs(Context context, SupportSQLiteDatabase db) {
        if (context == null) return;

        SharedPreferences prefs =
                context.getSharedPreferences("leafstore", Context.MODE_PRIVATE);

        Set<String> ids =
                prefs.getStringSet("note_id_set", new HashSet<>());

        for (String id : ids) {

            String title = prefs.getString("note_title_" + id, "");
            String body = prefs.getString("note_body_" + id, "");
            String notedate = prefs.getString("note_date_set" + id, "");
            String notetime = prefs.getString("note_time_set" + id, "");
            String createDate = prefs.getString("note_date_" + id, "");
            boolean hide = prefs.getBoolean("false_" + id, false);

            db.execSQL(
                    "INSERT OR REPLACE INTO notes " +
                            "(id, title, body, notedate, notetime, create_date, hide) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    new Object[]{
                            id,
                            title,
                            body,
                            notedate,
                            notetime,
                            createDate,
                            hide ? 1 : 0
                    }
            );
        }
    }

    // ---------------------------------------------------------------------
    // INSTANCE
    // ---------------------------------------------------------------------
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {

                    appContext = context.getApplicationContext();

                    INSTANCE = Room.databaseBuilder(
                                    appContext,
                                    AppDatabase.class,
                                    "leafpad.db"
                            )
                            .addCallback(PREPOPULATE_CALLBACK)
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .fallbackToDestructiveMigrationOnDowngrade()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
