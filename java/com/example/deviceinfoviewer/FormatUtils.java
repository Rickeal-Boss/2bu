package com.example.deviceinfoviewer;

import java.util.Locale;

/**
 * 格式化工具类，提供频率、字节、温度、信号强度、百分比等格式化方法
 */
public final class FormatUtils {

    private FormatUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 格式化频率，内部为KHz，自动转换为合适的单位显示
     * @param khz 频率值，单位KHz
     * @return 格式化后的字符串，如 "2.40 GHz"、"1800 MHz"
     */
    public static String formatFreq(long khz) {
        if (khz <= 0) {
            return "N/A";
        }
        if (khz >= 1_000_000L) {
            return String.format(Locale.US, "%.2f GHz", khz / 1_000_000.0);
        } else if (khz >= 1_000L) {
            return String.format(Locale.US, "%.0f MHz", khz / 1_000.0);
        } else {
            return String.format(Locale.US, "%d KHz", khz);
        }
    }

    /**
     * 格式化字节数，自动转换为合适的单位
     * @param bytes 字节数
     * @return 格式化后的字符串，如 "128 GB"、"512 MB"、"256 KB"
     */
    public static String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "0 B";
        }
        if (bytes >= 1_073_741_824L) { // 1 GB
            return String.format(Locale.US, "%.2f GB", bytes / 1_073_741_824.0);
        } else if (bytes >= 1_048_576L) { // 1 MB
            return String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0);
        } else if (bytes >= 1_024L) { // 1 KB
            return String.format(Locale.US, "%.0f KB", bytes / 1_024.0);
        } else {
            return String.format(Locale.US, "%d B", bytes);
        }
    }

    /**
     * 格式化摄氏温度
     * @param temp 温度值（摄氏度）
     * @return 格式化后的字符串，如 "32.5°C"
     */
    public static String formatTempCelsius(float temp) {
        if (Float.isNaN(temp)) {
            return "N/A";
        }
        return String.format(Locale.US, "%.1f°C", temp);
    }

    /**
     * 格式化信号强度（dBm）
     * @param dbm 信号强度值
     * @return 格式化后的字符串，如 "-65 dBm"
     */
    public static String formatDbm(int dbm) {
        if (dbm == Integer.MAX_VALUE || dbm == Integer.MIN_VALUE) {
            return "N/A";
        }
        return String.format(Locale.US, "%d dBm", dbm);
    }

    /**
     * 格式化百分比
     * @param pct 百分比值（0-100）
     * @return 格式化后的字符串，如 "85%"
     */
    public static String formatPercent(int pct) {
        if (pct < 0 || pct > 100) {
            return "N/A";
        }
        return pct + "%";
    }
}
