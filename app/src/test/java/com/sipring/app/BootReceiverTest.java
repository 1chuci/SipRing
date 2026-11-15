package com.sipring.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class BootReceiverTest {

    @Test
    public void bootWhenNotRunningDoesNotStartService() {
        Context context = RuntimeEnvironment.getApplication();
        Prefs.setRunningState(context, false, false, 0L, 0L, 0L);

        new BootReceiver().onReceive(context, new Intent(Intent.ACTION_BOOT_COMPLETED));

        assertNull(Shadows.shadowOf((android.app.Application) context).getNextStartedService());
    }

    @Test
    public void bootWhenRunningStartsRestoreService() {
        Context context = RuntimeEnvironment.getApplication();
        long now = System.currentTimeMillis();
        Prefs.setRunningState(context, true, false, now + 60_000L, 60_000L, 60_000L);

        new BootReceiver().onReceive(context, new Intent(Intent.ACTION_BOOT_COMPLETED));

        ShadowApplication shadow = Shadows.shadowOf((android.app.Application) context);
        Intent started = shadow.getNextStartedService();
        assertNotNull(started);
        assertEquals(TimerService.ACTION_RESTORE, started.getAction());
        assertNotNull(started.getComponent());
        assertEquals(TimerService.class.getName(), started.getComponent().getClassName());
    }

    @Test
    public void packageReplacedAlsoRestoresRunningTimer() {
        Context context = RuntimeEnvironment.getApplication();
        long now = System.currentTimeMillis();
        Prefs.setRunningState(context, true, false, now + 60_000L, 60_000L, 60_000L);

        new BootReceiver().onReceive(context,
                new Intent(Intent.ACTION_MY_PACKAGE_REPLACED));

        ShadowApplication shadow = Shadows.shadowOf((android.app.Application) context);
        assertNotNull(shadow.getNextStartedService());
    }
}
