package com.sipring.app;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Appearance settings and the local privacy statement. */
public class SettingsActivity extends Activity {

    private LinearLayout modeGroup;
    private LinearLayout accentGroup;
    private TextView tvAccentName;
    private TextView tvVersion;
    private TextView tvFooter;
    private ScrollView scroll;
    private String versionName = "0.0.4";
    private int versionCode = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            ThemeManager.applyTheme(this);
        } catch (RuntimeException e) {
            setTheme(R.style.Theme_SipRing);
        }
        super.onCreate(savedInstanceState);
        ThemeManager.configureSystemBars(this);
        setContentView(R.layout.activity_settings);

        modeGroup = findViewById(R.id.modeGroup);
        accentGroup = findViewById(R.id.accentGroup);
        tvAccentName = findViewById(R.id.tvAccentName);
        tvVersion = findViewById(R.id.tvVersion);
        tvFooter = findViewById(R.id.tvFooter);
        scroll = findViewById(R.id.scroll);
        ImageButton back = findViewById(R.id.btnBack);
        back.setOnClickListener(v -> finish());

        applyInsets();
        readVersion();
        buildModeButtons();
        buildAccentSwatches();
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

    private void readVersion() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (info.versionName != null) {
                versionName = info.versionName;
            }
            versionCode = info.versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
            // Keep the fallback version.
        }
        tvVersion.setText(getString(R.string.about_version, versionName, versionCode));
        tvFooter.setText(getString(R.string.settings_footer, versionName));
    }

    private void buildModeButtons() {
        modeGroup.removeAllViews();
        String selected = Prefs.themeMode(this);
        addModeButton(ThemeManager.MODE_SYSTEM, R.string.mode_system,
                ThemeManager.MODE_SYSTEM.equals(selected));
        addModeButton(ThemeManager.MODE_LIGHT, R.string.mode_light,
                ThemeManager.MODE_LIGHT.equals(selected));
        addModeButton(ThemeManager.MODE_DARK, R.string.mode_dark,
                ThemeManager.MODE_DARK.equals(selected));
    }

    private void addModeButton(String mode, int label, boolean selected) {
        TextView button = new TextView(this);
        button.setText(label);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setGravity(Gravity.CENTER);
        button.setIncludeFontPadding(false);
        button.setBackgroundResource(R.drawable.bg_chip);
        button.setTextColor(getColorStateList(R.color.chip_text_selector));
        button.setSelected(selected);
        button.setOnClickListener(v -> {
            Prefs.setThemeMode(this, mode);
            recreate();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, dp(40), 1f);
        params.setMarginEnd(dp(7));
        modeGroup.addView(button, params);
    }

    private void buildAccentSwatches() {
        accentGroup.removeAllViews();
        String selected = Prefs.accent(this);
        for (String accent : ThemeManager.ACCENTS) {
            addAccentSwatch(accent, selected.equals(accent));
        }
        tvAccentName.setText(accentLabel(selected));
    }

    private void addAccentSwatch(String accent, boolean selected) {
        int accentColor;
        int secondaryColor;
        switch (accent) {
            case ThemeManager.ACCENT_OCEAN:
                accentColor = getColor(R.color.accent_ocean);
                secondaryColor = getColor(R.color.accent_ocean_secondary);
                break;
            case ThemeManager.ACCENT_VIOLET:
                accentColor = getColor(R.color.accent_violet);
                secondaryColor = getColor(R.color.accent_violet_secondary);
                break;
            case ThemeManager.ACCENT_AMBER:
                accentColor = getColor(R.color.accent_amber);
                secondaryColor = getColor(R.color.accent_amber_secondary);
                break;
            case ThemeManager.ACCENT_ROSE:
                accentColor = getColor(R.color.accent_rose);
                secondaryColor = getColor(R.color.accent_rose_secondary);
                break;
            default:
                accentColor = getColor(R.color.accent_aqua);
                secondaryColor = getColor(R.color.accent_aqua_secondary);
                break;
        }

        TextView swatch = new TextView(this);
        swatch.setText(selected ? "✓" : "");
        swatch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        swatch.setTextColor(ThemeColors.get(this, R.attr.sipChipTextSelected));
        swatch.setGravity(Gravity.CENTER);
        swatch.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{secondaryColor, accentColor});
        background.setShape(GradientDrawable.OVAL);
        background.setStroke(
                dp(selected ? 2 : 1),
                selected ? ThemeColors.get(this, R.attr.sipTextPrimary)
                        : ThemeColors.get(this, R.attr.sipStroke));
        swatch.setBackground(background);
        swatch.setContentDescription(accentLabel(accent));
        swatch.setOnClickListener(v -> {
            Prefs.setAccent(this, accent);
            recreate();
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(42), dp(42));
        params.setMarginEnd(dp(12));
        accentGroup.addView(swatch, params);
    }

    private String accentLabel(String accent) {
        switch (accent) {
            case ThemeManager.ACCENT_OCEAN:
                return getString(R.string.accent_ocean);
            case ThemeManager.ACCENT_VIOLET:
                return getString(R.string.accent_violet);
            case ThemeManager.ACCENT_AMBER:
                return getString(R.string.accent_amber);
            case ThemeManager.ACCENT_ROSE:
                return getString(R.string.accent_rose);
            default:
                return getString(R.string.accent_aqua);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
