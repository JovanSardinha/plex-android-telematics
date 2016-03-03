package ai.plex.poc.android.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

import ai.plex.poc.android.database.SnapShotContract;

/**
 * Created by terek on 02/03/16.
 * This service is responsible for interacting with the remote API to submit data.
 * Currently only uploads data
 */
public class RemoteApiService extends IntentService {
    //Tag for logging purposes
    private static final String TAG = RemoteApiService.class.getSimpleName();

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public RemoteApiService() {
        super("RemoteApiService");
    }

    /**
     * Current default behaviour is to take in data and attempt to submit it to the remote api
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        //Try to convert the data to a JsonArray
        try {
            JSONArray dataArray = new JSONArray(intent.getStringExtra("data"));
            JSONObject dataIds = new JSONObject(intent.getStringExtra("dataIds"));

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
                    URL url = new URL("http://"+ ai.plex.poc.android.activities.Constants.IP_ADDRESS +"/"+ api_route);

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
                        //Notify the database service to update the records in the database to indicate successful upload
                        Intent updateDataAsSubmittedIntent = new Intent("ai.plex.poc.android.updateDataAsSubmitted", null, this, DatabaseService.class);
                        updateDataAsSubmittedIntent.putExtra("dataIds",dataIds.toString());
                        startService(updateDataAsSubmittedIntent);
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
    public void onCreate() {
        super.onCreate();
        //Flag the database service to prevent subsequent requests to upload data
        DatabaseService.isUploading = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Tell the database service that uploading has finished
        Intent databaseServiceIntent = new Intent("ai.plex.poc.android.uploadingComplete", null, this, DatabaseService.class);
        startService(databaseServiceIntent);
    }
}
