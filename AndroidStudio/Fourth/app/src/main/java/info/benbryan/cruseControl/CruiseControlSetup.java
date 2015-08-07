package info.benbryan.cruseControl;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

public class CruiseControlSetup extends ActionBarActivity {

    private Handler mainHandler;

    private class ButtonSet{
        public final EditText adc, dac;
        public final Button button;
        public ButtonSet(Button button, EditText adc, EditText dac){
            this.adc = adc;
            this.dac = dac;
            this.button = button;
        }
    }
    ArrayList<ButtonSet> buttonSets = new ArrayList<>();

    public CruiseControlSetup getThis(){
        return this;
    }

    final HashMap<Integer, Integer> profileDacToAdc = new HashMap<>();
    ImageView imageViewDacToAdcPlot;

    private CruiseControlService cruseControlService;
    CheckBox checkBoxProfileLoaded;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acc__setup);
        mainHandler = new Handler(getApplicationContext().getMainLooper());
        cruseControlService = GlobalVariables.getInstance().getCruiseControlService();

        buttonSets.add(new ButtonSet((Button) findViewById(R.id.buttonResume), (EditText) findViewById(R.id.editTextResumeADC), (EditText) findViewById(R.id.editTextResumeDAC)));
        buttonSets.add(new ButtonSet((Button)findViewById(R.id.buttonAccelerate), (EditText)findViewById(R.id.editTextAccelerateADC), (EditText)findViewById(R.id.editTextAccelerateDAC)));
        buttonSets.add(new ButtonSet((Button)findViewById(R.id.buttonSet), (EditText)findViewById(R.id.editTextSetADC), (EditText)findViewById(R.id.editTextSetDAC)));
        buttonSets.add(new ButtonSet((Button)findViewById(R.id.buttonCoast), (EditText)findViewById(R.id.editTextCoastADC), (EditText)findViewById(R.id.editTextCoastDAC)));
        buttonSets.add(new ButtonSet((Button)findViewById(R.id.buttonOff), (EditText)findViewById(R.id.editTextOffADC), (EditText)findViewById(R.id.editTextOffDAC)));

        final RadioButton radioButtonSample = (RadioButton)findViewById(R.id.radioButtonSample);
        final RadioButton radioButtonTest = (RadioButton)findViewById(R.id.radioButtonTest);

        imageViewDacToAdcPlot = (ImageView)findViewById(R.id.imageViewDacToAdcPlot);

        checkBoxProfileLoaded = (CheckBox)findViewById(R.id.checkBoxProfileLoaded);
        Button buttonProfile = (Button)findViewById(R.id.buttonRunTests);

        radioButtonTest.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onRadioButtonTestCheckedChanged(buttonView, isChecked);
            }
        });

        buttonProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ProgressDialog progressDialog = new ProgressDialog(CruiseControlSetup.this);
                progressDialog.setMax(cruseControlService.getMaxDAC());
                progressDialog.setMessage("Generating profile");
                progressDialog.setProgress(0);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.show();
                Executors.newSingleThreadExecutor().submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            cruseControlService.setADC_Thresh(0, 2000);
                            cruseControlService.setRelay(0, true);
                            cruseControlService.addCruseControlListener(cruseControlListenerRunTests);
                            for (int dacValue = 0; dacValue < cruseControlService.getMaxDAC(); dacValue++) {
                                if (!progressDialog.isShowing()) {
                                    return;
                                }
                                progressDialog.setProgress(dacValue);
                                cruseControlService.setDAC(dacValue);
                                Thread.sleep(50);
                                for (int run = 0; ; run++) {
                                    if (run > 10) {
                                        break;
//                                throw new IllegalStateException(); // First throwable I found
                                    }
                                    cruseControlListenerRunTests.getAndClearSample();
                                    cruseControlService.sampleADC(0);
                                    Thread.sleep(50);
                                    CruiseControlListenerRunTests.Sample sample = cruseControlListenerRunTests.getAndClearSample();
                                    if (sample != null) {
                                        int adcValue = sample.value;
                                        profileDacToAdc.put(dacValue, adcValue);
                                        break;
                                    }
                                }
                            }
                            SettingsManager.saveDacToADC_Data(getApplicationContext(), profileDacToAdc);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    loadDacToADC_Data();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            cruseControlService.removeCruseControlListener(cruseControlListenerRunTests);
                            progressDialog.dismiss();
                        }
                    }
                });
            }
        });
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                loadDacToADC_Data();
                initButtonProfiles();
            }
        });
    }

    private void initButtonProfiles(){
        final HashMap<String, ButtonProfile> buttonProfiles = SettingsManager.loadButtonsProfile(getApplicationContext());
        if (buttonProfiles != null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (ButtonSet buttonSet : buttonSets) {
                        ButtonProfile profile = buttonProfiles.get(buttonSet.button.getText());
                        if (profile == null) {
                            continue;
                        }
                        buttonSet.adc.setText(String.valueOf(profile.adc));
                        buttonSet.dac.setText(String.valueOf(profile.dac));
                    }
                }
            });
        }
        for (final ButtonSet buttonSet:buttonSets){
            buttonSet.button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return buttonAction(buttonSet, event);
                }
            });
            buttonSet.adc.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    int adcMeasured = 0;
                    try {
                        adcMeasured = Integer.parseInt(s.toString());
                    } catch (NumberFormatException ex) {
                    }
                    Set<Map.Entry<Integer, Integer>> entrys = profileDacToAdc.entrySet();
                    float sum = 0;
                    float count = 0;
                    for (Map.Entry<Integer, Integer> entry : entrys) {
                        float dac = entry.getKey();
                        float adc = entry.getValue();
                        if (adcMeasured == adc) {
                            sum += dac;
                            count++;
                        }
                    }
                    int mean = Math.round(sum / count);
                    buttonSet.dac.setText(String.valueOf(mean));
                    saveButtonProfiles();
                }
            });
        }
    }

    private void loadDacToADC_Data(){
        final HashMap<Integer, Integer> temp = SettingsManager.loadDacToADC_Data(getApplicationContext());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                profileDacToAdc.clear();
                if ((temp == null) || temp.isEmpty()) {
                    checkBoxProfileLoaded.setChecked(false);
                } else {
                    profileDacToAdc.putAll(temp);
                    checkBoxProfileLoaded.setChecked(true);
                    mainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            drawDacToAdc();
                        }
                    }, 100);
                }
            }
        });
    }

    Timer timerTest = new Timer();
    final CruiseControlListener cruiseControlListenerRadioButtonTest = new CruiseControlListener() {
        @Override
        public void lineRecieved(String line) { }

        @Override
        public void adcReading(final int adcIdx, final int adcCurrent) {
            if (adcIdx != 0){
                return;
            }
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (ButtonSet buttonSet:buttonSets){
                        int adcTarget;
                        try {
                            adcTarget = Integer.parseInt(buttonSet.adc.getText().toString());
                        } catch (NumberFormatException ex){
                            continue;
                        }
                        if (Math.abs(adcCurrent-adcTarget)<5){
                            buttonSet.button.setBackgroundColor(Color.GREEN);
                        } else {
                            buttonSet.button.setBackgroundColor(Color.LTGRAY);
                        }
                    }
                }
            });
        }

        @Override
        public void onConnect() {

        }

        @Override
        public void onDisconnect() {

        }
    };
    private void onRadioButtonTestCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked){
            cruseControlService.setADC_Thresh(0,2);
            cruseControlService.addCruseControlListener(cruiseControlListenerRadioButtonTest);
            cruseControlService.sampleADC(0);
            timerTest = new Timer();
            timerTest.schedule(new TimerTask() {
                @Override
                public void run() {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            cruseControlService.sampleADC(0);
                        }
                    });
                }
            }, 1000, 1000);
        } else {
            timerTest.cancel();
            cruseControlService.removeCruseControlListener(cruiseControlListenerRadioButtonTest);
            for (ButtonSet buttonSet:buttonSets){
                buttonSet.button.setBackgroundColor(Color.LTGRAY);
            }
        }
    }

    private void drawDacToAdc(){
        int w = imageViewDacToAdcPlot.getWidth(), h = imageViewDacToAdcPlot.getHeight();

        Bitmap.Config conf = Bitmap.Config.ARGB_4444;
        Bitmap bmp = Bitmap.createBitmap(w, h, conf);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();

        float minDac = Float.MAX_VALUE, maxDac = Float.MIN_VALUE;
        float minAdc = Float.MAX_VALUE, maxAdc = Float.MIN_VALUE;
        Set<Map.Entry<Integer, Integer>> entrys = profileDacToAdc.entrySet();
        for (Map.Entry<Integer, Integer> entry:entrys){
            float dac = entry.getKey();
            float adc = entry.getValue();
            if (dac < minDac){
                minDac = dac;
            }
            if (dac > maxDac){
                maxDac = dac;
            }
            if (adc < minAdc){
                minAdc = adc;
            }
            if (adc > maxAdc){
                maxAdc = adc;
            }
        }
        for (Map.Entry<Integer, Integer> entry:entrys){
            float dac = entry.getKey();
            float adc = entry.getValue();
            float x = (dac-minDac)/(maxDac-minDac)*w;
            float y = h-(adc-minAdc)/(maxAdc-minAdc)*h;
            canvas.drawPoint(x, y, paint);
        }
        imageViewDacToAdcPlot.setImageBitmap(bmp);
    }

    CruiseControlListenerRunTests cruseControlListenerRunTests = new CruiseControlListenerRunTests();
    public class CruiseControlListenerRunTests implements CruiseControlListener {
        class Sample{
            final public int adcIdx;
            final int value;
            public Sample(int adcIdx, int value){
                this.adcIdx = adcIdx;
                this.value = value;
            }
        }
        Sample sample = null;
        public Sample getAndClearSample(){
            Sample s = sample;
            sample = null;
            return s;
        }

        @Override
        public void lineRecieved(String line) {

        }

        @Override
        public void adcReading(int adcIdx, int value) {
            if (adcIdx == 0){
                sample = new Sample(adcIdx, value);
            }
        }

        @Override
        public void onConnect() {

        }

        @Override
        public void onDisconnect() {

        }
    };

    public boolean buttonAction(final ButtonSet buttonSet, final MotionEvent event){
        final RadioGroup radioGroupProgramTest = (RadioGroup)findViewById(R.id.radioGroupProgramTest);
        switch (radioGroupProgramTest.getCheckedRadioButtonId()){
            case R.id.radioButtonSample:
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        final int adcIdx = 0;
                        cruseControlService.addCruseControlListener(new CruiseControlListener() {
                            @Override
                            public void lineRecieved(String line) {
                            }
                            @Override
                            public void adcReading(final int adcIdx0, final int adcMeasured) {
                                if (adcIdx0 == adcIdx) {
                                    cruseControlService.removeCruseControlListener(this);
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            buttonSet.adc.setText(String.valueOf(adcMeasured));
                                        }
                                    });
                                }
                            }
                            @Override
                            public void onConnect() { }
                            @Override
                            public void onDisconnect() { }
                        });
                        cruseControlService.sampleADC(adcIdx);
                        return true; // if you want to handle the touch event
                }
                break;
            case R.id.radioButtonTest:
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        String text = buttonSet.dac.getText().toString();
                        if (text.equals(R.string.unsetADC_ResultText)){
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setMessage("DAC value is unset").setTitle("Error");
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        } else {
                            int value = Integer.parseInt(text);
                            cruseControlService.setDAC(value);
                        }
                        return true; // if you want to handle the touch event
                    case MotionEvent.ACTION_UP:
                        cruseControlService.setDAC(cruseControlService.getMaxDAC());
                        return true; // if you want to handle the touch event
                }

                break;
        }
        return false; // if you want to handle the touch event
    }

    private void saveButtonProfiles() {
        HashMap<String, ButtonProfile> buttonProfiles = new HashMap<>();
        for (ButtonSet buttonSet:buttonSets){
            try {
                String buttonName = buttonSet.button.getText().toString();
                int adc = Integer.parseInt(buttonSet.adc.getText().toString());
                int dac = Integer.parseInt(buttonSet.dac.getText().toString());
                buttonProfiles.put(buttonName, new ButtonProfile(dac, adc));
            } catch (NumberFormatException ex){
                ex.printStackTrace();
            }
         }
        try {
            SettingsManager.saveButtonsProfile(getApplicationContext(), buttonProfiles);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_acc__setup, menu);
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
