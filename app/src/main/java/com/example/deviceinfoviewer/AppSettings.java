package com.example.deviceinfoviewer;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences 封装类，管理应用设置
 */
public class AppSettings {

    private static final String PREF_NAME = "device_info_viewer_settings";
    private static final String KEY_REFRESH_INTERVAL_MS = "refresh_interval_ms";
    private static final String KEY_FLOATING_WINDOW_ENABLED = "floating_window_enabled";
    private static final String KEY_FLOATING_WINDOW_OPACITY = "floating_window_opacity";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_FLOATING_WINDOW_X = "floating_window_x";
    private static final String KEY_FLOATING_WINDOW_Y = "floating_window_y";
    private static final String KEY_DUAL_CELL_BATTERY = "dual_cell_battery";

    private static final int DEFAULT_REFRESH_INTERVAL_MS = 2000;
    private static final boolean DEFAULT_FLOATING_WINDOW_ENABLED = false;
    private static final float DEFAULT_FLOATING_WINDOW_OPACITY = 0.85f;
    private static final boolean DEFAULT_DARK_MODE = true;

    private final SharedPreferences prefs;

    private static AppSettings instance;

    private AppSettings(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AppSettings getInstance(Context context) {
        if (instance == null) {
            instance = new AppSettings(context);
        }
        return instance;
    }

    public int getRefreshIntervalMs() {
        return prefs.getInt(KEY_REFRESH_INTERVAL_MS, DEFAULT_REFRESH_INTERVAL_MS);
    }

    public void setRefreshIntervalMs(int intervalMs) {
        prefs.edit().putInt(KEY_REFRESH_INTERVAL_MS, intervalMs).apply();
    }

    public boolean isFloatingWindowEnabled() {
        return prefs.getBoolean(KEY_FLOATING_WINDOW_ENABLED, DEFAULT_FLOATING_WINDOW_ENABLED);
    }

    public void setFloatingWindowEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_FLOATING_WINDOW_ENABLED, enabled).apply();
    }

    public float getFloatingWindowOpacity() {
        return prefs.getFloat(KEY_FLOATING_WINDOW_OPACITY, DEFAULT_FLOATING_WINDOW_OPACITY);
    }

    public void setFloatingWindowOpacity(float opacity) {
        prefs.edit().putFloat(KEY_FLOATING_WINDOW_OPACITY, opacity).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE);
    }

    public void setDarkMode(boolean darkMode) {
        prefs.edit().putBoolean(KEY_DARK_MODE, darkMode).apply();
    }

    public int getFloatingWindowX() {
        return prefs.getInt(KEY_FLOATING_WINDOW_X, -1);
    }

    public void setFloatingWindowX(int x) {
        prefs.edit().putInt(KEY_FLOATING_WINDOW_X, x).apply();
    }

    public int getFloatingWindowY() {
        return prefs.getInt(KEY_FLOATING_WINDOW_Y, -1);
    }

    public void setFloatingWindowY(int y) {
        prefs.edit().putInt(KEY_FLOATING_WINDOW_Y, y).apply();
    }

    public boolean isDualCellBattery() {
        return prefs.getBoolean(KEY_DUAL_CELL_BATTERY, false);
    }

    public void setDualCellBattery(boolean dualCell) {
        prefs.edit().putBoolean(KEY_DUAL_CELL_BATTERY, dualCell).apply();
    }
}
