package ai.plex.poc.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by terek on 06/01/16.
 */
// Broadcast receiver for receiving status updates from the IntentService
public class ResponseReceiver extends BroadcastReceiver
{
    // Prevents instantiation
    public ResponseReceiver() {
    }
    // Called when the BroadcastReceiver gets an Intent it's registered to receive

    public void onReceive(Context context, Intent intent) {

        /*
         * Handle Intents here.
         *
         */


        CharSequence text = "The service is responding!!!!";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();



    }
}