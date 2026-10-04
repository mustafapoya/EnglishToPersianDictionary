package net.golbarg.engtoper.db;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class OfflineDatabaseHandler extends SQLiteOpenHelper {
    public static  final String TAG = OfflineDatabaseHandler.class.getName();
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "eng_per.db";
    private static final String DB_PATH_SUFFIX = "/databases/";
    Context context;

    public OfflineDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    public void copyDatabaseFromAsset() throws IOException {
        InputStream inputStream = context.getAssets().open(DATABASE_NAME);

        // Path to the just created empty db
        String outFileName = getDatabasePath();

        // if the path does not exist first, create it
        File f = new File(context.getApplicationInfo().dataDir + DB_PATH_SUFFIX);
        if(!f.exists()) {
            f.mkdir();
        }

        // Copy to a temporary file first so an interrupted copy never looks like a finished database
        File tempFile = new File(outFileName + ".tmp");
        OutputStream outputStream = new FileOutputStream(tempFile);

        //transfer bytes from the input file to the output file
        byte[] buffer = new byte[1024];
        int length;

        while((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        // close the streams
        outputStream.flush();
        outputStream.close();
        inputStream.close();

        if (!tempFile.renameTo(new File(outFileName))) {
            tempFile.delete();
            throw new IOException("Could not move the copied database into place");
        }
    }

    public String getDatabasePath() {
        return context.getApplicationInfo().dataDir + DB_PATH_SUFFIX + DATABASE_NAME;
    }

    public SQLiteDatabase openDatabase() throws SQLException {
        File dbFile = ensureCopied();
        return SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.CREATE_IF_NECESSARY);
    }

    /**
     * The app can be entered without the splash screen (look-up from another app, the daily
     * reminder), so every open copies the asset first; otherwise SQLiteOpenHelper would create an
     * empty database file and the real one would never be copied.
     */
    @Override
    public SQLiteDatabase getReadableDatabase() {
        ensureCopied();
        return super.getReadableDatabase();
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        ensureCopied();
        return super.getWritableDatabase();
    }

    private static final Object COPY_LOCK = new Object();

    private File ensureCopied() {
        File dbFile = context.getDatabasePath(DATABASE_NAME);
        synchronized (COPY_LOCK) {
            if (!dbFile.exists()) {
                try {
                    copyDatabaseFromAsset();
                    Log.d(TAG, "openDatabase: copying success from assets folder");
                } catch (IOException e) {
                    throw new RuntimeException("Error Creating source database", e);
                }
            }
        }
        return dbFile;
    }

    // The database is copied ready-made from assets, so there is nothing to create or migrate
    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
