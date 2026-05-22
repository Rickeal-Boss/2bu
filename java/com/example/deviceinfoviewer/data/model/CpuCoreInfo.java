package com.example.deviceinfoviewer.data.model;

/**
 * CPU 核心信息
 */
public class CpuCoreInfo {
    private int coreIndex;
    private long currentFreqKHz;
    private long maxFreqKHz;
    private long minFreqKHz;
    private String governor;

    public CpuCoreInfo() {}

    public CpuCoreInfo(int coreIndex, long currentFreqKHz, long maxFreqKHz, long minFreqKHz, String governor) {
        this.coreIndex = coreIndex;
        this.currentFreqKHz = currentFreqKHz;
        this.maxFreqKHz = maxFreqKHz;
        this.minFreqKHz = minFreqKHz;
        this.governor = governor;
    }

    public int getCoreIndex() { return coreIndex; }
    public void setCoreIndex(int coreIndex) { this.coreIndex = coreIndex; }

    public long getCurrentFreqKHz() { return currentFreqKHz; }
    public void setCurrentFreqKHz(long currentFreqKHz) { this.currentFreqKHz = currentFreqKHz; }

    public long getMaxFreqKHz() { return maxFreqKHz; }
    public void setMaxFreqKHz(long maxFreqKHz) { this.maxFreqKHz = maxFreqKHz; }

    public long getMinFreqKHz() { return minFreqKHz; }
    public void setMinFreqKHz(long minFreqKHz) { this.minFreqKHz = minFreqKHz; }

    public String getGovernor() { return governor; }
    public void setGovernor(String governor) { this.governor = governor; }

    @Override
    public String toString() {
        return "CpuCoreInfo{coreIndex=" + coreIndex
                + ", currentFreqKHz=" + currentFreqKHz
                + ", maxFreqKHz=" + maxFreqKHz
                + ", minFreqKHz=" + minFreqKHz
                + ", governor='" + governor + "'}";
    }
}
