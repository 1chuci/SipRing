package com.sipring.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.View;
import android.view.Window;

/** Central place for display mode, accent palette and system bar styling. */
final class ThemeManager {

    static final String MODE_SYSTEM = "system";
    static final String MODE_LIGHT = "light";
    static final String MODE_DARK = "dark";

    static final String ACCENT_AQUA = "aqua";
    static final String ACCENT_OCEAN = "ocean";
    static final String ACCENT_VIOLET = "violet";
    static final String ACCENT_AMBER = "amber";
    static final String ACCENT_ROSE = "rose";

    static final String[] ACCENTS = {
            ACCENT_AQUA, ACCENT_OCEAN, ACCENT_VIOLET, ACCENT_AMBER, ACCENT_ROSE
    };

    private ThemeManager() {
    }

    static void applyTheme(Activity activity) {
        activity.setTheme(appTheme(activity));
    }

    static boolean isLight(Context context) {
        String mode = Prefs.themeMode(context);
        if (MODE_LIGHT.equals(mode)) {
            return true;
        }
        if (MODE_DARK.equals(mode)) {
            return false;
        }
        int night = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return night != Configuration.UI_MODE_NIGHT_YES;
    }

    static String signature(Context context) {
        return Prefs.themeMode(context) + "|" + Prefs.accent(context);
    }

    static int appTheme(Context context) {
        boolean light = isLight(context);
        String accent = Prefs.accent(context);
        if (light) {
            switch (accent) {
                case ACCENT_OCEAN:
                    return R.style.Theme_SipRing_Light_Ocean;
                case ACCENT_VIOLET:
                    return R.style.Theme_SipRing_Light_Violet;
                case ACCENT_AMBER:
                    return R.style.Theme_SipRing_Light_Amber;
                case ACCENT_ROSE:
                    return R.style.Theme_SipRing_Light_Rose;
                default:
                    return R.style.Theme_SipRing_Light;
            }
        }
        switch (accent) {
            case ACCENT_OCEAN:
                return R.style.Theme_SipRing_Ocean;
            case ACCENT_VIOLET:
                return R.style.Theme_SipRing_Violet;
            case ACCENT_AMBER:
                return R.style.Theme_SipRing_Amber;
            case ACCENT_ROSE:
                return R.style.Theme_SipRing_Rose;
            default:
                return R.style.Theme_SipRing;
        }
    }

    static int dialogTheme(Context context) {
        boolean light = isLight(context);
        String accent = Prefs.accent(context);
        if (light) {
            switch (accent) {
                case ACCENT_OCEAN:
                    return R.style.Theme_SipRing_Dialog_Light_Ocean;
                case ACCENT_VIOLET:
                    return R.style.Theme_SipRing_Dialog_Light_Violet;
                case ACCENT_AMBER:
                    return R.style.Theme_SipRing_Dialog_Light_Amber;
                case ACCENT_ROSE:
                    return R.style.Theme_SipRing_Dialog_Light_Rose;
                default:
                    return R.style.Theme_SipRing_Dialog_Light;
            }
        }
        switch (accent) {
            case ACCENT_OCEAN:
                return R.style.Theme_SipRing_Dialog_Ocean;
            case ACCENT_VIOLET:
                return R.style.Theme_SipRing_Dialog_Violet;
            case ACCENT_AMBER:
                return R.style.Theme_SipRing_Dialog_Amber;
            case ACCENT_ROSE:
                return R.style.Theme_SipRing_Dialog_Rose;
            default:
                return R.style.Theme_SipRing_Dialog;
        }
    }

    /** Bright accent color for system notification surfaces. */
    static int notificationAccent(Context context) {
        switch (Prefs.accent(context)) {
            case ACCENT_OCEAN:
                return context.getColor(R.color.accent_ocean);
            case ACCENT_VIOLET:
                return context.getColor(R.color.accent_violet);
            case ACCENT_AMBER:
                return context.getColor(R.color.accent_amber);
            case ACCENT_ROSE:
                return context.getColor(R.color.accent_rose);
            default:
                return context.getColor(R.color.accent_aqua);
        }
    }

    static void configureSystemBars(Activity activity) {
        Window window = activity.getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        if (isLight(activity)) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        window.getDecorView().setSystemUiVisibility(flags);
    }
}
