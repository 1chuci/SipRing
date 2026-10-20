package com.sipring.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ReminderRenderTest {

    @Test
    public void renderReminderScreen() throws Exception {
        Intent intent = new Intent(
                org.robolectric.RuntimeEnvironment.getApplication(), ReminderActivity.class)
                .putExtra(ReminderActivity.EXTRA_TEST, true);
        ActivityController<ReminderActivity> controller =
                Robolectric.buildActivity(ReminderActivity.class, intent);
        Activity activity = controller.setup().get();
        View root = activity.findViewById(R.id.root);

        float density = activity.getResources().getDisplayMetrics().density;
        int width = Math.round(411 * density);
        int height = Math.round(800 * density);
        root.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, width, height);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        root.draw(canvas);

        File dir = new File(System.getProperty("user.dir"), "build/reminder-render");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, "reminder.png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        System.out.println("REMINDER_RENDER=" + file.getAbsolutePath());
        controller.pause().stop().destroy();
    }
}
