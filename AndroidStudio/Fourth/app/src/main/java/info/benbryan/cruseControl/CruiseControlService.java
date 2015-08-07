package info.benbryan.cruseControl;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.audiofx.BassBoost;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class CruiseControlService extends Service {

    ArrayList<CruiseControlListener> cruiseControlListeners = new ArrayList<>();
    BluetoothDevice bluetoothDeviceCruseControl = null;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private InputStream inputStreamCruseControl = null;
    private OutputStream outputStreamCruseControl = null;
    private Thread threadOutputStreamReader;
    private BufferedReader bufferedReaderReaderCurseControl;
    private NotificationManager notificationManager;
    private BluetoothSocket bluetoothSocketCruseControl;
    private BluetoothDevice bluetoothDevice;
    private Location myLocationLast = null;
    private int pollForBluetoothDevicePeroid = 30*1000;
    private Handler mHandler;
    HashMap<String, ButtonProfile> buttonsProfile;

    public void setBluetoohDevice(BluetoothDevice device) {
        SettingsManager.set(getApplicationContext(), SettingsManager.KEY_BluetoothDeviceAddress, device.getAddress());
        setupBlutoothConnection();
    }

    public boolean bluetoothIsConnected() {
        if (bluetoothSocketCruseControl == null){
            return false;
        }
        return bluetoothSocketCruseControl.isConnected();
    }

    private enum NotificaionId{
        ACTIVE, NO_BLUETOOTH, NO_BLUETOOTH_ENABLED
    }

    public void addCruseControlListener(CruiseControlListener listener){
        cruiseControlListeners.add(listener);
    }
    public void removeCruseControlListener(CruiseControlListener listener){
        cruiseControlListeners.remove(listener);
    }

    public void sampleADC(int idx){
        sendCommand("adc=0");
    }
    public void setDAC(int value){
        sendCommand("dac=" + String.valueOf(value));
    }

    private void listenToLocation(final boolean yes){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (yes){
                    LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                } else {
                    LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    mLocationManager.removeUpdates(locationListener);
                }
            }
        });
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location myLocation) {
            myLocationLast = myLocation;
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

    public void setupBlutoothConnection() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            CharSequence text = "No bluetooth adapter found";
            Notification notification = new Notification(R.drawable.wikisoeedia, text, System.currentTimeMillis());
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, CruiseControl.class), 0);
            notification.setLatestEventInfo(this, getText(R.string.text_cruise_control_service_label), text, contentIntent);
            notificationManager.notify(NotificaionId.NO_BLUETOOTH.ordinal(), notification);
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }

        String cruseControlAddress = SettingsManager.get(this, SettingsManager.KEY_BluetoothDeviceAddress);
        if (cruseControlAddress == null) {
            promptToSelectBluetoothDevice();
            return;
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        BluetoothDevice bluetoothDeviceTemp = null;
        for (BluetoothDevice device : pairedDevices) {
            if (device.getAddress().equals(cruseControlAddress)){
                bluetoothDeviceTemp = device;
                break;
            }
        }
        if (bluetoothDeviceTemp == null){
            promptToSelectBluetoothDevice();
            return;
        }
        if ( (this.bluetoothDevice != null) &&
             this.bluetoothDevice.getAddress().equals(bluetoothDeviceTemp.getAddress()) &&
             (bluetoothSocketCruseControl != null) &&
             bluetoothSocketCruseControl.isConnected()){
            return;
        }
        if ((bluetoothSocketCruseControl != null) && bluetoothSocketCruseControl.isConnected()) {
            closeBluetoothConnection();
        }
        try {
            bluetoothSocketCruseControl = bluetoothDeviceTemp.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocketCruseControl.connect();
            inputStreamCruseControl = bluetoothSocketCruseControl.getInputStream();
            bufferedReaderReaderCurseControl = new BufferedReader(new InputStreamReader(inputStreamCruseControl));
            outputStreamCruseControl = bluetoothSocketCruseControl.getOutputStream();
            bluetoothDevice = bluetoothDeviceTemp;
            showNotificationCruiseControlConnected();
            threadOutputStreamReader = new Thread(new Runnable() {
                @Override
                public void run() {
                    while ((bluetoothSocketCruseControl != null) && bluetoothSocketCruseControl.isConnected()) {
                        try {
                            String line = bufferedReaderReaderCurseControl.readLine();
                            if (line.isEmpty()) {
                                continue;
                            }
                            System.out.println("Response: " + line);
                            parseAndSendLine(line);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            threadOutputStreamReader.start();
            listenToLocation(true);
            for (CruiseControlListener listener:cruiseControlListeners){
                listener.onConnect();
            }
            pollForBluetoothDeviceTimer.cancel();
            pollForBluetoothDeviceTimer = null;
        } catch (IOException e) {
//            e.printStackTrace();
//
            listenToLocation(false);
        }
    }

    private void promptToSelectBluetoothDevice() {
        Intent intent = new Intent(this, SelectBluetoothDevice.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void parseAndSendLine(String line) {
        for (CruiseControlListener listener: cruiseControlListeners){
            String parts[] = line.split("=");
            if ((parts.length == 2) && parts[0].startsWith("adc")){
                try {
                    parts[0] = parts[0].replaceFirst("adc", "");
                    int idx = Integer.parseInt(parts[0]);
                    int value = Integer.parseInt(parts[1]);
                    listener.adcReading(idx, value);
                    continue;
                } catch (NumberFormatException ex){ }
            }
            listener.lineRecieved(line);
        }
    }

    public void closeBluetoothConnection() {
        if (bluetoothSocketCruseControl == null){
            return;
        }
        if (!bluetoothSocketCruseControl.isConnected()){
            return;
        }
        listenToLocation(false);
        for (CruiseControlListener listener:cruiseControlListeners){
            listener.onDisconnect();
        }
        if (inputStreamCruseControl != null){
            try {
                inputStreamCruseControl.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStreamCruseControl = null;
        }
        if (outputStreamCruseControl != null){
            try {
                outputStreamCruseControl.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStreamCruseControl = null;
        }
        try {
            bluetoothSocketCruseControl.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        bluetoothSocketCruseControl = null;
        showNotificationCruiseControlNotConnected();
        pollForBluetoothDevice();
    }

    synchronized public void sendCommand(String command){
        if (outputStreamCruseControl == null){
            return;
        }
        try {
            outputStreamCruseControl.write(("\n\r" + command + "\n\r").getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void setDAC(){

    }

    public Integer getMaxDAC() {
        return 4090;
    }

    public void setADC_Thresh(int adcIdx, int thresh) {
        sendCommand("adcThresh" + String.valueOf(adcIdx) + "=" + String.valueOf(thresh));
    }

    public void setRelay(int relayIdx, boolean state) {
        if (state) {
            sendCommand("relay=1");
        } else {
            sendCommand("relay=0");
        }
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        CruiseControlService getService() {
            return CruiseControlService.this;
        }
    }

    @Override
    public void onCreate() {
        GlobalVariables.getInstance().setCruiseControlService(this);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        mHandler = new Handler(Looper.getMainLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                pollForBluetoothDevice();
                buttonsProfile = SettingsManager.loadButtonsProfile(getApplicationContext());
            }
        });
    }

    Timer pollForBluetoothDeviceTimer = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        closeBluetoothConnection();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        pollForBluetoothDevice();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            }
        }
    };

    private void pollForBluetoothDevice(){
        if (pollForBluetoothDeviceTimer != null){
            pollForBluetoothDeviceTimer.cancel();
        }
        pollForBluetoothDeviceTimer = new Timer();
        pollForBluetoothDeviceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                setupBlutoothConnection();
            }
        }, 0, pollForBluetoothDevicePeroid);
    }

    @Override
    public void onDestroy() {
        if (pollForBluetoothDeviceTimer != null) {
            pollForBluetoothDeviceTimer.cancel();
        }

        // Cancel the persistent notification.
//        notificationManager.cancel(NOTIFICATION);
        notificationManager.cancelAll();
        unregisterReceiver(mReceiver);

        // Tell the user we stopped.
        Toast.makeText(this, R.string.text_cruise_control_service_stopped, Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new LocalBinder();

    private void showNotification() {
        CharSequence text = getText(R.string.text_cruise_control_service_started);
        Notification notification = new Notification(R.drawable.wikisoeedia, text, System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, CruiseControl.class), 0);
        notification.setLatestEventInfo(this, getText(R.string.text_cruise_control_service_label), text, contentIntent);
        notificationManager.notify(NotificaionId.ACTIVE.ordinal(), notification);
    }

    private void showNotificationCruiseControlConnected() {
        CharSequence text = "Connected";
        Notification notification = new Notification(R.drawable.wikisoeedia, text, System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, CruiseControl.class), 0);
        notification.setLatestEventInfo(this, getText(R.string.text_cruise_control_service_label), text, contentIntent);
        notificationManager.notify(NotificaionId.NO_BLUETOOTH.ordinal(), notification);
    }

    private void showNotificationCruiseControlNotConnected() {
        CharSequence text = "Disconnected";
        Notification notification = new Notification(R.drawable.wikisoeedia, text, System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, CruiseControl.class), 0);
        notification.setLatestEventInfo(this, getText(R.string.text_cruise_control_service_label), text, contentIntent);
        notificationManager.notify(NotificaionId.NO_BLUETOOTH.ordinal(), notification);
    }

}
