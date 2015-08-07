package info.benbryan.cruseControl;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CruiseControl extends FragmentActivity implements OnMapReadyCallback {

    private LocationManager mLocationManager;
    private GoogleMap googleMap = null;
    Location myLocationLast = null;
    Marker myLocationMarker = null;
    private Handler mHandler;
    private CruiseControlService cruseControlService = null;
    boolean cruiseControlServiceIsBound = false;
    private HashMap<Marker, SpeedLimitSign> speedLimitSigns = new HashMap<>();
    private Wikispeedia wikispeedia = null;

    ArrayList<Button> speedLimitButtons = new ArrayList<>();
    private CheckBox checkBoxCruiseControlConnected;
    private CheckBox checkBoxFollowLocation;
    private EditText editTextTargetSpeed;
    private EditText editTextCurrentSpeed;
    private ExecutorService executorDeleteSign;
    private ExecutorService executorGetSign;
    private SpeedLimitSignDbHelper speedLimitSignDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cruise_control);
        mHandler = new Handler();
        executorDeleteSign = Executors.newSingleThreadExecutor();
        executorGetSign = Executors.newSingleThreadExecutor();

        speedLimitButtons.add((Button)findViewById(R.id.buttonMPH_30));
        speedLimitButtons.add((Button)findViewById(R.id.buttonMPH_35));
        speedLimitButtons.add((Button)findViewById(R.id.buttonMPH_40));
        speedLimitButtons.add((Button)findViewById(R.id.buttonMPH_45));
        speedLimitButtons.add((Button)findViewById(R.id.buttonMPH_50));
        speedLimitButtons.add((Button)findViewById(R.id.buttonMPH_55));
        speedLimitButtons.add((Button)findViewById(R.id.buttonMPH_60));
        speedLimitButtons.add((Button)findViewById(R.id.buttonMPH_65));
        speedLimitButtons.add((Button) findViewById(R.id.buttonMPH_70));
        speedLimitButtons.add((Button) findViewById(R.id.buttonMPH_75));
        speedLimitButtons.add((Button) findViewById(R.id.buttonMPH_80));

        editTextTargetSpeed = (EditText)findViewById(R.id.editTextTargetSpeed);
        editTextCurrentSpeed = (EditText)findViewById(R.id.editTextCurrentSpeed);

        checkBoxCruiseControlConnected = (CheckBox)findViewById(R.id.checkBoxCruiseControlConnected);
        checkBoxFollowLocation = (CheckBox)findViewById(R.id.checkBoxFollowLocation);

        Button deleteButton = (Button)findViewById(R.id.buttonMPH_Delete);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteNearestSign();
            }
        });

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                initMap();
                initLocation();
                Intent cruiseControlServiceIntent = new Intent(getApplicationContext(), CruiseControlService.class);
                startService(cruiseControlServiceIntent);
                bindService(cruiseControlServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
                for (Button button : speedLimitButtons) {
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            buttonMPH_pressed((Button) v);
                        }
                    });
                }
            }
        }, 200);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                initWikispeedia();
            }
        }, 100);

        speedLimitSignDbHelper = new SpeedLimitSignDbHelper(getApplicationContext());
