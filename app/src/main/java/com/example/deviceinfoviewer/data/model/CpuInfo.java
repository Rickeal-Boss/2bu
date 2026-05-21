package com.example.deviceinfoviewer.data.model;

import java.util.ArrayList;
import java.util.List;

/**
 * CPU 整体信息
 */
public class CpuInfo {
    private String architecture = "";
    private int coreCount = 0;
    private List<CpuCoreInfo> cores = new ArrayList<>();
    private float temperatureCelsius = Float.NaN;
    private long timestamp = 0L;

    public CpuInfo() {}

    public String getArchitecture() { return architecture; }
    public void setArchitecture(String architecture) { this.architecture = architecture; }

    public int getCoreCount() { return coreCount; }
    public void setCoreCount(int coreCount) { this.coreCount = coreCount; }

    public List<CpuCoreInfo> getCores() { return cores; }
    public void setCores(List<CpuCoreInfo> cores) { this.cores = cores; }

    public float getTemperatureCelsius() { return temperatureCelsius; }
    public void setTemperatureCelsius(float temperatureCelsius) { this.temperatureCelsius = temperatureCelsius; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "CpuInfo{architecture='" + architecture + "', coreCount=" + coreCount
                + ", cores=" + cores + ", temperatureCelsius=" + temperatureCelsius
                + ", timestamp=" + timestamp + "}";
    }
}
