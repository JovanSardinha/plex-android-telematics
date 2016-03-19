package ai.plex.poc.android.services;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import ai.plex.poc.android.database.SnapShotContract;
import ai.plex.poc.android.database.SnapShotDBHelper;

/**
 * Created by terek on 18/03/16.
 * This intent service is responsible for cleaning up the database
 */
public class CleanUpService extends IntentService{
    private static final String TAG = CleanUpService.class.getSimpleName();
    private SQLiteDatabase db;
    //Used to send toast message to the UI
    //Todo: Remove the handler as it is only required for debugging purposes
    private Handler mHandler;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public CleanUpService(String name) {
        super(name);
    }

    public CleanUpService(){
        super("CleanUpService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CleanUpService.this, "Clean up service started!", Toast.LENGTH_SHORT).show();
                }
            });


            cleanData(SnapShotContract.LinearAccelerationEntry.TABLE_NAME, SnapShotContract.LinearAccelerationEntry.COLUMN_IS_RECORD_UPLOADED);
            cleanData(SnapShotContract.GyroscopeEntry.TABLE_NAME, SnapShotContract.GyroscopeEntry.COLUMN_IS_RECORD_UPLOADED);
            cleanData(SnapShotContract.MagneticEntry.TABLE_NAME, SnapShotContract.MagneticEntry.COLUMN_IS_RECORD_UPLOADED);
            cleanData(SnapShotContract.RotationEntry.TABLE_NAME, SnapShotContract.RotationEntry.COLUMN_IS_RECORD_UPLOADED);
            cleanData(SnapShotContract.LocationEntry.TABLE_NAME, SnapShotContract.LocationEntry.COLUMN_IS_RECORD_UPLOADED);
            cleanData(SnapShotContract.DetectedActivityEntry.TABLE_NAME, SnapShotContract.DetectedActivityEntry.COLUMN_IS_RECORD_UPLOADED);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CleanUpService.this, "Clean up completed!", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception ex){
            Log.d(TAG, "onHandleIntent: Exception while deleting data");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CleanUpService.this, "Something went wrong while cleaning!", Toast.LENGTH_SHORT).show();
                }
            });
            ex.printStackTrace();
        } finally {
            //Release resources
            if (db != null)
                db.close();
        }
    }

    private void cleanData(String table, String column){
        if (db == null)
            db = SnapShotDBHelper.getsInstance(this).getWritableDatabase();

        String selection = column + "= 'true'";

        Integer count = db.delete(table, selection, null );

        Log.d(TAG, "cleanData: " + count + table + "records were deleted");
    }
}
