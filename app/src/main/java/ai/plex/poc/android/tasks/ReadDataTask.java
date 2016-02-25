package ai.plex.poc.android.tasks;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ai.plex.poc.android.activities.Constants;
import ai.plex.poc.android.activities.Welcome;
import ai.plex.poc.android.database.SnapShotContract;
import ai.plex.poc.android.database.SnapShotDBHelper;

/**
 * Created by terek on 24/02/16.
 * This task is responsible for asynchronously reading chunks of data from the database and returning it in the form of an array of JsonObjects
 */
public class ReadDataTask extends AsyncTask<String, String,ArrayList<JSONObject>> {
    //Private copy of the context
    private Context context;

    private final static String TAG = ReadDataTask.class.getSimpleName();

    //Task constructor taking in a context
    public ReadDataTask(Context context){
        //set the context for the task
        this.context = context;
    }

    @Override
    protected ArrayList<JSONObject> doInBackground(String... params) {
        SubmitLinearAcceleration(params[0]);
        SubmitMagnetic(params[0]);
        SubmitLocation(params[0]);
        SubmitDetectedActivity(params[0]);
        SubmitGyroscope(params[0]);
        SubmitRotation(params[0]);
        return null;
    }

    public void SubmitLinearAcceleration(String username) {
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(context).getWritableDatabase();
        ArrayList<JSONObject> data = new ArrayList();
        Integer lastRecord = -1;
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.LinearAccelerationEntry.TABLE_NAME, null);
            int counter = 0;

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

                counter++;
                //updateStatus("Acc 0/" + data.size());
                Welcome welcomeActivity = context instanceof Welcome ? (Welcome) context : null;

                //Update the main UI
                if (welcomeActivity != null){
                    welcomeActivity.incrementGlobalCounter(1);
                    publishProgress();
                }

