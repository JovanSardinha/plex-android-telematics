package ai.plex.poc.android.sensorListeners;

import android.content.Context;
import android.hardware.Sensor;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationListener;

/**
 * Created by terek on 10/02/16.
 */
public class LocationMonitor implements LocationListener {
    private Context applicationContext;


    public LocationMonitor(Context context){
        this.applicationContext = context;
    }


    @Override
    public void onLocationChanged(Location location) {
        new SensorDataWriter(applicationContext,-1, true).writeData(null, location);
        Log.i("LOCATION", "New Location at: " + location.getLatitude() + "/" + location.getLongitude() + " at " + location.getSpeed());
    }
}
