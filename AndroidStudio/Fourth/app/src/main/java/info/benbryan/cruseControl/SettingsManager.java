package info.benbryan.cruseControl;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class SettingsManager {

    public final static String KEY_BluetoothDeviceAddress = "BluetoothDeviceAddress";
    public final static String KEY_NAME_PRIVATE = "name_private";
    public final static String KEY_NAME_PUBLIC = "name_public";
    public final static String KEY_EMAIL = "email";
    private final static String PrefsFileName = "CruseControlPrefs.txt";
    public static final String KEY_WIKISPEEDIA_ENABLED = "wikispeedia_enabled";

    private static Properties defaultProperties(){
        Properties p = new Properties();
        return p;
    }

    public static String get(Context context, String name){
        Properties p = getProperties(context);
        return p.getProperty(name);
    }

    public static Properties getProperties(Context context){
        Properties p = defaultProperties();
        try {
            FileInputStream fis = context.openFileInput(PrefsFileName);
            p.loadFromXML(fis);
            fis.close();
            return p;
        } catch (FileNotFoundException e) {
            try {
                FileOutputStream fos = context.openFileOutput(PrefsFileName, Context.MODE_PRIVATE);
                p.storeToXML(fos, "");
                fos.close();

                FileInputStream fis = context.openFileInput(PrefsFileName);
                p.loadFromXML(fis);
                fis.close();
                return p;

            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void set(Context context, String name, String value){
        Properties p = getProperties(context);
        p.setProperty(name,value);
        try {
            FileOutputStream fos = context.openFileOutput(PrefsFileName, Context.MODE_PRIVATE);
            p.storeToXML(fos, "");
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<Integer, Integer> loadDacToADC_Data(Context context) {
        HashMap<Integer, Integer> profileDacToAdc = new HashMap<>();
        FileInputStream dacToAdcFile;
        try {
            dacToAdcFile = context.openFileInput(context.getString(R.string.file_profile_DAC_to_ADC));
        } catch (FileNotFoundException e) {
            return null;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(dacToAdcFile));
        try {
            while (br.ready()){
                String line = br.readLine();
                String parts[] = line.split("->");
                if (parts.length == 2){
                    try {
                        profileDacToAdc.put(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                    } catch (NumberFormatException ex){ }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                dacToAdcFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return profileDacToAdc;
    }

    public static void saveDacToADC_Data(Context context, HashMap<Integer, Integer> profileDacToAdc) throws IOException {
        FileOutputStream dacToAdcFile = context.openFileOutput(context.getString(R.string.file_profile_DAC_to_ADC), context.MODE_PRIVATE );
        Set<Map.Entry<Integer, Integer>> entries = profileDacToAdc.entrySet();
        for (Map.Entry<Integer, Integer> entry : entries) {
            String line = String.valueOf(entry.getKey()) + "->" + String.valueOf(entry.getValue())+"\n";
            dacToAdcFile.write(line.getBytes());
        }
        dacToAdcFile.close();
    }

    public static HashMap<String, ButtonProfile> loadButtonsProfile(Context context) {
        HashMap<String, ButtonProfile> profileButtons = new HashMap<>();
        FileInputStream fis;
        try {
            fis = context.openFileInput(context.getString(R.string.file_profileButtons));
        } catch (FileNotFoundException e) {
            return null;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        try {
            while (br.ready()){
                String line = br.readLine();
                String nameValueparts[] = line.split("=");
                if (nameValueparts.length == 2){
                    String buttonName = nameValueparts[0];
                    String dacAdcValues[] = nameValueparts[1].split(",");
                    if (dacAdcValues.length == 2){
                        try {
                            int adc = Integer.parseInt(dacAdcValues[0]);
                            int dac = Integer.parseInt(dacAdcValues[1]);
                            profileButtons.put(buttonName, new ButtonProfile(dac, adc));
                        } catch (NumberFormatException ex){ }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return profileButtons;
    }

    public static void saveButtonsProfile(Context context, HashMap<String, ButtonProfile> profileButtons) throws IOException {
        FileOutputStream fos = context.openFileOutput(context.getString(R.string.file_profileButtons), context.MODE_PRIVATE );
        Set<Map.Entry<String, ButtonProfile>> entries = profileButtons.entrySet();
        for (Map.Entry<String, ButtonProfile> entry : entries) {
            String buttonName = entry.getKey();
            ButtonProfile buttonProfile = entry.getValue();
            String line = buttonName + "=" + String.valueOf(buttonProfile.adc) + "," + String.valueOf(buttonProfile.dac) + "\n";
            fos.write(line.getBytes());
        }
        fos.close();
    }


}
