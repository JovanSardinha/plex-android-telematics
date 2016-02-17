package ai.plex.poc.android.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.Sensor;
import android.util.Log;

import ai.plex.poc.android.database.SnapShotContract.*;

/**
 * Created by terek on 07/01/16.
 */
public class SnapShotDBHelper extends SQLiteOpenHelper {
    private static final String TAG = SnapShotDBHelper.class.getSimpleName();
    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "snapShot.db";
    private static SnapShotDBHelper sInstance;

    public SnapShotDBHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized SnapShotDBHelper getsInstance(Context context){
        if (sInstance == null){
            sInstance = new SnapShotDBHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
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

        final String SQL_CREATE_LOCATION = "CREATE TABLE " + LocationEntry.TABLE_NAME+ "(" +
                LocationEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                LocationEntry.COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
                LocationEntry.COLUMN_LONGITUDE + " REAL NOT NULL, " +
                LocationEntry.COLUMN_LATITUDE + " REAL NOT NULL, " +
                LocationEntry.COLUMN_SPEED + " REAL NOT NULL, " +
                LocationEntry.COLUMN_IS_DRIVING + " STRING NOT NULL"+");";

        db.execSQL(SQL_CREATE_LOCATION);

        final String SQL_CREATE_DETECTED_ACTIVITY = "CREATE TABLE " + DetectedActivityEntry.TABLE_NAME+ "(" +
                DetectedActivityEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                DetectedActivityEntry.COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
                DetectedActivityEntry.COLUMN_NAME + " INTEGER NOT NULL, " +
                DetectedActivityEntry.COLUMN_CONFIDENCDE + " INTEGER NOT NULL"+
                DetectedActivityEntry.COLUMN_IS_DRIVING + " STRING NOT NULL"+");";

        db.execSQL(SQL_CREATE_DETECTED_ACTIVITY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + LinearAccelerationEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + RotationEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + GyroscopeEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + MagneticEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + LocationEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DetectedActivityEntry.TABLE_NAME);
        onCreate(db);
    }

    public static boolean clearTables(SQLiteDatabase db){
        try {
            Integer count1 = db.delete(LinearAccelerationEntry.TABLE_NAME, null, null);
            Integer count2 = db.delete(GyroscopeEntry.TABLE_NAME, null, null);
            Integer count3 = db.delete(MagneticEntry.TABLE_NAME, null, null);
            Integer count4 = db.delete(RotationEntry.TABLE_NAME, null, null);
            Integer count5 = db.delete(LocationEntry.TABLE_NAME, null, null);
            Integer count6 = db.delete(DetectedActivityEntry.TABLE_NAME, null, null);
            Log.d(TAG, "Deleted " + String.valueOf(count1) + " rows from LinearAcceleration.");
            Log.d(TAG, "Deleted " + String.valueOf(count2) + " rows from Gyroscope.");
            Log.d(TAG, "Deleted " + String.valueOf(count3) + " rows from Magnetic.");
            Log.d(TAG, "Deleted " + String.valueOf(count4) + " rows from Rotation.");
            Log.d(TAG, "Deleted " + String.valueOf(count5) + " rows from Location.");
            Log.d(TAG, "Deleted " + String.valueOf(count6) + " rows from DetectedActivity.");
            return  true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