//        try {
//
//            ArrayList<SpeedLimitSign> signs = speedLimitSignDbHelper.query(new GPS_Rectange(new double[]{-1, 1}, new double[]{-1, 1}));
//            speedLimitSignDbHelper.delete(signs.get(0));
//        } catch (Exception ex){
//            ex.printStackTrace();
//        }
    }

    private void initWikispeedia() {
        String enabled =  SettingsManager.get(getApplicationContext(), SettingsManager.KEY_WIKISPEEDIA_ENABLED);
        if ((enabled == null) || (!Boolean.valueOf(enabled))){
            wikispeedia = null;
            return;
        }
        String namePrivate =  SettingsManager.get(getApplicationContext(), SettingsManager.KEY_NAME_PRIVATE);
        String namePublic =  SettingsManager.get(getApplicationContext(), SettingsManager.KEY_NAME_PUBLIC);
        String email =  SettingsManager.get(getApplicationContext(), SettingsManager.KEY_EMAIL);
        if ((namePrivate == null) || (namePublic == null) || (email == null)){
            Intent intent = new Intent(getApplicationContext(), WikispeediaSetup.class);
            startActivityForResult(intent, RequestCodes.WikispeediaSettings.ordinal());
        } else {
            try {
                wikispeedia = new Wikispeedia(namePrivate, namePublic, email);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteNearestSign() {
        if (googleMap == null) {
            return;
        }
        Marker nearest = null;
        double shortestDist = Double.MAX_VALUE;
        for(Marker marker:speedLimitSigns.keySet()){
            LatLng mPos = marker.getPosition();
            LatLng myPos = myLocationMarker.getPosition();
            double d = Math.sqrt(Math.pow(mPos.latitude - myPos.latitude, 2) + Math.pow(mPos.longitude - myPos.longitude, 2));
            if (d < shortestDist){
                shortestDist = d;
                nearest = marker;
            }
        }
        if (nearest != null){
            nearest.remove();
        }
        speedLimitSigns.remove(nearest);
    }

    private void buttonMPH_pressed(Button button) {
        int speedLimit = Integer.parseInt(button.getText().toString());
        if (myLocationLast == null){
            Toast.makeText(getApplicationContext(), "No GPS location", Toast.LENGTH_SHORT).show();
            return;
        }
        if (myLocationLast.getTime() < (new Date().getTime()-2000)){
            Toast.makeText(getApplicationContext(), "GPS info too old", Toast.LENGTH_SHORT).show();
            return;
        }
        float bearing = myLocationLast.getBearing();
        if (bearing == 0.0){
            Toast.makeText(getApplicationContext(), "GPS has no bearing", Toast.LENGTH_SHORT).show();
            return;
        }
        float speed_mph = myLocationLast.getSpeed()*2.23694f;
        if (speed_mph < 9){
            Toast.makeText(getApplicationContext(), "You must be moving at least 10 MPH", Toast.LENGTH_SHORT).show();
        }

        double latitude = myLocationLast.getLatitude();
        double longitude = myLocationLast.getLongitude();
        double altitude = myLocationLast.getAltitude();
        final SpeedLimitSign speedLimitSign = new SpeedLimitSign(latitude, longitude, altitude, bearing, speedLimit, new Date().getTime(), -1);
        addSpeedLimitSign(speedLimitSign);
        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                if (speedLimitSignDbHelper != null){
                    speedLimitSignDbHelper.insert(speedLimitSign);
                }
                if (wikispeedia != null) {
                    wikispeedia.postSign(speedLimitSign);
                }
                return null;
            }
        };
        task.execute();
    }

    private Marker addSpeedLimitSign(SpeedLimitSign speedLimitSign){
        String snippet =
                "MPH:" + speedLimitSign.speedLimit + "\n" +
                "latitude:" + speedLimitSign.latitude + "\n" +
                "longitude:" + speedLimitSign.longitude + "\n" +
                "altitude:" + speedLimitSign.altitude + "\n" +
                "bearing:" + speedLimitSign.bearing;
        BitmapDescriptor bitmapDescriptor =  speedLimitSignIcon(speedLimitSign, false);
        MarkerOptions speedLimitMarker = new MarkerOptions()
                .icon(bitmapDescriptor)
                .position(new LatLng(  speedLimitSign.latitude, speedLimitSign.longitude))
                .title("Speed Limit Sign")
                .snippet(snippet)
                .anchor(0.5f, 0.5f);
        Marker marker = googleMap.addMarker(speedLimitMarker);
        speedLimitSigns.put(marker, speedLimitSign);
        return marker;
    }

    private BitmapDescriptor speedLimitSignIcon(SpeedLimitSign speedLimitSign, boolean highlight){
        Bitmap.Config conf = Bitmap.Config.ARGB_4444;
        int width = 70, height = 70;
        Bitmap bmp = Bitmap.createBitmap(width, height, conf);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.CENTER);

        float mLength = (float) (Math.sqrt(Math.pow(width, 2)+Math.pow(width, 2))/3);
        float theta = (float) (speedLimitSign.bearing/180*Math.PI+Math.PI/2);

        Path path = new Path();
        path.moveTo((float) (width/2-Math.cos(theta+Math.PI/2)*mLength/2),  (float)(height/2-Math.sin(theta+Math.PI/2)*mLength/2));
        path.lineTo((float) (width/2-Math.cos(theta)*mLength),              (float)(height/2-Math.sin(theta)*mLength));
        path.lineTo((float) (width/2-Math.cos(theta-Math.PI/2)*mLength/2),  (float)(height/2-Math.sin(theta-Math.PI/2)*mLength/2));
        paint.setColor(Color.RED);
        canvas.drawPath(path, paint);
        paint.setColor(Color.BLACK);
        canvas.drawOval(new RectF(13, 13, width - 13, height - 13), paint);
        if (highlight){
            paint.setColor(Color.GREEN);
        } else {
            paint.setColor(Color.WHITE);
        }
        canvas.drawOval(new RectF(15, 15, width - 15, height - 15), paint);
        paint.setTextSize(30);
        paint.setColor(Color.BLACK);
        canvas.drawText(String.valueOf(speedLimitSign.speedLimit), 35, 45, paint);
        if (speedLimitSign.isDeleted()){
            paint.setColor(Color.RED);
            paint.setStrokeWidth(3);
            canvas.drawLine(0, 0, width, height, paint);
            canvas.drawLine(0,width,height,0, paint);
        }
        return BitmapDescriptorFactory.fromBitmap(bmp);
    }

    private BitmapDescriptor numericIcon(double value){
        Bitmap.Config conf = Bitmap.Config.ARGB_4444;
        int width = 150, height = 70;
        Bitmap bmp = Bitmap.createBitmap(width, height, conf);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.LEFT);
