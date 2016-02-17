package ai.plex.poc.android.broadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

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
import java.util.Date;

import ai.plex.poc.android.database.SnapShotContract;
import ai.plex.poc.android.database.SnapShotDBHelper;

/**
 * Created by terek on 08/01/16.
 */
public class WifiBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = WifiBroadcastReceiver.class.getSimpleName();
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        // Check wifi status
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        // Check if WIFI is connected
        if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI){
            Log.d(TAG, "Connected to WIFI!");

        }
    }

    public class PostDataTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... events){

            ConnectivityManager connMgr = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                InputStream is = null;
                // Only display the first 500 characters of the retrieved
                // web page content.
                int len = 500;

                try {
                    //Define the URL
                    URL url = new URL("http://10.42.0.1:8080/androidAcceleration");
                    //URL url = new URL("http://40.122.215.160:8080/androidAcceleration");

                    String message = events[0];

                    //Open a connection
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    //Set connection details
                    conn.setReadTimeout(10000 /* milliseconds */);
                    conn.setConnectTimeout(15000 /* milliseconds */);
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setFixedLengthStreamingMode(message.getBytes().length);

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
