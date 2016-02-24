package ai.plex.poc.android.tasks;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

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
import java.util.List;

import ai.plex.poc.android.activities.Welcome;
import ai.plex.poc.android.database.SnapShotContract;

/**
 * Created by terek on 24/02/16.
 * This asyncTask is responsible for submitting data to the rest service
 */
public class PostDataTask extends AsyncTask<List<JSONObject>, Void, Void> {
    //Private copy of the context
    private Context context;

    private final static String TAG = PostDataTask.class.getSimpleName();


    //Task constructor taking in a context
    public PostDataTask(Context context){
        //set the context for the task
        this.context = context;
    }

    protected Void doInBackground(List<JSONObject>... events){
        if (events[0].isEmpty()) {
            return null;
        }

        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
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

                Welcome welcomeActivity = context instanceof Welcome ? (Welcome) context : null;

                //Update the main UI
                if (welcomeActivity != null){
                    welcomeActivity.decrementGlobalCounter(dataPoints.length());
                    publishProgress();
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
        return null;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);

        //Update the activity by the reference the task is holding on to, keep in mind this is running on the main thread
        Welcome welcomeActivity = context instanceof Welcome ? (Welcome) context : null;
        if (context != null){
            welcomeActivity.updateStatus(String.valueOf(welcomeActivity.getGlobalCounter()) + " items remaining");
        }
    }
}