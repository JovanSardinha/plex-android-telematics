package ai.plex.poc.android.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.util.Log;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
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
    private static LocationMonitor locationMonitor;

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

        if (locationMonitor == null){
            locationMonitor = new LocationMonitor(this.getApplicationContext());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(10000);
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
        if (mGoogleApiClient != null && isTrackingLocation && isRecording){
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest, locationMonitor);
        Log.d("LOCATION", "Location update started ..............: ");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}


