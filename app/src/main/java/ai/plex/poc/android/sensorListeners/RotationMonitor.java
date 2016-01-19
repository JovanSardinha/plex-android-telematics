package ai.plex.poc.android.sensorListeners;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by terek on 07/01/16.
 */
public class RotationMonitor implements SensorEventListener {
    private Context applicationContext;
    private SensorManager sensorManager;
    private Sensor sensor;

    public RotationMonitor(Context context, Sensor sensor){
        this.applicationContext = context;
        this.sensorManager = (SensorManager)applicationContext.getSystemService(Context.SENSOR_SERVICE);
        this.sensor = sensor;
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        new SensorDataWriter(applicationContext,Sensor.TYPE_ROTATION_VECTOR).writeData(event);
    }

    public void pause() {
        sensorManager.unregisterListener(this, sensor);
    }
}