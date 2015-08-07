package info.benbryan.cruseControl;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class HeadsetReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {

        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            String cruseControlAddress = SettingsManager.get(context, SettingsManager.KEY_BluetoothDeviceAddress);
            if (cruseControlAddress != null){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getAddress().equals(cruseControlAddress)){
                    CruiseControlService service = GlobalVariables.getInstance().getCruiseControlService();
                    if (service != null){
                        service.closeBluetoothConnection();
                    }
                }
            }
        }
    }
}