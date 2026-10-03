package net.golbarg.engtoper.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.golbarg.engtoper.models.PhraseEnglish;
import net.golbarg.engtoper.models.SearchFilter;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class TablePhraseEnglish {
    public static final String TABLE_NAME = "phrase";
    public static final String KEY_ID = "_id";
    public static final String KEY_LANGUAGE_FROM = "l_from";
    public static final String KEY_LANGUAGE_TO = "l_to";
    public static final String KEY_FAVORITE = "favorite";
    public static final String KEY_ARTICLE = "article";
    public static final String KEY_TYPE = "type";
    public static final String[] ALL_COLUMNS = {KEY_ID, KEY_LANGUAGE_FROM, KEY_LANGUAGE_TO, KEY_FAVORITE, KEY_ARTICLE, KEY_TYPE};
    private final OfflineDatabaseHandler offlineDatabaseHandler;

    public TablePhraseEnglish(Context context) {
        offlineDatabaseHandler = new OfflineDatabaseHandler(context);
        offlineDatabaseHandler.openDatabase();
    }

    public PhraseEnglish get(int id) {
        SQLiteDatabase db = offlineDatabaseHandler.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, ALL_COLUMNS, KEY_ID + "=?", new String[]{String.valueOf(id)}, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            PhraseEnglish item = mapColumn(cursor);
            cursor.close();
            return item;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    public ArrayList<PhraseEnglish> search(String query, SearchFilter filter, int limit) {
        ArrayList<PhraseEnglish> result = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return result;
        }
        query = query.trim();

        SQLiteDatabase db = offlineDatabaseHandler.getReadableDatabase();
        Cursor cursor = null;

        try {
            if (filter == SearchFilter.STARTS_WITH) {
                String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + KEY_LANGUAGE_FROM + " LIKE ? ORDER BY LENGTH(" + KEY_LANGUAGE_FROM + ") ASC, " + KEY_LANGUAGE_FROM + " ASC LIMIT ?";
                cursor = db.rawQuery(sql, new String[]{query + "%", String.valueOf(limit)});
            } else if (filter == SearchFilter.EXACT) {
                String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + KEY_LANGUAGE_FROM + " = ? COLLATE NOCASE LIMIT ?";
                cursor = db.rawQuery(sql, new String[]{query, String.valueOf(limit)});
            } else { // CONTAINS
                String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + KEY_LANGUAGE_FROM + " LIKE ? ORDER BY CASE WHEN " + KEY_LANGUAGE_FROM + " LIKE ? THEN 0 ELSE 1 END, LENGTH(" + KEY_LANGUAGE_FROM + ") ASC LIMIT ?";
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

    public ArrayList<PhraseEnglish> getAll() {
        ArrayList<PhraseEnglish> result = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_NAME + " LIMIT 200";

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

    public ArrayList<PhraseEnglish> getBookmarks() {
        ArrayList<PhraseEnglish> result = new ArrayList<>();
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

    public PhraseEnglish getRandomWord() {
        SQLiteDatabase db = offlineDatabaseHandler.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " WHERE LENGTH(" + KEY_LANGUAGE_FROM + ") BETWEEN 4 AND 12 AND " + KEY_LANGUAGE_FROM + " NOT LIKE '% %' ORDER BY RANDOM() LIMIT 1",
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            PhraseEnglish item = mapColumn(cursor);
            cursor.close();
            return item;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    public ArrayList<PhraseEnglish> getRandomWords(int count, int excludeId) {
        ArrayList<PhraseEnglish> list = new ArrayList<>();
        SQLiteDatabase db = offlineDatabaseHandler.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " WHERE " + KEY_ID + " != ? AND LENGTH(" + KEY_LANGUAGE_FROM + ") BETWEEN 3 AND 14 ORDER BY RANDOM() LIMIT ?",
                new String[]{String.valueOf(excludeId), String.valueOf(count)}
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(mapColumn(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public int countEntries() {
        SQLiteDatabase db = offlineDatabaseHandler.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public int countBookmarks() {
        SQLiteDatabase db = offlineDatabaseHandler.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE " + KEY_FAVORITE + " = 1", null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public void updateFavorite(PhraseEnglish phrase) {
        ContentValues values = new ContentValues();
        values.put(KEY_FAVORITE, phrase.getFavorite());
        offlineDatabaseHandler.getWritableDatabase().update(TABLE_NAME, values, KEY_ID + "=?",
                new String[]{String.valueOf(phrase.getId())});
    }

    @SuppressLint("Range")
    public PhraseEnglish mapColumn(Cursor cursor) {
        String language_to = "";
        try {
            byte[] blob = cursor.getBlob(cursor.getColumnIndex(KEY_LANGUAGE_TO));
            if (blob != null) {
                language_to = new String(blob, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new PhraseEnglish(
                cursor.getInt(cursor.getColumnIndex(KEY_ID)),
                cursor.getString(cursor.getColumnIndex(KEY_LANGUAGE_FROM)),
                language_to,
                cursor.getInt(cursor.getColumnIndex(KEY_FAVORITE)),
                cursor.getString(cursor.getColumnIndex(KEY_ARTICLE)),
                cursor.getInt(cursor.getColumnIndex(KEY_TYPE))
        );
    }
}
