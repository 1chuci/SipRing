package com.sipring.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;

import java.util.Locale;

/**
 * The hero control: a circular, tappable countdown ring with a small water tank
 * inside it. Everything is drawn with the framework Canvas, so the APK has no
 * UI-library dependencies.
 */
public class TimerRingView extends View {

    private static final int STATE_IDLE = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_PAUSED = 2;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bloomPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint discPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint waterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dropPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path innerCircle = new Path();
    private final Path waterPath = new Path();
    private final Path dropPath = new Path();
    private final RectF arcRect = new RectF();

    private final int aqua;
    private final int cyan;
    private final int mint;
    private final int textPrimary;
    private final int textSecondary;
    private final int textFaint;
    private final int amber;
    private final float density;

    private int state = STATE_IDLE;
    private float fraction = 1f;
    private long remainingMs;
    private long totalMs;
    private int intervalMinutes = 30;
    private String windowLabel = "";
    private String timeText = "30:00";
    private String lastAccessibilityText = "";
    private float wavePhase;
    private float downX;
    private float downY;
    private boolean moved;

    private float cx;
    private float cy;
    private float arcRadius;
    private float innerRadius;

    private android.animation.ValueAnimator waveAnimator;

    public TimerRingView(Context context) {
        this(context, null);
    }

    public TimerRingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimerRingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        aqua = context.getColor(R.color.aqua);
        cyan = context.getColor(R.color.cyan);
        mint = context.getColor(R.color.mint);
        textPrimary = context.getColor(R.color.text_primary);
        textSecondary = context.getColor(R.color.text_secondary);
        textFaint = context.getColor(R.color.text_faint);
        amber = context.getColor(R.color.amber);

        setClickable(true);
        setFocusable(true);
        setSoundEffectsEnabled(true);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(dp(9f));
        trackPaint.setColor(Color.parseColor("#163B42"));
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(dp(9f));
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        progressGlowPaint.setStyle(Paint.Style.STROKE);
        progressGlowPaint.setStrokeWidth(dp(20f));
        progressGlowPaint.setStrokeCap(Paint.Cap.ROUND);
        progressGlowPaint.setColor(aqua);
        progressGlowPaint.setAlpha(38);

        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(dp(1f));
        tickPaint.setColor(Color.parseColor("#4C8B93"));

        bloomPaint.setStyle(Paint.Style.FILL);

        discPaint.setStyle(Paint.Style.FILL);

        innerStrokePaint.setStyle(Paint.Style.STROKE);
        innerStrokePaint.setStrokeWidth(dp(1f));
        innerStrokePaint.setColor(Color.parseColor("#2C6570"));
        innerStrokePaint.setAlpha(120);

        waterPaint.setStyle(Paint.Style.FILL);

        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setStrokeWidth(dp(1.2f));
        wavePaint.setColor(aqua);
        wavePaint.setAlpha(95);

        dropPaint.setStyle(Paint.Style.FILL);
        dropPaint.setColor(aqua);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        textPaint.setColor(textPrimary);

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        labelPaint.setColor(textSecondary);

