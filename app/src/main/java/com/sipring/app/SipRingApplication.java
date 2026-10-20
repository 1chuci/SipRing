package com.sipring.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/** Tracks whether any SipRing screen is currently visible in this process. */
public class SipRingApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static int resumedCount;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    static boolean isForeground() {
        return resumedCount > 0;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        resumedCount++;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        resumedCount = Math.max(0, resumedCount - 1);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}
