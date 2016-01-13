package ai.plex.poc.android.activities;

import android.app.IntentService;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import ai.plex.poc.android.R;
import ai.plex.poc.android.database.SnapShotContract;
import ai.plex.poc.android.database.SnapShotDBHelper;
import ai.plex.poc.android.services.Constants;
import ai.plex.poc.android.services.MotionDataService;

public class Welcome extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        CheckBox checkBox1 = (CheckBox) findViewById(R.id.isDrivingcheckBox);
        boolean checked = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("isDriving", false);
        checkBox1.setChecked(checked);

        //Start the background service
        Intent mServiceIntent = new Intent(this, MotionDataService.class);
        startService(mServiceIntent);
    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.isDrivingcheckBox:
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isDriving", checked).commit();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_welcome, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onSubmitDataClicked(View button){
        SQLiteDatabase db = new SnapShotDBHelper(this).getWritableDatabase();
        SubmitAcceleration(db);
        SubmitLinearAcceleration(db);
        SubmitGyroscope(db);
        SubmitMagnetic(db);
        SubmitRotation(db);
    }

    public void SubmitAcceleration(SQLiteDatabase db){
        try {
            Cursor cursor = db.rawQuery("Select * from " + SnapShotContract.AccelerationEntry.TABLE_NAME, null);

            while (cursor.moveToNext()) {

                Log.d("ACCELERATION", String.valueOf(cursor.getFloat(cursor.getColumnIndex(SnapShotContract.AccelerationEntry.COLUMN_X))));
                float x = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.AccelerationEntry.COLUMN_X));
                float y = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.AccelerationEntry.COLUMN_Y));
                float z = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.AccelerationEntry.COLUMN_Z));
                long timestamp = cursor.getLong(cursor.getColumnIndex(SnapShotContract.AccelerationEntry.COLUMN_TIMESTAMP));
                String isDriving = cursor.getString(cursor.getColumnIndex(SnapShotContract.AccelerationEntry.COLUMN_IS_DRIVING));

                JSONObject responseObject = new JSONObject();
                responseObject.put("deviceType", "Android");
                responseObject.put("deviceOsVersion", Build.VERSION.RELEASE);
                responseObject.put("timestamp", timestamp);
                responseObject.put("x", x);
                responseObject.put("y", y);
                responseObject.put("z", z);
                responseObject.put("isDriving", isDriving);
                responseObject.put("userId", "tjudi");

                new PostDataTask().execute(responseObject.toString(),"androidAcceleration");
            }
            db.delete(SnapShotContract.AccelerationEntry.TABLE_NAME, null, null);

        } catch (Exception ex){
            Log.e("Write Excption","Error writing data!");
        }
    }

    public void SubmitLinearAcceleration(SQLiteDatabase db){
        try {
            Cursor cursor = db.rawQuery("Select * from " + SnapShotContract.LinearAccelerationEntry.TABLE_NAME, null);

            while (cursor.moveToNext()) {

                float x = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.LinearAccelerationEntry.COLUMN_X));
                float y = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.LinearAccelerationEntry.COLUMN_Y));
                float z = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.LinearAccelerationEntry.COLUMN_Z));
                long timestamp = cursor.getLong(cursor.getColumnIndex(SnapShotContract.LinearAccelerationEntry.COLUMN_TIMESTAMP));
                String isDriving = cursor.getString(cursor.getColumnIndex(SnapShotContract.LinearAccelerationEntry.COLUMN_IS_DRIVING));

                JSONObject responseObject = new JSONObject();
                responseObject.put("deviceType", "Android");
                responseObject.put("deviceOsVersion", Build.VERSION.RELEASE);
                responseObject.put("timestamp", timestamp);
                responseObject.put("x", x);
                responseObject.put("y", y);
                responseObject.put("z", z);
                responseObject.put("isDriving", isDriving);
                responseObject.put("userId", "tjudi");

                new PostDataTask().execute(responseObject.toString(),"androidLinearAcceleration");
            }

            db.delete(SnapShotContract.LinearAccelerationEntry.TABLE_NAME, null, null);

        } catch (Exception ex){
            Log.e("Write Excption","Error writing data!");
        }
    }

    public void SubmitGyroscope(SQLiteDatabase db){
        try {
            Cursor cursor = db.rawQuery("Select * from " + SnapShotContract.GyroscopeEntry.TABLE_NAME, null);

            while (cursor.moveToNext()) {

                float x = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_X));
                float y = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_Y));
                float z = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_Z));
                long timestamp = cursor.getLong(cursor.getColumnIndex(SnapShotContract.GyroscopeEntry.COLUMN_TIMESTAMP));
                String isDriving = cursor.getString(cursor.getColumnIndex(SnapShotContract.GyroscopeEntry.COLUMN_IS_DRIVING));

                JSONObject responseObject = new JSONObject();
                responseObject.put("deviceType", "Android");
                responseObject.put("deviceOsVersion", Build.VERSION.RELEASE);
                responseObject.put(SnapShotContract.GyroscopeEntry.COLUMN_TIMESTAMP, timestamp);
                responseObject.put(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_X, x);
                responseObject.put(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_X, y);
                responseObject.put(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_X, z);
                responseObject.put(SnapShotContract.GyroscopeEntry.COLUMN_IS_DRIVING, isDriving);
                responseObject.put("userId", "tjudi");

                new PostDataTask().execute(responseObject.toString(),"androidGyroscope");
            }

            db.delete(SnapShotContract.GyroscopeEntry.TABLE_NAME, null, null);

        } catch (Exception ex){
            Log.e("Write Excption","Error writing data!");
        }
    }

    public void SubmitMagnetic(SQLiteDatabase db){
        try {
            Cursor cursor = db.rawQuery("Select * from " + SnapShotContract.MagneticEntry.TABLE_NAME, null);

            while (cursor.moveToNext()) {

                float x = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.MagneticEntry.COLUMN_X));
                float y = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.MagneticEntry.COLUMN_Y));
                float z = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.MagneticEntry.COLUMN_Z));
                long timestamp = cursor.getLong(cursor.getColumnIndex(SnapShotContract.MagneticEntry.COLUMN_TIMESTAMP));
                String isDriving = cursor.getString(cursor.getColumnIndex(SnapShotContract.MagneticEntry.COLUMN_IS_DRIVING));

                JSONObject responseObject = new JSONObject();
                responseObject.put("deviceType", "Android");
                responseObject.put("deviceOsVersion", Build.VERSION.RELEASE);
                responseObject.put(SnapShotContract.MagneticEntry.COLUMN_TIMESTAMP, timestamp);
                responseObject.put(SnapShotContract.MagneticEntry.COLUMN_X, x);
                responseObject.put(SnapShotContract.MagneticEntry.COLUMN_Y, y);
                responseObject.put(SnapShotContract.MagneticEntry.COLUMN_Z, z);
                responseObject.put(SnapShotContract.MagneticEntry.COLUMN_IS_DRIVING, isDriving);
                responseObject.put("userId", "tjudi");

                new PostDataTask().execute(responseObject.toString(),"androidMagnetic");
            }

            db.delete(SnapShotContract.MagneticEntry.TABLE_NAME, null, null);

        } catch (Exception ex){
            Log.e("Write Excption","Error writing data!");
        }
    }

    public void SubmitRotation(SQLiteDatabase db){
        try {
            Cursor cursor = db.rawQuery("Select * from " + SnapShotContract.RotationEntry.TABLE_NAME, null);

            while (cursor.moveToNext()) {

                float x = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.RotationEntry.COLUMN_X_SIN));
                float y = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.RotationEntry.COLUMN_Y_SIN));
                float z = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.RotationEntry.COLUMN_Z_SIN));
                float cos = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.RotationEntry.COLUMN_COS));
                float accuracy = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.RotationEntry.COLUMN_ACCURACY));
                long timestamp = cursor.getLong(cursor.getColumnIndex(SnapShotContract.RotationEntry.COLUMN_TIMESTAMP));
                String isDriving = cursor.getString(cursor.getColumnIndex(SnapShotContract.RotationEntry.COLUMN_IS_DRIVING));

                JSONObject responseObject = new JSONObject();
                responseObject.put("deviceType", "Android");
                responseObject.put("deviceOsVersion", Build.VERSION.RELEASE);
                responseObject.put(SnapShotContract.RotationEntry.COLUMN_TIMESTAMP, timestamp);
                responseObject.put(SnapShotContract.RotationEntry.COLUMN_X_SIN, x);
                responseObject.put(SnapShotContract.RotationEntry.COLUMN_Y_SIN, y);
                responseObject.put(SnapShotContract.RotationEntry.COLUMN_Z_SIN, z);
                responseObject.put(SnapShotContract.RotationEntry.COLUMN_COS, cos);
                responseObject.put(SnapShotContract.RotationEntry.COLUMN_ACCURACY, accuracy);
                responseObject.put(SnapShotContract.RotationEntry.COLUMN_IS_DRIVING, isDriving);
                responseObject.put("userId", "tjudi");

                new PostDataTask().execute(responseObject.toString(),"androidRotation");
            }

            db.delete(SnapShotContract.RotationEntry.TABLE_NAME, null, null);

        } catch (Exception ex){
            Log.e("Write Excption","Error writing data!");
        }
    }



    public class PostDataTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... events){

            ConnectivityManager connMgr = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                InputStream is = null;
                // Only display the first 500 characters of the retrieved
                // web page content.
                int len = 500;

                try {
                    //Define the URL
                    //URL url = new URL("http://10.42.0.1:8080/"+ events[1]);
                    URL url = new URL("http://40.122.215.160:8080/"+ events[1]);

                    String message = events[0];

                    //Open a connection
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    //Set connection details
                    conn.setReadTimeout(10000 /* milliseconds */);
                    conn.setConnectTimeout(15000 /* milliseconds */);
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);


                    //Set header details
                    conn.setRequestProperty("Content-Type","application/json;charset=utf-8");
                    conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");

                    //Connect
                    conn.connect();

                    //Setup data to send
                    OutputStream os = new BufferedOutputStream(conn.getOutputStream());
                    os.write(message.getBytes());
                    os.flush();

                    //Write the data
                    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                    wr.write(events[0]);
                    wr.flush();

                    int response = conn.getResponseCode();
                    Log.d("DEBUG_TAG", "The response is: " + response);
                    is = conn.getInputStream();
                    // Convert the InputStream into a string
                    String contentAsString = readIt(is, len);
                    Log.d("DEBUG_TAG", "The response data is: " + contentAsString);
                } catch (Exception e) {
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