        subPaint.setTextAlign(Paint.Align.CENTER);
        subPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        subPaint.setColor(textFaint);
    }

    /** Update the view from a service snapshot. */
    public void setTimer(TimerSnapshot snapshot, String window) {
        this.windowLabel = window == null ? "" : window;
        this.intervalMinutes = snapshot.intervalMinutes;
        this.totalMs = snapshot.totalMs;
        this.remainingMs = snapshot.remainingMs;
        this.fraction = snapshot.fraction();
        if (!snapshot.running) {
            state = STATE_IDLE;
            fraction = 1f;
        } else if (snapshot.paused) {
            state = STATE_PAUSED;
        } else {
            state = STATE_RUNNING;
        }
        this.timeText = formatCountdown(remainingMs);
        updateAccessibility();
        invalidate();
    }

    private void updateAccessibility() {
        String text;
        if (state == STATE_IDLE) {
            text = getContext().getString(R.string.cd_start, intervalMinutes);
        } else if (state == STATE_PAUSED) {
            text = getContext().getString(R.string.cd_paused, spokenTime(remainingMs));
        } else {
            text = getContext().getString(R.string.cd_running, spokenTime(remainingMs));
        }
        if (!text.equals(lastAccessibilityText)) {
            lastAccessibilityText = text;
            setContentDescription(text);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cx = w / 2f;
        cy = h / 2f;
        arcRadius = Math.min(w, h) / 2f - dp(21f);
        innerRadius = arcRadius - dp(22f);

        arcRect.set(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius);
        innerCircle.reset();
        innerCircle.addCircle(cx, cy, innerRadius, Path.Direction.CW);

        SweepGradient sweep = new SweepGradient(cx, cy,
                new int[]{cyan, aqua, mint, aqua, cyan},
                new float[]{0f, 0.32f, 0.58f, 0.82f, 1f});
        Matrix matrix = new Matrix();
        matrix.postRotate(-90f, cx, cy);
        sweep.setLocalMatrix(matrix);
        progressPaint.setShader(sweep);

        bloomPaint.setShader(new RadialGradient(cx, cy, arcRadius + dp(28f),
                new int[]{0x332F7C82, 0x141C4C53, 0x00000000},
                new float[]{0f, 0.58f, 1f}, Shader.TileMode.CLAMP));

        discPaint.setShader(new RadialGradient(cx, cy - innerRadius * 0.32f,
                innerRadius * 1.25f,
                new int[]{0xFF123B44, 0xFF0A242C, 0xFF071B22},
                new float[]{0f, 0.62f, 1f}, Shader.TileMode.CLAMP));

        waterPaint.setShader(new LinearGradient(cx, cy - innerRadius, cx, cy + innerRadius,
                new int[]{0x4D43D8C4, 0x2A1F8C89, 0x0A0B4A4D},
                new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (arcRadius <= 0f) {
            return;
        }

        // Soft halo behind the glass disc.
        canvas.drawCircle(cx, cy, arcRadius + dp(26f), bloomPaint);

        // Fine instrument-style tick marks.
        tickPaint.setAlpha(38);
        for (int i = 0; i < 72; i++) {
            double angle = Math.toRadians(i * 5f - 90f);
            boolean major = i % 18 == 0;
            float inner = arcRadius + dp(major ? 8f : 11f);
            float outer = arcRadius + dp(major ? 15f : 14f);
            tickPaint.setAlpha(major ? 105 : 42);
            tickPaint.setStrokeWidth(dp(major ? 1.4f : 1f));
            canvas.drawLine(
                    cx + (float) Math.cos(angle) * inner,
                    cy + (float) Math.sin(angle) * inner,
                    cx + (float) Math.cos(angle) * outer,
                    cy + (float) Math.sin(angle) * outer,
                    tickPaint);
        }

        // Track + gradient progress arc.
        canvas.drawArc(arcRect, -90f, 360f, false, trackPaint);
        float sweep = Math.max(0f, Math.min(359.8f, fraction * 360f));
        if (sweep > 0.4f) {
            canvas.drawArc(arcRect, -90f, sweep, false, progressGlowPaint);
            canvas.drawArc(arcRect, -90f, sweep, false, progressPaint);
        }

        // Leading light.
        if (fraction > 0.002f && fraction < 0.999f) {
            double angle = Math.toRadians(-90f + fraction * 360f);
            float dotX = cx + (float) Math.cos(angle) * arcRadius;
            float dotY = cy + (float) Math.sin(angle) * arcRadius;
            Paint dotHalo = progressGlowPaint;
            dotHalo.setStyle(Paint.Style.FILL);
            dotHalo.setColor(aqua);
            dotHalo.setAlpha(70);
            canvas.drawCircle(dotX, dotY, dp(9f), dotHalo);
            dotHalo.setAlpha(255);
            canvas.drawCircle(dotX, dotY, dp(3.4f), dotHalo);
            dotHalo.setStyle(Paint.Style.STROKE);
            dotHalo.setStrokeWidth(dp(20f));
            dotHalo.setAlpha(38);
        }

        // Inner glass.
        canvas.drawCircle(cx, cy, innerRadius, discPaint);

        drawWater(canvas);

        canvas.drawCircle(cx, cy, innerRadius, innerStrokePaint);

        drawCenterContent(canvas);
    }

    private void drawWater(Canvas canvas) {
        float waterTop = cy + innerRadius - (2f * innerRadius * fraction);

        canvas.save();
        canvas.clipPath(innerCircle);

        waterPath.reset();
        waterPath.moveTo(cx - innerRadius, waterTop);
        float width = innerRadius * 2f;
        int steps = 48;
        for (int i = 1; i <= steps; i++) {
            float x = cx - innerRadius + width * i / steps;
            float y = waterTop + (float) Math.sin(
                    (i / (float) steps) * Math.PI * 3.1 + wavePhase) * dp(3.2f)
                    + (float) Math.sin(
                    (i / (float) steps) * Math.PI * 1.3 - wavePhase * 0.7f) * dp(1.4f);
            waterPath.lineTo(x, y);
        }
        waterPath.lineTo(cx + innerRadius, cy + innerRadius + dp(8f));
        waterPath.lineTo(cx - innerRadius, cy + innerRadius + dp(8f));
        waterPath.close();
        canvas.drawPath(waterPath, waterPaint);

        // Two thin surface lines make the water feel alive without stealing focus.
        wavePaint.setAlpha(100);
        drawWaveLine(canvas, waterTop, 3.2f, 0f, 1f);
        wavePaint.setAlpha(48);
        drawWaveLine(canvas, waterTop + dp(4.5f), 2.1f, 1.7f, 0.85f);

        canvas.restore();
    }

    private void drawWaveLine(Canvas canvas, float baseY, float amplitude,
                              float phaseOffset, float alphaScale) {
        Path path = new Path();
        float width = innerRadius * 2f;
        int steps = 56;
        for (int i = 0; i <= steps; i++) {
            float x = cx - innerRadius + width * i / steps;
            float y = baseY + (float) Math.sin(
                    (i / (float) steps) * Math.PI * 3.1 + wavePhase + phaseOffset)
                    * dp(amplitude);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        wavePaint.setAlpha((int) (85 * alphaScale));
        canvas.drawPath(path, wavePaint);
    }

    private void drawCenterContent(Canvas canvas) {
        if (state == STATE_IDLE) {
            drawDrop(canvas, cx, cy - dp(43f));
            textPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            textPaint.setTextSize(sp(37f));
            textPaint.setColor(textPrimary);
            textPaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
            canvas.drawText(getContext().getString(R.string.ring_start), cx, cy + dp(17f), textPaint);

            labelPaint.setTextSize(sp(9f));
            labelPaint.setLetterSpacing(0.18f);
            labelPaint.setColor(textFaint);
            canvas.drawText(getContext().getString(R.string.ring_tap_start),
                    cx, cy + dp(47f), labelPaint);
            labelPaint.setLetterSpacing(0f);
            return;
        }

        labelPaint.setTextSize(sp(10f));
        labelPaint.setLetterSpacing(0.08f);
        if (state == STATE_PAUSED) {
            labelPaint.setColor(amber);
            canvas.drawText(getContext().getString(R.string.ring_paused_label),
                    cx, cy - dp(38f), labelPaint);
        } else {
            labelPaint.setColor(textSecondary);
            canvas.drawText(getContext().getString(R.string.ring_next_label),
                    cx, cy - dp(38f), labelPaint);
        }

        float timeSize = timeText.length() >= 7 ? sp(45f) : sp(58f);
        textPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        textPaint.setTextSize(timeSize);
        textPaint.setColor(textPrimary);
        textPaint.setLetterSpacing(0.01f);
        textPaint.setShadowLayer(dp(13f), 0f, 0f, 0x445AE8C8);
        canvas.drawText(timeText, cx, cy + timeSize * 0.35f, textPaint);
        textPaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
        textPaint.setLetterSpacing(0f);

        subPaint.setTextSize(sp(9.5f));
        subPaint.setColor(textFaint);
        String sub = getContext().getString(
                R.string.ring_sub_format, intervalMinutes, windowLabel);
        canvas.drawText(sub, cx, cy + dp(58f), subPaint);
        labelPaint.setLetterSpacing(0f);
    }

    private void drawDrop(Canvas canvas, float centerX, float centerY) {
        dropPath.reset();
        dropPath.moveTo(centerX, centerY - dp(15f));
        dropPath.cubicTo(centerX + dp(9f), centerY - dp(4f),
                centerX + dp(11f), centerY + dp(5f),
                centerX, centerY + dp(12f));
        dropPath.cubicTo(centerX - dp(11f), centerY + dp(5f),
                centerX - dp(9f), centerY - dp(4f),
                centerX, centerY - dp(15f));
        dropPath.close();
        dropPaint.setColor(aqua);
        dropPaint.setAlpha(215);
        canvas.drawPath(dropPath, dropPaint);

        dropPaint.setColor(mint);
        dropPaint.setAlpha(160);
        canvas.drawCircle(centerX - dp(3.4f), centerY + dp(1.5f), dp(2.1f), dropPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (arcRadius > 0f && Math.hypot(event.getX() - cx, event.getY() - cy)
                        > arcRadius + dp(22f)) {
                    return false;
                }
                downX = event.getX();
                downY = event.getY();
                moved = false;
                animate().scaleX(0.974f).scaleY(0.974f).setDuration(80L).start();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.hypot(event.getX() - downX, event.getY() - downY)
                        > ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
                    moved = true;
                    animate().scaleX(1f).scaleY(1f).setDuration(100L).start();
                }
                return true;
            case MotionEvent.ACTION_UP:
                animate().scaleX(1f).scaleY(1f).setDuration(130L).start();
                if (!moved && event.getX() >= 0 && event.getX() <= getWidth()
                        && event.getY() >= 0 && event.getY() <= getHeight()) {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                animate().scaleX(1f).scaleY(1f).setDuration(130L).start();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isShown()) {
            startWave();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopWave();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE && isShown()) {
            startWave();
        } else {
            stopWave();
        }
    }

    private void startWave() {
        if (waveAnimator == null) {
            waveAnimator = android.animation.ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
            waveAnimator.setDuration(5_800L);
            waveAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            waveAnimator.setInterpolator(new LinearInterpolator());
            waveAnimator.addUpdateListener(animation -> {
                wavePhase = (float) animation.getAnimatedValue();
                postInvalidateOnAnimation();
            });
        }
        if (!waveAnimator.isStarted()) {
            waveAnimator.start();
        }
    }

    private void stopWave() {
        if (waveAnimator != null) {
            waveAnimator.cancel();
        }
    }

    private String formatCountdown(long ms) {
        long seconds = Math.max(0L, (ms + 999L) / 1000L);
        long h = seconds / 3600L;
        long m = (seconds % 3600L) / 60L;
        long s = seconds % 60L;
        if (h > 0) {
            return String.format(Locale.CHINA, "%d:%02d:%02d", h, m, s);
        }
        return String.format(Locale.CHINA, "%02d:%02d", m, s);
    }

    private String spokenTime(long ms) {
        long seconds = Math.max(0L, (ms + 999L) / 1000L);
        long h = seconds / 3600L;
        long m = (seconds % 3600L) / 60L;
        if (h > 0) {
            return h + " 小时 " + m + " 分钟";
        }
        return Math.max(1L, m) + " 分钟";
    }

    private float dp(float value) {
        return value * density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
