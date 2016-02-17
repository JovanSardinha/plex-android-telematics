package ai.plex.poc.android.services;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationResult;

import ai.plex.poc.android.sensorListeners.SensorDataWriter;
import ai.plex.poc.android.sensorListeners.SensorType;

/**
 * Created by ashish on 16/02/16.
 */
public class LocationMonitorService extends IntentService {
    private static final String TAG = LocationMonitorService.class.getSimpleName();

    public LocationMonitorService() {
        super("LocationMonitorService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (LocationResult.hasResult(intent)) {
            Location location = LocationResult.extractResult(intent).getLastLocation();
            new SensorDataWriter(this, SensorType.LOCATION).writeData(location);
            Log.i(TAG, "New Location at: " + location.getLatitude() + "/" + location.getLongitude() + " at " + location.getSpeed());
        } else {
        Log.d(TAG, "Intent had no data returned");
        }
    }
}
