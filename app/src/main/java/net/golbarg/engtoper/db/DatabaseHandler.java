package net.golbarg.engtoper.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "dictionary_db";

    private static DatabaseHandler instance;

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /** Shared helper so every repository uses the same connection. */
    public static synchronized DatabaseHandler getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHandler(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TableSearchHistory.createTableQuery());
        db.execSQL(TableCardProgress.createTableQuery());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(TableSearchHistory.createTableQuery());
        }
        if (oldVersion < 3) {
            db.execSQL(TableCardProgress.createTableQuery());
        }
    }
}
