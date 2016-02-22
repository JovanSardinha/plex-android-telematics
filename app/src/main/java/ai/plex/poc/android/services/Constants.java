package ai.plex.poc.android.services;

/**
 * Created by terek on 06/01/16.
 */

public final class Constants {

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

}
