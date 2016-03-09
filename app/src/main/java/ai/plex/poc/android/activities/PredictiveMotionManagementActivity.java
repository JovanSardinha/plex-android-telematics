package ai.plex.poc.android.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
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

import ai.plex.poc.android.*;
import ai.plex.poc.android.Constants;
import ai.plex.poc.android.database.SnapShotDBHelper;
import ai.plex.poc.android.sensorListeners.SensorType;
import ai.plex.poc.android.services.UploadDataService;
import ai.plex.poc.android.services.PredictiveMotionDataService;

/**
 * This activity provides the UI to control the predictive motion data service running in
 * the background
 */
public class PredictiveMotionManagementActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private final static String TAG = PredictiveMotionManagementActivity.class.getSimpleName();

    // Constant for requesting location permissions
    final int requestLocationPermissionId = 123;

    static int globalCounter = 0;

    PredictiveMotionDataService mService;
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
                ai.plex.poc.android.Constants.ACTIVITY_UPDATE_BROADCAST_ACTION);

        // Instantiates a new ActivityUpdateReceiver
        mActivityUpdateReceiver = new ActivityUpdateReceiver();

        // Registers the DownloadStateReceiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mActivityUpdateReceiver,
                activityUpdateIntentFilter);

        // The filter's action is LOCATION_UPDATE_BROADCAST_ACTION
        IntentFilter locationIntentFilter = new IntentFilter(
                Constants.LOCATION_UPDATE_BROADCAST_ACTION);

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
        Intent mServiceIntent = new Intent(this, PredictiveMotionDataService.class);

        if (!PredictiveMotionDataService.isRunning()) {
            mServiceIntent.setAction(Constants.ACTIONS.START_PREDICTIVE_MOTION_SERVICE_IN_FOREGROUND);
            startService(mServiceIntent);
        }
        //bind to the predictive motion data service running in the background
        bindService(mServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to PredictiveMotionDataService, cast the IBinder and get PredictiveMotionDataService instance
            PredictiveMotionDataService.MotionDataBinder binder = (PredictiveMotionDataService.MotionDataBinder) service;
            mService = binder.getService();
            mBound = true;
            Log.d(TAG, "Bound to PredictiveMotionDataService.");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            //mBound = false;
            Log.d(TAG, "Unbound from PredictiveMotionDataService.");
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
            Log.d(TAG, "PredictiveMotionDataService not bound. Bind service to start updates.");
            return;
        }

        boolean checked = false;
        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.isDrivingToggleButton:
                checked = ((ToggleButton) view).isChecked();
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isDriving", checked).commit();
                updateStatus("Driving - " + (checked ? "ON" : "OFF"));
                break;
            case R.id.isRecordingToggleButton:
                checked = ((ToggleButton) view).isChecked();
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("isRecording", checked).commit();
                toggleSwitches(checked);
                updateStatus("Recording - " + (checked ? "ON" : "OFF"));
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
                    updateStatus("Cleared data");
                } catch (Exception e) {
                    updateStatus("Error clearing data");
                }
                break;
            case R.id.submitButton:
                try {
                    mService.stopAllSensors();
                    stopRecording();
                    String username = PreferenceManager.getDefaultSharedPreferences(this).getString("username", "default_user");
                    submitData(username);
                    updateStatus("Data submission complete");

                } catch (Exception ex) {
                    ex.printStackTrace();
                    updateStatus("Error submitting data");
                }
        }
    }

    //Submits the collected sensor data
    private void submitData(String username) {
        // Start the background service
        //globalCounter = 0;
        Intent submitDataIntent = new Intent("ai.plex.poc.android.submitData",null,this,UploadDataService.class);
        submitDataIntent.putExtra("userId", username);
        startService(submitDataIntent);
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

    public void updateStatus(String str) {
        TextView statusTextBox = (TextView) findViewById(R.id.statusText);
        statusTextBox.setText(str);
    }

    public synchronized void incrementGlobalCounter(int amount){
        globalCounter += amount;
    }

    public synchronized void decrementGlobalCounter(int amount){
        globalCounter -= amount;
    }

    public int getGlobalCounter(){
        return globalCounter;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        globalCounter =0;
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


    @Override
    public void onDestroy() {
        // If the ActivityUpdateReceiver still exists, unregister it and set it to null
        if (mActivityUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mActivityUpdateReceiver);
            mActivityUpdateReceiver = null;
        }

        unbindService(mConnection);

        // Must always call the super method at the end.
        super.onDestroy();
    }

    private class ActivityUpdateReceiver extends BroadcastReceiver {
        private ActivityUpdateReceiver() {
            // prevents instantiation by other packages.
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String activityName = intent.getStringExtra(Constants.ACTIVITY_NAME);
            int activityConfidence = intent.getIntExtra(Constants.ACTIVITY_CONFIDENCE, -1);
            TextView activityDetectorStatus = (TextView) findViewById(R.id.activityDetectorText);
            activityDetectorStatus.setText(activityName + " - " + activityConfidence);

            boolean isDriving = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("isDriving", false);
            boolean isRecording = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("isRecording", false);
            updateStatus("[Recording, Driving] = [" + isRecording + ", " + isDriving + "]");
        }
    }

    private class LocationUpdateReceiver extends BroadcastReceiver {
        private LocationUpdateReceiver() {
            // prevents instantiation by other packages.
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Double latitude = intent.getDoubleExtra(Constants.LATITUDE, -1.0);
            Double longitude = intent.getDoubleExtra(Constants.LONGITUDE, -1.0);
            TextView locationStatus = (TextView) findViewById(R.id.locationStatus);
            locationStatus.setText("[" + latitude + ", " + longitude+"]");
        }
    }
}
