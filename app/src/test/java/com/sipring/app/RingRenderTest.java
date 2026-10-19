package com.sipring.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class RingRenderTest {

    @Test
    public void renderLongCountdowns() throws Exception {
        Activity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        TimerRingView ring = activity.findViewById(R.id.ring);

        int width = Math.round(346 * activity.getResources().getDisplayMetrics().density);
        int height = Math.round(296 * activity.getResources().getDisplayMetrics().density);
        ring.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY));
        ring.layout(0, 0, width, height);

        render(activity, ring, 120 * 60_000L, "ring-120m.png");
        render(activity, ring, 47700_000L, "ring-13h.png");
    }

    private void render(Activity activity, TimerRingView ring, long remaining, String name)
            throws Exception {
        ring.setTimer(new TimerSnapshot(true, false, remaining, 180 * 60_000L,
                120, 8 * 60, 22 * 60), "08:00 – 22:00");
        ring.invalidate();
        Bitmap bitmap = Bitmap.createBitmap(ring.getWidth(), ring.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        ring.draw(canvas);

        File dir = new File(System.getProperty("user.dir"), "build/ring-render");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, name);
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        System.out.println("RING_RENDER=" + file.getAbsolutePath() + " bytes=" + file.length());
    }
}
