package ai.plex.poc.android.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import java.util.HashSet;

import ai.plex.poc.android.database.SnapShotContract;
import ai.plex.poc.android.database.SnapShotDBHelper;

/**
 * Created by terek on 07/01/16.
 */
public class TestDb extends AndroidTestCase {
    public static final String LOG_TAG= TestDb.class.getSimpleName();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTheDatabase();
    }

    void deleteTheDatabase(){
        mContext.deleteDatabase(SnapShotDBHelper.DATABASE_NAME);
    }

    public void testCreateDb() throws Throwable{
        SQLiteDatabase db  = new SnapShotDBHelper(this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());

        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type ='table'", null);

        assertTrue("Database has not been created correctly", c.moveToFirst());

        c = db.rawQuery("PRAGMA table_info(" + SnapShotContract.AccelerationEntry.TABLE_NAME+")", null);

        assertTrue("Unable to query the database for table information.", c.moveToFirst());

        final HashSet<String> accelerationColumnHashSet = new HashSet<String>();
        accelerationColumnHashSet.add(SnapShotContract.AccelerationEntry._ID);
        accelerationColumnHashSet.add(SnapShotContract.AccelerationEntry.COLUMN_TIMESTAMP);
        accelerationColumnHashSet.add(SnapShotContract.AccelerationEntry.COLUMN_X);
        accelerationColumnHashSet.add(SnapShotContract.AccelerationEntry.COLUMN_Y);
        accelerationColumnHashSet.add(SnapShotContract.AccelerationEntry.COLUMN_Z);

        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            accelerationColumnHashSet.remove(columnName);
        } while(c.moveToNext());

        assertTrue("Error: The database doesn't contain all of the required location entry columns",
                accelerationColumnHashSet.isEmpty());
        db.close();

    }






}
