package net.golbarg.engtoper.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.golbarg.engtoper.models.PhrasePersian;
import net.golbarg.engtoper.models.SearchFilter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class TablePhrasePersian {
    public static final String TABLE_NAME = "phrase2";
    public static final String KEY_ID = "_id";
    public static final String KEY_LANGUAGE_FROM = "l_from";
    public static final String KEY_LANGUAGE_TO = "l_to";
    public static final String KEY_FAVORITE = "favorite";
    public static final String KEY_ARTICLE = "article";
    public static final String KEY_TYPE = "type";
    public static final String[] ALL_COLUMNS = {KEY_ID, KEY_LANGUAGE_FROM, KEY_LANGUAGE_TO, KEY_FAVORITE, KEY_ARTICLE, KEY_TYPE};
    private final OfflineDatabaseHandler offlineDatabaseHandler;

    public TablePhrasePersian(Context context) {
        offlineDatabaseHandler = new OfflineDatabaseHandler(context);
        offlineDatabaseHandler.openDatabase();
    }

    public PhrasePersian get(int id) {
        SQLiteDatabase db = offlineDatabaseHandler.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, ALL_COLUMNS, KEY_ID + "=?", new String[]{String.valueOf(id)}, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            PhrasePersian item = mapColumn(cursor);
            cursor.close();
            return item;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    public ArrayList<PhrasePersian> search(String query, SearchFilter filter, int limit) {
        ArrayList<PhrasePersian> result = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return result;
        }
        query = query.trim();

        SQLiteDatabase db = offlineDatabaseHandler.getReadableDatabase();
        Cursor cursor = null;

        try {
            if (filter == SearchFilter.STARTS_WITH) {
                String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + KEY_LANGUAGE_FROM + " NOT LIKE '%@%' AND " + KEY_LANGUAGE_FROM + " LIKE ? ORDER BY LENGTH(" + KEY_LANGUAGE_FROM + ") ASC, " + KEY_LANGUAGE_FROM + " ASC LIMIT ?";
                cursor = db.rawQuery(sql, new String[]{query + "%", String.valueOf(limit)});
            } else if (filter == SearchFilter.EXACT) {
                String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + KEY_LANGUAGE_FROM + " NOT LIKE '%@%' AND " + KEY_LANGUAGE_FROM + " = ? LIMIT ?";
                cursor = db.rawQuery(sql, new String[]{query, String.valueOf(limit)});
            } else { // CONTAINS
                String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + KEY_LANGUAGE_FROM + " NOT LIKE '%@%' AND " + KEY_LANGUAGE_FROM + " LIKE ? ORDER BY CASE WHEN " + KEY_LANGUAGE_FROM + " LIKE ? THEN 0 ELSE 1 END, LENGTH(" + KEY_LANGUAGE_FROM + ") ASC LIMIT ?";
                cursor = db.rawQuery(sql, new String[]{"%" + query + "%", query + "%", String.valueOf(limit)});
            }

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    result.add(mapColumn(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return result;
    }

    public ArrayList<PhrasePersian> getAll() {
        ArrayList<PhrasePersian> result = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_NAME + " WHERE " + KEY_LANGUAGE_FROM + " NOT LIKE '%@%' LIMIT 200";

        SQLiteDatabase db = offlineDatabaseHandler.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                result.add(mapColumn(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return result;
    }

    public ArrayList<PhrasePersian> getBookmarks() {
        ArrayList<PhrasePersian> result = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_NAME + " WHERE " + KEY_FAVORITE + " = 1 ORDER BY " + KEY_LANGUAGE_FROM + " ASC";

        SQLiteDatabase db = offlineDatabaseHandler.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                result.add(mapColumn(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return result;
    }

    public int countEntries() {
        SQLiteDatabase db = offlineDatabaseHandler.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE " + KEY_LANGUAGE_FROM + " NOT LIKE '%@%'", null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public int countBookmarks() {
        SQLiteDatabase db = offlineDatabaseHandler.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE " + KEY_FAVORITE + " = 1", null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public void updateFavorite(PhrasePersian phrase) {
        ContentValues values = new ContentValues();
        values.put(KEY_FAVORITE, phrase.getFavorite());
        offlineDatabaseHandler.getWritableDatabase().update(TABLE_NAME, values, KEY_ID + "=?",
                new String[]{String.valueOf(phrase.getId())});
    }

    @SuppressLint("Range")
    public PhrasePersian mapColumn(Cursor cursor) {
        String language_to = "";
        try {
            byte[] blob = cursor.getBlob(cursor.getColumnIndex(KEY_LANGUAGE_TO));
            if (blob != null) {
                language_to = new String(blob, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new PhrasePersian(
                cursor.getInt(cursor.getColumnIndex(KEY_ID)),
                cursor.getString(cursor.getColumnIndex(KEY_LANGUAGE_FROM)),
                language_to,
                cursor.getInt(cursor.getColumnIndex(KEY_FAVORITE)),
                cursor.getString(cursor.getColumnIndex(KEY_ARTICLE)),
                cursor.getInt(cursor.getColumnIndex(KEY_TYPE))
        );
    }
}