//        paint.setColor(Color.BLACK);
//        canvas.drawOval(new RectF(13, 13, width - 13, height - 13), paint);
//        paint.setColor(Color.WHITE);
//        canvas.drawOval(new RectF(15, 15, width - 15, height - 15), paint);
        paint.setTextSize(30);
        paint.setColor(Color.BLACK);
        if (value == Double.MAX_VALUE){
            canvas.drawText("Inf", 0, 45, paint);
        } else {
            canvas.drawText(String.valueOf(Math.round(value*100)/100), 0, 45, paint);
        }
        return BitmapDescriptorFactory.fromBitmap(bmp);
    }

    CruiseControlListener cruiseControlListener = new CruiseControlListener() {
        @Override
        public void lineRecieved(String line) {

        }

        @Override
        public void adcReading(int adcIdx, int value) {

        }

        @Override
        public void onConnect() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    checkBoxCruiseControlConnected.setChecked(true);
                }
            });
        }

        @Override
        public void onDisconnect() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    checkBoxCruiseControlConnected.setChecked(false);
                }
            });
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            cruseControlService = ((CruiseControlService.LocalBinder)service).getService();
            cruseControlService.addCruseControlListener(cruiseControlListener);
            checkBoxCruiseControlConnected.setChecked(cruseControlService.bluetoothIsConnected());
            Toast.makeText(getApplicationContext(), R.string.local_service_connected, Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            cruseControlService = null;
            Toast.makeText(getApplicationContext(), R.string.local_service_disconnected, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }

    Location lastSpeedUpdatePoint = null;
    Location lastDatabaseUpdatePoint = null;
    Marker markerLastSpeedLimitSign = null;

    private class SignMetric{
        float angleToSignForward, angleToSignBackward, distanceToSign, offsetFromPath;
    }

    private SignMetric signMetric(Location location, SpeedLimitSign sign){
        SignMetric metric = new SignMetric();
        Location signLocation = new Location("CruiseControl");
        signLocation.setLatitude(sign.latitude);
        signLocation.setLongitude(sign.longitude);
        signLocation.setBearing((float) sign.bearing);
        float bearing = location.bearingTo(signLocation);
        float angleToSign = bearing-location.getBearing();
        while(angleToSign<0){
            angleToSign+=360;
        }
        while(angleToSign>360){
            angleToSign-=360;
        }
        metric.angleToSignForward = angleToSign;
        metric.angleToSignBackward = angleToSign-180;
        metric.distanceToSign = location.distanceTo(signLocation);
        metric.offsetFromPath = (float) (Math.sin(angleToSign / 180 * Math.PI)*metric.distanceToSign);
        return metric;
    }

    private class SpeedLimitSignExt{
        SpeedLimitSign speedLimitSign;
        SignMetric signMetricBest;

        public SpeedLimitSignExt(SignMetric signMetricBest, SpeedLimitSign speedLimitSign) {
            this.signMetricBest = signMetricBest;
            this.speedLimitSign = speedLimitSign;
        }
    }
    SpeedLimitSignExt speedLimitSignExtTarget = null;

    public void updateSpeedLimitSignExtTarget(Location myLocation){
        if (speedLimitSigns.size() == 0) {
            return;
        }
        SpeedLimitSign bestSign;
        if (speedLimitSignExtTarget == null) {
            bestSign = speedLimitSigns.entrySet().iterator().next().getValue();
        } else {
            bestSign = speedLimitSignExtTarget.speedLimitSign;
        }
        SignMetric bestMetric = signMetric(myLocation, bestSign);
        for (SpeedLimitSign signTest:speedLimitSigns.values()) {
            SignMetric metric = signMetric(myLocation, signTest);
//                                entry.getKey().setIcon(numericIcon(metric.angleToSignBackward));
            if ((metric.offsetFromPath > 10) || (metric.distanceToSign>1000)){
                continue;
            }
            if (metric.distanceToSign < bestMetric.distanceToSign){
                if ((Math.abs(metric.angleToSignForward) <  10)
                        && (speedLimitSignExtTarget != null)
                        && (speedLimitSignExtTarget.speedLimitSign.speedLimit > signTest.speedLimit)
                        && metric.distanceToSign < 500){
                    bestMetric = metric;
                    bestSign = signTest;
                }
                if ((Math.abs(metric.angleToSignBackward) <  10)){
                    bestMetric = metric;
                    bestSign = signTest;
                }
            }
        }

        final SignMetric bestSignMetricHold = bestMetric;
        final SpeedLimitSign bestSpeedLimitSign = bestSign;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if ((speedLimitSignExtTarget != null) && (speedLimitSignExtTarget.speedLimitSign == bestSpeedLimitSign)) {
                    return;
                }
                if (speedLimitSignExtTarget != null){
                    try {
                        for (Map.Entry<Marker, SpeedLimitSign> entry :speedLimitSigns.entrySet()){
                            if (entry.getValue().equals(speedLimitSignExtTarget.speedLimitSign)){
                                entry.getKey().setIcon(speedLimitSignIcon(speedLimitSignExtTarget.speedLimitSign, false));
                            }
                        }
                    } catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
                speedLimitSignExtTarget = new SpeedLimitSignExt(bestSignMetricHold, bestSpeedLimitSign);
                editTextTargetSpeed.setText(String.valueOf(speedLimitSignExtTarget.speedLimitSign.speedLimit));
                for (Map.Entry<Marker, SpeedLimitSign> entry :speedLimitSigns.entrySet()){
                    if (entry.getValue().equals(speedLimitSignExtTarget.speedLimitSign)){
                        entry.getKey().setIcon(speedLimitSignIcon(speedLimitSignExtTarget.speedLimitSign, true));
                    }
                }
            }
        });
    }

    private void initMap(){
        MapFragment mMapFragment = MapFragment.newInstance();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.mapSigns, mMapFragment);
        fragmentTransaction.commit();
        mMapFragment.getMapAsync(this);
    }
    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location myLocation) {
            if (googleMap == null){
                return;
            }
            editTextCurrentSpeed.setText(String.format("%.2f", myLocation.getSpeed() * 2.23694));
            if (lastDatabaseUpdatePoint == null) {
                lastDatabaseUpdatePoint = myLocation;
                double span = 0.01;
                GPS_Rectange rectange = new GPS_Rectange(
                        new double[]{myLocation.getLatitude() -span, myLocation.getLatitude() +span},
                        new double[]{myLocation.getLongitude()-span, myLocation.getLongitude()+span}
                );
                updateSpeedLimitSigns(rectange);
            } else if (lastDatabaseUpdatePoint.distanceTo(myLocation)>250) {
                lastDatabaseUpdatePoint = myLocation;
                double span = 0.01;
                GPS_Rectange rectange = new GPS_Rectange(
                        new double[]{myLocation.getLatitude() -span, myLocation.getLatitude() +span},
                        new double[]{myLocation.getLongitude()-span, myLocation.getLongitude()+span}
                );
                updateSpeedLimitSigns(rectange);
            }
            if (lastSpeedUpdatePoint == null) {
                lastSpeedUpdatePoint = myLocation;
                updateSpeedLimitSignExtTarget(myLocation);
            } else if (lastSpeedUpdatePoint.distanceTo(myLocation)>50) {
                lastSpeedUpdatePoint = myLocation;
                updateSpeedLimitSignExtTarget(myLocation);

                executorGetSign.shutdown();
                executorGetSign = Executors.newSingleThreadExecutor();
//                executorGetSign.submit(new Runnable() {
//                    @Override
//                    public void run() {
                        if (wikispeedia != null) {
                            final SpeedLimitSign sign = wikispeedia.getSign(myLocation.getLatitude(), myLocation.getLongitude(), myLocation.getBearing());
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (sign != null) {
                                        editTextTargetSpeed.setText(String.valueOf(sign.speedLimit));
                                        if (markerLastSpeedLimitSign != null) {
                                            markerLastSpeedLimitSign.remove();
                                        }
                                        markerLastSpeedLimitSign = addSpeedLimitSign(sign);
                                    } else {
                                        editTextTargetSpeed.setText(String.valueOf(-1));
                                    }
                                }
                            });
                        }
