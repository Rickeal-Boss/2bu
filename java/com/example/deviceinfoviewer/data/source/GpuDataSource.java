package com.example.deviceinfoviewer.data.source;

import com.example.deviceinfoviewer.data.model.GpuInfo;

import java.util.List;

/**
 * GPU 数据源 — 增强版
 * 高通 Adreno + ARM Mali + PowerVR + 全设备通用路径
 * 新增：负载率、频率范围、调速器、OpenGL Renderer
 */
public class GpuDataSource {

    public GpuInfo getGpuInfo() {
        GpuInfo info = new GpuInfo();
        info.setTimestamp(System.currentTimeMillis());

        // 1. 型号 & 厂商 & OpenGL 渲染器
        resolveGpuModel(info);

        // 2. 频率 (当前 + 最小 + 最大)
        resolveGpuFrequency(info);

        // 3. 调速器信息
        resolveGovernor(info);

        // 4. 负载率
        resolveLoad(info);

        // 5. 温度
        info.setTemperatureCelsius(getGpuTemperature());

        return info;
    }

    // ===== GPU 型号 & 厂商 & 渲染器 =====
    private void resolveGpuModel(GpuInfo info) {
        // 系统属性 → 型号
        String[] modelProps = {"ro.gpu.chip", "ro.gfx.driver", "ro.hardware.egl",
                "ro.board.platform", "ro.chipname", "ro.soc.manufacturer"};
        for (String prop : modelProps) {
            String val = SysFsReader.readProp(prop);
            if (!val.isEmpty()) {
                info.setModel(val);
                break;
            }
        }
        // 厂商
        String vendor = SysFsReader.readProp("ro.soc.manufacturer");
        if (!vendor.isEmpty()) info.setVendor(vendor);

        // OpenGL ES 渲染器 (如 "Adreno (TM) 730")
        String renderer = SysFsReader.readProp("ro.gles.version");
        if (!renderer.isEmpty() && info.getModel().isEmpty()) {
            info.setModel(renderer);
        }
        // EGL 信息
        String eglVendor = SysFsReader.readProp("ro.hardware.egl");
        if (!eglVendor.isEmpty()) {
            // 提取 Adreno/Mali/PowerVR 名称
            info.setRenderer(eglVendor);
        }

        // Exynos / ARM Mali 文件
        String model = SysFsReader.readLine("/sys/kernel/gpu/gpu_model");
        if (!model.isEmpty()) info.setModel(model.trim());

        // Mali gpuinfo
        String gpuInfoLine = SysFsReader.readLine("/sys/class/misc/mali0/device/gpuinfo");
        if (!gpuInfoLine.isEmpty() && info.getModel().isEmpty()) {
            info.setModel(gpuInfoLine.trim());
        }
    }

    // ===== GPU 频率 (多平台, 增强) =====
    private void resolveGpuFrequency(GpuInfo info) {
        long curFreq = -1;
        long minFreq = -1;
        long maxFreq = -1;

        // --- 高通 Adreno: /sys/class/kgsl/kgsl-3d0/ ---
        if (SysFsReader.fileExists("/sys/class/kgsl/kgsl-3d0/")) {
            curFreq = tryReadFreqHz("/sys/class/kgsl/kgsl-3d0/gpuclk");
            if (curFreq <= 0) curFreq = tryReadFreqHz("/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq");
            if (curFreq <= 0) curFreq = tryReadFreqHz("/sys/class/kgsl/kgsl-3d0/clock_mhz");

            minFreq = tryReadFreqHz("/sys/class/kgsl/kgsl-3d0/devfreq/min_freq");
            maxFreq = tryReadFreqHz("/sys/class/kgsl/kgsl-3d0/devfreq/max_freq");

            if (curFreq > 0) { info.setFrequencyKHz(curFreq / 1000); }
            if (minFreq > 0) { info.setMinFreqKHz(minFreq / 1000); }
            if (maxFreq > 0) { info.setMaxFreqKHz(maxFreq / 1000); }
            if (curFreq > 0) return;
        }

        // --- ARM Mali (通用 devfreq) ---
        List<String> devfreqDirs = SysFsReader.listDir("/sys/class/devfreq/");
        for (String dir : devfreqDirs) {
            if (dir.toLowerCase().contains("gpu") || dir.toLowerCase().contains("mali")
                    || dir.toLowerCase().contains("sgpu") || dir.toLowerCase().contains("gpufreq")) {
                String base = "/sys/class/devfreq/" + dir + "/";
                curFreq = tryReadFreqHz(base + "cur_freq");
                if (curFreq > 0) {
                    info.setFrequencyKHz(curFreq / 1000);
                    minFreq = tryReadFreqHz(base + "min_freq");
                    maxFreq = tryReadFreqHz(base + "max_freq");
                    if (minFreq > 0) info.setMinFreqKHz(minFreq / 1000);
                    if (maxFreq > 0) info.setMaxFreqKHz(maxFreq / 1000);
                    return;
                }
            }
        }

        // --- Mali debugfs ---
        curFreq = tryReadFreqHz("/sys/kernel/gpu/gpu_freq_max");
        if (curFreq <= 0) curFreq = tryReadFreqHz("/sys/kernel/gpu/gpu_clock");
        if (curFreq > 0) { info.setFrequencyKHz(curFreq / 1000); return; }

        // --- MTK ---
        curFreq = tryReadFreqHz("/sys/module/ged/parameters/gpu_freq");
        if (curFreq > 0) { info.setFrequencyKHz(curFreq / 1000); return; }

        // --- PowerVR ---
        curFreq = tryReadFreqHz("/sys/kernel/gpu/gpu_freq");
        if (curFreq > 0) { info.setFrequencyKHz(curFreq / 1000); return; }

        // --- Mali /proc/mali ---
        curFreq = tryReadFreqHz("/proc/mali/gpu_freq");
        if (curFreq > 0) info.setFrequencyKHz(curFreq / 1000);
    }

