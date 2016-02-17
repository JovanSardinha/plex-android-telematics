package ai.plex.poc.android.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import ai.plex.poc.android.sensorListeners.SensorDataWriter;
import ai.plex.poc.android.sensorListeners.SensorType;

/**
 * Created by ashish on 16/02/16.
 */
public class ActivityRecognitionIntentService extends IntentService {
    private static final String TAG = ActivityRecognitionIntentService.class.getSimpleName();

    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity detectedActivity = result.getMostProbableActivity();

            int confidence = detectedActivity.getConfidence();
            String mostProbableName = getActivityName(detectedActivity.getType());

            new SensorDataWriter(this, SensorType.ACTIVITY_DETECTOR).writeData(detectedActivity);
            Log.d(TAG, "Detected activity: " + mostProbableName + "( with confidence " + confidence + ")");
        } else {
            Log.d(TAG, "Intent had no data returned");
        }
    }

    private String getActivityName(int type) {
        switch (type)
        {
            case DetectedActivity.IN_VEHICLE:
                return "In Vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "On Bicycle";
            case DetectedActivity.ON_FOOT:
                return "On Foot";
            case DetectedActivity.WALKING:
                return "Walking";
            case DetectedActivity.STILL:
                return "Still";
            case DetectedActivity.TILTING:
                return "Tilting";
            case DetectedActivity.RUNNING:
                return "Running";
            case DetectedActivity.UNKNOWN:
                return "Unknown";
        }
        return "N/A";
    }
}
