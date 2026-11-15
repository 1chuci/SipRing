package com.sipring.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Resumes an in-progress timer after a reboot or an app update.
 *
 * <p>AlarmManager clears every scheduled alarm when the device reboots, and a
 * package update can kill the process. Without this receiver a running timer
 * would silently die while {@link Prefs#isRunning(Context)} still reports
 * {@code true}. We only restore when the user had explicitly started a timer,
 * so "swipe away = stop" stays intact.</p>
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        boolean resume = Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action);
        if (!resume || !Prefs.isRunning(context)) {
            return;
        }
        try {
            Intent service = new Intent(context, TimerService.class)
                    .setAction(TimerService.ACTION_RESTORE);
            context.startForegroundService(service);
        } catch (RuntimeException e) {
            // Some OEMs restrict background starts even for BOOT_COMPLETED.
            // The timer will resume the next time the user opens the app.
            android.util.Log.w("SipRing", "Unable to restore timer after boot", e);
        }
    }
}
