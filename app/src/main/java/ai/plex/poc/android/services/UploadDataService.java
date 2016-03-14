package ai.plex.poc.android.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import ai.plex.poc.android.Constants;
import ai.plex.poc.android.database.SnapShotContract;
import ai.plex.poc.android.database.SnapShotDBHelper;

/**
 * Created by terek on 02/03/16.
 * This service is responsible for reading data from the database stored by the
 * predictive motion data service and uploading it to the server for analysis.
 *
 * The service supports soft interruption by setting the static variable terminationRequested
 * to true.
 *
 * When the service is called with the action to stop it, this causes the service
 * to set the terminateRequested variable to false. The methods executed as a part of the
 * thread handler execution constantly check that variable and will stop looping through
 * data once the variable is identified as true
 * */
public class UploadDataService extends Service {
    // Handler that receives messages and executes the upload methods on a worker thread
    private final class UploadDataServiceHandler extends Handler {

        //Constructor that takes in the looper of the thread it is to be attached to
        public UploadDataServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //Indicate that the service is running
            isRunning = true;

            String userId = (String) msg.obj;
            if (userId != null) {
                //The upload operation
                uploadData((String) msg.obj);
            }

            //Reset running to false
            isRunning = false;

            //Stop the service, now that you are done
            stopSelf();
        }
    }

    //Control variable used to terminate the service
    //The variable is volatile becuase it used to interrupt thread executing sequential submission tasks
    private static volatile boolean terminateRequested = false;

    //Variable that indicates if the service is currently running
    private static volatile boolean isRunning = false;

    //Thread and handler for executing the upload operations on a separate thread
    private HandlerThread uploadDataServiceThread;
    private UploadDataServiceHandler mUploadDataServiceHandler;

    //Tag for logging purposes
    private static final String TAG = UploadDataService.class.getSimpleName();

    public UploadDataService(){
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Initialize shared control variables
        terminateRequested = false;
        isRunning = false;

        //Prepare worker threadHandler and handler
        if (uploadDataServiceThread == null){
            uploadDataServiceThread = new HandlerThread("UploadDataServiceThread", Thread.NORM_PRIORITY);
            //Start the newly minted thread
            uploadDataServiceThread.start();
            //Get the looper to associate the handler with the newly created thread
            mUploadDataServiceHandler = new UploadDataServiceHandler(uploadDataServiceThread.getLooper());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Determine instructions
        switch (intent.getAction()){
            case Constants.ACTIONS.START_UPLOAD_SERVICE:
                Log.d(TAG, "onStartCommand: Start Upload Service Action Received");

                //Prevent multiple requests to upload the data
                if (!isRunning) {
                    //Pass information to the handler to execute
                    Message msg = mUploadDataServiceHandler.obtainMessage();
                    msg.arg1 = startId;
                    msg.obj = intent.getStringExtra("userId");
                    mUploadDataServiceHandler.sendMessage(msg);
                }
                break;
            case Constants.ACTIONS.STOP_UPLOAD_SERVICE:
                //Prevent stopping a service which is not running
                if (isRunning) {
                    terminateRequested = true;
                    Log.d(TAG, "onStartCommand: Stop Upload Service Action Received");
                } else {
                    //Stop the service right away
                    stopSelf();
                }
                break;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * The submitData action, reads
     * recorded data from the database and submits it to the API
     * All methods called from this method support soft termination
     * via the terminateRequested variable
     * @param
     */
    private void uploadData(String userId) {
        submitLinearAcceleration(userId);
        submitMagnetic(userId);
        submitLocation(userId);
        submitDetectedActivity(userId);
        submitGyroscope(userId);
        submitRotation(userId);
    }

    /**
     * Method submits acceleration data and supports soft service termination requests
     * by checking the terminationRequested variable
     * @param username
     */
    private void submitLinearAcceleration(String username) {
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(this).getWritableDatabase();
        //This array contains the read data
        JSONArray data = new JSONArray();

        //This array will hold the ids of the read data, this will be used later to update database record to indicate successful upload
        JSONObject dataIdsObject = new JSONObject();
        //This array will hold all the ids and will be included in the dataIdsObject
        JSONArray dataIds = new JSONArray();

        Cursor cursor = null;
        Integer recordsRead = 0;

        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.LinearAccelerationEntry.TABLE_NAME + " where " + SnapShotContract.LinearAccelerationEntry.COLUMN_IS_RECORD_UPLOADED + " = 'false'", null);
            int counter = 0;

            //Continue working unless terminated
            while (cursor.moveToNext() && !terminateRequested) {
                Integer id = cursor.getInt(cursor.getColumnIndex(SnapShotContract.LinearAccelerationEntry._ID));
                float x = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.LinearAccelerationEntry.COLUMN_X));
                float y = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.LinearAccelerationEntry.COLUMN_Y));
                float z = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.LinearAccelerationEntry.COLUMN_Z));
                long timestamp = cursor.getLong(cursor.getColumnIndex(SnapShotContract.LinearAccelerationEntry.COLUMN_TIMESTAMP));
                String isDriving = cursor.getString(cursor.getColumnIndex(SnapShotContract.LinearAccelerationEntry.COLUMN_IS_DRIVING));

                //Add the id to the array of read ids
                dataIds.put(id);

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
                data.put(responseObject);

                counter++;

                if (counter >= Constants.MAX_ENTRIES_PER_API_SUBMISSION){
                    //Bundle the array in the JSONObject
                    dataIdsObject.put("dataType", SnapShotContract.LinearAccelerationEntry.TABLE_NAME);
                    dataIdsObject.put("data", dataIds);
                    //Call the post data service
                    submitDataToApi(data, dataIdsObject);
                    //reset data and processedids
                    data = new JSONArray();
                    dataIdsObject = new JSONObject();
                    dataIds = new JSONArray();
                    counter = 0;
                }
            }

            //Catch remaining items < MAX_ENTRIES_PER_API_SUBMISSION
            if (data.length() > 0 ) {
                //Bundle the array in the JSONObject
                dataIdsObject.put("dataType", SnapShotContract.LinearAccelerationEntry.TABLE_NAME);
                dataIdsObject.put("data", dataIds);
                //Call the post data service
                submitDataToApi(data, dataIdsObject);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error submitting linear acceleration data to API.");
            ex.printStackTrace();
        } finally {
            cursor.close();
            db.close();
        }
        Log.d(TAG, "submitData: " + recordsRead + " were read!");
    }

    /**
     * Method submits gyroscope data and supports soft service termination requests
     * by checking the terminationRequested variable
     * @param username
     */
    private void submitGyroscope(String username) {
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(this).getWritableDatabase();
        //This array contains the read data
        JSONArray data = new JSONArray();

        //This array will hold the ids of the read data, this will be used later to update database record to indicate successful upload
        JSONObject dataIdsObject = new JSONObject();
        //This array will hold all the ids and will be included in the dataIdsObject
        JSONArray dataIds = new JSONArray();

        Cursor cursor = null;
        Integer recordsRead = 0;

        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.GyroscopeEntry.TABLE_NAME + " where " + SnapShotContract.GyroscopeEntry.COLUMN_IS_RECORD_UPLOADED + " = 'false'", null);
            int counter = 0;

            while (cursor.moveToNext() && !terminateRequested) {

                Integer id = cursor.getInt(cursor.getColumnIndex(SnapShotContract.GyroscopeEntry._ID));
                float x = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_X));
                float y = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_Y));
                float z = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.GyroscopeEntry.COLUMN_ANGULAR_SPEED_Z));
                long timestamp = cursor.getLong(cursor.getColumnIndex(SnapShotContract.GyroscopeEntry.COLUMN_TIMESTAMP));
                String isDriving = cursor.getString(cursor.getColumnIndex(SnapShotContract.GyroscopeEntry.COLUMN_IS_DRIVING));

                //Add the id to the array of read ids
                dataIds.put(id);

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
                data.put(responseObject);

                counter++;

                if (counter >= Constants.MAX_ENTRIES_PER_API_SUBMISSION){
                    //Bundle the array in the JSONObject
                    dataIdsObject.put("dataType", SnapShotContract.GyroscopeEntry.TABLE_NAME);
                    dataIdsObject.put("data", dataIds);
                    //Call the post data service
                    submitDataToApi(data, dataIdsObject);
                    //reset data and processedids
                    data = new JSONArray();
                    dataIdsObject = new JSONObject();
                    dataIds = new JSONArray();
                    counter = 0;
                }
            }

            //Catch remaining items < MAX_ENTRIES_PER_API_SUBMISSION
            if (data.length() > 0 ) {
                //Bundle the array in the JSONObject
                dataIdsObject.put("dataType", SnapShotContract.GyroscopeEntry.TABLE_NAME);
                dataIdsObject.put("data", dataIds);
                //Call the post data service
                submitDataToApi(data, dataIdsObject);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error submitting linear gyroscope data to API.");
            ex.printStackTrace();
        } finally {
            cursor.close();
            db.close();
        }
        Log.d(TAG, "submitData: " + recordsRead + " were read!");
    }

    /**
     * Method submits magnetic data and supports soft service termination requests
     * by checking the terminationRequested variable
     * @param username
     */
    private void submitMagnetic(String username) {
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(this).getWritableDatabase();
        //This array contains the read data
        JSONArray data = new JSONArray();

        //This array will hold the ids of the read data, this will be used later to update database record to indicate successful upload
        JSONObject dataIdsObject = new JSONObject();
        //This array will hold all the ids and will be included in the dataIdsObject
        JSONArray dataIds = new JSONArray();


        Cursor cursor = null;
        Integer recordsRead = 0;

        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.MagneticEntry.TABLE_NAME + " where " + SnapShotContract.MagneticEntry.COLUMN_IS_RECORD_UPLOADED + " = 'false'", null);
            int counter = 0;

            while (cursor.moveToNext() && !terminateRequested) {

                Integer id = cursor.getInt(cursor.getColumnIndex(SnapShotContract.MagneticEntry._ID));
                float x = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.MagneticEntry.COLUMN_X));
                float y = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.MagneticEntry.COLUMN_Y));
                float z = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.MagneticEntry.COLUMN_Z));
                long timestamp = cursor.getLong(cursor.getColumnIndex(SnapShotContract.MagneticEntry.COLUMN_TIMESTAMP));
                String isDriving = cursor.getString(cursor.getColumnIndex(SnapShotContract.MagneticEntry.COLUMN_IS_DRIVING));

                //Add the id to the array of read ids
                dataIds.put(id);

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
                data.put(responseObject);

                counter++;

                if (counter >= Constants.MAX_ENTRIES_PER_API_SUBMISSION){
                    //Bundle the array in the JSONObject
                    dataIdsObject.put("dataType", SnapShotContract.MagneticEntry.TABLE_NAME);
                    dataIdsObject.put("data", dataIds);
                    //Call the post data service
                    submitDataToApi(data, dataIdsObject);
                    //reset data and processedids
                    data = new JSONArray();
                    dataIdsObject = new JSONObject();
                    dataIds = new JSONArray();
                    counter = 0;
                }
            }

            //Catch remaining items < MAX_ENTRIES_PER_API_SUBMISSION
            if (data.length() > 0 ) {
                //Bundle the array in the JSONObject
                dataIdsObject.put("dataType", SnapShotContract.MagneticEntry.TABLE_NAME);
                dataIdsObject.put("data", dataIds);
                //Call the post data service
                submitDataToApi(data, dataIdsObject);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error submitting linear magnetic data to API.");
            ex.printStackTrace();
        } finally {
            cursor.close();
            db.close();
        }
        Log.d(TAG, "submitData: " + recordsRead + " were read!");
    }

    /**
     * Method submits rotation data and supports soft service termination requests
     * by checking the terminationRequested variable
     * @param username
     */
    private void submitRotation(String username) {
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(this).getWritableDatabase();
        //This array contains the read data
        JSONArray data = new JSONArray();

        //This array will hold the ids of the read data, this will be used later to update database record to indicate successful upload
        JSONObject dataIdsObject = new JSONObject();
        //This array will hold all the ids and will be included in the dataIdsObject
        JSONArray dataIds = new JSONArray();


        Cursor cursor = null;
        Integer recordsRead = 0;

        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.RotationEntry.TABLE_NAME + " where " + SnapShotContract.RotationEntry.COLUMN_IS_RECORD_UPLOADED + " = 'false'", null);
            int counter = 0;

            while (cursor.moveToNext() && !terminateRequested) {

                Integer id = cursor.getInt(cursor.getColumnIndex(SnapShotContract.RotationEntry._ID));
                float x = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.RotationEntry.COLUMN_X_SIN));
                float y = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.RotationEntry.COLUMN_Y_SIN));
                float z = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.RotationEntry.COLUMN_Z_SIN));
                float cos = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.RotationEntry.COLUMN_COS));
                float accuracy = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.RotationEntry.COLUMN_ACCURACY));
                long timestamp = cursor.getLong(cursor.getColumnIndex(SnapShotContract.RotationEntry.COLUMN_TIMESTAMP));
                String isDriving = cursor.getString(cursor.getColumnIndex(SnapShotContract.RotationEntry.COLUMN_IS_DRIVING));

                //Add the id to the array of read ids
                dataIds.put(id);

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
                data.put(responseObject);

                counter++;

                if (counter >= Constants.MAX_ENTRIES_PER_API_SUBMISSION){
                    //Bundle the array in the JSONObject
                    dataIdsObject.put("dataType", SnapShotContract.RotationEntry.TABLE_NAME);
                    dataIdsObject.put("data", dataIds);
                    //Call the post data service
                    submitDataToApi(data, dataIdsObject);
                    //reset data and processedids
                    data = new JSONArray();
                    dataIdsObject = new JSONObject();
                    dataIds = new JSONArray();
                    counter = 0;
                }
            }

            //Catch remaining items < MAX_ENTRIES_PER_API_SUBMISSION
            if (data.length() > 0 ) {
                //Bundle the array in the JSONObject
                dataIdsObject.put("dataType", SnapShotContract.RotationEntry.TABLE_NAME);
                dataIdsObject.put("data", dataIds);
                //Call the post data service
                submitDataToApi(data, dataIdsObject);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error submitting rotation data to API.");
            ex.printStackTrace();
        } finally {
            cursor.close();
            db.close();
        }
        Log.d(TAG, "submitData: " + recordsRead + " were read!");
    }

    /**
     * Method submits location data and supports soft service termination requests
     * by checking the terminationRequested variable
     * @param username
     */
    private void submitLocation(String username) {
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(this).getWritableDatabase();
        //This array contains the read data
        JSONArray data = new JSONArray();

        //This array will hold the ids of the read data, this will be used later to update database record to indicate successful upload
        JSONObject dataIdsObject = new JSONObject();
        //This array will hold all the ids and will be included in the dataIdsObject
        JSONArray dataIds = new JSONArray();

        Cursor cursor = null;
        Integer recordsRead = 0;

        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.LocationEntry.TABLE_NAME + " where " + SnapShotContract.LocationEntry.COLUMN_IS_RECORD_UPLOADED + " = 'false'", null);
            int counter = 0;

            while (cursor.moveToNext() && !terminateRequested) {

                Integer id = cursor.getInt(cursor.getColumnIndex(SnapShotContract.LocationEntry._ID));
                float x = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.LocationEntry.COLUMN_LATITUDE));
                float y = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.LocationEntry.COLUMN_LONGITUDE));
                float z = cursor.getFloat(cursor.getColumnIndex(SnapShotContract.LocationEntry.COLUMN_SPEED));
                long timestamp = cursor.getLong(cursor.getColumnIndex(SnapShotContract.LocationEntry.COLUMN_TIMESTAMP));
                String isDriving = cursor.getString(cursor.getColumnIndex(SnapShotContract.LocationEntry.COLUMN_IS_DRIVING));

                //Add the id to the array of read ids
                dataIds.put(id);

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
                data.put(responseObject);

                counter++;

                if (counter >= Constants.MAX_ENTRIES_PER_API_SUBMISSION){
                    //Bundle the array in the JSONObject
                    dataIdsObject.put("dataType", SnapShotContract.LocationEntry.TABLE_NAME);
                    dataIdsObject.put("data", dataIds);
                    //Call the post data service
                    submitDataToApi(data, dataIdsObject);
                    //reset data and processedids
                    data = new JSONArray();
                    dataIdsObject = new JSONObject();
                    dataIds = new JSONArray();
                    counter = 0;
                }
            }

            //Catch remaining items < MAX_ENTRIES_PER_API_SUBMISSION
            if (data.length() > 0 ) {
                //Bundle the array in the JSONObject
                dataIdsObject.put("dataType", SnapShotContract.LocationEntry.TABLE_NAME);
                dataIdsObject.put("data", dataIds);
                //Call the post data service
                submitDataToApi(data, dataIdsObject);
            }ConnectivityManager mConnectionManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = mConnectionManager.getActiveNetworkInfo();
        } catch (Exception ex) {
            Log.e(TAG, "Error submitting location data to API.");
            ex.printStackTrace();
        } finally {
            cursor.close();
            db.close();
        }
        Log.d(TAG, "submitData: " + recordsRead + " were read!");
    }

    /**
     * Method submits detected activity data and supports soft service termination requests
     * by checking the terminationRequested variable
     * @param username
     */
    private void submitDetectedActivity(String username) {
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(this).getWritableDatabase();
        //This array contains the read data
        JSONArray data = new JSONArray();

        //This array will hold the ids of the read data, this will be used later to update database record to indicate successful upload
        JSONObject dataIdsObject = new JSONObject();
        //This array will hold all the ids and will be included in the dataIdsObject
        JSONArray dataIds = new JSONArray();

        Cursor cursor = null;
        Integer recordsRead = 0;

        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.DetectedActivityEntry.TABLE_NAME + " where " + SnapShotContract.DetectedActivityEntry.COLUMN_IS_RECORD_UPLOADED + " = 'false'", null);
            int counter = 0;

            while (cursor.moveToNext() && !terminateRequested) {

                Integer id = cursor.getInt(cursor.getColumnIndex(SnapShotContract.DetectedActivityEntry._ID));
                int name = cursor.getInt(cursor.getColumnIndex(SnapShotContract.DetectedActivityEntry.COLUMN_NAME));
                int confidence = cursor.getInt(cursor.getColumnIndex(SnapShotContract.DetectedActivityEntry.COLUMN_CONFIDENCDE));
                long timestamp = cursor.getLong(cursor.getColumnIndex(SnapShotContract.DetectedActivityEntry.COLUMN_TIMESTAMP));
                String isDriving = cursor.getString(cursor.getColumnIndex(SnapShotContract.DetectedActivityEntry.COLUMN_IS_DRIVING));

                //Add the id to the array of read ids
                dataIds.put(id);

                JSONObject responseObject = new JSONObject();
                responseObject.put("deviceType", "Android");
                responseObject.put("deviceOsVersion", Build.VERSION.RELEASE);
                responseObject.put("dataType",SnapShotContract.DetectedActivityEntry.TABLE_NAME);
                responseObject.put(SnapShotContract.DetectedActivityEntry.COLUMN_TIMESTAMP, timestamp);
                responseObject.put(SnapShotContract.DetectedActivityEntry.COLUMN_NAME, name);
                responseObject.put(SnapShotContract.DetectedActivityEntry.COLUMN_CONFIDENCDE, confidence);
                responseObject.put(SnapShotContract.DetectedActivityEntry.COLUMN_IS_DRIVING, isDriving);
                responseObject.put("userId", username);
                data.put(responseObject);

                counter++;

                if (counter >= Constants.MAX_ENTRIES_PER_API_SUBMISSION){
                    //Bundle the array in the JSONObject
                    dataIdsObject.put("dataType", SnapShotContract.DetectedActivityEntry.TABLE_NAME);
                    dataIdsObject.put("data", dataIds);
                    //Call the post data service
                    submitDataToApi(data, dataIdsObject);
                    //reset data and processedids
                    data = new JSONArray();
                    dataIdsObject = new JSONObject();
                    dataIds = new JSONArray();
                    counter = 0;
                }
            }

            //Catch remaining items < MAX_ENTRIES_PER_API_SUBMISSION
            if (data.length() > 0 ) {
                //Bundle the array in the JSONObject
                dataIdsObject.put("dataType", SnapShotContract.DetectedActivityEntry.TABLE_NAME);
                dataIdsObject.put("data", dataIds);
                //Call the post data service
                submitDataToApi(data, dataIdsObject);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error submitting detected activity data to API.");
            ex.printStackTrace();
        } finally {
            cursor.close();
            db.close();
        }
        Log.d(TAG, "submitData: " + recordsRead + " were read!");
    }

    /**
     * Submits data to the API
     * @param dataArray
     * @param dataIds
     */
    private void submitDataToApi(JSONArray dataArray, JSONObject dataIds) {
        //Try to convert the data to a JsonArray
        try {
            //Check that the intent is carrying data
            if ( dataArray == null || dataArray.length() <= 0) {
                return;
            }

            ConnectivityManager mConnectionManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = mConnectionManager.getActiveNetworkInfo();

            String dataType = "";
            String api_route = "";

            //Figure out which API endpoint to use based on the type of data being passed in
            dataType = dataArray.optJSONObject(0).get("dataType").toString();
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

            //Verify that the user is connected to WIFI
            if (networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                JSONObject requestData = new JSONObject();
                //JSONArray dataPoints = dataArray.optJSONObject(0));
                try {
                    requestData.put("entries", dataArray);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                InputStream is = null;
                // Only display the first 500 characters of the retrieved
                // web page content.

                try {
                    //Define the URL
                    URL url = new URL("http://"+ Constants.IP_ADDRESS +"/"+ api_route);

                    String message = requestData.toString();

                    //Open a connection
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    //Set connection details
                    connection.setReadTimeout(10000 /* milliseconds */);
                    connection.setConnectTimeout(15000 /* milliseconds */);
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);

                    //Set header details
                    connection.setRequestProperty("Content-Type","application/json;charset=utf-8");
                    connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");

                    //Connect
                    connection.connect();

                    //Setup data to send
                    OutputStream os = new BufferedOutputStream(connection.getOutputStream());
                    os.write(message.getBytes());
                    os.flush();

                    int response = connection.getResponseCode();
                    Log.d(TAG, "The response was: " + response);

                    if (response == HttpURLConnection.HTTP_OK){
                        //updateDataAsSubmitted(dataIds);
                        Intent updateDatabaseIntent = new Intent(this, UpdateDataService.class);
                        updateDatabaseIntent.putExtra("dataIdsIn", dataIds.toString());
                        startService(updateDatabaseIntent);
                    }
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
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Make sure that you destory the handler thread to free up resources
        if (uploadDataServiceThread != null){
            uploadDataServiceThread.quit();
        }
    }
}