//                    }
//                });
            }

            if (checkBoxFollowLocation.isChecked()){
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()));
                googleMap.moveCamera(cameraUpdate);
            }

            if (myLocationLast == null){
                final double searchDist = 0.18; // about 10 miles
                final GPS_Rectange rect = new GPS_Rectange(
                        new double[]{myLocation.getLatitude()-searchDist, myLocation.getLatitude()+searchDist},
                        new double[]{myLocation.getLongitude()-searchDist, myLocation.getLongitude()+searchDist});
//                AsyncTask task = new AsyncTask() {
//                    @Override
//                    protected Object doInBackground(Object[] params) {
//                        try {
//                            File yourFile = new File("/mnt/extSdCard/markers.xml");
//                            FileInputStream fis = new FileInputStream(yourFile);
//                            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
//                            StringBuilder sb = new StringBuilder();
//                            while (br.ready()){
//                                sb.append(br.readLine());
//                            }
//                            fis.close();
////                              final ArrayList<SpeedLimitSign> speedLimitSigns = SpeedLimitSign.getSigns(rect, SettingsManager.get(getApplicationContext(),SettingsManager.KEY_NAME_PRIVATE));
//                            final ArrayList<SpeedLimitSign> speedLimitSigns = MarkerParser.parse(sb.toString());
//                            mHandler.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    try {
//                                        for (SpeedLimitSign speedLimitSign:speedLimitSigns){
//                                            addSpeedLimitSign(speedLimitSign);
//                                        }
//                                    } catch (Exception e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                            });
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                        return null;
//                    }
//                };
//                task.execute();
            }
            myLocationLast = myLocation;
            LatLng myLatLong = new LatLng(myLocationLast.getLatitude(), myLocationLast.getLongitude());
            if (myLocationMarker == null) {
                googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(myLatLong, 14)));
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.anchor(0.5f, 0.5f);
                markerOptions.position(myLatLong);
                markerOptions.title("Some guy");
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow_downs));
                myLocationMarker = googleMap.addMarker(markerOptions);
            } else {
                myLocationMarker.setPosition(myLatLong);
                myLocationMarker.setRotation(myLocation.getBearing());
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    private void initLocation(){
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    final Context context = this;
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(final Marker marker) {
                try {
                    if (speedLimitSigns.get(marker) != null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage(R.string.test_delete_speed_limit)
                                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        final SpeedLimitSign speedLimitSign = speedLimitSigns.remove(marker);
                                        try {
                                            executorDeleteSign.submit(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (wikispeedia != null) {
                                                        String result = wikispeedia.deleteSign(speedLimitSign);
                                                        if (result == null) {
                                                            return;
                                                        }
                                                    }
                                                    if (speedLimitSignDbHelper != null) {
                                                        speedLimitSignDbHelper.delete(speedLimitSign);
                                                    }
                                                    mHandler.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            try {
                                                                marker.remove();
                                                            } catch (Exception ex) {
                                                                ex.printStackTrace();
                                                            }
                                                        }
                                                    });
                                                }
                                            });
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                });
                        builder.create().show();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
