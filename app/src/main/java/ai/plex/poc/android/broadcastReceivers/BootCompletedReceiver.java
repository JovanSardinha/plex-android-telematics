package ai.plex.poc.android.broadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ai.plex.poc.android.services.PredictiveMotionDataService;

/**
 * Created by terek on 09/03/16.
 * A receiver responsible for starting the PredictiveMotionDataService when
 * the phone boots up
 * */
public class BootCompletedReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        //Starts the predictive motion data service
        Intent startServiceIntent = new Intent(context, PredictiveMotionDataService.class);
        context.startService(startServiceIntent);
    }
}
