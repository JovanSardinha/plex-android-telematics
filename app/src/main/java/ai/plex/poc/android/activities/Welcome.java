package ai.plex.poc.android.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;
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
import java.util.ArrayList;
import java.util.List;

import ai.plex.poc.android.R;
import ai.plex.poc.android.database.SnapShotContract;
import ai.plex.poc.android.database.SnapShotDBHelper;
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

        setCheckBoxesFromPreferences();

        EditText usernameTextBox = (EditText) findViewById(R.id.usernameTextBox);
        usernameTextBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putString("username", s.toString()).commit();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //Start the background service
        Intent mServiceIntent = new Intent(this, MotionDataService.class);
        startService(mServiceIntent);
    }

    private void setCheckBoxesFromPreferences(){
        ToggleButton drivingToggle = (ToggleButton) findViewById(R.id.isDrivingToggleButton);
        ToggleButton recordingToggle = (ToggleButton) findViewById(R.id.isRecordingToggleButton);
        Switch accelerationSwitch = (Switch) findViewById(R.id.accelerationSwitch);
        Switch gyroscopeSwitch = (Switch) findViewById(R.id.gyroscopeSwitch);
        Switch magnoSwitch = (Switch) findViewById(R.id.magneticSwitch);
        Switch rotationSwitch = (Switch) findViewById(R.id.rotationSwitch);
        EditText usernameText = (EditText) findViewById(R.id.usernameTextBox);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDriving = prefs.getBoolean("isDriving", false);
        boolean isRecording = prefs.getBoolean("isRecording", false);
        boolean isTrackingAcceleration = prefs.getBoolean("isTrackingAcceleration", false);
        boolean isTrackingGyroscope = prefs.getBoolean("isTrackingGyroscope", false);
        boolean isTrackingMagnetic = prefs.getBoolean("isTrackingMagnetic", false);
        boolean isTrackingRotation = prefs.getBoolean("isTrackingRotation", false);
        String username = prefs.getString("username", "default_user");

        recordingToggle.setChecked(isRecording);
        drivingToggle.setChecked(isDriving);
        accelerationSwitch.setChecked(isTrackingAcceleration);
        gyroscopeSwitch.setChecked(isTrackingGyroscope);
        magnoSwitch.setChecked(isTrackingMagnetic);
        rotationSwitch.setChecked(isTrackingRotation);
        usernameText.setText(username);
    }

    public void onClicked(View view) {
        boolean checked = false;
        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.isDrivingToggleButton:
                checked = ((ToggleButton) view).isChecked();
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isDriving", checked).commit();
                break;
            case R.id.isRecordingToggleButton:
                checked = ((ToggleButton) view).isChecked();
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isRecording", checked).commit();
                restartMotionDataService();
                break;
            case R.id.accelerationSwitch:
                checked = ((Switch) view).isChecked();
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isTrackingAcceleration", checked).commit();
                restartMotionDataService();
                break;
            case R.id.gyroscopeSwitch:
                checked = ((Switch) view).isChecked();
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isTrackingGyroscope", checked).commit();
                restartMotionDataService();
                break;
            case R.id.magneticSwitch:
                checked = ((Switch) view).isChecked();
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isTrackingMagnetic", checked).commit();
                restartMotionDataService();
                break;
            case R.id.rotationSwitch:
                checked = ((Switch) view).isChecked();
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isTrackingRotation", checked).commit();
                restartMotionDataService();
                break;
            case R.id.clearButton:
                SnapShotDBHelper.clearTables(SnapShotDBHelper.getsInstance(this).getWritableDatabase());
                break;
            case R.id.submitButton:
                try {
                    String username = PreferenceManager.getDefaultSharedPreferences(this).getString("username", "default_user");
                    SubmitLinearAcceleration(username);
                    SubmitGyroscope(username);
                    SubmitMagnetic(username);
                    SubmitRotation(username);
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                }
        }
    }


    private void restartMotionDataService() {
        //Restart Service
        Intent mServiceIntent = new Intent(this, MotionDataService.class);
        mServiceIntent.putExtra("Restart", true);
        startService(mServiceIntent);
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

    public void SubmitAcceleration(String username) {
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(this).getWritableDatabase();
        ArrayList<JSONObject> data = new ArrayList();
        Cursor cursor = null;
        Integer lastRecord = -1;
        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.AccelerationEntry.TABLE_NAME, null);

            while (cursor.moveToNext()) {

                Log.d("ACCELERATION", String.valueOf(cursor.getFloat(cursor.getColumnIndex(SnapShotContract.AccelerationEntry.COLUMN_X))));
                lastRecord = cursor.getInt(cursor.getColumnIndex(SnapShotContract.AccelerationEntry._ID));
                float x = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.AccelerationEntry.COLUMN_X));
                float y = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.AccelerationEntry.COLUMN_Y));
                float z = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.AccelerationEntry.COLUMN_Z));
                long timestamp = cursor.getLong(cursor.getColumnIndex(SnapShotContract.AccelerationEntry.COLUMN_TIMESTAMP));
                String isDriving = cursor.getString(cursor.getColumnIndex(SnapShotContract.AccelerationEntry.COLUMN_IS_DRIVING));

                JSONObject responseObject = new JSONObject();
                responseObject.put("deviceType", "Android");
                responseObject.put("deviceOsVersion", Build.VERSION.RELEASE);
                responseObject.put(SnapShotContract.AccelerationEntry.COLUMN_TIMESTAMP, timestamp);
                responseObject.put(SnapShotContract.AccelerationEntry.COLUMN_X, x);
                responseObject.put(SnapShotContract.AccelerationEntry.COLUMN_Y, y);
                responseObject.put(SnapShotContract.AccelerationEntry.COLUMN_Z, z);
                responseObject.put(SnapShotContract.AccelerationEntry.COLUMN_IS_DRIVING, isDriving);
                responseObject.put("userId", username);

                data.add(responseObject);
            }
            for ( int i = 0; i < data.size(); i = i+10){
                if (i * (data.size()/10) <= data.size()) {
                    List set = data.subList(i, i + 10);
                    new PostDataTask().execute(set);
                } else {
                    List set = data.subList(i, data.size());
                    new PostDataTask().execute(set);
                }

            }
            int count = db.delete(SnapShotContract.AccelerationEntry.TABLE_NAME, SnapShotContract.AccelerationEntry._ID + "<=?", new String[] { String.valueOf(lastRecord)});
            Log.d("DELETED RECORDS", String.valueOf(count));
        } catch (Exception ex) {
            Log.e("Write Excption", "Error writing data!");
        } finally {
            cursor.close();
            db.close();
        }
    }

    public void SubmitLinearAcceleration(String username) {
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(this).getWritableDatabase();
        ArrayList<JSONObject> data = new ArrayList();
        Integer lastRecord = -1;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.LinearAccelerationEntry.TABLE_NAME, null);

            while (cursor.moveToNext()) {
                lastRecord = cursor.getInt(cursor.getColumnIndex(SnapShotContract.LinearAccelerationEntry._ID));
                float x = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.LinearAccelerationEntry.COLUMN_X));
                float y = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.LinearAccelerationEntry.COLUMN_Y));
                float z = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.LinearAccelerationEntry.COLUMN_Z));
                long timestamp = cursor.getLong(cursor.getColumnIndex(SnapShotContract.LinearAccelerationEntry.COLUMN_TIMESTAMP));
                String isDriving = cursor.getString(cursor.getColumnIndex(SnapShotContract.LinearAccelerationEntry.COLUMN_IS_DRIVING));

                JSONObject responseObject = new JSONObject();
                responseObject.put("deviceType", "Android");
                responseObject.put("deviceOsVersion", Build.VERSION.RELEASE);
                responseObject.put("dataType",SnapShotContract.LinearAccelerationEntry.TABLE_NAME);
                responseObject.put(SnapShotContract.LinearAccelerationEntry.COLUMN_TIMESTAMP, timestamp);
                responseObject.put(SnapShotContract.LinearAccelerationEntry.COLUMN_X, x);
                responseObject.put(SnapShotContract.LinearAccelerationEntry.COLUMN_Y, y);
                responseObject.put(SnapShotContract.LinearAccelerationEntry.COLUMN_Z, z);
                responseObject.put(SnapShotContract.LinearAccelerationEntry.COLUMN_IS_DRIVING, isDriving);
                responseObject.put("userId", username);

                data.add(responseObject);
            }

            for ( int i = 0; i < data.size(); i = i+10){
                if (i+10 <= data.size()) {
                    List set = data.subList(i, i + 10);
                    new PostDataTask().execute(set);
                } else {
                    List set = data.subList(i, data.size());
                    new PostDataTask().execute(set);
                }
            }

            int count = db.delete(SnapShotContract.LinearAccelerationEntry.TABLE_NAME, SnapShotContract.LinearAccelerationEntry._ID + "<=?", new String[] { String.valueOf(lastRecord)});
            Log.d("DELETED RECORDS", String.valueOf(count));
        } catch (Exception ex) {
            Log.e("Write Excption", "Error writing data!");
        } finally {
            cursor.close();
            db.close();
        }
    }

    public void SubmitGyroscope(String username) {
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(this).getWritableDatabase();
        ArrayList<JSONObject> data = new ArrayList();
        Integer lastRecord = -1;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.GyroscopeEntry.TABLE_NAME, null);

            while (cursor.moveToNext()) {
                lastRecord = cursor.getInt(cursor.getColumnIndex(SnapShotContract.GyroscopeEntry._ID));
                float x = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_X));
                float y = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_Y));
                float z = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_Z));
                long timestamp = cursor.getLong(cursor.getColumnIndex(SnapShotContract.GyroscopeEntry.COLUMN_TIMESTAMP));
                String isDriving = cursor.getString(cursor.getColumnIndex(SnapShotContract.GyroscopeEntry.COLUMN_IS_DRIVING));

                JSONObject responseObject = new JSONObject();
                responseObject.put("deviceType", "Android");
                responseObject.put("deviceOsVersion", Build.VERSION.RELEASE);
                responseObject.put("dataType",SnapShotContract.GyroscopeEntry.TABLE_NAME);
                responseObject.put(SnapShotContract.GyroscopeEntry.COLUMN_TIMESTAMP, timestamp);
                responseObject.put(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_X, x);
                responseObject.put(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_X, y);
                responseObject.put(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_X, z);
                responseObject.put(SnapShotContract.GyroscopeEntry.COLUMN_IS_DRIVING, isDriving);
                responseObject.put("userId", username);

                data.add(responseObject);
            }
            for ( int i = 0; i < data.size(); i = i+10){
                if (i+10 <= data.size()) {
                    List set = data.subList(i, i + 10);
                    new PostDataTask().execute(set);
                } else {
                    List set = data.subList(i, data.size());
                    new PostDataTask().execute(set);
                }
            }
            int count = db.delete(SnapShotContract.GyroscopeEntry.TABLE_NAME, SnapShotContract.GyroscopeEntry._ID + "<=?", new String[] { String.valueOf(lastRecord)});
            Log.d("DELETED RECORDS", String.valueOf(count));
        } catch (Exception ex) {
            Log.e("Write Excption", "Error writing data!");
        } finally {
            cursor.close();
            db.close();
        }
    }

    public void SubmitMagnetic(String username) {
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(this).getWritableDatabase();
        ArrayList<JSONObject> data = new ArrayList();
        Integer lastRecord = -1;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.MagneticEntry.TABLE_NAME, null);

            while (cursor.moveToNext()) {
                lastRecord = cursor.getInt(cursor.getColumnIndex(SnapShotContract.MagneticEntry._ID));
                float x = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.MagneticEntry.COLUMN_X));
                float y = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.MagneticEntry.COLUMN_Y));
                float z = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.MagneticEntry.COLUMN_Z));
                long timestamp = cursor.getLong(cursor.getColumnIndex(SnapShotContract.MagneticEntry.COLUMN_TIMESTAMP));
                String isDriving = cursor.getString(cursor.getColumnIndex(SnapShotContract.MagneticEntry.COLUMN_IS_DRIVING));

                JSONObject responseObject = new JSONObject();
                responseObject.put("deviceType", "Android");
                responseObject.put("deviceOsVersion", Build.VERSION.RELEASE);
                responseObject.put("dataType",SnapShotContract.MagneticEntry.TABLE_NAME);
                responseObject.put(SnapShotContract.MagneticEntry.COLUMN_TIMESTAMP, timestamp);
                responseObject.put(SnapShotContract.MagneticEntry.COLUMN_X, x);
                responseObject.put(SnapShotContract.MagneticEntry.COLUMN_Y, y);
                responseObject.put(SnapShotContract.MagneticEntry.COLUMN_Z, z);
                responseObject.put(SnapShotContract.MagneticEntry.COLUMN_IS_DRIVING, isDriving);
                responseObject.put("userId", username);
                data.add(responseObject);
            }
            for ( int i = 0; i < data.size(); i = i+10){
                if (i+10 <= data.size()) {
                    List set = data.subList(i, i + 10);
                    new PostDataTask().execute(set);
                } else {
                    List set = data.subList(i, data.size());
                    new PostDataTask().execute(set);
                }
            }
            int count = db.delete(SnapShotContract.MagneticEntry.TABLE_NAME, SnapShotContract.MagneticEntry._ID + "<=?", new String[] { String.valueOf(lastRecord)});
            Log.d("DELETED RECORDS", String.valueOf(count));
        } catch (Exception ex) {
            Log.e("Write Excption", "Error writing data!");
        } finally {
            cursor.close();
            db.close();
        }
    }

    public void SubmitRotation(String username) {
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(this).getWritableDatabase();
        ArrayList<JSONObject> data = new ArrayList();
        Integer lastRecord = -1;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.RotationEntry.TABLE_NAME, null);
            while (cursor.moveToNext()) {
                lastRecord = cursor.getInt(cursor.getColumnIndex(SnapShotContract.RotationEntry._ID));
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
                responseObject.put("dataType",SnapShotContract.RotationEntry.TABLE_NAME);
                responseObject.put(SnapShotContract.RotationEntry.COLUMN_TIMESTAMP, timestamp);
                responseObject.put(SnapShotContract.RotationEntry.COLUMN_X_SIN, x);
                responseObject.put(SnapShotContract.RotationEntry.COLUMN_Y_SIN, y);
                responseObject.put(SnapShotContract.RotationEntry.COLUMN_Z_SIN, z);
                responseObject.put(SnapShotContract.RotationEntry.COLUMN_COS, cos);
                responseObject.put(SnapShotContract.RotationEntry.COLUMN_ACCURACY, accuracy);
                responseObject.put(SnapShotContract.RotationEntry.COLUMN_IS_DRIVING, isDriving);
                responseObject.put("userId", username);

                data.add(responseObject);
            }

            for ( int i = 0; i < data.size(); i = i+10){
                if (i+10 <= data.size()) {
                    List set = data.subList(i, i + 10);
                    new PostDataTask().execute(set);
                } else {
                    List set = data.subList(i, data.size());
                    new PostDataTask().execute(set);
                }
            }

            int count = db.delete(SnapShotContract.RotationEntry.TABLE_NAME, SnapShotContract.RotationEntry._ID + "<=?", new String[] { String.valueOf(lastRecord)});
            Log.d("DELETED RECORDS", String.valueOf(count));
        } catch (Exception ex) {
            Log.e("Write Excption", "Error writing data!");
        } finally {
            cursor.close();
            db.close();
        }
    }

    public class PostDataTask extends AsyncTask<List<JSONObject>, Void, Void> {
        protected Void doInBackground(List<JSONObject>... events){

            ConnectivityManager connMgr = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

            String dataType = "";
            String api_route = "";
            try {
                 dataType = events[0].get(0).get("dataType").toString();
                switch (dataType){
                    case SnapShotContract.LinearAccelerationEntry.TABLE_NAME:
                        api_route = "androidLinearAccelerations";
                        break;
                    case SnapShotContract.GyroscopeEntry.TABLE_NAME:
                        api_route = "androidGyroscopes";
                        break;
                    case SnapShotContract.MagneticEntry.TABLE_NAME:
                        api_route = "androidMagnetics";
                        break;
                    case SnapShotContract.RotationEntry.TABLE_NAME:
                        api_route = "androidRotations";
                        break;
                }
            } catch (Exception e){
                e.printStackTrace();
            }

            if (networkInfo != null && networkInfo.isConnected()) {
                    JSONObject requestData = new JSONObject();
                    JSONArray dataPoints = new JSONArray(events[0]);
                    try {
                        requestData.put("entries", dataPoints);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    InputStream is = null;
                    // Only display the first 500 characters of the retrieved
                    // web page content.

                    try {
                        //Define the URL
                        URL url = new URL("http://"+ ai.plex.poc.android.activities.Constants.IP_ADDRESS +"/"+ api_route);

                        String message = requestData.toString();

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
                        wr.write(message);
                        wr.flush();

                        int response = conn.getResponseCode();
                        Log.d("Records submitted", "The response is: " + response);
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
                Log.d("Network Connection", "WIFI is not connected, data can't be submitted");
            }
            return null;
        }

    }
}
