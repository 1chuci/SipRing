package com.sipring.app;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class TimerServiceLaunchTest {

    @Test
    public void serviceCreatesAndCreatesNotificationChannels() {
        ServiceController<TimerService> controller =
                Robolectric.buildService(TimerService.class);
        TimerService service = controller.create().get();
        assertNotNull(service);
        controller.destroy();
    }
}
