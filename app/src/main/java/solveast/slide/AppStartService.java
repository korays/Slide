package solveast.slide;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AppStartService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Intent main = new Intent(this, FullscreenActivity.class);
        main.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(main);
        this.stopSelf();
    }
}