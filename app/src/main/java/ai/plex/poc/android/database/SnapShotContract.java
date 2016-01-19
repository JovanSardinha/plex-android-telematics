package ai.plex.poc.android.database;

import android.hardware.Sensor;
import android.provider.BaseColumns;

/**
 * Created by terek on 07/01/16.
 */
public class SnapShotContract {

    public static final class AccelerationEntry implements BaseColumns{
        public static final String TABLE_NAME = "acceleration";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_X = "x";
        public static final String COLUMN_Y = "y";
        public static final String COLUMN_Z = "z";
        public static final String COLUMN_IS_DRIVING = "isDriving";
    }

    public static final class LinearAccelerationEntry implements BaseColumns{
        public static final String TABLE_NAME = "linearAcceleration";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_X = "x";
        public static final String COLUMN_Y = "y";
        public static final String COLUMN_Z = "z";
        public static final String COLUMN_IS_DRIVING = "isDriving";
    }

    public static final class GyroscopeEntry implements BaseColumns{
        public static final String TABLE_NAME = "gyroscope";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_ANGULAR_SPEED_X = "angularSpeedX";
        public static final String COLUMN_ANGULAR_SPEED_Y = "angularSpeedY";
        public static final String COLUMN_ANGULAR_SPEED_Z = "angularSpeedZ";
        public static final String COLUMN_IS_DRIVING = "isDriving";
    }

    public static final class RotationEntry implements BaseColumns{
        public static final String TABLE_NAME = "rotation";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_X_SIN = "xSin";
        public static final String COLUMN_Y_SIN = "ySin";
        public static final String COLUMN_Z_SIN = "zSin";
        public static final String COLUMN_COS = "cos";
        public static final String COLUMN_ACCURACY = "accuarcy";
        public static final String COLUMN_IS_DRIVING = "isDriving";
    }

    public static final class MagneticEntry implements BaseColumns{
        public static final String TABLE_NAME = "magnetic";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_X = "x";
        public static final String COLUMN_Y = "y";
        public static final String COLUMN_Z = "z";
        public static final String COLUMN_IS_DRIVING = "isDriving";
    }

    public static final class DataManagerEntry implements BaseColumns{
        public static final String TABLE_NAME = "datamanager";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_LAST_ACCELERATION_RECORD = "lastAccelerationRecord";
    }
}
