package ai.plex.poc.android.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import ai.plex.poc.android.R;
import ai.plex.poc.android.broadcastReceivers.WifiBroadcastReceiver;
import ai.plex.poc.android.sensorListeners.AccelerationMonitor;
import ai.plex.poc.android.sensorListeners.GyroscopeMonitor;
import ai.plex.poc.android.sensorListeners.LinearAccelerationMonitor;
import ai.plex.poc.android.sensorListeners.MagneticMonitor;
import ai.plex.poc.android.sensorListeners.RotationMonitor;

/**
 * Intent service used to obtain telematics data from the phone
 */
public class MotionDataService extends IntentService {

    private boolean isDriving;


    public MotionDataService() {
        super("MotionDataService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //final IntentFilter wifiFilter = new IntentFilter();
        //wifiFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        //wifiFilter.addAction("android.net.wifi.STATE_CHANGE");
        //registerReceiver(new WifiBroadcastReceiver(), wifiFilter);
    }

    /**
     *
     * @param workIntent
     */
    @Override
    protected void onHandleIntent(Intent workIntent) {
        // Gets data from the incoming Intent
        try {
            readSensorData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readSensorData() throws InterruptedException {
        SensorManager mSensorManager= (SensorManager) getSystemService(Context.SENSOR_SERVICE);


        Sensor accelerationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor linearAccelerationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor gyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor rotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        Sensor magneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        HandlerThread sensorHandlerThread = new HandlerThread("SesnorListener");

        sensorHandlerThread.start();

        Handler sensorHandler = new Handler(sensorHandlerThread.getLooper());

        if ( accelerationSensor!= null){
            mSensorManager.registerListener(new AccelerationMonitor(this.getApplicationContext()), accelerationSensor, 100000000, sensorHandler);
        }
        if (linearAccelerationSensor != null)
        {
            //mSensorManager.registerListener(new LinearAccelerationMonitor(this.getApplicationContext()), linearAccelerationSensor, 1000000, sensorHandler);
        }
        if (gyroscopeSensor != null)
        {
            //mSensorManager.registerListener(new GyroscopeMonitor(this.getApplicationContext()), gyroscopeSensor, 1000000, sensorHandler);
        }
        if (rotationSensor != null)
        {
            //mSensorManager.registerListener(new RotationMonitor(this.getApplicationContext()), rotationSensor, 1000000, sensorHandler);
        }
        if (magneticSensor != null)
        {
            //mSensorManager.registerListener(new MagneticMonitor(this.getApplicationContext()), magneticSensor, 1000000, sensorHandler);
        }
    }
}


