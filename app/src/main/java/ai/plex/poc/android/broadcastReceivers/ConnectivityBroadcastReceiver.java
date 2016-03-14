package ai.plex.poc.android.broadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

import ai.plex.poc.android.Constants;
import ai.plex.poc.android.services.UploadDataService;

/**
 * Created by terek on 08/01/16.
 * This broadcast receiver is responsible for starting the upload service when
 * it identifies that the phone is connected to WIFI
 */
public class ConnectivityBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = ConnectivityBroadcastReceiver.class.getSimpleName();
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        //WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        ConnectivityManager mConnectionManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = mConnectionManager.getActiveNetworkInfo();

        switch (intent.getAction()){
            case ConnectivityManager.CONNECTIVITY_ACTION:
                if (networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
                    Log.d(TAG, "onReceive: Wifi Enabled Broadcast Received");
                    //Start an intent to start the upload service
                    Intent mIntent = new Intent(Constants.ACTIONS.START_UPLOAD_SERVICE,null, context, UploadDataService.class);
                    //Add userId as an extra
                    mIntent.putExtra("userId", PreferenceManager.getDefaultSharedPreferences(context).getString("userId", "default_user"));
                    //Start the service
                    context.startService(mIntent);

                } else {
                    Log.d(TAG, "onReceive: Wifi Disabled Broadcast Received");
                    //Start an intent to stop the upload service
                    Intent mIntent = new Intent(Constants.ACTIONS.STOP_UPLOAD_SERVICE,null, context, UploadDataService.class);
                    //Send the intent
                    context.startService(mIntent);
                }
                break;
        }
    }
}
