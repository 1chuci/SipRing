package com.sipring.app;

import android.app.AlarmManager;
import android.os.Build;

import java.lang.reflect.Method;

/**
 * Shared reflection helpers for platform APIs that do not exist on the lower
 * end of our minSdk (26) but must never appear on the launch path there.
 */
final class SystemChecks {

    private SystemChecks() {
    }

    /**
     * Reflectively calls {@link AlarmManager#canScheduleExactAlarms()} on
     * Android 12+ (API 31), where the method first appeared. Returns
     * {@code false} on older platforms or when the reflective call fails, so
     * callers can safely fall back to inexact alarms.
     */
    static boolean canScheduleExactAlarms(AlarmManager alarmManager) {
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
}
