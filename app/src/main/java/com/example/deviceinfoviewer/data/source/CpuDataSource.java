package com.example.deviceinfoviewer.data.source;

import com.example.deviceinfoviewer.data.model.CpuCoreInfo;
import com.example.deviceinfoviewer.data.model.CpuInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * CPU 数据源，通过 sysfs 获取 CPU 频率和温度
 */
public class CpuDataSource {

    private static final String CPU_BASE = "/sys/devices/system/cpu/";
    private static final String THERMAL_BASE = "/sys/class/thermal/";

    public CpuInfo getCpuInfo() {
        CpuInfo info = new CpuInfo();
        info.setArchitecture(System.getProperty("os.arch", "unknown"));
        info.setTimestamp(System.currentTimeMillis());

        List<CpuCoreInfo> cores = new ArrayList<>();
        int coreIndex = 0;
        while (true) {
            String cpuDir = CPU_BASE + "cpu" + coreIndex + "/cpufreq/";
            if (!SysFsReader.fileExists(cpuDir)) {
                break;
            }
            CpuCoreInfo core = new CpuCoreInfo();
            core.setCoreIndex(coreIndex);
            core.setCurrentFreqKHz(SysFsReader.readLong(cpuDir + "scaling_cur_freq"));
            core.setMaxFreqKHz(SysFsReader.readLong(cpuDir + "scaling_max_freq"));
            core.setMinFreqKHz(SysFsReader.readLong(cpuDir + "scaling_min_freq"));
            core.setGovernor(SysFsReader.readLine(cpuDir + "scaling_governor"));
            cores.add(core);
            coreIndex++;
        }
        info.setCoreCount(cores.size());
        info.setCores(cores);
        info.setTemperatureCelsius(getCpuTemperature());
        return info;
    }

    /**
     * 获取 CPU 温度，扫描所有 thermal zone 查找 CPU 相关传感器
     */
    public float getCpuTemperature() {
        // 扫描所有 thermal zone 查找匹配的传感器类型
        List<String> zones = SysFsReader.listDir(THERMAL_BASE);
        for (String zone : zones) {
            String typePath = THERMAL_BASE + zone + "/type";
            String type = SysFsReader.readLine(typePath);
            String tempPath = THERMAL_BASE + zone + "/temp";
            if (isCpuRelatedZone(type)) {
                float temp = SysFsReader.readFloat(tempPath);
                if (!Float.isNaN(temp)) {
                    if (temp > 1000f) {
                        return temp / 1000f;
                    }
                    return temp;
                }
            }
        }

        // 回退：也搜索 /sys/devices/virtual/thermal/
        String virtualThermalBase = "/sys/devices/virtual/thermal/";
        zones = SysFsReader.listDir(virtualThermalBase);
        for (String zone : zones) {
            String typePath = virtualThermalBase + zone + "/type";
            String type = SysFsReader.readLine(typePath);
            String tempPath = virtualThermalBase + zone + "/temp";
            if (isCpuRelatedZone(type)) {
                float temp = SysFsReader.readFloat(tempPath);
                if (!Float.isNaN(temp)) {
                    if (temp > 1000f) {
                        return temp / 1000f;
                    }
                    return temp;
                }
            }
        }

        return Float.NaN;
    }

    /**
     * 判断 thermal zone type 是否与 CPU 相关
     */
    private static boolean isCpuRelatedZone(String type) {
        if (type == null || type.isEmpty()) return false;
        String lower = type.toLowerCase();
        return lower.contains("cpu") || lower.contains("tsens") || lower.contains("soc")
                || lower.contains("x86_pkg_temp") || lower.contains("acpitz")
                || lower.contains("t-sen") || lower.contains("bcl")
                || lower.contains("virtual") || lower.contains("ddr");
    }
}
