package ai.plex.poc.android.sensorListeners;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
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
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Date;

import ai.plex.poc.android.R;
import ai.plex.poc.android.database.SnapShotContract;
import ai.plex.poc.android.database.SnapShotDBHelper;

/**
 * Created by terek on 07/01/16.
 */
public class AccelerationMonitor implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mLight;
    private Context applicationContext;

    public AccelerationMonitor(Context context){
        this.applicationContext = context;
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        // The light sensor returns a single value.
        // Many sensors return 3 values, one for each axis.
        try {

            //Get the database
            SQLiteDatabase db = new SnapShotDBHelper(applicationContext).getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(SnapShotContract.AccelerationEntry.COLUMN_X, event.values[0]);
            values.put(SnapShotContract.AccelerationEntry.COLUMN_Y, event.values[1]);
            values.put(SnapShotContract.AccelerationEntry.COLUMN_Z, event.values[2]);
            //Round off the milliseconds to seconds
            long timestamp = ((new Date().getTime() + 500) /1000)*1000;
            values.put(SnapShotContract.AccelerationEntry.COLUMN_TIMESTAMP, timestamp);


            long rowId = db.insert(SnapShotContract.AccelerationEntry.TABLE_NAME,null,values);

            Cursor cursor = db.query(SnapShotContract.AccelerationEntry.TABLE_NAME, null, SnapShotContract.AccelerationEntry.COLUMN_TIMESTAMP + "= ?", new String[]{String.valueOf(timestamp)}, null, null, null);

            cursor.moveToFirst();

            float x = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.AccelerationEntry.COLUMN_X));
            float y = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.AccelerationEntry.COLUMN_Y));
            float z = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.AccelerationEntry.COLUMN_Z));
            long stamp = cursor.getLong(cursor.getColumnIndex(SnapShotContract.AccelerationEntry.COLUMN_TIMESTAMP));


            JSONObject accelearationObject = new JSONObject();
            accelearationObject.put("x", x);
            accelearationObject.put("y", y);
            accelearationObject.put("z", z);

            JSONObject responseObject = new JSONObject();
            responseObject.put("accelerometer", accelearationObject);

            new PostDataTask().execute(responseObject.toString());

        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public class PostDataTask extends AsyncTask<String, Void, Void>{
        protected Void doInBackground(String... events){

            ConnectivityManager connMgr = (ConnectivityManager)
                    applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                InputStream is = null;
                // Only display the first 500 characters of the retrieved
                // web page content.
                int len = 500;

                try {

                    URL url = new URL("http://40.122.215.160:8080/snapShots");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    conn.setRequestProperty("Content-Type","application/json");
                    conn.setReadTimeout(10000 /* milliseconds */);
                    conn.setConnectTimeout(15000 /* milliseconds */);
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);

                    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                    wr.write(events[0]);
                    wr.flush();

                    // Starts the query
                    conn.connect();
                    int response = conn.getResponseCode();
                    Log.d("DEBUG_TAG", "The response is: " + response);
                    is = conn.getInputStream();

                    // Convert the InputStream into a string
                    String contentAsString = readIt(is, len);
                    Log.d("DEBUG_TAG", "The response data is: " + contentAsString);

                    Thread.sleep(10000);

                    // Makes sure that the InputStream is closed after the app is
                    // finished using it.
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                // display error
            }

            return null;
        }

    }

    // Reads an InputStream and converts it to a String.
    public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }
}