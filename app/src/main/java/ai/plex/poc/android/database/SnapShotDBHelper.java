package ai.plex.poc.android.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;

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
                AccelerationEntry.COLUMN_Z + " REAL NOT NULL, " +
                AccelerationEntry.COLUMN_IS_DRIVING + " STRING NOT NULL"+");";

        db.execSQL(SQL_CREATE_ACCELERATION);

        final String SQL_CREATE_LINEAR_ACCELERATION = "CREATE TABLE " + LinearAccelerationEntry.TABLE_NAME+ "(" +
                LinearAccelerationEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                LinearAccelerationEntry.COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
                LinearAccelerationEntry.COLUMN_X + " REAL NOT NULL, " +
                LinearAccelerationEntry.COLUMN_Y + " REAL NOT NULL, " +
                LinearAccelerationEntry.COLUMN_Z + " REAL NOT NULL, " +
                LinearAccelerationEntry.COLUMN_IS_DRIVING + " STRING NOT NULL"+");";

        db.execSQL(SQL_CREATE_LINEAR_ACCELERATION);

        final String SQL_CREATE_GYROSCOPE = "CREATE TABLE " + GyroscopeEntry.TABLE_NAME+ "(" +
                GyroscopeEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                GyroscopeEntry.COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
                GyroscopeEntry.COLUMN_ANGULAR_SPEED_X + " REAL NOT NULL, " +
                GyroscopeEntry.COLUMN_ANGULAR_SPEED_Y + " REAL NOT NULL, " +
                GyroscopeEntry.COLUMN_ANGULAR_SPEED_Z + " REAL NOT NULL, " +
                GyroscopeEntry.COLUMN_IS_DRIVING + " STRING NOT NULL"+");";

        db.execSQL(SQL_CREATE_GYROSCOPE);

        final String SQL_CREATE_ROTATION = "CREATE TABLE " + RotationEntry.TABLE_NAME+ "(" +
                RotationEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                RotationEntry.COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
                RotationEntry.COLUMN_X_SIN + " REAL NOT NULL, " +
                RotationEntry.COLUMN_Y_SIN + " REAL NOT NULL, " +
                RotationEntry.COLUMN_Z_SIN + " REAL NOT NULL, " +
                RotationEntry.COLUMN_ACCURACY + " REAL NOT NULL, " +
                RotationEntry.COLUMN_COS + " REAL NOT NULL, " +
                RotationEntry.COLUMN_IS_DRIVING + " STRING NOT NULL"+");";

        db.execSQL(SQL_CREATE_ROTATION);

        final String SQL_CREATE_MAGNETIC = "CREATE TABLE " + MagneticEntry.TABLE_NAME+ "(" +
                MagneticEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                MagneticEntry.COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
                MagneticEntry.COLUMN_X + " REAL NOT NULL, " +
                MagneticEntry.COLUMN_Y + " REAL NOT NULL, " +
                MagneticEntry.COLUMN_Z + " REAL NOT NULL, " +
                MagneticEntry.COLUMN_IS_DRIVING + " STRING NOT NULL"+");";

        db.execSQL(SQL_CREATE_MAGNETIC);

        final String SQL_CREATE_DATAMANAGER = "CREATE TABLE " + DataManagerEntry.TABLE_NAME+ "(" +
                DataManagerEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                DataManagerEntry.COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
                DataManagerEntry.COLUMN_LAST_ACCELERATION_RECORD + " INTEGER NOT NULL);";

        db.execSQL(SQL_CREATE_DATAMANAGER);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXITS "+ AccelerationEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXITS "+ DataManagerEntry.TABLE_NAME);
        onCreate(db);
    }
}