    // ===== 调速器信息 =====
    private void resolveGovernor(GpuInfo info) {
        // 高通 Adreno
        String gov = SysFsReader.readLine("/sys/class/kgsl/kgsl-3d0/devfreq/governor");
        if (!gov.isEmpty()) {
            info.setGovernor(gov.trim());
            String availGovs = SysFsReader.readAll("/sys/class/kgsl/kgsl-3d0/devfreq/available_governors");
            if (!availGovs.isEmpty()) {
                info.setAvailableGovernors(availGovs.replace('\n', ' ').trim());
            }
            return;
        }

        // 通用 devfreq
        List<String> dirs = SysFsReader.listDir("/sys/class/devfreq/");
        for (String dir : dirs) {
            if (dir.toLowerCase().contains("gpu") || dir.toLowerCase().contains("mali")) {
                gov = SysFsReader.readLine("/sys/class/devfreq/" + dir + "/governor");
                if (!gov.isEmpty()) {
                    info.setGovernor(gov.trim());
                    String availGovs = SysFsReader.readAll("/sys/class/devfreq/" + dir + "/available_governors");
                    if (!availGovs.isEmpty()) {
                        info.setAvailableGovernors(availGovs.replace('\n', ' ').trim());
                    }
                    return;
                }
            }
        }

        // CPU GPU 调速器属性
        gov = SysFsReader.readProp("ro.gpu.governor");
        if (!gov.isEmpty()) info.setGovernor(gov);
    }

    // ===== GPU 负载率 =====
    private void resolveLoad(GpuInfo info) {
        // 高通 Adreno
        float load = SysFsReader.readFloat("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage");
        if (!Float.isNaN(load) && load > 0) {
            info.setLoadPercentage(load);
            return;
        }

        // 高通 gpubusy (格式: "used total" 如 "12345678 98765432")
        String gpuBusy = SysFsReader.readLine("/sys/class/kgsl/kgsl-3d0/gpubusy");
        if (!gpuBusy.isEmpty()) {
            String[] parts = gpuBusy.trim().split("\\s+");
            if (parts.length >= 2) {
                try {
                    long used = Long.parseLong(parts[0]);
                    long total = Long.parseLong(parts[1]);
                    if (total > 0) {
                        info.setLoadPercentage((float) used / total * 100f);
                        return;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // Mali: /sys/class/devfreq/*gpu*/load
        List<String> dirs = SysFsReader.listDir("/sys/class/devfreq/");
        for (String dir : dirs) {
            if (dir.toLowerCase().contains("gpu") || dir.toLowerCase().contains("mali")) {
                String loadStr = SysFsReader.readLine("/sys/class/devfreq/" + dir + "/load");
                if (!loadStr.isEmpty()) {
                    // 格式: "frequency load%" 如 "675000000 45%"
                    String[] parts = loadStr.split("@");
                    if (parts.length == 1) parts = loadStr.split("\\s+");
                    for (String part : parts) {
                        part = part.replace("%", "").trim();
                        try {
                            float val = Float.parseFloat(part);
                            if (val > 0 && val <= 100) {
                                info.setLoadPercentage(val);
                                return;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
    }

    private long tryReadFreqHz(String path) {
        if (!SysFsReader.fileExists(path)) return -1;
        long val = SysFsReader.readLong(path);
        if (val <= 0) return -1;
        // 自动检测单位：> 1e8 可能已是 Hz；< 1e3 可能是 MHz
        if (val < 1000) val *= 1_000_000;  // MHz → Hz
        return val;
    }

    // ===== GPU 温度 (全平台 thermal zone 扫描) =====
    private float getGpuTemperature() {
        String[] thermalBases = {"/sys/class/thermal/", "/sys/devices/virtual/thermal/"};
        for (String base : thermalBases) {
            List<String> zones = SysFsReader.listDir(base);
            for (String zone : zones) {
                String type = SysFsReader.readLine(base + zone + "/type").toLowerCase().trim();
                if (isGpuThermal(type)) {
                    float temp = SysFsReader.readFloat(base + zone + "/temp");
                    if (!Float.isNaN(temp)) {
                        return temp > 1000f ? temp / 1000f : temp;
                    }
                }
            }
        }
        return Float.NaN;
    }

    private boolean isGpuThermal(String type) {
        return type.contains("gpu") || type.contains("kgsl") || type.contains("mali")
                || type.contains("mtktsgpu") || type.contains("tztsgpu")
                || type.contains("sgpu") || type.contains("gpuss");
    }
}
