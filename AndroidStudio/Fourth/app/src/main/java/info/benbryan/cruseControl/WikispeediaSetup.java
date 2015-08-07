package info.benbryan.cruseControl;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class WikispeediaSetup extends PreferenceActivity {
    /**
     * Determines whether to always show the simplified settings UI, where
     * settings are presented in a single list. When false, settings are shown
     * as a master/detail two-pane view on tablets. When true, a single pane is
     * shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = false;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupSimplePreferencesScreen();
    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }
        PreferenceCategory fakeHeader;
        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.pref_general);

        // Add 'notifications' preferences, and a corresponding header.
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_wikispeedia_user);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_wikispeedia_user);

        initEnabled();
        initUsernamePrivate();
        initUsernamePublic();
        initEmail();

    }

    private void initEnabled(){
        final CheckBoxPreference checkboxPreferenceEnabled =        (CheckBoxPreference) getPreferenceManager().findPreference(getString(R.string.key_wikispeedia_enabled));
        String str = SettingsManager.get(getApplicationContext(), SettingsManager.KEY_WIKISPEEDIA_ENABLED);
        if (str != null){
            try {
                checkboxPreferenceEnabled.setChecked(Boolean.valueOf(str));
            } catch (Exception ex){ }
        }
        checkboxPreferenceEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue instanceof Boolean){
                    Boolean newValueBoolean = (Boolean) newValue;
                    SettingsManager.set(getApplicationContext(), SettingsManager.KEY_WIKISPEEDIA_ENABLED, String.valueOf(newValueBoolean.booleanValue()));
                    CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
                    checkBoxPreference.setChecked(newValueBoolean);
                }
                return false;
            }
        });
    }

    private void initUsernamePrivate(){
        final EditTextPreference editText =(EditTextPreference) getPreferenceManager().findPreference(getString(R.string.key_username_private));
        String str = SettingsManager.get(getApplicationContext(), SettingsManager.KEY_NAME_PRIVATE);
        if (str != null){
            editText.setText(str);
            editText.setSummary(str);
        }
        editText.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue instanceof String) {
                    String str = (String) newValue;
                    SettingsManager.set(getApplicationContext(), SettingsManager.KEY_NAME_PRIVATE, str);
                    EditTextPreference editTextPreference = (EditTextPreference) preference;
                    editTextPreference.setSummary(str);
                }
                return false;
            }
        });
    }
    private void initUsernamePublic(){
        final EditTextPreference editText =(EditTextPreference) getPreferenceManager().findPreference(getString(R.string.key_username_public));
        String str = SettingsManager.get(getApplicationContext(), SettingsManager.KEY_NAME_PUBLIC);
        if (str != null){
            editText.setText(str);
            editText.setSummary(str);
        }
        editText.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue instanceof String) {
                    String str = (String) newValue;
                    SettingsManager.set(getApplicationContext(), SettingsManager.KEY_NAME_PUBLIC, str);
                    EditTextPreference editTextPreference = (EditTextPreference) preference;
                    editTextPreference.setSummary(str);
                }
                return false;
            }
        });
    }

    private void initEmail(){
        final EditTextPreference editText =(EditTextPreference) getPreferenceManager().findPreference(getString(R.string.key_email));
        String str = SettingsManager.get(getApplicationContext(), SettingsManager.KEY_EMAIL);
        if (str != null){
            editText.setText(str);
            editText.setSummary(str);
        }
        editText.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue instanceof String) {
                    String str = (String) newValue;
                    SettingsManager.set(getApplicationContext(), SettingsManager.KEY_EMAIL, str);
                    EditTextPreference editTextPreference = (EditTextPreference) preference;
                    editTextPreference.setSummary(str);
                }
                return false;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        if (!isSimplePreferences(this)) {
            loadHeadersFromResource(R.xml.pref_headers, target);
        }
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
//            bindPreferenceSummaryToValue(findPreference("example_text"));
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class PreferenceFragmentUser extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_wikispeedia_user);
//            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }
    }

}
