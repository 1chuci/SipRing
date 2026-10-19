package com.sipring.app;

import static org.junit.Assert.assertNotNull;

import android.app.Activity;
import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class ThemeSettingsTest {

    private Application app() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void lightOceanThemeLaunchesMainAndSettings() {
        Prefs.setThemeMode(app(), ThemeManager.MODE_LIGHT);
        Prefs.setAccent(app(), ThemeManager.ACCENT_OCEAN);

        ActivityController<MainActivity> main =
                Robolectric.buildActivity(MainActivity.class);
        Activity mainActivity = main.setup().get();
        assertNotNull(mainActivity);
        main.pause().stop().destroy();

        ActivityController<SettingsActivity> settings =
                Robolectric.buildActivity(SettingsActivity.class);
        Activity settingsActivity = settings.setup().get();
        assertNotNull(settingsActivity);
        settings.pause().stop().destroy();
    }

    @Test
    public void darkVioletThemeLaunchesMainAndSettings() {
        Prefs.setThemeMode(app(), ThemeManager.MODE_DARK);
        Prefs.setAccent(app(), ThemeManager.ACCENT_VIOLET);

        ActivityController<MainActivity> main =
                Robolectric.buildActivity(MainActivity.class);
        Activity mainActivity = main.setup().get();
        assertNotNull(mainActivity);
        main.pause().stop().destroy();

        ActivityController<SettingsActivity> settings =
                Robolectric.buildActivity(SettingsActivity.class);
        Activity settingsActivity = settings.setup().get();
        assertNotNull(settingsActivity);
        settings.pause().stop().destroy();
    }
}
