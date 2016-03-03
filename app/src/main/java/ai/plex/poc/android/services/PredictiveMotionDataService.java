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
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.common.collect.EvictingQueue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ai.plex.poc.android.sensorListeners.GyroscopeMonitor;
import ai.plex.poc.android.sensorListeners.LinearAccelerationMonitor;
import ai.plex.poc.android.sensorListeners.MagneticMonitor;
import ai.plex.poc.android.sensorListeners.RotationMonitor;
import ai.plex.poc.android.sensorListeners.SensorDataWriter;
import ai.plex.poc.android.sensorListeners.SensorType;

/**
 * Created by ashish on 24/02/16.
 */
public class PredictiveMotionDataService extends IntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static boolean isRunning = false;
    private static boolean isDriving = false;

    private static final String TAG = PredictiveMotionDataService.class.getSimpleName();

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

    private static int sensorDelayInterval = 1000000; // specified in microseconds
    private static long initialActivityDetectionRequestInterval = 1000;
    private static long activityDetectionRequestInterval = 1000;
    private static long maxActivityDetectionRequestInterval = 5 * 60 * 1000; // 5 min
    private static final double minDistanceTravelled = 100; // Must travel 100 m in 20s in order to keep recording
    private static EvictingQueue<Location> recentLocations = EvictingQueue.create(20);


    // Binder given to clients
    private final IBinder mBinder = new MotionDataBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class MotionDataBinder extends Binder {
        public PredictiveMotionDataService getService() {
            // Return this instance of PredictiveMotionDataService so clients can call public methods
            return PredictiveMotionDataService.this;
        }
    }

    public PredictiveMotionDataService() {
        super("PredictiveMotionDataService");
    }

    public static boolean isRunning() {
        return isRunning;
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
                if (linearAccelerationSensor != null)
                    mSensorManager.registerListener(linearAccelerationMonitor, linearAccelerationSensor,
                            sensorDelayInterval, sensorHandler);
                break;
            case ROTATION:
                if (rotationSensor != null)
                    mSensorManager.registerListener(rotationMonitor, rotationSensor,
                            sensorDelayInterval, sensorHandler);
                break;
            case GYROSCOPE:
                if (gyroscopeSensor != null)
                    mSensorManager.registerListener(gyroscopeMonitor, gyroscopeSensor,
                            sensorDelayInterval, sensorHandler);
                break;
            case MAGNETIC:
                if (magneticSensor != null)
                    mSensorManager.registerListener(magneticMonitor, magneticSensor,
                            sensorDelayInterval, sensorHandler);
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
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        try {
             if (ActivityRecognitionResult.hasResult(intent)) {
                ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
                DetectedActivity detectedActivity = result.getMostProbableActivity();

                int confidence = detectedActivity.getConfidence();
                String mostProbableName = getActivityName(detectedActivity.getType());

                new SensorDataWriter(this, SensorType.ACTIVITY_DETECTOR).writeData(detectedActivity);
                Log.d(TAG, "Detected activity: " + mostProbableName + "(w confidence " + confidence + ")");

                Intent localIntent = new Intent(Constants.ACTIVITY_UPDATE_BROADCAST_ACTION)
                        // Puts the status into the Intent
                        .putExtra(Constants.ACTIVITY_NAME, mostProbableName)
                        .putExtra(Constants.ACTIVITY_CONFIDENCE, confidence);

                // Broadcasts the Intent to receivers in this app.
                LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

                 // In vehicle
                 if (!isDriving) {
                      if (detectedActivity.getType() == DetectedActivity.IN_VEHICLE) {
                        resetActivityDetectionRequestInterval();
                        startDriving();
                        startAllSensors();
                     } else if (activityDetectionRequestInterval < maxActivityDetectionRequestInterval) {
                       activityDetectionRequestInterval = nextActivityDetectionRequestInterval();
                        Log.d(TAG, "Backoff time updated to " + activityDetectionRequestInterval / 1000 + " s");
                        startSensor(SensorType.ACTIVITY_DETECTOR);
                     }
                 }
            } else if (LocationResult.hasResult(intent)) {
                Location location = LocationResult.extractResult(intent).getLastLocation();
                new SensorDataWriter(this, SensorType.LOCATION).writeData(location);
                Log.i(TAG, "New Location at: " + location.getLatitude() + "/" + location.getLongitude() + " at " + location.getSpeed());

                Intent localIntent = new Intent(Constants.LOCATION_UPDATE_BROADCAST_ACTION)
                    .putExtra(Constants.LATITUDE, location.getLatitude())
                    .putExtra(Constants.LONGITUDE, location.getLongitude());

                // Broadcasts the Intent to receivers in this app.
                LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
                recentLocations.add(location);
                double recentDistance = recentDistanceTravelled();
                if (recentLocations.size() == 20 && recentDistance < minDistanceTravelled) {
                    stopDriving();
                }
                Log.d(TAG, "RecentDistanceTravelled :" + recentDistance);
            } else {
                Log.d(TAG, "Intent had no data returned");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getActivityName(int type) {
        switch (type)
        {
            case DetectedActivity.IN_VEHICLE:
                return "In Vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "On Bicycle";
            case DetectedActivity.ON_FOOT:
                return "On Foot";
            case DetectedActivity.WALKING:
                return "Walking";
            case DetectedActivity.STILL:
                return "Still";
            case DetectedActivity.TILTING:
                return "Tilting";
            case DetectedActivity.RUNNING:
                return "Running";
            case DetectedActivity.UNKNOWN:
                return "Unknown";
        }
        return "N/A";
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "GoogleApiClient connected.");
        if (!isRunning) {
            isRunning = true;
            stopAllSensors();
            startSensor(SensorType.ACTIVITY_DETECTOR);
        }
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
                Intent i = new Intent(this, PredictiveMotionDataService.class);
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
            Intent i = new Intent(this, PredictiveMotionDataService.class);
            mActivityRecognitionPendingIntent = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, activityDetectionRequestInterval, mActivityRecognitionPendingIntent);
        Log.d(TAG, "Activity detection started.");
    }

    private void stopActivityDetection() {
        if (mActivityRecognitionPendingIntent != null) {
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, mActivityRecognitionPendingIntent);
            Log.d(TAG, "Activity detection stopped.");
        }
    }

    private void resetActivityDetectionRequestInterval() {
        activityDetectionRequestInterval = initialActivityDetectionRequestInterval;
    }

    private long nextActivityDetectionRequestInterval() {
        long interval;
        interval = activityDetectionRequestInterval * 2;
        interval = interval >= maxActivityDetectionRequestInterval ?
                maxActivityDetectionRequestInterval : interval;
        return interval;
    }

    private void startDriving() {
        isDriving = true;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isRecording", true).commit();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isDriving", true).commit();
        Log.d(TAG, "Started driving.");
    }

    private void stopDriving() {
        isDriving = false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isRecording", false).commit();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isDriving", false).commit();
        resetActivityDetectionRequestInterval();
        recentLocations.clear();

        // Stop everything but activity detection
        stopSensor(SensorType.LINEAR_ACCELERATION);
        stopSensor(SensorType.GYROSCOPE);
        stopSensor(SensorType.ROTATION);
        stopSensor(SensorType.MAGNETIC);
        stopSensor(SensorType.LOCATION);

        Log.d(TAG, "Stopped driving.");
    }

    private double distanceBetweenPoints(Location point1, Location point2) {
        if (point1 == null || point2 == null)
            return 0.0;
        // approximate radius of earth in m
        double R = 6373000.0;

        double lat1 = Math.toRadians(point1.getLatitude());
        double lon1 = Math.toRadians(point1.getLongitude());
        double lat2 = Math.toRadians(point2.getLatitude());
        double lon2 = Math.toRadians(point2.getLongitude());

        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;

        double a = Math.pow(Math.sin(dlat / 2),2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    private double recentDistanceTravelled() {
        if (recentLocations.isEmpty() || recentLocations.size() == 1)
            return 0.0;

        double distance = 0.0;
        Iterator<Location> iter = recentLocations.iterator();
        Location prev = iter.hasNext() ? iter.next() : null;
        while (iter.hasNext()) {
            Location curr = iter.next();
            distance = distance + distanceBetweenPoints(prev, curr);
            prev = curr;
        }
        return distance;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        super.onDestroy();
        stopDriving();
        stopLocationUpdates();
        stopActivityDetection();
    }
}
