package ai.plex.poc.android.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import ai.plex.poc.android.database.SnapShotContract.*;

/**
 * Created by terek on 07/01/16.
 */
public class SnapShotDBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "snapShot.db";

    public SnapShotDBHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_ACCELERATION = "CREATE TABLE " + AccelerationEntry.TABLE_NAME+ "(" +
                AccelerationEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                AccelerationEntry.COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
                AccelerationEntry.COLUMN_X + " REAL NOT NULL, " +
                AccelerationEntry.COLUMN_Y + " REAL NOT NULL, " +
                AccelerationEntry.COLUMN_Z + " REAL NOT NULL " + ");";

        db.execSQL(SQL_CREATE_ACCELERATION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXITS "+ AccelerationEntry.TABLE_NAME);
        onCreate(db);
    }
}
