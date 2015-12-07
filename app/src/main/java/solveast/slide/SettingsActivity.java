package solveast.slide;


import android.annotation.TargetApi;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

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
public class SettingsActivity extends AppCompatPreferenceActivity {

    // Boolean to detect if listener runs for the first time at activity start
    private static boolean initialized = false;


    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    public static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(final Preference preference, Object value) {
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

            } else if (preference.getKey().equals("start_timer") || preference.getKey().equals("stop_timer")) {
                if (stringValue.equals("true")) {
                    final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
                    // Don't show time picker when settings opened for the first time
                    if (initialized) {
                        final Calendar currentCalendar = Calendar.getInstance();
                        // Get current time
                        final int day = currentCalendar.get(Calendar.DAY_OF_YEAR);
                        final int hour = currentCalendar.get(Calendar.HOUR_OF_DAY);
                        final int minute = currentCalendar.get(Calendar.MINUTE);
                        final TimePickerDialog mTimePicker;
                        mTimePicker = new TimePickerDialog(preference.getContext(), new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                                Calendar setCalendar = Calendar.getInstance();
                                // If selected time earlier than current time set time to next day
                                if ((selectedHour < hour) || (selectedHour == hour && selectedMinute < minute)) {
                                    setCalendar.set(Calendar.DAY_OF_YEAR, day + 1);
                                }
                                setCalendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                                setCalendar.set(Calendar.MINUTE, selectedMinute);
                                setCalendar.set(Calendar.SECOND, 0);

                                SharedPreferences.Editor editor = sharedPrefs.edit();
                                // Store time as string to use as summary
                                editor.putString(preference.getKey() + "_summary", selectedHour + ":" + selectedMinute);
                                // Store time in milliseconds to set alarm
                                editor.putLong(preference.getKey() + "_timeInMillis", setCalendar.getTimeInMillis());
                                editor.apply();
                                preference.setSummary(selectedHour + ":" + selectedMinute);
                            }
                        }, hour, minute, true);
                        mTimePicker.setCancelable(false);
                        //hide cancel button
                        mTimePicker.setButton(DialogInterface.BUTTON_NEGATIVE, "", new Message());
                        mTimePicker.setTitle(preference.getTitle());
                        mTimePicker.show();
                    }
                    // Set summary from stored string on activity start
                    preference.setSummary(sharedPrefs.getString(preference.getKey() + "_summary", ""));
                } else {
                    preference.setSummary(null);
                }

                // As stop timer is the last one, we are ready to show time picker
                if (preference.getKey().equals("stop_timer")) {
                    initialized = true;
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }

            return true;
        }
    };


    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        if (preference instanceof SwitchPreference || preference instanceof CheckBoxPreference) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(), false));
        } else {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || SourcePreferenceFragment.class.getName().equals(fragmentName)
                || AnimationPreferenceFragment.class.getName().equals(fragmentName)
                || TimerPreferenceFragment.class.getName().equals(fragmentName)
                || SaveLoadPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows source preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SourcePreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_source);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("internal_path"));
            findPreference("internal_path").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent fileIntent = new Intent(getActivity(), FileChooser.class);
                    fileIntent.setAction("internal");
                    startActivityForResult(fileIntent, 1);
                    return false;
                }
            });
            bindPreferenceSummaryToValue(findPreference("external_path"));
            findPreference("external_path").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent fileIntent = new Intent(getActivity(), FileChooser.class);
                    fileIntent.setAction("external");
                    startActivityForResult(fileIntent, 1);
                    return false;
                }
            });
            bindPreferenceSummaryToValue(findPreference("dropbox"));
            findPreference("dropbox").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent fileIntent = new Intent(getActivity(), FileChooser.class);
                    fileIntent.setAction("dropbox");
                    startActivityForResult(fileIntent, 1);
                    return false;
                }
            });

        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == RESULT_OK) {
                //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
                String sourceType = data.getStringExtra("source_type");
                String prefKey;
                if (sourceType.equals("dropbox")) {
                    prefKey = sourceType;
                    prefs.edit().putString("dropbox_cache_path", data.getStringExtra("dropbox_cache_path")).apply();
                } else {
                    prefKey = sourceType + "_path";
                }
                prefs.edit().putString(prefKey, data.getStringExtra("image_source_path")).apply();
                bindPreferenceSummaryToValue(findPreference(prefKey));
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows animation preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AnimationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_animation);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("animation_type"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class TimerPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_timer);
            setHasOptionsMenu(true);

            initialized = false;
            bindPreferenceSummaryToValue(findPreference("start_timer"));
            bindPreferenceSummaryToValue(findPreference("stop_timer"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows save & load settings preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SaveLoadPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_save_load);
            setHasOptionsMenu(true);


            EditTextPreference savePreference = (EditTextPreference) findPreference("save");

            final SharedPreferences savePrefs = getActivity().getSharedPreferences("save_settings", MODE_PRIVATE);
            int savedCount = savePrefs.getInt("saved_settings_count", 0);
            if (savedCount != 0) {
                PreferenceScreen screen = getPreferenceScreen();
                PreferenceCategory category = new PreferenceCategory(getActivity());
                category.setKey("load_category");
                category.setTitle("Load Settings");
                screen.addPreference(category);
                for (int i = 0; i < savedCount; i++) {
                    String savedProfile = savePrefs.getString("saved_setting_" + i, "");
                    category.addPreference(createLoadPref(getActivity(), savedProfile));
                }
                setPreferenceScreen(screen);
            }


            savePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    PreferenceCategory category;
                    if (findPreference("load_category") == null) {
                        category = new PreferenceCategory(getActivity());
                        category.setKey("load_category");
                        category.setTitle("Load Settings");
                    } else {
                        category = (PreferenceCategory) findPreference("load_category");
                    }

                    // Update saved profile counter
                    int savedCount = savePrefs.getInt("saved_settings_count", 0);
                    savePrefs.edit().putInt("saved_settings_count", savedCount + 1).apply();

                    // Store profile name to savePrefs
                    String profileName = o.toString();
                    savePrefs.edit().putString("saved_setting_" + savedCount, profileName).apply();

                    // Create new Preferences and copy all default preferences to new profile preferences.
                    SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    SharedPreferences profilePrefs = getActivity().getSharedPreferences(profileName, MODE_PRIVATE);
                    copyPrefs(defPrefs, profilePrefs);

                    PreferenceScreen screen = getPreferenceScreen();


                    screen.addPreference(category);
                    category.addPreference(createLoadPref(getActivity(), o.toString()));
                    setPreferenceScreen(screen);
                    Toast.makeText(getActivity(), "Settings saved!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });


        }

        private Preference createLoadPref(final Context context, final String loadName) {
            Preference loadPref = new Preference(context);
            loadPref.setTitle(loadName);
            loadPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences loadPrefs = context.getSharedPreferences(loadName, MODE_PRIVATE);
                    copyPrefs(loadPrefs, defPrefs);
                    Toast.makeText(getActivity(), "Settings loaded!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
            return loadPref;
        }

        private static void copyPrefs(SharedPreferences from, SharedPreferences to) {
            SharedPreferences.Editor ed = to.edit();
            for(Map.Entry<String,?> entry : from.getAll().entrySet()){
                Object v = entry.getValue();
                String key = entry.getKey();
                if(v instanceof Boolean)
                    ed.putBoolean(key, (Boolean) v);
                else if(v instanceof Float)
                    ed.putFloat(key, (Float) v);
                else if(v instanceof Integer)
                    ed.putInt(key, (Integer) v);
                else if(v instanceof Long)
                    ed.putLong(key, (Long) v);
                else if(v instanceof String)
                    ed.putString(key, ((String)v));
            }
            ed.apply();
        }


        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }


}
