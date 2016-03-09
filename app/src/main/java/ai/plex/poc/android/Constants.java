package ai.plex.poc.android;

/**
 * Created by terek on 06/01/16.
 */

public final class Constants {
    public static final String IP_ADDRESS = "40.122.215.160:8080";

    public static final int MAX_ENTRIES_PER_API_SUBMISSION = 700;

    // Defines a custom Intent action
    public static final String BROADCAST_ACTION =
            "ai.plex.poc.android.BROADCAST";

    // Defines the key for the status "extra" in an Intent
    public static final String EXTENDED_DATA_STATUS =
            "ai.plex.poc.android.ALIVE";

    public static final String ACTIVITY_UPDATE_BROADCAST_ACTION =
            "ai.plex.poc.android.ACTIVITY_UPDATE_BROADCAST_ACTION";

    // Extra's for activity update broadcast Intent
    public static final String ACTIVITY_NAME =
            "ai.plex.poc.android.ACTIVITY_NAME";

    public static final String ACTIVITY_CONFIDENCE =
            "ai.plex.poc.android.ACTIVITY_CONFIDENCE";

    public static final String LOCATION_UPDATE_BROADCAST_ACTION =
            "ai.plex.poc.android.LOCATION_UPDATE_BROADCAST_ACTION";

    // Extra's for activity update broadcast Intent
    public static final String LATITUDE =
            "ai.plex.poc.android.LATITUDE";

    public static final String LONGITUDE =
            "ai.plex.poc.android.LONGITUDE";

    public final class ACTIONS {
        public static final String START_PREDICTIVE_MOTION_SERVICE_IN_FOREGROUND = "startPredictiveMotionServiceInForeground";
        public static final String START_MAIN_ACTION = "startMainAction";
        public static final String STOP_PREDICTIVE_MOTION_SERVICE_IN_FOREGROUND = "stopPredictiveMotionServiceInForeground";
    }

    public final class NOTIFICATION_ID {
        public static final int FOREGROUND_SERVICE = 1;
    }
}
