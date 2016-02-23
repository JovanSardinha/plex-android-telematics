package ai.plex.poc.android.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
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
import android.widget.TextView;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ai.plex.poc.android.R;
import ai.plex.poc.android.database.SnapShotContract;
import ai.plex.poc.android.database.SnapShotDBHelper;
import ai.plex.poc.android.sensorListeners.SensorType;
import ai.plex.poc.android.services.MotionDataService;

public class Welcome extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private final static String TAG = Welcome.class.getSimpleName();

    // Constant for requesting location permissions
    final int requestLocationPermissionId = 123;

    MotionDataService mService;
    boolean mBound = false;
    ActivityUpdateReceiver mActivityUpdateReceiver;
    LocationUpdateReceiver mLocationUpdateReceiver;

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

        EditText usernameTextBox = (EditText) findViewById(R.id.usernameTextBox);
        String username = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("username", null);
        if (username != null) {
            usernameTextBox.setText(username);
        }

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

        // Request location permissions if not granted already
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("LOCATION", "Requesting Fine Location permissions.");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    requestLocationPermissionId);
        }

        // The filter's action is ACTIVITY_UPDATE_BROADCAST_ACTION
        IntentFilter activityUpdateIntentFilter = new IntentFilter(
                ai.plex.poc.android.services.Constants.ACTIVITY_UPDATE_BROADCAST_ACTION);

        // Instantiates a new ActivityUpdateReceiver
        mActivityUpdateReceiver = new ActivityUpdateReceiver();

        // Registers the DownloadStateReceiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mActivityUpdateReceiver,
                activityUpdateIntentFilter);

        // The filter's action is LOCATION_UPDATE_BROADCAST_ACTION
        IntentFilter locationIntentFilter = new IntentFilter(
                ai.plex.poc.android.services.Constants.LOCATION_UPDATE_BROADCAST_ACTION);

        // Instantiates a new LocationUpdateReceiver
        mLocationUpdateReceiver = new LocationUpdateReceiver();

        // Registers the DownloadStateReceiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mLocationUpdateReceiver,
                locationIntentFilter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Start the background service
        Intent mServiceIntent = new Intent(this, MotionDataService.class);

        if (MotionDataService.isRunning()) {
            mServiceIntent.putExtra("ai.plex.poc.android.startService", true);
            startService(mServiceIntent);
        }

        bindService(mServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to MotionDataService, cast the IBinder and get MotionDataService instance
            MotionDataService.MotionDataBinder binder = (MotionDataService.MotionDataBinder) service;
            mService = binder.getService();
            mBound = true;
            Log.d(TAG, "Bound to MotionDataService.");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            Log.d(TAG, "Unbound from MotionDataService.");
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case requestLocationPermissionId: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("LOCATION", "Location permissions granted :)");
                } else {
                    Log.e("LOCATION", "Location permissions not granted :(");
                }
                return;
            }
        }
    }

    private void toggleSwitches(boolean on) {
        Switch accelerationSwitch = (Switch) findViewById(R.id.accelerationSwitch);
        Switch gyroscopeSwitch = (Switch) findViewById(R.id.gyroscopeSwitch);
        Switch magneticSwitch = (Switch) findViewById(R.id.magneticSwitch);
        Switch rotationSwitch = (Switch) findViewById(R.id.rotationSwitch);
        Switch locationSwitch = (Switch) findViewById(R.id.locationSwitch);
        Switch activityDetectionSwitch = (Switch) findViewById(R.id.activityDetectionSwitch);

        accelerationSwitch.setChecked(on);
        gyroscopeSwitch.setChecked(on);
        magneticSwitch.setChecked(on);
        rotationSwitch.setChecked(on);
        locationSwitch.setChecked(on);
        activityDetectionSwitch.setChecked(on);
    }

    public void onClicked(View view) {
        if (!mBound) {
            Log.d(TAG, "MotionDataService not bound. Bind service to start updates.");
            return;
        }

        TextView statusTextBox = (TextView) findViewById(R.id.statusText);

        boolean checked = false;
        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.isDrivingToggleButton:
                checked = ((ToggleButton) view).isChecked();
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isDriving", checked).commit();
                statusTextBox.setText("Driving - " + (checked? "ON" : "OFF"));
                break;
            case R.id.isRecordingToggleButton:
                checked = ((ToggleButton) view).isChecked();
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isRecording", checked).commit();
                toggleSwitches(checked);
                statusTextBox.setText("Recording - " + (checked ? "ON" : "OFF"));
                if (checked) {
                    mService.startAllSensors();
                } else {
                    mService.stopAllSensors();
                }
                break;
            case R.id.accelerationSwitch:
                checked = ((Switch) view).isChecked();
                if (checked) {
                    mService.startSensor(SensorType.LINEAR_ACCELERATION);
                } else {
                    mService.stopSensor(SensorType.LINEAR_ACCELERATION);
                }
                break;
            case R.id.gyroscopeSwitch:
                checked = ((Switch) view).isChecked();
                if (checked) {
                    mService.startSensor(SensorType.GYROSCOPE);
                } else {
                    mService.stopSensor(SensorType.GYROSCOPE);
                }
                break;
            case R.id.magneticSwitch:
                checked = ((Switch) view).isChecked();
                if (checked) {
                    mService.startSensor(SensorType.MAGNETIC);
                } else {
                    mService.stopSensor(SensorType.MAGNETIC);
                }
                break;
            case R.id.rotationSwitch:
                checked = ((Switch) view).isChecked();
                if (checked) {
                    mService.startSensor(SensorType.ROTATION);
                } else {
                    mService.stopSensor(SensorType.ROTATION);
                }
                break;
            case R.id.locationSwitch:
                checked = ((Switch) view).isChecked();
                if (checked) {
                    mService.startSensor(SensorType.LOCATION);
                } else {
                    mService.stopSensor(SensorType.LOCATION);
                }
                break;
            case R.id.activityDetectionSwitch:
                checked = ((Switch) view).isChecked();
                if (checked) {
                    mService.startSensor(SensorType.ACTIVITY_DETECTOR);
                } else {
                    mService.stopSensor(SensorType.ACTIVITY_DETECTOR);
                }
                break;
            case R.id.clearButton:
                try {
                    SnapShotDBHelper.clearTables(SnapShotDBHelper.getsInstance(this).getWritableDatabase());
                    statusTextBox.setText("Cleared data");
                } catch (Exception e) {
                    statusTextBox.setText("Error clearing data");
                }
                break;
            case R.id.submitButton:
                try {
                    mService.stopAllSensors();
                    stopRecording();

                    String username = PreferenceManager.getDefaultSharedPreferences(this).getString("username", "default_user");
                    SubmitLinearAcceleration(username);
                    SubmitGyroscope(username);
                    SubmitMagnetic(username);
                    SubmitRotation(username);
                    SubmitLocation(username);
                    SubmitDetectedActivity(username);
                    statusTextBox.setText("Submitted data");

                } catch (Exception ex) {
                    ex.printStackTrace();
                    statusTextBox.setText("Error submitting data");
                }
        }
    }

    private void stopRecording() {

        ToggleButton drivingToggle = (ToggleButton) findViewById(R.id.isDrivingToggleButton);
        ToggleButton recordingToggle = (ToggleButton) findViewById(R.id.isRecordingToggleButton);
        drivingToggle.setChecked(false);
        recordingToggle.setChecked(false);

        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isDriving", false).commit();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isRecording", false).commit();

        toggleSwitches(false);
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

            int page_size = 10;
            int num_pages = (data.size()/page_size) + 1;
            for (int page = 0; page < num_pages; page++){
                int start = page * page_size;
                int end = start + page_size;
                end = end > data.size() ? data.size() : end;
                List set = data.subList(start, end);
                new PostDataTask().execute(set);
            }

            int count = db.delete(SnapShotContract.LinearAccelerationEntry.TABLE_NAME, null, null);
            Log.d(TAG, "Deleted " + String.valueOf(count) + " linear acceleration records.");
        } catch (Exception ex) {
            Log.e(TAG, "Error submitting lienar acceleration data to API.");
            ex.printStackTrace();
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

            int page_size = 10;
            int num_pages = (data.size()/page_size) + 1;
            for (int page = 0; page < num_pages; page++){
                int start = page * page_size;
                int end = start + page_size;
                end = end > data.size() ? data.size() : end;
                List set = data.subList(start, end);
                new PostDataTask().execute(set);
            }
            int count = db.delete(SnapShotContract.GyroscopeEntry.TABLE_NAME, null, null);
            Log.d(TAG, "Deleted " + String.valueOf(count) + " gyroscope records.");
        } catch (Exception ex) {
            Log.e(TAG, "Error submitting gyroscope data to API.");
            ex.printStackTrace();
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

            int page_size = 10;
            int num_pages = (data.size()/page_size) + 1;
            for (int page = 0; page < num_pages; page++){
                int start = page * page_size;
                int end = start + page_size;
                end = end > data.size() ? data.size() : end;
                List set = data.subList(start, end);
                new PostDataTask().execute(set);
            }
            int count = db.delete(SnapShotContract.MagneticEntry.TABLE_NAME, null, null);
            Log.d(TAG, "Deleted " + String.valueOf(count) + " magnetic records.");
        } catch (Exception ex) {
            Log.e(TAG, "Error submitting magnetic data to API.");
            ex.printStackTrace();
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

            int page_size = 10;
            int num_pages = (data.size()/page_size) + 1;
            for (int page = 0; page < num_pages; page++){
                int start = page * page_size;
                int end = start + page_size;
                end = end > data.size() ? data.size() : end;
                List set = data.subList(start, end);
                new PostDataTask().execute(set);
            }

            int count = db.delete(SnapShotContract.RotationEntry.TABLE_NAME, null, null);
            Log.d(TAG, "Deleted " + String.valueOf(count) + " rotation records.");
        } catch (Exception ex) {
            Log.e(TAG, "Error submitting rotation data to API.");
            ex.printStackTrace();
        } finally {
            cursor.close();
            db.close();
        }
    }

    public void SubmitLocation(String username) {
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(this).getWritableDatabase();
        ArrayList<JSONObject> data = new ArrayList();
        Integer lastRecord = -1;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.LocationEntry.TABLE_NAME, null);
            while (cursor.moveToNext()) {
                lastRecord = cursor.getInt(cursor.getColumnIndex(SnapShotContract.LocationEntry._ID));
                float x = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.LocationEntry.COLUMN_LATITUDE));
                float y = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.LocationEntry.COLUMN_LONGITUDE));
                float z = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.LocationEntry.COLUMN_SPEED));
                long timestamp = cursor.getLong(cursor.getColumnIndex(SnapShotContract.LocationEntry.COLUMN_TIMESTAMP));
                String isDriving = cursor.getString(cursor.getColumnIndex(SnapShotContract.LocationEntry.COLUMN_IS_DRIVING));

                JSONObject responseObject = new JSONObject();
                responseObject.put("deviceType", "Android");
                responseObject.put("deviceOsVersion", Build.VERSION.RELEASE);
                responseObject.put("dataType",SnapShotContract.LocationEntry.TABLE_NAME);
                responseObject.put(SnapShotContract.LocationEntry.COLUMN_TIMESTAMP, timestamp);
                responseObject.put(SnapShotContract.LocationEntry.COLUMN_LATITUDE, x);
                responseObject.put(SnapShotContract.LocationEntry.COLUMN_LONGITUDE, y);
                responseObject.put(SnapShotContract.LocationEntry.COLUMN_SPEED, z);
                responseObject.put(SnapShotContract.LocationEntry.COLUMN_IS_DRIVING, isDriving);
                responseObject.put("userId", username);

                data.add(responseObject);
            }

            int page_size = 10;
            int num_pages = (data.size()/page_size) + 1;
            for (int page = 0; page < num_pages; page++){
                int start = page * page_size;
                int end = start + page_size;
                end = end > data.size() ? data.size() : end;
                List set = data.subList(start, end);
                new PostDataTask().execute(set);
            }

            int count = db.delete(SnapShotContract.LocationEntry.TABLE_NAME, null, null);
            Log.d(TAG, "Deleted " + String.valueOf(count) + " location records.");
        } catch (Exception ex) {
            Log.e(TAG, "Error submitting location data to API.");
            ex.printStackTrace();
        } finally {
            cursor.close();
            db.close();
        }
    }


    public void SubmitDetectedActivity(String username) {
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(this).getWritableDatabase();
        ArrayList<JSONObject> data = new ArrayList();
        Cursor cursor = null;
        Integer lastRecord = -1;
        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.DetectedActivityEntry.TABLE_NAME, null);

            while (cursor.moveToNext()) {
                lastRecord = cursor.getInt(cursor.getColumnIndex(SnapShotContract.DetectedActivityEntry._ID));
                int name = cursor.getInt(cursor.getColumnIndex(SnapShotContract.DetectedActivityEntry.COLUMN_NAME));
                int confidence = cursor.getInt(cursor.getColumnIndex(SnapShotContract.DetectedActivityEntry.COLUMN_CONFIDENCDE));
                long timestamp = cursor.getLong(cursor.getColumnIndex(SnapShotContract.DetectedActivityEntry.COLUMN_TIMESTAMP));
                String isDriving = cursor.getString(cursor.getColumnIndex(SnapShotContract.DetectedActivityEntry.COLUMN_IS_DRIVING));

                JSONObject responseObject = new JSONObject();
                responseObject.put("deviceType", "Android");
                responseObject.put("deviceOsVersion", Build.VERSION.RELEASE);
                responseObject.put("dataType",SnapShotContract.DetectedActivityEntry.TABLE_NAME);
                responseObject.put(SnapShotContract.DetectedActivityEntry.COLUMN_TIMESTAMP, timestamp);
                responseObject.put(SnapShotContract.DetectedActivityEntry.COLUMN_NAME, name);
                responseObject.put(SnapShotContract.DetectedActivityEntry.COLUMN_CONFIDENCDE, confidence);
                responseObject.put(SnapShotContract.DetectedActivityEntry.COLUMN_IS_DRIVING, isDriving);
                responseObject.put("userId", username);

                data.add(responseObject);
            }

            int page_size = 10;
            int num_pages = (data.size()/page_size) + 1;
            for (int page = 0; page < num_pages; page++){
                int start = page * page_size;
                int end = start + page_size;
                end = end > data.size() ? data.size() : end;
                List set = data.subList(start, end);
                new PostDataTask().execute(set);
            }
            int count = db.delete(SnapShotContract.DetectedActivityEntry.TABLE_NAME, null, null);
            Log.d(TAG, "Deleted " + String.valueOf(count) + " activity detection records.");
        } catch (Exception ex) {
            Log.e(TAG, "Error submitting activity data to API.");
            ex.printStackTrace();
        } finally {
            cursor.close();
            db.close();
        }
    }

    public class PostDataTask extends AsyncTask<List<JSONObject>, Void, Void> {
        protected Void doInBackground(List<JSONObject>... events){
            if (events[0].isEmpty()) {
                return null;
            }

            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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
                    case SnapShotContract.LocationEntry.TABLE_NAME:
                        api_route = "androidLocations";
                        break;
                    case SnapShotContract.DetectedActivityEntry.TABLE_NAME:
                        api_route = "androidActivities";
                        break;
                }
            } catch (Exception e){
                e.printStackTrace();
            }

            if (networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
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
                Log.d(TAG, "WIFI is not connected, data can't be submitted");
            }
            return null;
        }

    }

    @Override
    public void onDestroy() {
        // If the ActivityUpdateReceiver still exists, unregister it and set it to null
        if (mActivityUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mActivityUpdateReceiver);
            mActivityUpdateReceiver = null;
        }

        // Must always call the super method at the end.
        super.onDestroy();
    }

    private class ActivityUpdateReceiver extends BroadcastReceiver {
        private ActivityUpdateReceiver() {
            // prevents instantiation by other packages.
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String activityName = intent.getStringExtra(ai.plex.poc.android.services.Constants.ACTIVITY_NAME);
            int activityConfidence = intent.getIntExtra(ai.plex.poc.android.services.Constants.ACTIVITY_CONFIDENCE, -1);
            TextView activityDetectorStatus = (TextView) findViewById(R.id.activityDetectorText);
            activityDetectorStatus.setText(activityName + " - " + activityConfidence);
        }
    }

    private class LocationUpdateReceiver extends BroadcastReceiver {
        private LocationUpdateReceiver() {
            // prevents instantiation by other packages.
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Double latitude = intent.getDoubleExtra(ai.plex.poc.android.services.Constants.LATITUDE, -1.0);
            Double longitude = intent.getDoubleExtra(ai.plex.poc.android.services.Constants.LONGITUDE, -1.0);
            TextView activityDetectorStatus = (TextView) findViewById(R.id.locationStatus);
            activityDetectorStatus.setText("[" + latitude + ", " + longitude+"]");
        }
    }
}
