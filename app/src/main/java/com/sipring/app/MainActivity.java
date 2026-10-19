package com.sipring.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.lang.reflect.Method;

/** Single-screen SipRing experience. */
public class MainActivity extends Activity {

    private static final int REQ_NOTIFICATIONS = 101;
    private static final int[] PRESETS = {15, 30, 45, 60, 90};

    private TimerRingView ring;
    private TextView tvRingHint;
    private TextView btnStop;
    private TextView tvIntervalSummary;
    private TextView tvWindowSummary;
    private TextView btnWindowStart;
    private TextView btnWindowEnd;
    private TextView tvVersion;
    private TextView tvFooter;
    private TextView tvNotifyState;
    private TextView tvNotifyAction;
    private TextView tvBatteryState;
    private TextView tvBatteryAction;
    private TextView tvAlarmState;
    private TextView tvAlarmAction;
    private LinearLayout rowNotify;
    private LinearLayout rowBattery;
    private LinearLayout rowAlarm;
    private LinearLayout chipGroup;
    private ScrollView scroll;

    private final List<TextView> presetChips = new ArrayList<>();
    private TextView customChip;
    private TimerService.LocalBinder binder;
    private boolean bound;
    private int interval;
    private int windowStart;
    private int windowEnd;
    private int uiInterval = -1;
    private int uiWindowStart = -1;
    private int uiWindowEnd = -1;
    private long lastTapAt;
    private String versionName = "0.0.2";
    private boolean uiReady;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final Runnable uiTick = new Runnable() {
        @Override
        public void run() {
            if (!uiReady) {
                return;
            }
            if (binder != null) {
                try {
                    render(binder.snapshot());
                } catch (RuntimeException e) {
                    Log.e("SipRing", "Unable to render timer state", e);
                    showStartupError(e);
                    return;
                }
            }
            uiHandler.postDelayed(this, 200L);
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (!uiReady || !(service instanceof TimerService.LocalBinder)) {
                return;
            }
            binder = (TimerService.LocalBinder) service;
            try {
                render(binder.snapshot());
            } catch (RuntimeException e) {
                Log.e("SipRing", "Unable to render service state", e);
                showStartupError(e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder = null;
            if (Prefs.isRunning(MainActivity.this)) {
                render(localSnapshot());
            } else {
                renderIdleLocal();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            initializeUi();
        } catch (Throwable error) {
            Log.e("SipRing", "Startup failed", error);
            showStartupError(error);
        }
    }

    private void initializeUi() {
        configureWindow();
        setContentView(R.layout.activity_main);

        bindViews();
        applyInsets();
        readVersion();
        loadSettings();
        buildChips();
        wireListeners();
        updateSettingsUi();
        updatePermissionRows();

        if (Prefs.isRunning(this)) {
            render(localSnapshot());
            try {
                startForegroundService(new Intent(this, TimerService.class)
                        .setAction(TimerService.ACTION_RESTORE));
            } catch (RuntimeException ignored) {
                // Opening the app will bind to the service anyway.
            }
        } else {
            renderIdleLocal();
        }
        uiReady = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!uiReady) {
            return;
        }
        Intent intent = new Intent(this, TimerService.class);
        try {
            bound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (RuntimeException e) {
            Log.e("SipRing", "Unable to bind timer service", e);
            bound = false;
        }
        uiHandler.removeCallbacks(uiTick);
        uiHandler.post(uiTick);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!uiReady) {
            return;
        }
        updatePermissionRows();
        refreshRunningNotification();
    }

    @Override
    protected void onStop() {
        uiHandler.removeCallbacks(uiTick);
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
            binder = null;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void bindViews() {
        ring = findViewById(R.id.ring);
        tvRingHint = findViewById(R.id.tvRingHint);
        btnStop = findViewById(R.id.btnStop);
        tvIntervalSummary = findViewById(R.id.tvIntervalSummary);
        tvWindowSummary = findViewById(R.id.tvWindowSummary);
        btnWindowStart = findViewById(R.id.btnWindowStart);
        btnWindowEnd = findViewById(R.id.btnWindowEnd);
        tvVersion = findViewById(R.id.tvVersion);
        tvFooter = findViewById(R.id.tvFooter);
        tvNotifyState = findViewById(R.id.tvNotifyState);
        tvNotifyAction = findViewById(R.id.tvNotifyAction);
        tvBatteryState = findViewById(R.id.tvBatteryState);
        tvBatteryAction = findViewById(R.id.tvBatteryAction);
        tvAlarmState = findViewById(R.id.tvAlarmState);
        tvAlarmAction = findViewById(R.id.tvAlarmAction);
        rowNotify = findViewById(R.id.rowNotify);
        rowBattery = findViewById(R.id.rowBattery);
        rowAlarm = findViewById(R.id.rowAlarm);
        chipGroup = findViewById(R.id.chipGroup);
        scroll = findViewById(R.id.scroll);
    }

    private void wireListeners() {
        ring.setOnClickListener(v -> onRingTapped());
        btnStop.setOnClickListener(v -> stopSession());
        btnWindowStart.setOnClickListener(v -> pickTime(true));
        btnWindowEnd.setOnClickListener(v -> pickTime(false));
        rowNotify.setOnClickListener(v -> onNotificationPermissionRow());
        rowBattery.setOnClickListener(v -> openBatteryOptimizationSettings());
        rowAlarm.setOnClickListener(v -> openExactAlarmSettings());
    }

    private void loadSettings() {
        interval = Prefs.interval(this);
        windowStart = Prefs.windowStart(this);
        windowEnd = Prefs.windowEnd(this);
    }

    private void readVersion() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (info.versionName != null) {
                versionName = info.versionName;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // Keep the fallback version.
        }
        tvVersion.setText(getString(R.string.version_format, versionName));
        tvFooter.setText(getString(R.string.footer_version, versionName));
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    private void applyInsets() {
        View root = findViewById(R.id.root);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            int bottom = insets.getSystemWindowInsetBottom();
            scroll.setPadding(0, top + dp(8), 0, bottom + dp(16));
            return insets;
        });
        root.requestApplyInsets();
    }

    private void onRingTapped() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastTapAt < 400L) {
            return;
        }
        lastTapAt = now;

