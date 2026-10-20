package com.sipring.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Locale;

/**
 * Owns the countdown and the ongoing foreground notification.
 *
 * <p>The service deliberately is not sticky and stops itself from
 * {@link #onTaskRemoved(Intent)}. Swiping SipRing out of recents therefore stops
 * the timer, exactly as requested.</p>
 */
public class TimerService extends Service {

    private static final String TAG = "SipRing";

    public static final String ACTION_START = "com.sipring.app.action.START";
    public static final String ACTION_RESTORE = "com.sipring.app.action.RESTORE";
    public static final String ACTION_PAUSE = "com.sipring.app.action.PAUSE";
    public static final String ACTION_RESUME = "com.sipring.app.action.RESUME";
    public static final String ACTION_STOP = "com.sipring.app.action.STOP";
    public static final String ACTION_DRANK = "com.sipring.app.action.DRANK";
    public static final String ACTION_SNOOZE = "com.sipring.app.action.SNOOZE";
    public static final String ACTION_ALARM = "com.sipring.app.action.ALARM";

    public static final String EXTRA_INTERVAL = "extra_interval";
    public static final String EXTRA_WINDOW_START = "extra_window_start";
    public static final String EXTRA_WINDOW_END = "extra_window_end";

    private static final String CHANNEL_TIMER = "sipring_timer";
    private static final String CHANNEL_REMINDER = "sipring_reminder";
    private static final int NOTIF_TIMER_ID = 1000;
    private static final int NOTIF_REMINDER_ID = 1001;
    private static final int ALARM_REQUEST_CODE = 41;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LocalBinder binder = new LocalBinder();

    private boolean running;
    private boolean paused;
    private int intervalMinutes;
    private int windowStart;
    private int windowEnd;
    private long targetWall;
    private long remainingMs;
    private long totalMs;

