package info.benbryan.cruseControl;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

public class CruiseControlTest extends ActionBarActivity {

    private ProgressBar progressBarVoltage0;
    private ListView listViewCommandHistory;
    private ArrayAdapter<Spanned> listViewCommandHistoryArrayAdapter;
    private EditText editTextADC_Thresh;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acc__test);

        progressBarVoltage0 = (ProgressBar)findViewById(R.id.progressBarVoltage0);
        listViewCommandHistory = (ListView)findViewById(R.id.listViewCommandHistory);
        listViewCommandHistory.clearChoices();
        listViewCommandHistoryArrayAdapter = new ArrayAdapter<Spanned>(this, android.R.layout.simple_list_item_1);
        listViewCommandHistory.setAdapter(listViewCommandHistoryArrayAdapter);

        editTextADC_Thresh = (EditText)findViewById(R.id.editTextADC_Thresh);
        editTextADC_Thresh.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int value = Integer.parseInt(s.toString());
                    sendCruseControlCommand("adcThresh0=" + String.valueOf(value));
                } catch (NumberFormatException ex) { }
            }
        });
        sendCruseControlCommand("adcThresh0=" + editTextADC_Thresh.getText());
        Button sampleADC0 = (Button)findViewById(R.id.buttonSampleADC0);
        sampleADC0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCruseControlCommand("adc=0");
            }
        });
        CheckBox checkBoxRelayOn = (CheckBox)findViewById(R.id.checkBoxRelayOn);
        checkBoxRelayOn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    sendCruseControlCommand("relay=1");
                } else {
                    sendCruseControlCommand("relay=0");
                }
            }
        });
        SeekBar seekBarVoltage = (SeekBar)findViewById(R.id.seekBarVoltage);
        seekBarVoltage.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sendCruseControlCommand("dac=" + String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    boolean mIsBound = false;
    private CruiseControlService cruseControlService;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            cruseControlService = ((CruiseControlService.LocalBinder)service).getService();

            // Tell the user about this for our demo.
            Toast.makeText(getApplicationContext(), R.string.local_service_connected, Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            cruseControlService = null;
            Toast.makeText(getApplicationContext(), R.string.local_service_disconnected, Toast.LENGTH_SHORT).show();
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(getApplicationContext(), CruiseControlService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    private void sendCruseControlCommand(String command){
        cruseControlService.sendCommand(command);
        String text = "<font color='blue'>"+command+"\n</font>";
        listViewCommandHistoryArrayAdapter.add(Html.fromHtml(text));
        listViewCommandHistoryArrayAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_acc__test, menu);
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