        if (binder != null) {
            TimerSnapshot snapshot = binder.snapshot();
            if (snapshot.running) {
                binder.pauseOrResume();
                render(binder.snapshot());
                return;
            }
            startSession();
            return;
        }

        if (Prefs.isRunning(this)) {
            try {
                startForegroundService(new Intent(this, TimerService.class)
                        .setAction(Prefs.isPaused(this)
                                ? TimerService.ACTION_RESUME
                                : TimerService.ACTION_PAUSE));
            } catch (RuntimeException e) {
                toast(R.string.toast_start_failed);
            }
            return;
        }
        startSession();
    }

    private void startSession() {
        boolean notifyAllowed = notificationsEnabled();
        if (!notifyAllowed) {
            requestNotificationPermissionIfNeeded();
        }
        Intent intent = new Intent(this, TimerService.class)
                .setAction(TimerService.ACTION_START)
                .putExtra(TimerService.EXTRA_INTERVAL, interval)
                .putExtra(TimerService.EXTRA_WINDOW_START, windowStart)
                .putExtra(TimerService.EXTRA_WINDOW_END, windowEnd);
        try {
            startForegroundService(intent);
            long total = interval * 60_000L;
            render(new TimerSnapshot(true, false, total, total,
                    interval, windowStart, windowEnd));
            toast(notifyAllowed ? getString(R.string.toast_started, interval)
                    : getString(R.string.toast_notify_needed));
        } catch (RuntimeException e) {
            toast(R.string.toast_start_failed);
        }
        uiHandler.postDelayed(() -> {
            if (binder != null) {
                render(binder.snapshot());
            }
        }, 150L);
    }

    private void stopSession() {
        if (binder != null) {
            binder.stopTimer();
        } else {
            try {
                startService(new Intent(this, TimerService.class)
                        .setAction(TimerService.ACTION_STOP));
            } catch (RuntimeException ignored) {
                // Nothing to stop.
            }
        }
        toast(R.string.toast_stopped);
        btnStop.setVisibility(View.GONE);
        tvRingHint.setText(R.string.hero_hint_idle);
        renderIdleLocal();
    }

    private void render(TimerSnapshot snapshot) {
        interval = snapshot.intervalMinutes;
        windowStart = snapshot.windowStart;
        windowEnd = snapshot.windowEnd;
        String window = windowLabel(windowStart, windowEnd);
        ring.setTimer(snapshot, window);

        if (snapshot.running) {
            btnStop.setVisibility(View.VISIBLE);
            setTextIfChanged(tvRingHint, getString(snapshot.paused
                    ? R.string.hero_hint_paused : R.string.hero_hint_running));
        } else {
            btnStop.setVisibility(View.GONE);
            setTextIfChanged(tvRingHint, getString(R.string.hero_hint_idle));
        }

        if (uiInterval != interval || uiWindowStart != windowStart || uiWindowEnd != windowEnd) {
            uiInterval = interval;
            uiWindowStart = windowStart;
            uiWindowEnd = windowEnd;
            updateSettingsUi();
        }
    }

    private void renderIdleLocal() {
        interval = Prefs.interval(this);
        windowStart = Prefs.windowStart(this);
        windowEnd = Prefs.windowEnd(this);
        uiInterval = -1;
        render(TimerSnapshot.idle(interval, windowStart, windowEnd));
    }

    private TimerSnapshot localSnapshot() {
        int currentInterval = Prefs.interval(this);
        int start = Prefs.windowStart(this);
        int end = Prefs.windowEnd(this);
        boolean running = Prefs.isRunning(this);
        boolean paused = Prefs.isPaused(this);
        long remaining = paused ? Prefs.remaining(this)
                : Math.max(0L, Prefs.targetWall(this) - System.currentTimeMillis());
        long total = Math.max(60_000L, Prefs.total(this));
        return new TimerSnapshot(running, paused, remaining, total, currentInterval, start, end);
    }

    private void updateSettingsUi() {
        tvIntervalSummary.setText(getString(R.string.interval_every, interval));
        tvWindowSummary.setText(windowLabel(windowStart, windowEnd));
        btnWindowStart.setText(formatTime(windowStart));
        btnWindowEnd.setText(formatTime(windowEnd));
        applyChipSelection();
    }

    private void buildChips() {
        chipGroup.removeAllViews();
        presetChips.clear();
        for (int minutes : PRESETS) {
            TextView chip = createChip(String.valueOf(minutes), minutes);
            presetChips.add(chip);
            chipGroup.addView(chip);
        }
        customChip = createChip(getString(R.string.interval_custom), -1);
        chipGroup.addView(customChip);
    }

    private TextView createChip(String label, int minutes) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        chip.setGravity(Gravity.CENTER);
        chip.setIncludeFontPadding(false);
        chip.setBackgroundResource(R.drawable.bg_chip);
        chip.setTextColor(getColorStateList(R.color.chip_text_selector));
        chip.setPadding(dp(11), 0, dp(11), 0);
        chip.setMinWidth(dp(40));
        chip.setMinHeight(dp(36));
        chip.setTag(minutes);
        chip.setOnClickListener(v -> {
            int value = (int) v.getTag();
            if (value < 0) {
                showCustomIntervalDialog();
            } else {
                setIntervalMinutes(value);
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(36));
        params.setMarginEnd(dp(6));
        chip.setLayoutParams(params);
        return chip;
    }

    private void applyChipSelection() {
        boolean custom = true;
        for (TextView chip : presetChips) {
            int minutes = (int) chip.getTag();
            boolean selected = minutes == interval;
            chip.setSelected(selected);
            if (selected) {
                custom = false;
            }
        }
        if (customChip != null) {
            customChip.setSelected(custom);
        }
    }

    private void setIntervalMinutes(int minutes) {
        minutes = Prefs.clamp(minutes, 5, 180);
        if (minutes == interval) {
            return;
        }
        boolean active = binder != null ? binder.snapshot().running : Prefs.isRunning(this);
        interval = minutes;
        Prefs.setInterval(this, minutes);
        if (binder != null) {
            binder.updateInterval(minutes);
        }
        updateSettingsUi();
        if (!active) {
            renderIdleLocal();
        }
        if (active) {
            toast(R.string.toast_interval_changed);
        }
    }

    private void showCustomIntervalDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_interval, null);
        EditText input = content.findViewById(R.id.inputMinutes);
        input.setText(String.valueOf(interval));
        input.setSelection(input.getText().length());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.SipRingDialog)
                .setView(content)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (positive != null) {
                positive.setTextColor(getColor(R.color.aqua));
                positive.setOnClickListener(v -> {
                    String raw = input.getText().toString().trim();
                    int value;
                    try {
                        value = Integer.parseInt(raw);
                    } catch (NumberFormatException e) {
                        input.setError(getString(R.string.invalid_interval));
                        return;
                    }
                    if (value < 5 || value > 180) {
                        input.setError(getString(R.string.invalid_interval));
                        return;
                    }
                    setIntervalMinutes(value);
                    dialog.dismiss();
                });
            }
            if (negative != null) {
                negative.setTextColor(getColor(R.color.text_secondary));
            }
            input.requestFocus();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            input.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                }
            }, 120L);
        });
        dialog.show();
    }

    private void pickTime(boolean start) {
        int minutes = start ? windowStart : windowEnd;
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                android.R.style.Theme_Material_Dialog_Alert,
                (TimePicker view, int hourOfDay, int minute) -> {
                    if (start) {
                        windowStart = hourOfDay * 60 + minute;
                    } else {
                        windowEnd = hourOfDay * 60 + minute;
                    }
                    persistWindow();
                },
                minutes / 60,
                minutes % 60,
                true);
        dialog.setTitle(start ? R.string.time_picker_start : R.string.time_picker_end);
        dialog.show();
    }

    private void persistWindow() {
        boolean active = binder != null ? binder.snapshot().running : Prefs.isRunning(this);
        Prefs.setWindow(this, windowStart, windowEnd);
        if (binder != null) {
            binder.updateWindow(windowStart, windowEnd);
        }
        updateSettingsUi();
        if (!active) {
            renderIdleLocal();
        } else {
            toast(R.string.toast_window_updated);
        }
    }

    private void onNotificationPermissionRow() {
        if (!notificationRuntimeGranted()) {
            if (Build.VERSION.SDK_INT >= 33) {
                boolean asked = Prefs.hasRequestedNotifications(this);
                boolean rationale = shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS);
                if (asked && !rationale) {
                    showNotificationPermissionDialog();
                } else {
                    requestNotificationPermission();
                }
            } else {
                openNotificationSettings();
            }
            return;
        }
        openNotificationSettings();
    }

    private boolean requestNotificationPermissionIfNeeded() {
        if (notificationsEnabled()) {
            return true;
        }
        if (!notificationRuntimeGranted() && Build.VERSION.SDK_INT >= 33) {
            boolean asked = Prefs.hasRequestedNotifications(this);
            boolean rationale = shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS);
            if (asked && !rationale) {
                showNotificationPermissionDialog();
            } else {
                requestNotificationPermission();
            }
        }
        return false;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) {
            return;
        }
        Prefs.markNotificationsRequested(this);
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
    }

    private void showNotificationPermissionDialog() {
        new AlertDialog.Builder(this, R.style.SipRingDialog)
                .setTitle(R.string.notif_permission_title)
                .setMessage(R.string.notif_permission_message)
                .setPositiveButton(R.string.go_settings,
                        (dialog, which) -> openNotificationSettings())
                .setNegativeButton(R.string.later, null)
                .show();
    }

    private boolean notificationsEnabled() {
        if (!notificationRuntimeGranted()) {
            return false;
        }
        android.app.NotificationManager nm =
                (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        return nm == null || nm.areNotificationsEnabled();
    }

    private boolean notificationRuntimeGranted() {
        return Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void updatePermissionRows() {
        boolean notify = notificationsEnabled();
        tvNotifyState.setText(notify
                ? R.string.permission_notify_on : R.string.permission_notify_off);
        tvNotifyState.setTextColor(getColor(notify ? R.color.aqua : R.color.amber));
        tvNotifyAction.setText(notify
                ? R.string.manage : R.string.permission_notify_action);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean battery = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        tvBatteryState.setText(battery
                ? R.string.permission_battery_on : R.string.permission_battery_off);
        tvBatteryState.setTextColor(getColor(battery ? R.color.aqua : R.color.amber));
        tvBatteryAction.setText(battery
                ? R.string.manage : R.string.permission_battery_action);

        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            boolean exact = canScheduleExactAlarms(am);
            tvAlarmState.setText(exact
                    ? R.string.permission_alarm_on : R.string.permission_alarm_off);
            tvAlarmState.setTextColor(getColor(exact ? R.color.aqua : R.color.amber));
            tvAlarmAction.setText(exact ? R.string.manage : R.string.permission_alarm_action);
            rowAlarm.setClickable(true);
        } else {
            tvAlarmState.setText(R.string.permission_alarm_na);
            tvAlarmState.setTextColor(getColor(R.color.text_secondary));
            tvAlarmAction.setText("");
            rowAlarm.setClickable(false);
        }
    }

    /**
     * Reflection keeps this method off the launch path on Android 8–11, where
     * AlarmManager#canScheduleExactAlarms does not exist yet.
     */
    private boolean canScheduleExactAlarms(AlarmManager alarmManager) {
        if (Build.VERSION.SDK_INT < 31 || alarmManager == null) {
            return false;
        }
        try {
            Method method = AlarmManager.class.getMethod("canScheduleExactAlarms");
            Object result = method.invoke(alarmManager);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private void openNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        try {
            startActivity(intent);
        } catch (RuntimeException e) {
            openAppDetails();
        }
    }

    @SuppressLint("BatteryLife")
    private void openBatteryOptimizationSettings() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (RuntimeException e) {
                openAppDetails();
            }
            return;
        }
        try {
            Intent intent = new Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (RuntimeException e) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (RuntimeException ignored) {
                openAppDetails();
            }
        }
    }

    private void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < 31) {
            return;
        }
        try {
            startActivity(new Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getPackageName())));
        } catch (RuntimeException e) {
            openAppDetails();
        }
    }

    private void openAppDetails() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        try {
            startActivity(intent);
        } catch (RuntimeException ignored) {
            // Nothing else we can do.
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATIONS) {
            updatePermissionRows();
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                refreshRunningNotification();
                Toast.makeText(this, R.string.permission_notify_on, Toast.LENGTH_SHORT).show();
            } else {
                showNotificationPermissionDialog();
            }
        }
    }

    private void refreshRunningNotification() {
        if (!Prefs.isRunning(this) || !notificationsEnabled()) {
            return;
        }
        if (binder != null) {
            binder.refreshNotification();
        } else {
            try {
                startForegroundService(new Intent(this, TimerService.class)
                        .setAction(TimerService.ACTION_RESTORE));
            } catch (RuntimeException ignored) {
                // The normal binding path will pick the service up.
            }
        }
    }

    /**
     * If the platform rejects a theme/resource at startup, showing the actual
     * exception is far more useful than flashing back to the launcher.
     */
    private void showStartupError(Throwable error) {
        uiReady = false;
        uiHandler.removeCallbacks(uiTick);
        String detail = Log.getStackTraceString(error);
        try {
            ScrollView fallback = new ScrollView(this);
            fallback.setBackgroundColor(getColor(R.color.bg_deep));
            fallback.setFillViewport(true);

            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(24), dp(28), dp(24), dp(28));

            TextView title = new TextView(this);
            title.setText(R.string.startup_failed_title);
            title.setTextColor(getColor(R.color.text_primary));
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            box.addView(title);

            TextView hint = new TextView(this);
            hint.setText(R.string.startup_failed_hint);
            hint.setTextColor(getColor(R.color.text_secondary));
            hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            hint.setLineSpacing(dp(3), 1f);
            LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            hintParams.topMargin = dp(10);
            box.addView(hint, hintParams);

            TextView text = new TextView(this);
            text.setText(detail);
            text.setTextIsSelectable(true);
            text.setTextColor(getColor(R.color.text_faint));
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
            text.setTypeface(Typeface.MONOSPACE);
            text.setPadding(0, dp(14), 0, dp(14));
            box.addView(text);

            Button copy = new Button(this);
            copy.setText(R.string.copy_error);
            copy.setOnClickListener(v -> {
                ClipboardManager clipboard =
                        (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("SipRing crash", detail));
                    toast(R.string.copied);
                }
            });
            box.addView(copy);

            Button close = new Button(this);
            close.setText(R.string.close);
            close.setOnClickListener(v -> finish());
            LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            closeParams.topMargin = dp(6);
            box.addView(close, closeParams);

            fallback.addView(box);
            setContentView(fallback);
        } catch (Throwable fallbackError) {
            TextView text = new TextView(this);
            text.setText(detail);
            text.setTextIsSelectable(true);
            text.setTextColor(Color.WHITE);
            text.setBackgroundColor(Color.BLACK);
            text.setPadding(dp(20), dp(20), dp(20), dp(20));
            setContentView(text);
        }
    }

    private String windowLabel(int start, int end) {
        if (start == end) {
            return getString(R.string.window_all_day);
        }
        return String.format(Locale.CHINA, "%02d:%02d – %02d:%02d",
                start / 60, start % 60, end / 60, end % 60);
    }

    private String formatTime(int minutes) {
        return String.format(Locale.CHINA, "%02d:%02d", minutes / 60, minutes % 60);
    }

    private void setTextIfChanged(TextView view, String text) {
        if (!text.contentEquals(view.getText())) {
            view.setText(text);
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
