package solveast.slide;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FullscreenActivity extends AppCompatActivity {

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private ImageView slideView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar
            slideView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
    };
    private boolean mVisible;

    private Toast unlockToast;
    private int clickCounter = 0;
    private final Runnable mUnlockRunnable = new Runnable() {
        @Override
        public void run() {
            clickCounter = 0;
        }
    };

    private List<String> images = new ArrayList<>();
    private int slideDuration;
    private int currentImageIndex = 0;
    private int animationIdIn;
    private int animationIdOut;

    final Runnable slideRunnable = new Runnable()
    {
        public void run()
        {
            animateandSlideShow();
            mHideHandler.postDelayed(this, slideDuration * 1000);
            Log.w("siktir", "slide runnable run");
        }
    };

    private final Runnable mCloseRunnable = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        slideView = (ImageView) findViewById(R.id.fullscreen_slide);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        unlockToast = Toast.makeText(this, "", Toast.LENGTH_LONG);

        // Set up the user interaction to manually show or hide the system UI.
        slideView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mVisible) {
                    unlockScreen();
                }
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        mHideHandler.removeCallbacks(slideRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        images.clear();
        getPreferences();

        if (images.isEmpty()) {
            findViewById(R.id.slideEmptyText).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.slideEmptyText).setVisibility(View.GONE);
            mHideHandler.postDelayed(slideRunnable, 0);
        }
    }


    private void getPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(FullscreenActivity.this);

        String intPath = prefs.getString("internal_path", "");
        if (!intPath.isEmpty()) {
            addImagePathsToList(intPath);
        }
        String extPath = prefs.getString("external_path", "");
        if (!extPath.isEmpty()) {
            addImagePathsToList(extPath);
        }
        String dropboxPath = prefs.getString("dropbox_cache_path", "");
        if (!dropboxPath.isEmpty()) {
            addImagePathsToList(dropboxPath);
        }
        String animationType = prefs.getString("animation_type", "fade");
        Log.w("siktir", "animation typee " + animationType);
        if (!animationType.isEmpty()) {
            switch (animationType) {
                case "fade":
                    animationIdIn = R.anim.fade_in;
                    animationIdOut = R.anim.fade_out;
                    break;
                case "rotate":
                    animationIdIn = R.anim.rotate_in;
                    animationIdOut = R.anim.rotate_out;
                    break;
                case "left":

                    break;
                case "right":

                    break;
            }
        }

        slideDuration = prefs.getInt("slide_period", 3);

        boolean startTimer = prefs.getBoolean("start_timer", false);
        Log.w("siktir", "start timer boolean " + startTimer);
        if (startTimer) {
            setStartAlarm(prefs.getLong("start_timer_timeInMillis", 0));
        }

        boolean stopTimer = prefs.getBoolean("stop_timer", false);
        if (stopTimer) {
            long stopTimeInMillis = prefs.getLong("stop_timer_timeInMillis", 0);
            // Reset stop timer if it is already old
            if (stopTimeInMillis < System.currentTimeMillis()) {
                prefs.edit().putBoolean("stop_timer", false).apply();
            }
            mHideHandler.postDelayed(mCloseRunnable, stopTimeInMillis - System.currentTimeMillis());
        }

    }

    private void addImagePathsToList(String folderPath) {
        File f = new File(folderPath);
        File file[] = f.listFiles();
        if (file!=null) {
            for (File aFile : file) {
                images.add(aFile.getAbsolutePath());
            }
        }
    }

    /**
     * User must touch the screen 3 times in 2 seconds to unlock
     */
    private void unlockScreen() {
        if (clickCounter == 2) {
            show();
            unlockToast.cancel();
            mHideHandler.removeCallbacks(mUnlockRunnable);
            clickCounter = 0;
        } else if (clickCounter < 2) {
            clickCounter++;
            unlockToast.setText("Click " + (3 - clickCounter) + " more time to exit fullscreen");
            unlockToast.show();
            mHideHandler.removeCallbacks(mUnlockRunnable);
            mHideHandler.postDelayed(mUnlockRunnable, 2000);
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        slideView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Helper method to start the animation on the splash screen
     */
    private void animateandSlideShow() {

        Animation animationOut = AnimationUtils.loadAnimation(this, animationIdOut);
        slideView.startAnimation(animationOut);


        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Delayed animation for 'slide in' after 'slide out'
                if (!images.isEmpty()) {
                    Picasso.with(FullscreenActivity.this).load(new File(images.get(currentImageIndex % images.size()))).into(slideView);
                    Animation animationIn = AnimationUtils.loadAnimation(FullscreenActivity.this, animationIdIn);
                    slideView.startAnimation(animationIn);
                }
            }
        }, animationOut.getDuration());

        currentImageIndex++;

    }

    private void setStartAlarm(long timeInMillis) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(getBaseContext(), AppStartBroadcastReceiver.class);
        intent.setAction(getString(R.string.intent_action_alarm_start));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                FullscreenActivity.this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Log.w("siktir", "alarm set to " + timeInMillis);

        // Set alarm manager to given timeInMillis
        alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);

        // Finish the currently running activity
        //MainActivity.this.finish();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(FullscreenActivity.this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.fullscreen) {
            hide();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
