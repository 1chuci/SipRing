package com.sipring.app;

import android.content.Context;
import android.content.SharedPreferences;

/** Small, single-source-of-truth wrapper around the app's local settings. */
final class Prefs {

    private static final String FILE = "sipring";
    private static final String KEY_INTERVAL = "interval_minutes";
    private static final String KEY_WINDOW_START = "window_start";
    private static final String KEY_WINDOW_END = "window_end";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_PAUSED = "paused";
    private static final String KEY_TARGET = "target_wall";
    private static final String KEY_REMAINING = "remaining_ms";
    private static final String KEY_TOTAL = "total_ms";
    private static final String KEY_NOTIF_REQUESTED = "notif_requested";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_ACCENT = "accent";
    private static final String KEY_STRONG_REMINDER = "strong_reminder";

    static final int DEFAULT_INTERVAL = 30;
    static final int DEFAULT_WINDOW_START = 8 * 60;
    static final int DEFAULT_WINDOW_END = 22 * 60;

    private Prefs() {
    }

    static SharedPreferences get(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static int interval(Context context) {
        return clamp(get(context).getInt(KEY_INTERVAL, DEFAULT_INTERVAL), 1, 180);
    }

    static int windowStart(Context context) {
        return clamp(get(context).getInt(KEY_WINDOW_START, DEFAULT_WINDOW_START), 0, 1439);
    }

    static int windowEnd(Context context) {
        return clamp(get(context).getInt(KEY_WINDOW_END, DEFAULT_WINDOW_END), 0, 1439);
    }

    static boolean isRunning(Context context) {
        return get(context).getBoolean(KEY_RUNNING, false);
    }

    static boolean isPaused(Context context) {
        return get(context).getBoolean(KEY_PAUSED, false);
    }

    static long targetWall(Context context) {
        return get(context).getLong(KEY_TARGET, 0L);
    }

    static long remaining(Context context) {
        return get(context).getLong(KEY_REMAINING, interval(context) * 60_000L);
    }

    static long total(Context context) {
        return get(context).getLong(KEY_TOTAL, interval(context) * 60_000L);
    }

    static void setInterval(Context context, int minutes) {
        get(context).edit().putInt(KEY_INTERVAL, clamp(minutes, 1, 180)).apply();
    }

    static void setWindow(Context context, int start, int end) {
        get(context).edit()
                .putInt(KEY_WINDOW_START, clamp(start, 0, 1439))
                .putInt(KEY_WINDOW_END, clamp(end, 0, 1439))
                .apply();
    }

    static void setRunningState(Context context, boolean running, boolean paused,
                                long target, long remaining, long total) {
        get(context).edit()
                .putBoolean(KEY_RUNNING, running)
                .putBoolean(KEY_PAUSED, paused)
                .putLong(KEY_TARGET, target)
                .putLong(KEY_REMAINING, remaining)
                .putLong(KEY_TOTAL, total)
                .apply();
    }

    static boolean hasRequestedNotifications(Context context) {
        return get(context).getBoolean(KEY_NOTIF_REQUESTED, false);
    }

    static void markNotificationsRequested(Context context) {
        get(context).edit().putBoolean(KEY_NOTIF_REQUESTED, true).apply();
    }

    static String themeMode(Context context) {
        String mode = get(context).getString(KEY_THEME_MODE, ThemeManager.MODE_SYSTEM);
        if (ThemeManager.MODE_LIGHT.equals(mode) || ThemeManager.MODE_DARK.equals(mode)) {
            return mode;
        }
        return ThemeManager.MODE_SYSTEM;
    }

    static void setThemeMode(Context context, String mode) {
        get(context).edit().putString(KEY_THEME_MODE, mode).apply();
    }

    static String accent(Context context) {
        String accent = get(context).getString(KEY_ACCENT, ThemeManager.ACCENT_AQUA);
        for (String candidate : ThemeManager.ACCENTS) {
            if (candidate.equals(accent)) {
                return accent;
            }
        }
        return ThemeManager.ACCENT_AQUA;
    }

    static void setAccent(Context context, String accent) {
        get(context).edit().putString(KEY_ACCENT, accent).apply();
    }

    static boolean strongReminder(Context context) {
        return get(context).getBoolean(KEY_STRONG_REMINDER, true);
    }

    static void setStrongReminder(Context context, boolean strong) {
        get(context).edit().putBoolean(KEY_STRONG_REMINDER, strong).apply();
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

