package solveast.slide;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;


public class AppStartBroadcastReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(intent.getAction().equalsIgnoreCase(Intent.ACTION_POWER_CONNECTED)) {
            if (prefs.getBoolean("charge_start", true)) {
                startSer(context);
            }
        } else if(intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            if (prefs.getBoolean("boot_start", true)) {
                startSer(context);
            }
        } else if (intent.getAction().equals(context.getString(R.string.intent_action_alarm_start))) {
            if (prefs.getBoolean("start_timer", false)) {
                // Reset start timer and start service
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean("start_timer", false);
                edit.putString("start_timer_summary", "");
                edit.apply();
                startSer(context);
            }
        }

    }

    private void startSer(Context context) {
        Intent serviceIntent = new Intent(context, AppStartService.class);
        context.startService(serviceIntent);
    }
}