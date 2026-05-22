package com.example.deviceinfoviewer.data.model;

/**
 * GPU 信息 — 增强版
 * 频率 / 温度 / 负载 / 调速器 / 频率范围
 */
public class GpuInfo {
    private String model = "";
    private String vendor = "";
    private String renderer = "";        // OpenGL ES Renderer
    private long frequencyKHz = -1L;
    private long minFreqKHz = -1L;
    private long maxFreqKHz = -1L;
    private String governor = "";
    private String availableGovernors = "";
    private float loadPercentage = Float.NaN;  // GPU 使用率 (%)
    private float temperatureCelsius = Float.NaN;
    private long timestamp = 0L;

    public GpuInfo() {}

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getRenderer() { return renderer; }
    public void setRenderer(String renderer) { this.renderer = renderer; }

    public long getFrequencyKHz() { return frequencyKHz; }
    public void setFrequencyKHz(long frequencyKHz) { this.frequencyKHz = frequencyKHz; }

    public long getMinFreqKHz() { return minFreqKHz; }
    public void setMinFreqKHz(long minFreqKHz) { this.minFreqKHz = minFreqKHz; }

    public long getMaxFreqKHz() { return maxFreqKHz; }
    public void setMaxFreqKHz(long maxFreqKHz) { this.maxFreqKHz = maxFreqKHz; }

    public String getGovernor() { return governor; }
    public void setGovernor(String governor) { this.governor = governor; }

    public String getAvailableGovernors() { return availableGovernors; }
    public void setAvailableGovernors(String v) { this.availableGovernors = v; }

    public float getLoadPercentage() { return loadPercentage; }
    public void setLoadPercentage(float loadPercentage) { this.loadPercentage = loadPercentage; }

    public float getTemperatureCelsius() { return temperatureCelsius; }
    public void setTemperatureCelsius(float temperatureCelsius) { this.temperatureCelsius = temperatureCelsius; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "GpuInfo{model='" + model + "', vendor='" + vendor
                + "', frequencyKHz=" + frequencyKHz + ", temp=" + temperatureCelsius
                + ", load=" + loadPercentage + "%, governor=" + governor + "}";
    }
}
