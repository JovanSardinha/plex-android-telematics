package ai.plex.poc.android.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import ai.plex.poc.android.database.SnapShotContract;
import ai.plex.poc.android.database.SnapShotDBHelper;

/**
 * Created by terek on 02/03/16.
 * This service is responsible for updating data in the database to indicate that
 * records have been uploaded
 */
public class UpdateDataService extends IntentService {
    //Tag for logging purposes
    private static final String TAG = UpdateDataService.class.getSimpleName();

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public UpdateDataService() {
        super("UpdateDataService");
    }

    /**
     * Current default behaviour is to take in the id of records in the
     * database and mark them as submitted so that they do not get uploaded
     * again
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            JSONObject dataIdsIn = new JSONObject(intent.getStringExtra("dataIdsIn"));
            updateDataAsSubmitted(dataIdsIn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Tell the database service that uploading has finished
        Intent databaseServiceIntent = new Intent("ai.plex.poc.android.uploadingComplete", null, this, UploadDataService.class);
        startService(databaseServiceIntent);
    }


    /***
     * Updates records in the database to indicate that they have been submitted
     * to the API
     * @param dataIds
     */
    private void updateDataAsSubmitted(JSONObject dataIds){
        try {
            String dataType = dataIds.get("dataType").toString();
            switch (dataType) {
                case SnapShotContract.LinearAccelerationEntry.TABLE_NAME:
                    markDataAsSubmitted(dataIds.getJSONArray("data"), SnapShotContract.LinearAccelerationEntry.TABLE_NAME, SnapShotContract.LinearAccelerationEntry.COLUMN_IS_RECORD_UPLOADED, SnapShotContract.LinearAccelerationEntry._ID);
                    break;
                case SnapShotContract.DetectedActivityEntry.TABLE_NAME:
                    markDataAsSubmitted(dataIds.getJSONArray("data"), SnapShotContract.DetectedActivityEntry.TABLE_NAME, SnapShotContract.DetectedActivityEntry.COLUMN_IS_RECORD_UPLOADED, SnapShotContract.DetectedActivityEntry._ID);
                    break;
                case SnapShotContract.GyroscopeEntry.TABLE_NAME:
                    markDataAsSubmitted(dataIds.getJSONArray("data"), SnapShotContract.GyroscopeEntry.TABLE_NAME, SnapShotContract.GyroscopeEntry.COLUMN_IS_RECORD_UPLOADED, SnapShotContract.GyroscopeEntry._ID);
                    break;
                case SnapShotContract.LocationEntry.TABLE_NAME:
                    markDataAsSubmitted(dataIds.getJSONArray("data"), SnapShotContract.LocationEntry.TABLE_NAME, SnapShotContract.LocationEntry.COLUMN_IS_RECORD_UPLOADED, SnapShotContract.LocationEntry._ID);
                    break;
                case SnapShotContract.MagneticEntry.TABLE_NAME:
                    markDataAsSubmitted(dataIds.getJSONArray("data"), SnapShotContract.MagneticEntry.TABLE_NAME, SnapShotContract.MagneticEntry.COLUMN_IS_RECORD_UPLOADED, SnapShotContract.MagneticEntry._ID);
                    break;
                case SnapShotContract.RotationEntry.TABLE_NAME:
                    markDataAsSubmitted(dataIds.getJSONArray("data"), SnapShotContract.RotationEntry.TABLE_NAME, SnapShotContract.RotationEntry.COLUMN_IS_RECORD_UPLOADED, SnapShotContract.RotationEntry._ID);
                    break;
            }
        } catch (Exception ex) {
            Log.d(TAG, "updateDataAsSubmitted: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Method updates the record in the database to identify that a record has been uploaded successfully
     * @param ids
     */
    private void markDataAsSubmitted(JSONArray ids, String tableName, String columnName, String idColumn ){
        try {
            SQLiteDatabase db = SnapShotDBHelper.getsInstance(this).getWritableDatabase();
            String queryPart = " IN (";
            for (int i = 0; i < ids.length(); i++) {
                queryPart += "'" + String.valueOf(ids.get(i)) + "',";
            }

            //remove the last comma and add a closing bracket
            queryPart = queryPart.substring(0, queryPart.lastIndexOf(',')) + ")";

            ContentValues values = new ContentValues();
            values.put(columnName, "true");
            String selection = idColumn + queryPart;
            //String[] selectionArgs = {queryPart};
            int result = db.update(tableName, values, selection, null);
            Log.d(TAG, "markDataAsSubmitted: Updated record with _id for " + result + " in the " + tableName);
        } catch (Exception ex){
            Log.d(TAG, "markDataAsSubmitted: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
