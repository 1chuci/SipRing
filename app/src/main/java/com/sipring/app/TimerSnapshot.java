package com.sipring.app;

/** Immutable UI-facing view of the timer service. */
public final class TimerSnapshot {

    public final boolean running;
    public final boolean paused;
    public final long remainingMs;
    public final long totalMs;
    public final int intervalMinutes;
    public final int windowStart;
    public final int windowEnd;

    public TimerSnapshot(boolean running, boolean paused, long remainingMs, long totalMs,
                         int intervalMinutes, int windowStart, int windowEnd) {
        this.running = running;
        this.paused = paused;
        this.remainingMs = Math.max(0L, remainingMs);
        this.totalMs = Math.max(1L, totalMs);
        this.intervalMinutes = intervalMinutes;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
    }

    public static TimerSnapshot idle(int intervalMinutes, int windowStart, int windowEnd) {
        long total = Math.max(1, intervalMinutes) * 60_000L;
        return new TimerSnapshot(false, false, total, total, intervalMinutes, windowStart, windowEnd);
    }

    public boolean isActive() {
        return running;
    }

    public float fraction() {
        return Math.max(0f, Math.min(1f, remainingMs / (float) totalMs));
    }
}
