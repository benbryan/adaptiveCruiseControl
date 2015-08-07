package info.benbryan.cruseControl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class Wikispeedia {
    private final String namePrivate, namePublic, email;

    public Wikispeedia(String namePrivate, String namePublic, String email) throws UnsupportedEncodingException {
        this.namePrivate = URLEncoder.encode(namePrivate, "UTF-8");
        this.namePublic = URLEncoder.encode(namePublic, "UTF-8");
        this.email = URLEncoder.encode(email, "UTF-8");
    }

    public ArrayList<SpeedLimitSign> getSigns(GPS_Rectange rect){
        URL url = toURL("http://www.wikispeedia.org/a/marks_bb2.php"
                + "?name=" + namePrivate
                + "&nelat=" + (rect.latitudeSpan[1])
                + "&swlat=" + (rect.latitudeSpan[0])
                + "&nelng=" + (rect.longitudeSpan[1])
                + "&swlng=" + (rect.longitudeSpan[0]));
        if (url == null){
            return null;
        }
        String response = doGet(url);
        if (response == null){
            return null;
        }
        return MarkerParser.parse(response);
    }

    public SpeedLimitSign getSign(double latitude, double longitude, double bearing) {
        URL url = toURL("http://www.wikispeedia.org/speed/geo.php"
                + "?name=" + namePrivate
                + "&latlngcog=" + String.valueOf((float)latitude)
                + "," + String.valueOf((float)longitude)
                + "," + String.valueOf((float)bearing));
        if (url == null){
            return null;
        }
        String response = doGet(url);
        if (response == null){
            return null;
        }
        ArrayList<SpeedLimitSign> markers = MarkerParser.parse(response);
        if (markers.size() > 1){
            for (SpeedLimitSign marker:markers){
                if (marker.isDeleted()){
                    continue;
                } else {
                    return marker;
                }
            }
            return markers.get(1);
        }
        return null;
    }

    private String doConn(HttpURLConnection conn) throws IOException {
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        char buff[] = new char[1024];
        int count = 0;
        StringBuilder sb = new StringBuilder();
        while ((count = br.read(buff)) > 0) {
            sb.append(buff, 0, count);
        }
        return sb.toString();
    }

    private String doPost(URL url){
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            return doConn(conn);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String doGet(URL url){
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            return doConn(conn);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public String deleteSign(SpeedLimitSign speedLimitSign) {
        double latitude = speedLimitSign.latitude;
        double longitude = speedLimitSign.longitude;
        double span =  .00001;
        final GPS_Rectange rect = new GPS_Rectange(
                new double[]{latitude-span, latitude+span},
                new double[]{longitude-span, longitude+span});
        URL url = toURL("http://www.wikispeedia.org/a/delete_bb2a.php"
                + "?name=" + namePrivate
                + "&nelat=" + (rect.latitudeSpan[1])
                + "&swlat=" + (rect.latitudeSpan[0])
                + "&nelng=" + (rect.longitudeSpan[1])
                + "&swlng=" + (rect.longitudeSpan[0])
                + "&delmail=" + email);
        if (url == null){
            return null;
        }
        return doPost(url);
    }

    private URL toURL(String str){
        try {
            return new URL(str);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String postSign(SpeedLimitSign speedLimitSign) {
        URL url = toURL("http://www.wikispeedia.org/a/process_submit_bb3.php"
                + "?name=" + namePrivate
                + "&mlat=" + speedLimitSign.latitude
                + "&mlon=" + speedLimitSign.longitude
                + "&malt_meters=" + speedLimitSign.altitude
                + "&mmph=" + speedLimitSign.speedLimit
                + "&mkph=" + "69"
                + "&mtag=" + namePublic
                + "&mcog=" + speedLimitSign.bearing
                + "&mhours=" + ""
                + "&delmail=" + email);
        if (url == null){
            return null;
        }
        return doPost(url);
    }

}