//                VisibleRegion vr = googleMap.getProjection().getVisibleRegion();
//                double left = vr.latLngBounds.southwest.longitude;
//                double top = vr.latLngBounds.northeast.latitude;
//                double right = vr.latLngBounds.northeast.longitude;
//                double bottom = vr.latLngBounds.southwest.latitude;
//                GPS_Rectange rectange = new GPS_Rectange(new double[]{top, bottom}, new double[]{left, right});
//                updateSpeedLimitSigns(rectange);
            }
        });
        Button buttonGoogleMapOverlayAccelerate = (Button)findViewById(R.id.buttonGoogleMapOverlayAccelerate);
        Button buttonGoogleMapOverlayCoast = (Button)findViewById(R.id.buttonGoogleMapOverlayCoast);
        Button buttonGoogleMapOverlayOff = (Button)findViewById(R.id.buttonGoogleMapOverlayOff);
        Button buttonGoogleMapOverlayOn = (Button)findViewById(R.id.buttonGoogleMapOverlayOn);
        Button buttonGoogleMapOverlayResume = (Button)findViewById(R.id.buttonGoogleMapOverlayResume);

        TableLayout tableLayoutGoogleMapOverlay = (TableLayout)findViewById(R.id.tableLayoutGoogleMapOverlay);
        tableLayoutGoogleMapOverlay.bringToFront();
