package com.git.amarradi.leafpad.model;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {
                Category.class
                // Note-Entity fügen wir später dazu, wenn wir Notizen migrieren
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract CategoryDao categoryDao();

    // Callback, der NUR beim ersten Erzeugen der Datenbank aufgerufen wird
    private static final RoomDatabase.Callback PREPOPULATE_CALLBACK =
            new RoomDatabase.Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    super.onCreate(db);

                    long now = System.currentTimeMillis();

                    // id wird automatisch generiert, deshalb nicht angeben
                    db.execSQL(
                            "INSERT INTO categories " +
                                    "(name, color_hex, sort_order, is_archived, created_at, updated_at) " +
                                    "VALUES (" +
                                    "'Rezept'," +          // name
                                    "'#000080'," +             // color_hex
                                    "0," +                // sort_order
                                    "0," +                // is_archived (0 = aktiv)
                                    now + "," +           // created_at (ms)
                                    now +                 // updated_at (ms)
                                    ")"
                    );
                }
            };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "leafpad.db"
                            )
                            .addCallback(PREPOPULATE_CALLBACK) // <-- hier kommt das Seeding dazu
                            .fallbackToDestructiveMigrationOnDowngrade()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
