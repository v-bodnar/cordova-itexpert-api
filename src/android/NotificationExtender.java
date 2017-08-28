package pl.itexpert.cordova;

import android.util.Log;

import com.onesignal.NotificationExtenderService;
import com.onesignal.OSNotificationReceivedResult;

/**
 * Created by volodymyr.bodnar on 8/25/2017.
 */

public class NotificationExtender extends NotificationExtenderService {
    private static String TAG = "NotificationExtender";

    @Override
    protected boolean onNotificationProcessing(OSNotificationReceivedResult notification) {
        Log.d(TAG, "onNotificationProcessing");
        return false; //return true if you process notification here,  it will prevent default processing
    }
}
