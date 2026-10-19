package com.sipring.app;

import static org.junit.Assert.assertNotNull;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.widget.TimePicker;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class DialogThemeTest {

    @Test
    public void lightAndDarkDialogsInflate() {
        Prefs.setThemeMode(RuntimeEnvironment.getApplication(), ThemeManager.MODE_LIGHT);
        Prefs.setAccent(RuntimeEnvironment.getApplication(), ThemeManager.ACCENT_ROSE);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        AlertDialog alert = new AlertDialog.Builder(activity, ThemeManager.dialogTheme(activity))
                .setTitle("test")
                .setMessage("test")
                .create();
        alert.show();
        assertNotNull(alert.getWindow());
        alert.dismiss();

        TimePickerDialog time = new TimePickerDialog(
                activity,
                ThemeManager.dialogTheme(activity),
                (TimePicker view, int hourOfDay, int minute) -> { },
                8, 0, true);
        time.show();
        assertNotNull(time.getWindow());
        time.dismiss();

        Prefs.setThemeMode(RuntimeEnvironment.getApplication(), ThemeManager.MODE_DARK);
        Prefs.setAccent(RuntimeEnvironment.getApplication(), ThemeManager.ACCENT_AMBER);
        activity.recreate();
        assertNotNull(activity);
    }
}
