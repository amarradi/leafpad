package com.git.amarradi.leafpad;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Note.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract NoteDao noteDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE==null) {
            //INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "database.db").build();
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "database.db").allowMainThreadQueries().build();
        }
        return INSTANCE;
    }
}
