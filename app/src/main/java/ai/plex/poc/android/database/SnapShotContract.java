package ai.plex.poc.android.database;

import android.provider.BaseColumns;

/**
 * Created by terek on 07/01/16.
 */
public class SnapShotContract {

    public static final class AccelerationEntry implements BaseColumns{
        public static final String TABLE_NAME = "acceleration";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_X = "x";
        public static final String COLUMN_Y = "y";
        public static final String COLUMN_Z = "z";
    }
}
