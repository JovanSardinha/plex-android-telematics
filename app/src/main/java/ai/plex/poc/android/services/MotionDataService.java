package ai.plex.poc.android.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;


import ai.plex.poc.android.sensorListeners.GyroscopeMonitor;
import ai.plex.poc.android.sensorListeners.LinearAccelerationMonitor;
import ai.plex.poc.android.sensorListeners.MagneticMonitor;
import ai.plex.poc.android.sensorListeners.RotationMonitor;

/**
 * Intent service used to obtain telematics data from the phone
 */
public class MotionDataService extends IntentService {

    private static HandlerThread sensorHandlerThread;
    private static Handler sensorHandler;

    private static SensorManager mSensorManager;
    private static LinearAccelerationMonitor linearAccelerationMonitor;
    private static GyroscopeMonitor gyroscopeMonitor;
    private static MagneticMonitor magneticMonitor;
    private static RotationMonitor rotationMonitor;

    private static Sensor linearAccelerationSensor;
    private static Sensor gyroscopeSensor;
    private static Sensor rotationSensor;
    private static Sensor magneticSensor;

    public MotionDataService() {
        super("MotionDataService");
    }

    private void initalize() {
        if (mSensorManager == null)
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //sensors
        if (linearAccelerationSensor == null)
            linearAccelerationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        if (gyroscopeSensor == null)
            gyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        if (rotationSensor == null)
            rotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        if (magneticSensor == null)
            magneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //listeners
        if (linearAccelerationMonitor == null)
            linearAccelerationMonitor = new LinearAccelerationMonitor(this.getApplicationContext(), linearAccelerationSensor);

        if (gyroscopeMonitor == null)
            gyroscopeMonitor = new GyroscopeMonitor(this.getApplicationContext(), gyroscopeSensor);

        if (magneticMonitor == null)
            magneticMonitor = new MagneticMonitor(this.getApplicationContext(), magneticSensor);

        if (rotationMonitor == null)
            rotationMonitor = new RotationMonitor(this.getApplicationContext(), rotationSensor);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     *
     * @param workIntent
     */
    @Override
    protected void onHandleIntent(Intent workIntent) {
        // Gets data from the incoming Intent
        initalize();
        try {
            if (workIntent.getBooleanExtra("Restart", false)){
                reSubscribeToSensorData();
            } else {
                subscribeToSensorData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reSubscribeToSensorData() {
        try {
            unsubscribeFromSensorData();
            subscribeToSensorData();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void unsubscribeFromSensorData() {
        mSensorManager.unregisterListener(linearAccelerationMonitor, linearAccelerationSensor);
        mSensorManager.unregisterListener(gyroscopeMonitor, gyroscopeSensor);
        mSensorManager.unregisterListener(magneticMonitor, magneticSensor);
        mSensorManager.unregisterListener(rotationMonitor, rotationSensor);
    }


    private void subscribeToSensorData() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDriving = prefs.getBoolean("isDriving", false);
        boolean isRecording = prefs.getBoolean("isRecording", false);
        boolean isTrackingAcceleration = prefs.getBoolean("isTrackingAcceleration", false);
        boolean isTrackingGyroscope = prefs.getBoolean("isTrackingGyroscope", false);
        boolean isTrackingMagnetic = prefs.getBoolean("isTrackingMagnetic", false);
        boolean isTrackingRotation = prefs.getBoolean("isTrackingRotation", false);

        if (sensorHandlerThread == null) {
            sensorHandlerThread = new HandlerThread("SesnorListener");
            sensorHandlerThread.start();
            sensorHandler = new Handler(sensorHandlerThread.getLooper());
        }

        if (linearAccelerationSensor != null && isTrackingAcceleration && isRecording) {
            mSensorManager.registerListener(linearAccelerationMonitor, linearAccelerationSensor, SensorManager.SENSOR_DELAY_NORMAL, sensorHandler);
        }
        if (gyroscopeSensor != null && isTrackingGyroscope && isRecording) {
            mSensorManager.registerListener(gyroscopeMonitor, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL, sensorHandler);
        }
        if (rotationSensor != null && isTrackingRotation && isRecording) {
            mSensorManager.registerListener(rotationMonitor, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL, sensorHandler);
        }
        if (magneticSensor != null && isTrackingMagnetic && isRecording) {
            mSensorManager.registerListener(magneticMonitor, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL, sensorHandler);
        }
    }
}


