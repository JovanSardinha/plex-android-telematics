package ai.plex.poc.android.sensorListeners;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.location.DetectedActivity;

import java.util.Date;
import ai.plex.poc.android.database.SnapShotContract;
import ai.plex.poc.android.database.SnapShotDBHelper;

/**
 * Created by terek on 14/01/16.
 */
public class SensorDataWriter {
    private Context context;
    private SQLiteDatabase db;
    private SensorType sensorType;

    public SensorDataWriter(Context context, SensorType sensorType){
        this.context = context;
        this.sensorType = sensorType;
    }

    public void writeData(SensorEvent event) {
        writeData(event, null, null);
    }

    public void writeData(Location location) {
        writeData(null, location, null);
    }

    public void writeData(DetectedActivity activity) {
        writeData(null, null, activity);
    }

    private void writeData(SensorEvent event, Location location, DetectedActivity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isRecording = prefs.getBoolean("isRecording", false);
        boolean isDriving = prefs.getBoolean("isDriving", false);

        if (isRecording) {
            try {
                    this.db = SnapShotDBHelper.getsInstance(context).getWritableDatabase();
                    ContentValues values = new ContentValues();
                    long rowId = -1;
                    switch (this.sensorType) {
                        case LINEAR_ACCELERATION:
                            values.put(SnapShotContract.LinearAccelerationEntry.COLUMN_X, event.values[0]);
                            values.put(SnapShotContract.LinearAccelerationEntry.COLUMN_Y, event.values[1]);
                            values.put(SnapShotContract.LinearAccelerationEntry.COLUMN_Z, event.values[2]);
                            values.put(SnapShotContract.LinearAccelerationEntry.COLUMN_IS_DRIVING, String.valueOf(isDriving));
                            values.put(SnapShotContract.LinearAccelerationEntry.COLUMN_TIMESTAMP, new Date().getTime());
                            values.put(SnapShotContract.LinearAccelerationEntry.COLUMN_IS_RECORD_UPLOADED, "false");
                            rowId = db.insert(SnapShotContract.LinearAccelerationEntry.TABLE_NAME, null, values);
                            break;
                        case GYROSCOPE:
                            values.put(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_X, event.values[0]);
                            values.put(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_Y, event.values[1]);
                            values.put(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_Z, event.values[2]);
                            values.put(SnapShotContract.GyroscopeEntry.COLUMN_IS_DRIVING, String.valueOf(isDriving));
                            values.put(SnapShotContract.GyroscopeEntry.COLUMN_TIMESTAMP, new Date().getTime());
                            values.put(SnapShotContract.GyroscopeEntry.COLUMN_IS_RECORD_UPLOADED, "false");
                            rowId = db.insert(SnapShotContract.GyroscopeEntry.TABLE_NAME, null, values);
                            break;
                        case MAGNETIC:
                            values.put(SnapShotContract.MagneticEntry.COLUMN_X, event.values[0]);
                            values.put(SnapShotContract.MagneticEntry.COLUMN_Y, event.values[1]);
                            values.put(SnapShotContract.MagneticEntry.COLUMN_Z, event.values[2]);
                            values.put(SnapShotContract.MagneticEntry.COLUMN_IS_DRIVING, String.valueOf(isDriving));
                            values.put(SnapShotContract.MagneticEntry.COLUMN_TIMESTAMP, new Date().getTime());
                            values.put(SnapShotContract.MagneticEntry.COLUMN_IS_RECORD_UPLOADED, "false");
                            rowId = db.insert(SnapShotContract.MagneticEntry.TABLE_NAME, null, values);
                            break;
                        case ROTATION:
                            values.put(SnapShotContract.RotationEntry.COLUMN_X_SIN, event.values[0]);
                            values.put(SnapShotContract.RotationEntry.COLUMN_Y_SIN, event.values[1]);
                            values.put(SnapShotContract.RotationEntry.COLUMN_Z_SIN, event.values[2]);
                            values.put(SnapShotContract.RotationEntry.COLUMN_COS, event.values[3]);
                            values.put(SnapShotContract.RotationEntry.COLUMN_ACCURACY, event.values[4]);
                            values.put(SnapShotContract.RotationEntry.COLUMN_IS_DRIVING, String.valueOf(isDriving));
                            values.put(SnapShotContract.RotationEntry.COLUMN_TIMESTAMP, new Date().getTime());
                            values.put(SnapShotContract.RotationEntry.COLUMN_IS_RECORD_UPLOADED, "false");
                            rowId = db.insert(SnapShotContract.RotationEntry.TABLE_NAME, null, values);
                            break;
                        case LOCATION:
                            values.put(SnapShotContract.LocationEntry.COLUMN_LATITUDE, location.getLatitude());
                            values.put(SnapShotContract.LocationEntry.COLUMN_LONGITUDE, location.getLongitude());
                            values.put(SnapShotContract.LocationEntry.COLUMN_SPEED, location.getSpeed());
                            values.put(SnapShotContract.LocationEntry.COLUMN_IS_DRIVING, String.valueOf(isDriving));
                            values.put(SnapShotContract.LocationEntry.COLUMN_TIMESTAMP, new Date().getTime());
                            values.put(SnapShotContract.LocationEntry.COLUMN_IS_RECORD_UPLOADED, "false");
                            rowId = db.insert(SnapShotContract.LocationEntry.TABLE_NAME, null, values);
                            break;
                        case ACTIVITY_DETECTOR:
                            values.put(SnapShotContract.DetectedActivityEntry.COLUMN_NAME, String.valueOf(activity.getType()));
                            values.put(SnapShotContract.DetectedActivityEntry.COLUMN_CONFIDENCDE, String.valueOf(activity.getConfidence()));
                            values.put(SnapShotContract.DetectedActivityEntry.COLUMN_TIMESTAMP, new Date().getTime());
                            values.put(SnapShotContract.DetectedActivityEntry.COLUMN_IS_DRIVING, String.valueOf(isDriving));
                            values.put(SnapShotContract.DetectedActivityEntry.COLUMN_IS_RECORD_UPLOADED, "false");
                            rowId = db.insert(SnapShotContract.DetectedActivityEntry.TABLE_NAME, null, values);
                            break;
                    }

                    Log.d(this.sensorType.toString() + " records written", String.valueOf(rowId));
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }
}
