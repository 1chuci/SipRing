package com.sipring.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

/** Soft aurora-like backdrop. Deliberately cheap: one gradient and three glows. */
public class AuroraView extends View {

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dustPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] dustX = new float[38];
    private final float[] dustY = new float[38];
    private final float[] dustR = new float[38];
    private final float density;
    private final int bgTop;
    private final int bg;
    private final int accent;
    private final int secondary;
    private final int dust;
    private final boolean light;

    private Shader backgroundShader;
    private Shader glowOne;
    private Shader glowTwo;
    private Shader glowThree;

    public AuroraView(Context context) {
        this(context, null);
    }

    public AuroraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AuroraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        bgTop = ThemeColors.get(context, R.attr.sipBgTop);
        bg = ThemeColors.get(context, R.attr.sipBg);
        accent = ThemeColors.get(context, R.attr.sipAccent);
        secondary = ThemeColors.get(context, R.attr.sipAccentSecondary);
        dust = ThemeColors.get(context, R.attr.sipTextFaint);
        light = ThemeManager.isLight(context);
        dustPaint.setColor(dust);
        dustPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        backgroundShader = new LinearGradient(
                0f, 0f, w * 0.25f, h,
                new int[]{bgTop, bg, bg},
                new float[]{0f, 0.52f, 1f}, Shader.TileMode.CLAMP);

        int glowAlpha = light ? 26 : 58;
        int midAlpha = light ? 12 : 20;
        glowOne = new RadialGradient(
                w * 0.12f, h * 0.06f, w * 0.82f,
                new int[]{ThemeColors.withAlpha(accent, glowAlpha),
                        ThemeColors.withAlpha(accent, midAlpha),
                        0x00000000},
                new float[]{0f, 0.48f, 1f}, Shader.TileMode.CLAMP);
        glowTwo = new RadialGradient(
                w * 0.98f, h * 0.31f, w * 0.66f,
                new int[]{ThemeColors.withAlpha(secondary, glowAlpha - 10),
                        ThemeColors.withAlpha(secondary, midAlpha),
                        0x00000000},
                new float[]{0f, 0.52f, 1f}, Shader.TileMode.CLAMP);
        glowThree = new RadialGradient(
                w * 0.4f, h * 0.95f, w * 0.8f,
                new int[]{ThemeColors.withAlpha(accent, glowAlpha - 12),
                        ThemeColors.withAlpha(accent, midAlpha - 4),
                        0x00000000},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);

        Random random = new Random(19L);
        for (int i = 0; i < dustX.length; i++) {
            dustX[i] = random.nextFloat() * w;
            dustY[i] = random.nextFloat() * h * 0.9f;
            dustR[i] = (0.6f + random.nextFloat() * 1.1f) * density;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        backgroundPaint.setShader(backgroundShader);
        canvas.drawRect(0f, 0f, w, h, backgroundPaint);

        glowPaint.setShader(glowOne);
        canvas.drawCircle(w * 0.12f, h * 0.06f, w * 0.82f, glowPaint);

        glowPaint.setShader(glowTwo);
        canvas.drawCircle(w * 0.98f, h * 0.31f, w * 0.66f, glowPaint);

        glowPaint.setShader(glowThree);
        canvas.drawCircle(w * 0.4f, h * 0.95f, w * 0.8f, glowPaint);

        for (int i = 0; i < dustX.length; i++) {
            dustPaint.setAlpha(light ? (8 + (i % 5) * 3) : (14 + (i % 5) * 6));
            canvas.drawCircle(dustX[i], dustY[i], dustR[i], dustPaint);
        }
    }
}
