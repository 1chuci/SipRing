package com.sipring.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Full-screen, lock-screen-friendly reminder for stronger visibility. */
public class ReminderActivity extends Activity {

    public static final String EXTRA_TEST = "extra_test";

    private Vibrator vibrator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable stopVibration = this::stopAlarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            ThemeManager.applyTheme(this);
        } catch (RuntimeException e) {
            setTheme(R.style.Theme_SipRing);
        }
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }

        setContentView(R.layout.activity_reminder);
        ThemeManager.configureSystemBars(this);

        ScrollView scroll = findViewById(R.id.scroll);
        View root = findViewById(R.id.root);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            scroll.setPadding(0, top + dp(8), 0, bottom + dp(16));
            return insets;
        });
        root.requestApplyInsets();

        boolean test = getIntent().getBooleanExtra(EXTRA_TEST, false);
        int interval = Prefs.interval(this);
        TextView time = findViewById(R.id.tvReminderTime);
        if (test) {
            time.setText(getString(R.string.reminder_test_time, interval));
        } else {
            String clock = new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date());
            time.setText(getString(R.string.reminder_time, clock, interval));
        }

        Button drank = findViewById(R.id.btnDrank);
        Button snooze = findViewById(R.id.btnSnooze);
        TextView pause = findViewById(R.id.btnPause);

        drank.setOnClickListener(v -> {
            if (!test) {
                startService(new Intent(this, TimerService.class)
                        .setAction(TimerService.ACTION_DRANK));
            }
            finish();
        });
        snooze.setOnClickListener(v -> {
            if (!test) {
                startService(new Intent(this, TimerService.class)
                        .setAction(TimerService.ACTION_SNOOZE));
                Toast.makeText(this, R.string.reminder_snoozed, Toast.LENGTH_SHORT).show();
            }
            finish();
        });
        pause.setOnClickListener(v -> {
            if (!test) {
                startService(new Intent(this, TimerService.class)
                        .setAction(TimerService.ACTION_PAUSE));
            }
            finish();
        });

        if (Prefs.strongReminder(this) || test) {
            startVibration();
        }
    }

    private void startVibration() {
        try {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) {
                return;
            }
            long[] pattern = {0L, 380L, 220L, 380L, 220L, 720L, 220L, 720L};
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
            handler.postDelayed(stopVibration, 20_000L);
        } catch (RuntimeException ignored) {
            // Vibration is a best-effort enhancement.
        }
    }

    private void stopAlarm() {
        handler.removeCallbacks(stopVibration);
        if (vibrator != null) {
            try {
                vibrator.cancel();
            } catch (RuntimeException ignored) {
                // Ignore.
            }
        }
    }

    @Override
    protected void onStop() {
        stopAlarm();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopAlarm();
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
