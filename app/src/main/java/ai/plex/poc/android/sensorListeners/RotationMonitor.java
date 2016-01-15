package ai.plex.poc.android.sensorListeners;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/**
 * Created by terek on 07/01/16.
 */
public class RotationMonitor implements SensorEventListener {
    private Context applicationContext;

    public RotationMonitor(Context context){
        this.applicationContext = context;
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        //new SensorDataWriter(applicationContext,Sensor.TYPE_ROTATION_VECTOR).execute(event);
    }
}