    private PowerManager.WakeLock wakeLock;
    private boolean foregroundStarted;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running || paused) {
                return;
            }
            if (System.currentTimeMillis() >= targetWall) {
                handleDue();
            }
            handler.postDelayed(this, 1_000L);
        }
    };

    public final class LocalBinder extends Binder {
        public TimerSnapshot snapshot() {
            return TimerService.this.snapshot();
        }

        public void pauseOrResume() {
            if (!running) {
                return;
            }
            if (paused) {
                resumeTimer();
            } else {
                pauseTimer();
            }
        }

        public void stopTimer() {
            TimerService.this.stopTimer();
        }

        public void updateInterval(int minutes) {
            setIntervalMinutes(minutes);
        }

        public void updateWindow(int start, int end) {
            setWindow(start, end);
        }

        public void refreshNotification() {
            if (running) {
                ensureForeground();
                updateTimerNotification();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        intervalMinutes = Prefs.interval(this);
        windowStart = Prefs.windowStart(this);
        windowEnd = Prefs.windowEnd(this);
        running = Prefs.isRunning(this);
        paused = Prefs.isPaused(this);
        totalMs = Math.max(60_000L, Prefs.total(this));

        if (running && !paused) {
            targetWall = Prefs.targetWall(this);
            if (targetWall <= 0L) {
                targetWall = System.currentTimeMillis() + Math.max(60_000L, Prefs.remaining(this));
            }
            remainingMs = Math.max(0L, targetWall - System.currentTimeMillis());
            totalMs = Math.max(totalMs, remainingMs);
        } else if (running) {
            remainingMs = Math.max(0L, Prefs.remaining(this));
            totalMs = Math.max(totalMs, remainingMs);
        } else {
            totalMs = intervalMinutes * 60_000L;
            remainingMs = totalMs;
        }

        createChannelsSafely();
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SipRing:reminder");
                wakeLock.setReferenceCounted(false);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to create wake lock", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createChannelsSafely();
        String action = intent != null ? intent.getAction() : null;

        if (action == null) {
            if (running) {
                ensureForeground();
                scheduleAlarm();
                startTicker();
            } else {
                stopSelf();
            }
            return START_NOT_STICKY;
        }

        switch (action) {
            case ACTION_START: {
                int interval = intent.getIntExtra(EXTRA_INTERVAL, Prefs.interval(this));
                int start = intent.getIntExtra(EXTRA_WINDOW_START, Prefs.windowStart(this));
                int end = intent.getIntExtra(EXTRA_WINDOW_END, Prefs.windowEnd(this));
                startSession(interval, start, end);
                break;
            }
            case ACTION_RESTORE:
                if (running) {
                    ensureForeground();
                    scheduleAlarm();
                    startTicker();
                } else {
                    stopSelf();
                }
                break;
            case ACTION_PAUSE:
                pauseTimer();
                break;
            case ACTION_RESUME:
                resumeTimer();
                break;
            case ACTION_STOP:
                stopTimer();
                break;
            case ACTION_DRANK:
                cancelReminderNotification();
                if (running && !paused) {
                    restartFromNow();
                }
                break;
            case ACTION_SNOOZE:
                snoozeTimer();
                break;
            case ACTION_ALARM:
                if (running && !paused) {
                    acquireWakeLock();
                    ensureForeground();
                    handleDue();
                    startTicker();
                    scheduleAlarm();
                } else {
                    cancelAlarm();
                    if (!running) {
                        stopSelf();
                    }
                }
                break;
            default:
                if (running) {
                    ensureForeground();
                } else {
                    stopSelf();
                }
                break;
        }

        // Notification actions can start the service when no timer is active.
        // Make sure such a service does not linger in the background.
        if (!running) {
            stopSelf();
        } else if (!foregroundStarted) {
            ensureForeground();
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // This is the "swipe away = stop" contract.
        stopTimer();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releaseWakeLock();
        super.onDestroy();
    }

    private TimerSnapshot snapshot() {
        long rem = paused ? remainingMs : Math.max(0L, targetWall - System.currentTimeMillis());
        return new TimerSnapshot(running, paused, rem, totalMs, intervalMinutes,
                windowStart, windowEnd);
    }

    private void startSession(int interval, int start, int end) {
        intervalMinutes = Prefs.clamp(interval, 5, 180);
        windowStart = Prefs.clamp(start, 0, 1439);
        windowEnd = Prefs.clamp(end, 0, 1439);
        long intervalMs = intervalMinutes * 60_000L;
        totalMs = intervalMs;
        remainingMs = intervalMs;
        paused = false;
        running = true;
        targetWall = scheduleAfter(System.currentTimeMillis(), intervalMs);
        remainingMs = Math.max(0L, targetWall - System.currentTimeMillis());
        totalMs = Math.max(intervalMs, remainingMs);
        cancelReminderNotification();

        Prefs.setInterval(this, intervalMinutes);
        Prefs.setWindow(this, windowStart, windowEnd);
        saveState();
        ensureForeground();
        scheduleAlarm();
        startTicker();
        updateTimerNotification();
    }

    private void pauseTimer() {
        if (!running || paused) {
            return;
        }
        remainingMs = Math.max(0L, targetWall - System.currentTimeMillis());
        paused = true;
        cancelAlarm();
        stopTicker();
        saveState();
        ensureForeground();
        updateTimerNotification();
    }

    private void resumeTimer() {
        if (!running || !paused) {
            return;
        }
        long delay = Math.max(1_000L, remainingMs);
        targetWall = scheduleAfter(System.currentTimeMillis(), delay);
        remainingMs = Math.max(0L, targetWall - System.currentTimeMillis());
        totalMs = Math.max(intervalMinutes * 60_000L, remainingMs);
        paused = false;
        saveState();
        ensureForeground();
        scheduleAlarm();
        startTicker();
        updateTimerNotification();
    }

    private void stopTimer() {
        running = false;
        paused = false;
        remainingMs = 0L;
        targetWall = 0L;
        totalMs = Math.max(60_000L, intervalMinutes * 60_000L);
        cancelAlarm();
        stopTicker();
        cancelReminderNotification();
        saveState();
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            foregroundStarted = false;
        }
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NOTIF_TIMER_ID);
        }
        stopSelf();
    }

    private void restartFromNow() {
        long intervalMs = intervalMinutes * 60_000L;
        totalMs = intervalMs;
        remainingMs = intervalMs;
        paused = false;
        targetWall = scheduleAfter(System.currentTimeMillis(), intervalMs);
        remainingMs = Math.max(0L, targetWall - System.currentTimeMillis());
        totalMs = Math.max(intervalMs, remainingMs);
        saveState();
        ensureForeground();
        scheduleAlarm();
        startTicker();
        updateTimerNotification();
    }

    private void snoozeTimer() {
        long delay = 10 * 60_000L;
        running = true;
        paused = false;
        totalMs = delay;
        remainingMs = delay;
        targetWall = System.currentTimeMillis() + delay;
        cancelReminderNotification();
        saveState();
        ensureForeground();
        scheduleAlarm();
        startTicker();
        updateTimerNotification();
    }

    private void setIntervalMinutes(int minutes) {
        intervalMinutes = Prefs.clamp(minutes, 5, 180);
        totalMs = intervalMinutes * 60_000L;
        Prefs.setInterval(this, intervalMinutes);
        if (running) {
            paused = false;
            long intervalMs = intervalMinutes * 60_000L;
            totalMs = intervalMs;
            remainingMs = intervalMs;
            targetWall = scheduleAfter(System.currentTimeMillis(), intervalMs);
            remainingMs = Math.max(0L, targetWall - System.currentTimeMillis());
            totalMs = Math.max(intervalMs, remainingMs);
            cancelReminderNotification();
            saveState();
            ensureForeground();
            scheduleAlarm();
            startTicker();
            updateTimerNotification();
        } else {
            remainingMs = totalMs;
            saveState();
        }
    }

    private void setWindow(int start, int end) {
        windowStart = Prefs.clamp(start, 0, 1439);
        windowEnd = Prefs.clamp(end, 0, 1439);
        Prefs.setWindow(this, windowStart, windowEnd);
        if (running && !paused) {
            long delay = Math.max(1_000L, targetWall - System.currentTimeMillis());
            targetWall = scheduleAfter(System.currentTimeMillis(), delay);
            remainingMs = Math.max(0L, targetWall - System.currentTimeMillis());
            totalMs = Math.max(intervalMinutes * 60_000L, remainingMs);
            saveState();
            scheduleAlarm();
        } else {
            saveState();
        }
        updateTimerNotification();
    }

    private void handleDue() {
        if (!running || paused) {
            return;
        }
        long now = System.currentTimeMillis();
        if (targetWall > now + 750L) {
            return;
        }

        long intervalMs = Math.max(60_000L, intervalMinutes * 60_000L);
        long next = scheduleAfter(targetWall, intervalMs);
        int guard = 0;
        while (next <= now && guard++ < 500) {
            next = scheduleAfter(next, intervalMs);
        }
        targetWall = next;
        totalMs = Math.max(intervalMs, Math.max(0L, targetWall - now));
        remainingMs = Math.max(0L, targetWall - now);
        saveState();
        scheduleAlarm();
        ensureForeground();
        updateTimerNotification();
        postReminderNotification();
        startTicker();
    }

    private long scheduleAfter(long from, long delayMs) {
        long delay = Math.max(1_000L, delayMs);
        long candidate = from + delay;
        if (inWindow(candidate)) {
            return candidate;
        }
        long nextStart = nextWindowStart(candidate);
        long shifted = nextStart + delay;
        return inWindow(shifted) ? shifted : nextStart;
    }

    private boolean inWindow(long wallTime) {
        if (windowStart == windowEnd) {
            return true;
        }
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(wallTime);
        int minutes = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        if (windowStart < windowEnd) {
            return minutes >= windowStart && minutes < windowEnd;
        }
        return minutes >= windowStart || minutes < windowEnd;
    }

    private long nextWindowStart(long after) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(after);
        Calendar w = (Calendar) c.clone();
        w.set(Calendar.HOUR_OF_DAY, windowStart / 60);
        w.set(Calendar.MINUTE, windowStart % 60);
        w.set(Calendar.SECOND, 0);
        w.set(Calendar.MILLISECOND, 0);
        if (w.getTimeInMillis() <= after) {
            w.add(Calendar.DAY_OF_YEAR, 1);
        }
        return w.getTimeInMillis();
    }

    private void startTicker() {
        handler.removeCallbacks(tick);
        if (running && !paused) {
            handler.post(tick);
        }
    }

    private void stopTicker() {
        handler.removeCallbacks(tick);
    }

    private void saveState() {
        Prefs.setRunningState(this, running, paused, targetWall, remainingMs, totalMs);
    }

    private void ensureForeground() {
        try {
            Notification notification = buildTimerNotification();
            if (Build.VERSION.SDK_INT >= 34) {
                try {
                    Method method = Service.class.getMethod(
                            "startForeground", int.class, Notification.class, int.class);
                    method.invoke(this, NOTIF_TIMER_ID, notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    Log.w(TAG, "Typed foreground start unavailable", e);
                    startForeground(NOTIF_TIMER_ID, notification);
                }
            } else {
                startForeground(NOTIF_TIMER_ID, notification);
            }
            foregroundStarted = true;
        } catch (RuntimeException e) {
            // A background start can be rejected on some OEM builds. The exact
            // alarm still posts the reminder, so do not take the process down.
            Log.w(TAG, "Unable to enter foreground", e);
        }
    }

    private void updateTimerNotification() {
        if (!foregroundStarted || !running) {
            return;
        }
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(NOTIF_TIMER_ID, buildTimerNotification());
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to update timer notification", e);
        }
    }

    private Notification buildTimerNotification() {
        String window = windowLabel();
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_TIMER)
                .setSmallIcon(R.drawable.ic_stat_drop)
                .setColor(ThemeManager.notificationAccent(this))
                .setContentTitle(getString(R.string.notif_timer_title))
                .setContentText(paused
                        ? getString(R.string.notif_timer_paused)
                        : getString(R.string.notif_timer_running, intervalMinutes, window))
                .setContentIntent(openAppIntent())
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setVisibility(Notification.VISIBILITY_PUBLIC);

        if (!paused) {
            builder.setWhen(targetWall)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setShowWhen(true);
        } else {
            builder.setShowWhen(false);
        }

        PendingIntent pauseOrResume = PendingIntent.getService(
                this,
                paused ? 52 : 51,
                new Intent(this, TimerService.class)
                        .setAction(paused ? ACTION_RESUME : ACTION_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(
                this,
                53,
                new Intent(this, TimerService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        builder.addAction(R.drawable.ic_stat_drop,
                getString(paused ? R.string.notif_timer_resume : R.string.notif_timer_pause),
                pauseOrResume);
        builder.addAction(R.drawable.ic_bell, getString(R.string.notif_timer_stop), stop);

        if (Build.VERSION.SDK_INT >= 31) {
            setForegroundImmediate(builder);
        }
        return builder.build();
    }

    private void postReminderNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }

        PendingIntent drank = PendingIntent.getService(
                this,
                61,
                new Intent(this, TimerService.class).setAction(ACTION_DRANK),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pause = PendingIntent.getService(
                this,
                62,
                new Intent(this, TimerService.class).setAction(ACTION_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent reminderIntent = new Intent(this, ReminderActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent fullScreen = PendingIntent.getActivity(
                this,
                80,
                reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        boolean strong = Prefs.strongReminder(this);
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_REMINDER)
                .setSmallIcon(R.drawable.ic_stat_drop)
                .setColor(ThemeManager.notificationAccent(this))
                .setContentTitle(getString(R.string.notif_reminder_title))
                .setContentText(getString(R.string.notif_reminder_text))
                .setStyle(new Notification.BigTextStyle()
                        .bigText(getString(R.string.notif_reminder_big, nextReminderLabel())))
                .setContentIntent(fullScreen)
                .setAutoCancel(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setWhen(System.currentTimeMillis())
                .setOnlyAlertOnce(false)
                .addAction(R.drawable.ic_stat_drop, getString(R.string.notif_drank), drank)
                .addAction(R.drawable.ic_bell, getString(R.string.notif_timer_pause), pause);

        if (strong) {
            builder.setFullScreenIntent(fullScreen, true)
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setPriority(Notification.PRIORITY_MAX);
        } else {
            builder.setCategory(Notification.CATEGORY_REMINDER)
                    .setPriority(Notification.PRIORITY_HIGH);
        }

        try {
            nm.notify(NOTIF_REMINDER_ID, builder.build());
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to post reminder", e);
        }
        acquireWakeLock();
    }

    private void cancelReminderNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NOTIF_REMINDER_ID);
        }
    }

    private PendingIntent openAppIntent() {
        Intent open = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                this,
                70,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void scheduleAlarm() {
        if (!running || paused) {
            cancelAlarm();
            return;
        }
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        long triggerAt = Math.max(System.currentTimeMillis() + 1_000L, targetWall);
        PendingIntent pi = alarmIntent();
        try {
            if (Build.VERSION.SDK_INT >= 31 && !canScheduleExactAlarms(am)) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Exact alarm denied, falling back to inexact", e);
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    private void cancelAlarm() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        PendingIntent existing = PendingIntent.getForegroundService(
                this,
                ALARM_REQUEST_CODE,
                new Intent(this, TimerService.class).setAction(ACTION_ALARM),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (existing != null) {
            am.cancel(existing);
            existing.cancel();
        }
    }

    private PendingIntent alarmIntent() {
        Intent intent = new Intent(this, TimerService.class).setAction(ACTION_ALARM);
        return PendingIntent.getForegroundService(
                this,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void acquireWakeLock() {
        if (wakeLock == null) {
            return;
        }
        try {
            if (!wakeLock.isHeld()) {
                wakeLock.acquire(20_000L);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Unable to acquire wake lock", e);
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void createChannelsSafely() {
        try {
            createChannels();
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to create notification channels", e);
        }
    }

    private void createChannels() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        NotificationChannel timer = new NotificationChannel(
                CHANNEL_TIMER,
                getString(R.string.notif_channel_timer),
                NotificationManager.IMPORTANCE_LOW);
        timer.setDescription(getString(R.string.notif_channel_timer_desc));
        timer.setShowBadge(false);
        timer.setSound(null, null);
        timer.enableVibration(false);
        timer.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(timer);

        NotificationChannel reminder = new NotificationChannel(
                CHANNEL_REMINDER,
                getString(R.string.notif_channel_reminder),
                NotificationManager.IMPORTANCE_HIGH);
        reminder.setDescription(getString(R.string.notif_channel_reminder_desc));
        reminder.setShowBadge(true);
        reminder.enableVibration(true);
        reminder.setVibrationPattern(new long[]{0L, 260L, 140L, 260L});
        reminder.enableLights(true);
        reminder.setLightColor(ThemeManager.notificationAccent(this));
        reminder.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        AudioAttributes audio = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        reminder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audio);
        nm.createNotificationChannel(reminder);
    }

    private String windowLabel() {
        if (windowStart == windowEnd) {
            return getString(R.string.window_all_day);
        }
        return String.format(Locale.CHINA, "%02d:%02d – %02d:%02d",
                windowStart / 60, windowStart % 60,
                windowEnd / 60, windowEnd % 60);
    }

    private String nextReminderLabel() {
        long delta = Math.max(0L, targetWall - System.currentTimeMillis());
        if (delta < 3_600_000L) {
            int minutes = (int) Math.max(1L, Math.round(delta / 60_000.0));
            return getString(R.string.notif_next_minutes, minutes);
        }
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(targetWall);
        Calendar now = Calendar.getInstance();
        boolean sameDay = target.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && target.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
        String clock = String.format(Locale.CHINA, "%02d:%02d",
                target.get(Calendar.HOUR_OF_DAY), target.get(Calendar.MINUTE));
        return getString(sameDay ? R.string.notif_next_today : R.string.notif_next_tomorrow,
                clock);
    }

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

    private void setForegroundImmediate(Notification.Builder builder) {
        if (Build.VERSION.SDK_INT < 31) {
            return;
        }
        try {
            Method method = Notification.Builder.class.getMethod(
                    "setForegroundServiceBehavior", int.class);
            method.invoke(builder, Notification.FOREGROUND_SERVICE_IMMEDIATE);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.w(TAG, "Unable to set foreground service behavior", e);
        }
    }
}

