package info.benbryan.cruseControl;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Set;


public class SelectBluetoothDevice extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acc__select_bluetooth_device);
        setTitle("Choose Cruse Control Device");

        final ListView listViewBluetoothDevices = (ListView)findViewById(R.id.listViewBluetoothDevices);
        final ArrayAdapter<BluetoothDeviceListItem> listViewBluetoothDevicesArrayAdapter = new ArrayAdapter<BluetoothDeviceListItem>(this, android.R.layout.simple_list_item_1 );
        listViewBluetoothDevices.setAdapter(listViewBluetoothDevicesArrayAdapter);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Device does not support Bluetooth").setTitle("Error");
            AlertDialog dialog = builder.create();
            System.exit(-1);
        }
        if (!mBluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);
        }
        if (!mBluetoothAdapter.isEnabled()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Cruse Control does not work without bluetooth enabeled").setTitle("Error");
            AlertDialog dialog = builder.create();
            System.exit(-2);
        }
        final Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        BluetoothDevice bluetoothDeviceCruseControl = null;
        for (BluetoothDevice device : pairedDevices) {
            listViewBluetoothDevicesArrayAdapter.add(new BluetoothDeviceListItem(device));
        }
        listViewBluetoothDevicesArrayAdapter.notifyDataSetChanged();

        listViewBluetoothDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object obj = listViewBluetoothDevices.getItemAtPosition(position);
                if (obj == null){
                    return;
                }
                if (obj instanceof BluetoothDeviceListItem){
                    BluetoothDevice device = ((BluetoothDeviceListItem)obj).device;
                    GlobalVariables.getInstance().getCruiseControlService().setBluetoohDevice(device);
                    finish();
                }
            }
        });
    }

    private class BluetoothDeviceListItem{
        final BluetoothDevice device;
        public BluetoothDeviceListItem(BluetoothDevice device){
            this.device = device;
        }
        @Override
        public String toString(){
            return device.getName() + "\n   " + device.getAddress();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_acc__select_bluetooth_device, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }
}
