package ai.plex.poc.android.sensorListeners;

import android.hardware.Sensor;

/**
 * Created by ashish on 17/02/16.
 */
public enum SensorType {
    LINEAR_ACCELERATION("LinearAcceleration", Sensor.TYPE_LINEAR_ACCELERATION),
    ROTATION("Rotation", Sensor.TYPE_ROTATION_VECTOR),
    GYROSCOPE("Gyroscope", Sensor.TYPE_GYROSCOPE),
    MAGNETIC("Magnetic", Sensor.TYPE_MAGNETIC_FIELD),
    LOCATION("Location", 10),
    ACTIVITY_DETECTOR("ActivityDetector", 11);

    private String stringValue;
    private int intValue;

    private SensorType(String toString, int value) {
        stringValue = toString;
        intValue = value;
    }

    @Override
    public String toString() {
        return stringValue;
    }
}