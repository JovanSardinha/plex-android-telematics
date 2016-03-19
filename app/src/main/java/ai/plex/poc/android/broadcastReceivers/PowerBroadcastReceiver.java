package ai.plex.poc.android.broadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import ai.plex.poc.android.services.CleanUpService;

/**
 * Created by terek on 18/03/16.
 * This broadcast receiver is responsible for capturing the power connection event.
 * This is used to do clean up tasks such as removing uploaded data
 */
public class PowerBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = PowerBroadcastReceiver.class.getSimpleName();
    private Context mContext;


    @Override
    public void onReceive(Context context, Intent intent) {
        this.mContext = context;
        Toast.makeText(mContext, "Power Connected", Toast.LENGTH_SHORT).show();

        //Start the cleanup intent service
        Intent cleanUpIntent = new Intent(mContext, CleanUpService.class);
        mContext.startService(cleanUpIntent);
    }
}
