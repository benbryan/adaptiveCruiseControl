package info.benbryan.cruseControl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;

import info.benbryan.cruseControl.GPS_Rectange;
import info.benbryan.cruseControl.SpeedLimitSign;

public class SpeedLimitSignDbHelper extends SQLiteOpenHelper {

    private final SQLiteDatabase db;

    private static final String TYPE_DOUBLE = " Double";
    private static final String TYPE_INT = " Integer";
    private static final String TYPE_Long = " Long";

    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + SpeedLimitSignEntry.TABLE_NAME + " (" +
            SpeedLimitSignEntry.COLUMN_NAME_ID + " INTEGER PRIMARY KEY," +
            SpeedLimitSignEntry.COLUMN_NAME_latitude  + TYPE_DOUBLE + COMMA_SEP +
            SpeedLimitSignEntry.COLUMN_NAME_longitude + TYPE_DOUBLE + COMMA_SEP +
            SpeedLimitSignEntry.COLUMN_NAME_altitude + TYPE_DOUBLE + COMMA_SEP +
            SpeedLimitSignEntry.COLUMN_NAME_bearing + TYPE_DOUBLE + COMMA_SEP +
            SpeedLimitSignEntry.COLUMN_NAME_speedLimit + TYPE_INT + COMMA_SEP +
            SpeedLimitSignEntry.COLUMN_NAME_dateDeleted + TYPE_Long  +
    " )";

    public static abstract class SpeedLimitSignEntry implements BaseColumns {
        public static final String TABLE_NAME = "entry";
        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_latitude = "latitude";
        public static final String COLUMN_NAME_longitude = "longitude";
        public static final String COLUMN_NAME_altitude = "altitude";
        public static final String COLUMN_NAME_bearing = "bearing";
        public static final String COLUMN_NAME_speedLimit = "speedLimit";
        public static final String COLUMN_NAME_dateDeleted = "dateDeleted";
    }

    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + SpeedLimitSignEntry.TABLE_NAME;

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "SpeedLimitSign.db";

    public SpeedLimitSignDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = getWritableDatabase();
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public long insert(SpeedLimitSign sign){
        ContentValues values = new ContentValues();
        values.put(SpeedLimitSignEntry.COLUMN_NAME_latitude, sign.latitude);
        values.put(SpeedLimitSignEntry.COLUMN_NAME_longitude, sign.longitude);
        values.put(SpeedLimitSignEntry.COLUMN_NAME_altitude, sign.altitude);
        values.put(SpeedLimitSignEntry.COLUMN_NAME_bearing, sign.bearing);
        values.put(SpeedLimitSignEntry.COLUMN_NAME_speedLimit, sign.speedLimit);
        values.put(SpeedLimitSignEntry.COLUMN_NAME_dateDeleted, sign.dateDeleted);

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert(SpeedLimitSignEntry.TABLE_NAME, null, values);
        return newRowId;
    }

    public ArrayList<SpeedLimitSign> query(GPS_Rectange rectange){
        ArrayList<SpeedLimitSign> signs = new ArrayList<>();
        Cursor c = getCursor(rectange);
        c.moveToFirst();
        for (int i = 0; i < c.getCount(); i++){
            double latitude = c.getDouble(c.getColumnIndex(SpeedLimitSignEntry.COLUMN_NAME_latitude));
            double longitude = c.getDouble(c.getColumnIndex(SpeedLimitSignEntry.COLUMN_NAME_longitude));
            double altitude = c.getDouble(c.getColumnIndex(SpeedLimitSignEntry.COLUMN_NAME_altitude));
            double bearing = c.getDouble(c.getColumnIndex(SpeedLimitSignEntry.COLUMN_NAME_bearing));
            int speedLimit = c.getInt(c.getColumnIndex(SpeedLimitSignEntry.COLUMN_NAME_speedLimit));
            long dateDeleted = c.getLong(c.getColumnIndex(SpeedLimitSignEntry.COLUMN_NAME_dateDeleted));
            SpeedLimitSign sign = new SpeedLimitSign(latitude, longitude, altitude, bearing, speedLimit, -1, dateDeleted);
            signs.add(sign);
            c.moveToNext();
        }
        c.close();
        return signs;
    }

    private Cursor getCursor(GPS_Rectange rectange){
        String[] projection = {
                SpeedLimitSignEntry.COLUMN_NAME_ID,
                SpeedLimitSignEntry.COLUMN_NAME_latitude,
                SpeedLimitSignEntry.COLUMN_NAME_longitude,
                SpeedLimitSignEntry.COLUMN_NAME_altitude,
                SpeedLimitSignEntry.COLUMN_NAME_bearing,
                SpeedLimitSignEntry.COLUMN_NAME_speedLimit,
                SpeedLimitSignEntry.COLUMN_NAME_dateDeleted
        };

        String sortOrder = null;

        String selection =   SpeedLimitSignEntry.COLUMN_NAME_latitude + ">? and "
                +SpeedLimitSignEntry.COLUMN_NAME_latitude + "<? and "
                +SpeedLimitSignEntry.COLUMN_NAME_longitude + ">? and "
                +SpeedLimitSignEntry.COLUMN_NAME_longitude + "<?";
        String selectionArgs[] = new String[]{
                String.valueOf(rectange.latitudeSpan[0]),
                String.valueOf(rectange.latitudeSpan[1]),
                String.valueOf(rectange.longitudeSpan[0]),
                String.valueOf(rectange.longitudeSpan[1])
        };

        Cursor c = db.query(
                SpeedLimitSignEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );
        return c;
    }

    public void delete(SpeedLimitSign sign) {
        double span = 0.00001;
        GPS_Rectange rect = new GPS_Rectange(
                new double[]{sign.latitude -span, sign.latitude +span},
                new double[]{sign.longitude-span, sign.longitude+span});
        String selection =
                  SpeedLimitSignEntry.COLUMN_NAME_latitude + ">? and "
                + SpeedLimitSignEntry.COLUMN_NAME_latitude + "<? and "
                + SpeedLimitSignEntry.COLUMN_NAME_longitude + ">? and "
                + SpeedLimitSignEntry.COLUMN_NAME_longitude + "<? and "
                + SpeedLimitSignEntry.COLUMN_NAME_speedLimit + " LIKE ? and "
                + SpeedLimitSignEntry.COLUMN_NAME_bearing + " LIKE ?";
        String[] selectionArgs = {
            String.valueOf(rect.latitudeSpan[0]),
            String.valueOf(rect.latitudeSpan[1]),
            String.valueOf(rect.longitudeSpan[0]),
            String.valueOf(rect.longitudeSpan[1]),
            String.valueOf(sign.speedLimit),
            String.valueOf(sign.bearing)
        };
        db.delete(SpeedLimitSignEntry.TABLE_NAME, selection, selectionArgs);
    }
}