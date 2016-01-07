package ai.plex.poc.android.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import ai.plex.poc.android.sensorListeners.AccelerationMonitor;

/**
 * Intent service used to obtain telematics data from the phone
 */
public class MotionDataService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     *
     */
    public MotionDataService() {
        super("MotionDataService");
    }



    /**
     *
     * @param workIntent
     */
    @Override
    protected void onHandleIntent(Intent workIntent) {
        // Gets data from the incoming Intent
        String dataString = workIntent.getDataString();


        try {
            readSensorData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readSensorData() {
        SensorManager mSensorManager= (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor: deviceSensors
             ) {
            Log.d("Sensors", "readSensorData: " + sensor.toString());
        }

        Sensor accelerationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if ( accelerationSensor!= null){
            mSensorManager.registerListener(new AccelerationMonitor(this.getApplicationContext()), accelerationSensor, 10000);
        }
        else
        {

        }

    }


}