                if (counter >= Constants.MAX_ENTRIES_PER_API_SUBMISSION){
                    new PostDataTask(context).execute(new ArrayList<JSONObject>(data));
                    data.clear();
                    counter = 0;
                }
            }

            new PostDataTask(context).execute(data);


            /*int page_size = 10;
            int num_pages = (data.size()/page_size) + 1;
            for (int page = 0; page < num_pages; page++){
                int start = page * page_size;
                int end = start + page_size;
                if (page % 10 == 0) {
                    publishProgress("Acc " + start +"/" + data.size());
                    //updateStatus("Acc " + start +"/" + data.size());
                }
                end = end > data.size() ? data.size() : end;
                List set = data.subList(start, end);
                new PostDataTask(context).execute(set);
            }*/

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

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);

        //Update the activity by the reference the task is holding on to, keep in mind this is running on the main thread
        Welcome welcomeActivity = context instanceof Welcome ? (Welcome) context : null;
        if (welcomeActivity != null){
            welcomeActivity.updateStatus(welcomeActivity.getGlobalCounter() + " items remaining");
        }
    }

    public void SubmitGyroscope(String username) {
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(context).getWritableDatabase();
        ArrayList<JSONObject> data = new ArrayList();
        Integer lastRecord = -1;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.GyroscopeEntry.TABLE_NAME, null);
            int counter = 0;

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

                counter++;
                //updateStatus("Acc 0/" + data.size());
                Welcome welcomeActivity = context instanceof Welcome ? (Welcome) context : null;

                //Update the main UI
                if (welcomeActivity != null){
                    welcomeActivity.incrementGlobalCounter(1);
                    publishProgress();
                }

                if (counter >= Constants.MAX_ENTRIES_PER_API_SUBMISSION){
                    new PostDataTask(context).execute(new ArrayList<JSONObject>(data));
                    data.clear();
                    counter = 0;
                }
            }

            new PostDataTask(context).execute(data);
            /*publishProgress("Gyro 0/" + data.size());
            //updateStatus("Gyro 0/" + data.size());

            int page_size = 10;
            int num_pages = (data.size()/page_size) + 1;
            for (int page = 0; page < num_pages; page++){
                int start = page * page_size;
                int end = start + page_size;
                if (page % 10 == 0) {
                    publishProgress("Gyro " + start +"/" + data.size());
                    //updateStatus("Gyro " + start +"/" + data.size());
                }
                end = end > data.size() ? data.size() : end;
                List set = data.subList(start, end);
                new PostDataTask(context).execute(set);
            }*/
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
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(context).getWritableDatabase();
        ArrayList<JSONObject> data = new ArrayList();
        Integer lastRecord = -1;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.MagneticEntry.TABLE_NAME, null);
            int counter = 0;

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
                counter++;
                //updateStatus("Acc 0/" + data.size());
                Welcome welcomeActivity = context instanceof Welcome ? (Welcome) context : null;

                //Update the main UI
                if (welcomeActivity != null){
                    welcomeActivity.incrementGlobalCounter(1);
                    publishProgress();
                }

                if (counter >= Constants.MAX_ENTRIES_PER_API_SUBMISSION){
                    new PostDataTask(context).execute(new ArrayList<JSONObject>(data));
                    data.clear();
                    counter = 0;
                }
            }

            new PostDataTask(context).execute(data);
            /*publishProgress("Mag 0/" + data.size());
            //updateStatus("Mag 0/" + data.size());
            int page_size = 10;
            int num_pages = (data.size()/page_size) + 1;
            for (int page = 0; page < num_pages; page++){
                int start = page * page_size;
                int end = start + page_size;
                if (page % 10 == 0) {
                    publishProgress("Mag " + start + "/" + data.size());
                    //updateStatus("Mag " + start + "/" + data.size());
                }
                end = end > data.size() ? data.size() : end;
                List set = data.subList(start, end);
                new PostDataTask(context).execute(set);
            }*/
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
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(context).getWritableDatabase();
        ArrayList<JSONObject> data = new ArrayList();
        Integer lastRecord = -1;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.RotationEntry.TABLE_NAME, null);
            int counter = 0;

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

                counter++;
                //updateStatus("Acc 0/" + data.size());
                Welcome welcomeActivity = context instanceof Welcome ? (Welcome) context : null;

                //Update the main UI
                if (welcomeActivity != null){
                    welcomeActivity.incrementGlobalCounter(1);
                    publishProgress();
                }


                if (counter >= Constants.MAX_ENTRIES_PER_API_SUBMISSION){
                    new PostDataTask(context).execute(new ArrayList<JSONObject>(data));
                    data.clear();
                    counter = 0;
                }
            }

            new PostDataTask(context).execute(data);

            /*publishProgress("Rot 0/" + data.size());
            //updateStatus("Rot 0/" + data.size());
            int page_size = 10;
            int num_pages = (data.size()/page_size) + 1;
            for (int page = 0; page < num_pages; page++){
                int start = page * page_size;
                int end = start + page_size;
                if (page % 10 == 0) {
                    publishProgress("Rot " + start + "/" + data.size());
                    //updateStatus("Rot " + start + "/" + data.size());
                }
                end = end > data.size() ? data.size() : end;
                List set = data.subList(start, end);
                new PostDataTask(context).execute(set);
            }*/

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
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(context).getWritableDatabase();
        ArrayList<JSONObject> data = new ArrayList();
        Integer lastRecord = -1;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.LocationEntry.TABLE_NAME, null);
            int counter = 0;


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

                counter++;
                //updateStatus("Acc 0/" + data.size());
                Welcome welcomeActivity = context instanceof Welcome ? (Welcome) context : null;

                //Update the main UI
                if (welcomeActivity != null){
                    welcomeActivity.incrementGlobalCounter(1);
                    publishProgress();
                }


                if (counter >= Constants.MAX_ENTRIES_PER_API_SUBMISSION){
                    new PostDataTask(context).execute(new ArrayList<JSONObject>(data));
                    data.clear();
                    counter = 0;
                }
            }

            new PostDataTask(context).execute(data);

            /*publishProgress("Loc 0/" + data.size());
            //updateStatus("Loc 0/" + data.size());
            int page_size = 10;
            int num_pages = (data.size()/page_size) + 1;
            for (int page = 0; page < num_pages; page++){
                int start = page * page_size;
                int end = start + page_size;
                if (page % 10 == 0) {
                    publishProgress("Loc " + start + "/" + data.size());
                    //updateStatus("Loc " + start + "/" + data.size());
                }
                end = end > data.size() ? data.size() : end;
                List set = data.subList(start, end);
                new PostDataTask(context).execute(set);
            }*/

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
        SQLiteDatabase db = SnapShotDBHelper.getsInstance(context).getWritableDatabase();
        ArrayList<JSONObject> data = new ArrayList();
        Cursor cursor = null;
        Integer lastRecord = -1;
        try {
            cursor = db.rawQuery("Select * from " + SnapShotContract.DetectedActivityEntry.TABLE_NAME, null);
            int counter = 0;

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

                counter++;
                //updateStatus("Acc 0/" + data.size());
                Welcome welcomeActivity = context instanceof Welcome ? (Welcome) context : null;

                //Update the main UI
                if (welcomeActivity != null){
                    welcomeActivity.incrementGlobalCounter(1);
                    publishProgress();
                }


                if (counter >= Constants.MAX_ENTRIES_PER_API_SUBMISSION){
                    new PostDataTask(context).execute(new ArrayList<JSONObject>(data));
                    data.clear();
                    counter = 0;
                }
            }

            new PostDataTask(context).execute(data);

            /*publishProgress("Act 0/" + data.size());
            //updateStatus("Act 0/" + data.size());
            int page_size = 10;
            int num_pages = (data.size()/page_size) + 1;
            for (int page = 0; page < num_pages; page++){
                int start = page * page_size;
                int end = start + page_size;
                if (page % 10 == 0) {
                    publishProgress("Act " + start + "/" + data.size());
                    //updateStatus("Act " + start + "/" + data.size());
                }
                end = end > data.size() ? data.size() : end;
                List set = data.subList(start, end);
                new PostDataTask(context).execute(set);
            }*/
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
}
