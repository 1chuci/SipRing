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
@Config(sdk = 34)
public class LaunchApi34Test {

    @Test
    public void activityLaunchesOnAndroid14() {
        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class);
        Activity activity = controller.setup().get();
        assertNotNull(activity);
        controller.pause().stop().destroy();
    }

    @Test
    public void serviceCreatesOnAndroid14() {
        ServiceController<TimerService> controller =
                Robolectric.buildService(TimerService.class);
        assertNotNull(controller.create().get());
        controller.destroy();
    }

    @Test
    public void reminderActivityLaunchesOnAndroid14() {
        ActivityController<ReminderActivity> controller =
                Robolectric.buildActivity(ReminderActivity.class);
        assertNotNull(controller.setup().get());
        controller.pause().stop().destroy();
    }
}
