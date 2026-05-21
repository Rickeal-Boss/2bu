package com.example.deviceinfoviewer.util;

import android.content.Context;
import android.content.Intent;

import com.example.deviceinfoviewer.FormatUtils;
import com.example.deviceinfoviewer.data.model.*;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 导出工具类，支持纯文本和 JSON 格式导出
 */
public final class ExportHelper {

    private ExportHelper() {}

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 导出为纯文本报告
     */
    public static String exportToText(DeviceRepository repository) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        sb.append("========================================\n");
        sb.append("       设备信息报告\n");
        sb.append("       导出时间: ").append(sdf.format(new Date())).append("\n");
        sb.append("========================================\n\n");

        // CPU
        CpuInfo cpu = repository.getCpuLiveData().getValue();
        if (cpu != null) {
            sb.append("[CPU 信息]\n");
            sb.append("架构: ").append(cpu.getArchitecture()).append("\n");
            sb.append("核心数: ").append(cpu.getCoreCount()).append("\n");
            sb.append("温度: ").append(FormatUtils.formatTempCelsius(cpu.getTemperatureCelsius())).append("\n");
            for (CpuCoreInfo core : cpu.getCores()) {
                sb.append("  核心").append(core.getCoreIndex()).append(": ")
                        .append(FormatUtils.formatFreq(core.getCurrentFreqKHz()))
                        .append(" (最大 ").append(FormatUtils.formatFreq(core.getMaxFreqKHz()))
                        .append(", 调度器: ").append(core.getGovernor()).append(")\n");
            }
            sb.append("\n");
        }

        // GPU
        GpuInfo gpu = repository.getGpuLiveData().getValue();
        if (gpu != null) {
            sb.append("[GPU 信息]\n");
            sb.append("型号: ").append(gpu.getModel()).append("\n");
            sb.append("厂商: ").append(gpu.getVendor()).append("\n");
            sb.append("频率: ").append(FormatUtils.formatFreq(gpu.getFrequencyKHz())).append("\n");
            sb.append("\n");
        }

        // 电池
        BatteryInfo battery = repository.getBatteryLiveData().getValue();
        if (battery != null) {
            sb.append("[电池信息]\n");
            sb.append("电量: ").append(FormatUtils.formatPercent(battery.getLevelPercent())).append("\n");
            sb.append("温度: ").append(FormatUtils.formatTempCelsius(battery.getTemperatureCelsius())).append("\n");
            sb.append("状态: ").append(battery.getChargeStatus()).append("\n");
            sb.append("健康: ").append(battery.getHealth()).append("\n");
            sb.append("循环次数: ").append(battery.getCycleCount()).append("\n");
            sb.append("\n");
        }

        // 内存
        MemoryInfo memory = repository.getMemoryLiveData().getValue();
        if (memory != null) {
            sb.append("[内存信息]\n");
            sb.append("总: ").append(FormatUtils.formatBytes(memory.getTotalKB() * 1024)).append("\n");
            sb.append("可用: ").append(FormatUtils.formatBytes(memory.getAvailableKB() * 1024)).append("\n");
            sb.append("\n");
        }

        // 存储
        StorageInfo storage = repository.getStorageLiveData().getValue();
        if (storage != null) {
            sb.append("[存储信息]\n");
            sb.append("内部存储总: ").append(FormatUtils.formatBytes(storage.getInternalTotalBytes())).append("\n");
            sb.append("已用: ").append(FormatUtils.formatBytes(storage.getInternalUsedBytes())).append("\n");
            sb.append("可用: ").append(FormatUtils.formatBytes(storage.getInternalAvailableBytes())).append("\n");
            sb.append("\n");
        }

        // 系统
        SystemInfo sys = repository.getSystemLiveData().getValue();
        if (sys != null) {
            sb.append("[系统信息]\n");
            sb.append("Android: ").append(sys.getAndroidVersion()).append("\n");
            sb.append("内核: ").append(sys.getKernelVersion()).append("\n");
            sb.append("\n");
        }

        sb.append("========================================\n");
        sb.append("        报告结束\n");
        sb.append("========================================\n");

        return sb.toString();
    }

    /**
     * 导出为 JSON 格式
     */
    public static String exportToJson(DeviceRepository repository) {
        // 简单的 JSON 导出
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"exportTime\": ").append(System.currentTimeMillis()).append(",\n");
        json.append("  \"cpu\": ").append(gson.toJson(repository.getCpuLiveData().getValue())).append(",\n");
        json.append("  \"gpu\": ").append(gson.toJson(repository.getGpuLiveData().getValue())).append(",\n");
        json.append("  \"battery\": ").append(gson.toJson(repository.getBatteryLiveData().getValue())).append(",\n");
        json.append("  \"memory\": ").append(gson.toJson(repository.getMemoryLiveData().getValue())).append(",\n");
        json.append("  \"storage\": ").append(gson.toJson(repository.getStorageLiveData().getValue())).append(",\n");
        json.append("  \"system\": ").append(gson.toJson(repository.getSystemLiveData().getValue())).append("\n");
        json.append("}");
        return json.toString();
    }

    /**
     * 通过 Intent 分享报告（直接用 EXTRA_TEXT）
     */
    public static void shareReport(Context context, String content, String title) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, content);
        context.startActivity(Intent.createChooser(shareIntent, "分享设备信息"));
    }
}
