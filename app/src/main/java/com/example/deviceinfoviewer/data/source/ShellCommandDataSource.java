package com.example.deviceinfoviewer.data.source;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Shell 命令数据源 — 通过 ProcessBuilder 执行 dumpsys / logcat 等系统命令，
 * 获取普通 API 和 sysfs 无法提供的深度系统信息。
 *
 * 支持的命令：
 * - dumpsys battery  → 电池详细信息（充电协议、充电电流上限等）
 * - dumpsys cpuinfo  → 各进程 CPU 负载排名
 * - dumpsys thermalservice → 全机温控策略与温度
 * - dumpsys meminfo  → 内存分配详情
 * - logcat -d -b events -t 50 → 系统事件日志（最近50条）
 * - cat /proc/stat    → CPU 时间统计
 */
public class ShellCommandDataSource {

    /** 命令执行超时 (秒) */
    private static final int TIMEOUT_SECONDS = 8;

    /**
     * 执行 shell 命令并返回完整输出
     */
    public static String exec(String... command) {
        StringBuilder output = new StringBuilder();
        Process process = null;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
            }
        } catch (Exception e) {
            return "";
        } finally {
            if (process != null) process.destroy();
        }
        return output.toString();
    }

    // ========== dumpsys 系列 ==========

    /**
     * 获取 dumpsys battery 输出
     * 包含：充电协议 (Wireless/USB/AC)、最大充电电流/电压、Charge counter 等
     */
    public static String getDumpsysBattery() {
        return exec("/system/bin/dumpsys", "battery");
    }

    /**
     * 获取 dumpsys thermalservice 输出
     * 包含：全机温控节流状态、各 sensor 温度、冷却设备状态
     */
    public static String getDumpsysThermal() {
        return exec("/system/bin/dumpsys", "thermalservice");
    }

    /**
     * 获取 dumpsys cpuinfo 输出
     * 包含：各进程 CPU 使用时间排名
     */
    public static String getDumpsysCpuinfo() {
        return exec("/system/bin/dumpsys", "cpuinfo");
    }

    /**
     * 获取 dumpsys meminfo 输出
     * 包含：系统整体 + 各进程的 PSS/RSS/Heap 等详细内存分配
     */
    public static String getDumpsysMeminfo() {
        return exec("/system/bin/dumpsys", "meminfo");
    }

    /**
     * 获取 dumpsys display 输出
     * 包含：屏幕分辨率、刷新率、HDR 能力等
     */
    public static String getDumpsysDisplay() {
        return exec("/system/bin/dumpsys", "display");
    }

    // ========== logcat 系列 ==========

    /**
     * 获取最近 N 条系统事件日志 (events buffer)
     */
    public static String getLogcatEvents(int count) {
        return exec("logcat", "-d", "-b", "events", "-t", String.valueOf(count));
    }

    /**
     * 获取最近 N 条主日志 (main buffer)
     */
    public static String getLogcatMain(int count) {
        return exec("logcat", "-d", "-b", "main", "-t", String.valueOf(count));
    }

    // ========== 解析辅助方法 ==========

    /**
     * 从 dumpsys battery 输出中提取指定键的值（值在 ": " 之后）
     * 示例：输入 "  Max charging current: 75000" → "75000"
     */
    public static String extractDumpsysValue(String dumpsysOutput, String key) {
        if (dumpsysOutput == null || dumpsysOutput.isEmpty()) return null;
        for (String line : dumpsysOutput.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(key + ":") || trimmed.startsWith(key + " ")) {
                int colonIdx = trimmed.indexOf(':');
                if (colonIdx >= 0) {
                    return trimmed.substring(colonIdx + 1).trim();
                }
            }
        }
        return null;
    }

    /**
     * 从 dumpsys battery 提取 Integer
     */
    public static int extractInt(String dumpsysOutput, String key) {
        String val = extractDumpsysValue(dumpsysOutput, key);
        if (val == null) return -1;
        try { return Integer.parseInt(val); }
        catch (NumberFormatException e) { return -1; }
    }

    /**
     * 从 dumpsys battery 提取 Long
     */
    public static long extractLong(String dumpsysOutput, String key) {
        String val = extractDumpsysValue(dumpsysOutput, key);
        if (val == null) return -1L;
        try { return Long.parseLong(val); }
        catch (NumberFormatException e) { return -1L; }
    }

    /**
     * 从 thermal service 输出中提取温度列表
     */
    public static List<Float> extractThermalTemperatures(String thermalOutput) {
        List<Float> temps = new ArrayList<>();
        if (thermalOutput == null || thermalOutput.isEmpty()) return temps;
        for (String line : thermalOutput.split("\n")) {
            // 匹配 "temperature: xx.x" 格式
            if (line.contains("temperature:") || line.contains("temp:")) {
                try {
                    int idx = line.indexOf("temperature:");
                    if (idx < 0) idx = line.indexOf("temp:");
                    String numPart = line.substring(idx).replaceAll("[^0-9.]", " ").trim();
                    String[] parts = numPart.split("\\s+");
                    for (String part : parts) {
                        if (!part.isEmpty()) {
                            try { temps.add(Float.parseFloat(part)); }
                            catch (NumberFormatException ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return temps;
    }

    // ========== /proc 额外读取 ==========

    /**
     * 读取 /proc/stat 获取 CPU 时间统计
     * 可用于计算各核心负载百分比
     */
    public static String getProcStat() {
        return SysFsReader.readAll("/proc/stat");
    }

    /**
     * 读取 /proc/version 内核完整版本字符串
     */
    public static String getKernelVersionFull() {
        return SysFsReader.readLine("/proc/version");
    }

    /**
     * 读取 /proc/uptime 系统启动时间
     */
    public static float getUptimeSeconds() {
        String line = SysFsReader.readLine("/proc/uptime");
        if (line.isEmpty()) return -1f;
        String[] parts = line.split("\\s+");
        try { return Float.parseFloat(parts[0]); }
        catch (NumberFormatException e) { return -1f; }
    }
}
