package ai.plex.poc.android.services;

import android.Manifest;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import ai.plex.poc.android.sensorListeners.GyroscopeMonitor;
import ai.plex.poc.android.sensorListeners.LinearAccelerationMonitor;
import ai.plex.poc.android.sensorListeners.LocationMonitor;
import ai.plex.poc.android.sensorListeners.MagneticMonitor;
import ai.plex.poc.android.sensorListeners.RotationMonitor;

/**
 * Intent service used to obtain telematics data from the phone
 */
public class MotionDataService extends IntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = MotionDataService.class.getSimpleName();

    //Used to allow sensor data to recorded on a separate thread
    private static HandlerThread sensorHandlerThread;
    private static Handler sensorHandler;

    //Coy of the instance manage
    private static SensorManager mSensorManager;

    //The listeners
    private static LinearAccelerationMonitor linearAccelerationMonitor;
    private static GyroscopeMonitor gyroscopeMonitor;
    private static MagneticMonitor magneticMonitor;
    private static RotationMonitor rotationMonitor;

    //The sensors
    private static Sensor linearAccelerationSensor;
    private static Sensor gyroscopeSensor;
    private static Sensor rotationSensor;
    private static Sensor magneticSensor;

    //Location API
    private static GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;

    public MotionDataService() {
        super("MotionDataService");
    }

    private void initalize() {
        if (mSensorManager == null)
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //Initialize Sensors and listeners
        if (linearAccelerationSensor == null) {
            linearAccelerationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            linearAccelerationMonitor = new LinearAccelerationMonitor(this.getApplicationContext(), linearAccelerationSensor);
        }

        if (gyroscopeSensor == null) {
            gyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            gyroscopeMonitor = new GyroscopeMonitor(this.getApplicationContext(), gyroscopeSensor);
        }

        if (rotationSensor == null) {
            rotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            rotationMonitor = new RotationMonitor(this.getApplicationContext(), rotationSensor);
        }

        if (magneticSensor == null) {
            magneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            magneticMonitor = new MagneticMonitor(this.getApplicationContext(), magneticSensor);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        buildGoogleApiClient();
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

    /**
     * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .build();
    }

    public void stop() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
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
        if (linearAccelerationMonitor != null && linearAccelerationSensor != null) {
            mSensorManager.unregisterListener(linearAccelerationMonitor, linearAccelerationSensor);
        }

        if (gyroscopeMonitor != null && gyroscopeSensor != null) {
            mSensorManager.unregisterListener(gyroscopeMonitor, gyroscopeSensor);
        }

        if (magneticMonitor != null && magneticSensor != null) {
            mSensorManager.unregisterListener(magneticMonitor, magneticSensor);
        }

        if (rotationMonitor != null && rotationSensor != null) {
            mSensorManager.unregisterListener(rotationMonitor, rotationSensor);
        }

        if (mGoogleApiClient != null){
            mGoogleApiClient.disconnect();
        }
    }


    private void subscribeToSensorData() {
        //Read shared preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDriving = prefs.getBoolean("isDriving", false);
        boolean isRecording = prefs.getBoolean("isRecording", false);
        boolean isTrackingAcceleration = prefs.getBoolean("isTrackingAcceleration", false);
        boolean isTrackingGyroscope = prefs.getBoolean("isTrackingGyroscope", false);
        boolean isTrackingMagnetic = prefs.getBoolean("isTrackingMagnetic", false);
        boolean isTrackingRotation = prefs.getBoolean("isTrackingRotation", false);
        boolean isTrackingLocation = prefs.getBoolean("isTrackingLocation", false);
        boolean isTrackingActivity = prefs.getBoolean("isTrackingActivity", false);

        //Create a new thread
        if (sensorHandlerThread == null) {
            sensorHandlerThread = new HandlerThread("SesnorListener");
            sensorHandlerThread.start();
            sensorHandler = new Handler(sensorHandlerThread.getLooper());
        }

        //register listeners on the SensorListener thread
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
        if (mGoogleApiClient != null && isRecording && (isTrackingLocation || isTrackingActivity)) {
            mGoogleApiClient.connect();
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Intent i = new Intent(this, LocationMonitorService.class);
            PendingIntent mLocationMonitoringIntent = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mLocationMonitoringIntent);
            Log.d(TAG, "Location update started.");
        }
    }

    private void startActivityDetection() {
        Intent i = new Intent(this, ActivityRecognitionIntentService.class);
        PendingIntent mActivityRecongPendingIntent = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 1, mActivityRecongPendingIntent);
        Log.d(TAG, "Activity detection started.");
    }

    @Override
    public void onConnected(Bundle bundle) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isTrackingLocation = prefs.getBoolean("isTrackingLocation", false);
        boolean isTrackingActivity = prefs.getBoolean("isTrackingActivity", false);
        if (isTrackingLocation) {
            startLocationUpdates();
        }
        if (isTrackingActivity) {
            startActivityDetection();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "googleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Failed to connect to googleApiClient");
    }
}


