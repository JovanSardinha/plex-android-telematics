package ai.plex.poc.android.sensorListeners;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Date;

import ai.plex.poc.android.database.SnapShotContract;
import ai.plex.poc.android.database.SnapShotDBHelper;

/**
 * Created by terek on 14/01/16.
 */
public class SensorDataWriter {
    private Context context;
    private SQLiteDatabase db;
    private int sensorType;

    public SensorDataWriter(Context context, int sensorType){
        this.context = context;
        this.sensorType = sensorType;
    }

    protected Void writeData(SensorEvent event) {
        // The light sensor returns a single value.
        // Many sensors return 3 values, one for each axis.
        //Check if the person is currently driving
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isDriving = prefs.getBoolean("isDriving", false);

        if (isDriving) {
            try {
                    this.db = new SnapShotDBHelper(context).getWritableDatabase();
                    ContentValues values = new ContentValues();
                    long rowId = -1;

                    switch (this.sensorType){
                        case Sensor.TYPE_ACCELEROMETER:
                            values.put(SnapShotContract.AccelerationEntry.COLUMN_X, event.values[0]);
                            values.put(SnapShotContract.AccelerationEntry.COLUMN_Y, event.values[1]);
                            values.put(SnapShotContract.AccelerationEntry.COLUMN_Z, event.values[2]);
                            values.put(SnapShotContract.AccelerationEntry.COLUMN_IS_DRIVING, String.valueOf(isDriving));
                            values.put(SnapShotContract.AccelerationEntry.COLUMN_TIMESTAMP, new Date().getTime());
                            rowId = db.insert(SnapShotContract.AccelerationEntry.TABLE_NAME, null, values);
                            break;
                        case Sensor.TYPE_GYROSCOPE:
                            values.put(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_X, event.values[0]);
                            values.put(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_Y, event.values[1]);
                            values.put(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_Z, event.values[2]);
                            values.put(SnapShotContract.GyroscopeEntry.COLUMN_IS_DRIVING, String.valueOf(isDriving));
                            values.put(SnapShotContract.GyroscopeEntry.COLUMN_TIMESTAMP, new Date().getTime());
                            rowId = db.insert(SnapShotContract.GyroscopeEntry.TABLE_NAME, null, values);
                            break;
                        case Sensor.TYPE_LINEAR_ACCELERATION:
                            values.put(SnapShotContract.LinearAccelerationEntry.COLUMN_X, event.values[0]);
                            values.put(SnapShotContract.LinearAccelerationEntry.COLUMN_Y, event.values[1]);
                            values.put(SnapShotContract.LinearAccelerationEntry.COLUMN_Z, event.values[2]);
                            values.put(SnapShotContract.LinearAccelerationEntry.COLUMN_IS_DRIVING, String.valueOf(isDriving));
                            values.put(SnapShotContract.LinearAccelerationEntry.COLUMN_TIMESTAMP, new Date().getTime());
                            rowId = db.insert(SnapShotContract.LinearAccelerationEntry.TABLE_NAME, null, values);
                            break;
                        case Sensor.TYPE_MAGNETIC_FIELD:
                            values.put(SnapShotContract.MagneticEntry.COLUMN_X, event.values[0]);
                            values.put(SnapShotContract.MagneticEntry.COLUMN_Y, event.values[1]);
                            values.put(SnapShotContract.MagneticEntry.COLUMN_Z, event.values[2]);
                            values.put(SnapShotContract.MagneticEntry.COLUMN_IS_DRIVING, String.valueOf(isDriving));
                            values.put(SnapShotContract.MagneticEntry.COLUMN_TIMESTAMP, new Date().getTime());
                            rowId = db.insert(SnapShotContract.MagneticEntry.TABLE_NAME, null, values);
                            break;
                        case Sensor.TYPE_ROTATION_VECTOR:
                            values.put(SnapShotContract.RotationEntry.COLUMN_X_SIN, event.values[0]);
                            values.put(SnapShotContract.RotationEntry.COLUMN_Y_SIN, event.values[1]);
                            values.put(SnapShotContract.RotationEntry.COLUMN_Z_SIN, event.values[2]);
                            values.put(SnapShotContract.RotationEntry.COLUMN_COS, event.values[3]);
                            values.put(SnapShotContract.RotationEntry.COLUMN_ACCURACY, event.values[4]);
                            values.put(SnapShotContract.RotationEntry.COLUMN_IS_DRIVING, String.valueOf(isDriving));
                            values.put(SnapShotContract.RotationEntry.COLUMN_TIMESTAMP, new Date().getTime());
                            rowId = db.insert(SnapShotContract.RotationEntry.TABLE_NAME, null, values);
                            break;
                    }
                    Log.d("Record Written", String.valueOf(rowId));


            } catch (Exception ex){
                ex.printStackTrace();
            } finally {
            }
        }
        return null;
        }
}
