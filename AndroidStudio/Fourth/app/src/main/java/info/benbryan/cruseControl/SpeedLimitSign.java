package info.benbryan.cruseControl;

import android.webkit.URLUtil;

import com.google.android.gms.plus.internal.model.people.PersonEntity;
import com.google.android.gms.plus.model.people.Person;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class SpeedLimitSign{
    public final double latitude, longitude, altitude, bearing;
    public final long dateCreated;
    public final int speedLimit;
    public final long dateDeleted;

    public SpeedLimitSign(double latitude, double longitude, double altitude, double bearing, int speedLimit, long dateCreated, long dateDeleted) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.bearing = bearing;
        this.dateCreated = dateCreated;
        this.speedLimit = speedLimit;
        this.dateDeleted = dateDeleted;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SpeedLimitSign){
            SpeedLimitSign sign = (SpeedLimitSign) o;
            if ((sign.latitude != latitude) || (sign.longitude != longitude) | (sign.bearing != bearing) || (sign.altitude != altitude) || (sign.speedLimit != speedLimit) ){
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (int) (latitude*longitude*altitude*speedLimit*bearing);
    }

    public boolean isDeleted() {
        return dateDeleted>0;
    }
}