package com.sipring.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Pulsing water-drop mark used on the full-screen reminder. */
public class PulseView extends View {

    private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint discPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dropPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path dropPath = new Path();

    private final int accent;
    private final int secondary;
    private final int surface;
    private final int surfaceHigh;
    private final int mint;
    private final float density;

    private float phase;
    private float cx;
    private float cy;
    private float centerRadius;
    private ValueAnimator animator;

    public PulseView(Context context) {
        this(context, null);
    }

    public PulseView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PulseView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        accent = ThemeColors.get(context, R.attr.sipAccent);
        secondary = ThemeColors.get(context, R.attr.sipAccentSecondary);
        surface = ThemeColors.get(context, R.attr.sipSurface);
        surfaceHigh = ThemeColors.get(context, R.attr.sipSurfaceHigh);
        mint = ThemeColors.get(context, R.attr.sipAccentMint);

        pulsePaint.setStyle(Paint.Style.STROKE);
        pulsePaint.setStrokeWidth(dp(1.4f));
        pulsePaint.setColor(accent);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1.2f));
        strokePaint.setColor(accent);
        strokePaint.setAlpha(150);

        dropPaint.setStyle(Paint.Style.FILL);
        dropPaint.setColor(accent);

        highlightPaint.setStyle(Paint.Style.FILL);
        highlightPaint.setColor(mint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cx = w / 2f;
        cy = h / 2f;
        centerRadius = Math.min(w, h) * 0.22f;
        discPaint.setShader(new RadialGradient(cx, cy - centerRadius * 0.3f,
                centerRadius * 1.35f,
                new int[]{surfaceHigh, surface},
                new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        dropPath.reset();
        dropPath.moveTo(cx, cy - centerRadius * 0.74f);
        dropPath.cubicTo(cx + centerRadius * 0.5f, cy - centerRadius * 0.18f,
                cx + centerRadius * 0.56f, cy + centerRadius * 0.22f,
                cx, cy + centerRadius * 0.55f);
        dropPath.cubicTo(cx - centerRadius * 0.56f, cy + centerRadius * 0.22f,
                cx - centerRadius * 0.5f, cy - centerRadius * 0.18f,
                cx, cy - centerRadius * 0.74f);
        dropPath.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float maxRadius = Math.min(getWidth(), getHeight()) * 0.47f;

        // Three outward rings, continuously fading and restarting.
        for (int i = 0; i < 3; i++) {
            float p = (phase + i / 3f) % 1f;
            float radius = centerRadius + (maxRadius - centerRadius) * p;
            pulsePaint.setAlpha((int) ((1f - p) * 92f));
            canvas.drawCircle(cx, cy, radius, pulsePaint);
        }

        canvas.drawCircle(cx, cy, centerRadius, discPaint);
        canvas.drawCircle(cx, cy, centerRadius, strokePaint);

        dropPaint.setColor(accent);
        dropPaint.setAlpha(230);
        canvas.drawPath(dropPath, dropPaint);

        highlightPaint.setColor(mint);
        highlightPaint.setAlpha(150);
        canvas.drawCircle(cx - centerRadius * 0.17f, cy + centerRadius * 0.02f,
                centerRadius * 0.1f, highlightPaint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isShown()) {
            startPulse();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopPulse();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE && isShown()) {
            startPulse();
        } else {
            stopPulse();
        }
    }

    private void startPulse() {
        if (animator == null) {
            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(1_900L);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(a -> {
                phase = (float) a.getAnimatedValue();
                postInvalidateOnAnimation();
            });
        }
        if (!animator.isStarted()) {
            animator.start();
        }
    }

    private void stopPulse() {
        if (animator != null) {
            animator.cancel();
        }
    }

    private float dp(float value) {
        return value * density;
    }
}
