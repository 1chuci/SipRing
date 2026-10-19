package com.sipring.app;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;

/** Resolves the custom palette attributes of the current activity theme. */
final class ThemeColors {

    private ThemeColors() {
    }

    static int get(Context context, int attr) {
        TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, value, true)) {
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return value.data;
            }
            if (value.resourceId != 0) {
                return context.getColor(value.resourceId);
            }
            return value.data;
        }
        return Color.WHITE;
    }

    static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
