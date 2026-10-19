package com.sipring.app;

import static org.junit.Assert.assertNotNull;

import android.app.Activity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 31)
public class LaunchApi31Test {

    @Test
    public void activityLaunchesOnAndroid12() {
        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class);
        Activity activity = controller.setup().get();
        assertNotNull(activity);
        controller.pause().stop().destroy();
    }

    @Test
    public void serviceCreatesOnAndroid12() {
        ServiceController<TimerService> controller =
                Robolectric.buildService(TimerService.class);
        assertNotNull(controller.create().get());
        controller.destroy();
    }
}
