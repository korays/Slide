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
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TimePicker;

import java.util.Calendar;
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
public class SettingsActivity extends AppCompatPreferenceActivity {

    public static final String KEY_INT_MEM_PATH = "internal_path";
    public static final String KEY_EXT_MEM_PATH = "external_path";

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
                        Log.w("siktir", "show timepicker  " + preference.getKey());
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

            Log.w("siktir", "pref: " + preference.getKey() + " value: " + value + " initialized: " + initialized);


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
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SourcePreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_source);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference(KEY_INT_MEM_PATH));
            findPreference(KEY_INT_MEM_PATH).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent fileIntent = new Intent(getActivity(), FileChooser.class);
                    fileIntent.setAction("internal");
                    startActivityForResult(fileIntent, 1);
                    return false;
                }
            });
            bindPreferenceSummaryToValue(findPreference(KEY_EXT_MEM_PATH));
            findPreference(KEY_EXT_MEM_PATH).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
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
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
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
     * This fragment shows notification preferences only. It is used when the
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
            //bindPreferenceSummaryToValue(findPreference("boot_start"));
            //bindPreferenceSummaryToValue(findPreference("charge_start"));
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
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SaveLoadPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_save_load);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("save"));
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
