package net.golbarg.engtoper.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import net.golbarg.engtoper.models.CardProgress;

import java.util.ArrayList;
import java.util.List;

public class TableCardProgress {
    public static final String TABLE_NAME = "card_progress";
    public static final String KEY_WORD_ID = "word_id";
    public static final String KEY_LANG = "lang";
    public static final String KEY_BOX = "box";
    public static final String KEY_DUE_AT = "due_at";
    public static final String KEY_REVIEWED_AT = "reviewed_at";

    private final DatabaseHandler dbHandler;

    public TableCardProgress(DatabaseHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    public static String createTableQuery() {
        return String.format(
                "CREATE TABLE IF NOT EXISTS %s (%s INTEGER NOT NULL, %s TEXT NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL, PRIMARY KEY (%s, %s))",
                TABLE_NAME, KEY_WORD_ID, KEY_LANG, KEY_BOX, KEY_DUE_AT, KEY_REVIEWED_AT, KEY_WORD_ID, KEY_LANG
        );
    }

    @Nullable
    public CardProgress get(int wordId, String lang) {
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_NAME, null, KEY_WORD_ID + "=? AND " + KEY_LANG + "=?",
                new String[]{String.valueOf(wordId), lang}, null, null, null, "1")) {
            return cursor.moveToFirst() ? mapColumn(cursor) : null;
        }
    }

    public void upsert(CardProgress progress) {
        ContentValues values = new ContentValues();
        values.put(KEY_WORD_ID, progress.getWordId());
        values.put(KEY_LANG, progress.getLang());
        values.put(KEY_BOX, progress.getBox());
        values.put(KEY_DUE_AT, progress.getDueAt());
        values.put(KEY_REVIEWED_AT, progress.getReviewedAt());
        dbHandler.getWritableDatabase().insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void delete(int wordId, String lang) {
        dbHandler.getWritableDatabase().delete(TABLE_NAME, KEY_WORD_ID + "=? AND " + KEY_LANG + "=?",
                new String[]{String.valueOf(wordId), lang});
    }

    public List<CardProgress> getDue(long now, int limit) {
        List<CardProgress> list = new ArrayList<>();
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_NAME, null, KEY_DUE_AT + "<=?", new String[]{String.valueOf(now)},
                null, null, KEY_DUE_AT + " ASC", String.valueOf(limit))) {
            while (cursor.moveToNext()) {
                list.add(mapColumn(cursor));
            }
        }
        return list;
    }

    public void deleteAll() {
        dbHandler.getWritableDatabase().delete(TABLE_NAME, null, null);
    }

    public int countDue(long now) {
        return count(KEY_DUE_AT + "<=?", String.valueOf(now));
    }

    /** Cards whose latest review happened at or after {@code since}. */
    public int countReviewedSince(long since) {
        return count(KEY_REVIEWED_AT + ">=?", String.valueOf(since));
    }

    public int countLearned(int minBox) {
        return count(KEY_BOX + ">=?", String.valueOf(minBox));
    }

    private int count(String selection, String arg) {
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE " + selection, new String[]{arg})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    @SuppressLint("Range")
    private CardProgress mapColumn(Cursor cursor) {
        return new CardProgress(
                cursor.getInt(cursor.getColumnIndex(KEY_WORD_ID)),
                cursor.getString(cursor.getColumnIndex(KEY_LANG)),
                cursor.getInt(cursor.getColumnIndex(KEY_BOX)),
                cursor.getLong(cursor.getColumnIndex(KEY_DUE_AT)),
                cursor.getLong(cursor.getColumnIndex(KEY_REVIEWED_AT))
        );
    }
}