//        buttonGoogleMapOverlayAccelerate.bringToFront();
//        buttonGoogleMapOverlayCoast.bringToFront();
//        buttonGoogleMapOverlayOff.bringToFront();
//        buttonGoogleMapOverlayOn.bringToFront();
//        buttonGoogleMapOverlayResume.bringToFront();

    }

    private void updateSpeedLimitSigns(GPS_Rectange rectange){
        ArrayList<SpeedLimitSign> signsNew = speedLimitSignDbHelper.query(rectange);
        for (SpeedLimitSign signNew:signsNew){
            if (speedLimitSigns.containsValue(signNew)){
                continue;
            } else {
                addSpeedLimitSign(signNew);
            }
        }
        ArrayList<Marker> toRemove = new ArrayList<>();
        for (Map.Entry<Marker, SpeedLimitSign> entry:speedLimitSigns.entrySet()){
            if (!signsNew.contains(entry.getValue())){
                toRemove.add(entry.getKey());
            }
        }
        for (Marker marker:toRemove){
            speedLimitSigns.remove(marker);
            marker.remove();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_acc__setup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        switch (id){
            case R.id.action_setup_cruse_control:
                intent = new Intent(getApplicationContext(), CruiseControlSetup.class);
                startActivityForResult(intent, RequestCodes.CruseControlSettings.ordinal());
                break;
            case R.id.action_setup_wikispeedia:
                intent = new Intent(getApplicationContext(), WikispeediaSetup.class);
                startActivityForResult(intent, RequestCodes.WikispeediaSettings.ordinal());
                break;
            case R.id.action_setup_service_restart:
                Intent cruiseControlServiceIntent = new Intent(getApplicationContext(), CruiseControlService.class);
                stopService(cruiseControlServiceIntent);
                startService(cruiseControlServiceIntent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == RequestCodes.WikispeediaSettings.ordinal()) {
            // Make sure the request was successful
//            if (resultCode == RESULT_OK) {
                initWikispeedia();
//            }
        }
    }
}
