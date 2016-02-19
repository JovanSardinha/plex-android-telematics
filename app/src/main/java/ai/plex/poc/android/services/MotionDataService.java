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

import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
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
import ai.plex.poc.android.sensorListeners.SensorType;

/**
 * Intent service used to obtain telematics data from the phone
 */
public class MotionDataService extends IntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static int instanceCount = 0;

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

    // Google client used for location, activity detection updates
    private static GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    PendingIntent mLocationMonitoringIntent;
    PendingIntent mActivityRecognitionPendingIntent;

    // Binder given to clients
    private final IBinder mBinder = new MotionDataBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class MotionDataBinder extends Binder {
        public MotionDataService getService() {
            // Return this instance of MotionDataService so clients can call public methods
            return MotionDataService.this;
        }
    }

    public MotionDataService() {
        super("MotionDataService");
        instanceCount++;
        Log.d(TAG, " instance count: " + instanceCount);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        linearAccelerationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        linearAccelerationMonitor = new LinearAccelerationMonitor(this.getApplicationContext(), linearAccelerationSensor);

        rotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        rotationMonitor = new RotationMonitor(this.getApplicationContext(), rotationSensor);

        gyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        gyroscopeMonitor = new GyroscopeMonitor(this.getApplicationContext(), gyroscopeSensor);

        magneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        magneticMonitor = new MagneticMonitor(this.getApplicationContext(), magneticSensor);

        mLocationRequest = new LocationRequest()
                .setInterval(1000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .build();

        mGoogleApiClient.connect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void startSensor(SensorType sensorType) {
        // Create new thread
        if (sensorHandlerThread == null) {
            sensorHandlerThread = new HandlerThread("SesnorListener");
            sensorHandlerThread.start();
            sensorHandler = new Handler(sensorHandlerThread.getLooper());
        }

        // Register listeners on the SensorListener thread
        switch(sensorType) {
            case LINEAR_ACCELERATION:
                mSensorManager.registerListener(linearAccelerationMonitor, linearAccelerationSensor,
                        SensorManager.SENSOR_DELAY_NORMAL, sensorHandler);
                break;
            case ROTATION:
                mSensorManager.registerListener(rotationMonitor, rotationSensor,
                        SensorManager.SENSOR_DELAY_NORMAL, sensorHandler);
                break;
            case GYROSCOPE:
                mSensorManager.registerListener(gyroscopeMonitor, gyroscopeSensor,
                        SensorManager.SENSOR_DELAY_NORMAL, sensorHandler);
                break;
            case MAGNETIC:
                mSensorManager.registerListener(magneticMonitor, magneticSensor,
                        SensorManager.SENSOR_DELAY_NORMAL, sensorHandler);
                break;
            case LOCATION:
                if (mGoogleApiClient.isConnected()) {
                    startLocationUpdates();
                } else {
                    Log.d(TAG, "GoogleApiClient not connected. Please try again.");
                    mGoogleApiClient.connect();
                }
                break;
            case ACTIVITY_DETECTOR:
                if (mGoogleApiClient.isConnected()) {
                    startActivityDetection();
                } else {
                    Log.d(TAG, "GoogleApiClient not connected. Please try again.");
                    mGoogleApiClient.connect();
                }
                break;
            default:
                Log.e(TAG, "startSensor: Invalid sensor type");
        }
    }

    public void stopSensor(SensorType sensorType) {
        // Unregister listeners
        switch(sensorType) {
            case LINEAR_ACCELERATION:
                if (linearAccelerationMonitor != null && linearAccelerationSensor != null) {
                    mSensorManager.unregisterListener(linearAccelerationMonitor, linearAccelerationSensor);
                }
                break;
            case ROTATION:
                if (rotationMonitor != null && rotationSensor != null) {
                    mSensorManager.unregisterListener(rotationMonitor, rotationSensor);
                }
                break;
            case GYROSCOPE:
                if (gyroscopeMonitor != null && gyroscopeSensor != null) {
                    mSensorManager.unregisterListener(gyroscopeMonitor, gyroscopeSensor);
                }
                break;
            case MAGNETIC:
                if (magneticMonitor != null && magneticSensor != null) {
                    mSensorManager.unregisterListener(magneticMonitor, magneticSensor);
                }
                break;
            case LOCATION:
                stopLocationUpdates();
                break;
            case ACTIVITY_DETECTOR:
                stopActivityDetection();
                break;
            default:
                Log.e(TAG, "startSensor: Invalid sensor type");
        }
    }

    public void startAllSensors() {
        startSensor(SensorType.LINEAR_ACCELERATION);
        startSensor(SensorType.ROTATION);
        startSensor(SensorType.GYROSCOPE);
        startSensor(SensorType.MAGNETIC);
        startSensor(SensorType.LOCATION);
        startSensor(SensorType.ACTIVITY_DETECTOR);
    }

    public void stopAllSensors() {
        stopSensor(SensorType.LINEAR_ACCELERATION);
        stopSensor(SensorType.ROTATION);
        stopSensor(SensorType.GYROSCOPE);
        stopSensor(SensorType.MAGNETIC);
        stopSensor(SensorType.LOCATION);
        stopSensor(SensorType.ACTIVITY_DETECTOR);
    }

    /**
     *
     * @param workIntent
     */
    @Override
    protected void onHandleIntent(Intent workIntent) {
        try {
            stopAllSensors();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "GoogleApiClient connected.");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "GoogleApiClient connection suspended.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "GoogleApiClient connection failed.");
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mLocationMonitoringIntent == null) {
                Intent i = new Intent(this, LocationMonitorService.class);
                mLocationMonitoringIntent = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            }

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mLocationMonitoringIntent);
            Log.d(TAG, "Location updates started.");
        } else {
            Log.d(TAG, "Request ACCESS_FINE_LOCATION permission to start location updates.");
        }
    }

    private void stopLocationUpdates() {
        if (mLocationMonitoringIntent != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationMonitoringIntent);
            Log.d(TAG, "Location updates stopped.");
        }
    }

    private void startActivityDetection() {
        if (mActivityRecognitionPendingIntent == null) {
            Intent i = new Intent(this, ActivityRecognitionIntentService.class);
            mActivityRecognitionPendingIntent = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 1000, mActivityRecognitionPendingIntent);
        Log.d(TAG, "Activity detection started.");
    }

    private void stopActivityDetection() {
        if (mActivityRecognitionPendingIntent != null) {
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, mActivityRecognitionPendingIntent);
            Log.d(TAG, "Activity detection stopped.");
        }
    }
}


