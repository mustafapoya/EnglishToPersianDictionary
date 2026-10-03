package net.golbarg.engtoper.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.golbarg.engtoper.models.SearchHistoryItem;

import java.util.ArrayList;

public class TableSearchHistory {
    public static final String TABLE_NAME = "search_history";
    public static final String KEY_ID = "id";
    public static final String KEY_QUERY = "query";
    public static final String KEY_LANG = "lang";
    public static final String KEY_TIMESTAMP = "timestamp";

    private final DatabaseHandler dbHandler;

    public TableSearchHistory(DatabaseHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    public static String createTableQuery() {
        return String.format(
                "CREATE TABLE IF NOT EXISTS %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT, %s TEXT, %s INTEGER)",
                TABLE_NAME, KEY_ID, KEY_QUERY, KEY_LANG, KEY_TIMESTAMP
        );
    }

    public static String dropTableQuery() {
        return "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public void addOrUpdate(String query, String lang) {
        if (query == null || query.trim().isEmpty()) return;
        query = query.trim();

        SQLiteDatabase db = dbHandler.getWritableDatabase();
        try {
            // Remove existing duplicate query in same language
            db.delete(TABLE_NAME, KEY_QUERY + "=? AND " + KEY_LANG + "=?", new String[]{query, lang});

            ContentValues values = new ContentValues();
            values.put(KEY_QUERY, query);
            values.put(KEY_LANG, lang);
            values.put(KEY_TIMESTAMP, System.currentTimeMillis());
            db.insert(TABLE_NAME, null, values);

            // Keep only latest 30 searches per language to prevent bloat
            db.execSQL(String.format(
                    "DELETE FROM %s WHERE %s = '%s' AND %s NOT IN (SELECT %s FROM %s WHERE %s = '%s' ORDER BY %s DESC LIMIT 30)",
                    TABLE_NAME, KEY_LANG, lang, KEY_ID, KEY_ID, TABLE_NAME, KEY_LANG, lang, KEY_TIMESTAMP
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Most recent searches for a language, or across both languages when {@code lang} is null. */
    public ArrayList<SearchHistoryItem> getRecent(String lang, int limit) {
        ArrayList<SearchHistoryItem> list = new ArrayList<>();
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        try {
            Cursor cursor = db.query(
                    TABLE_NAME,
                    new String[]{KEY_ID, KEY_QUERY, KEY_LANG, KEY_TIMESTAMP},
                    lang == null ? null : KEY_LANG + "=?",
                    lang == null ? null : new String[]{lang},
                    null,
                    null,
                    KEY_TIMESTAMP + " DESC",
                    String.valueOf(limit)
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    list.add(mapColumn(cursor));
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void delete(int id) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.delete(TABLE_NAME, KEY_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void clearAll(String lang) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.delete(TABLE_NAME, KEY_LANG + "=?", new String[]{lang});
    }

    @SuppressLint("Range")
    private SearchHistoryItem mapColumn(Cursor cursor) {
        return new SearchHistoryItem(
                cursor.getInt(cursor.getColumnIndex(KEY_ID)),
                cursor.getString(cursor.getColumnIndex(KEY_QUERY)),
                cursor.getString(cursor.getColumnIndex(KEY_LANG)),
                cursor.getLong(cursor.getColumnIndex(KEY_TIMESTAMP))
        );
    }
}
