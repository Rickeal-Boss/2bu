package com.example.deviceinfoviewer.data.model;

/**
 * 内存信息
 */
public class MemoryInfo {
    private long totalKB = -1L;
    private long availableKB = -1L;
    private long usedKB = -1L;
    private long swapTotalKB = -1L;
    private long swapUsedKB = -1L;
    private long zramOriginalKB = -1L;
    private long zramCompressedKB = -1L;
    private long zramMemUsedTotalKB = -1L;   // mm_stat: mem_used_total (实际占用)
    private float compressionRatio = -1f;
    private long timestamp = 0L;

    public MemoryInfo() {}

    public long getTotalKB() { return totalKB; }
    public void setTotalKB(long v) { this.totalKB = v; }
    public long getAvailableKB() { return availableKB; }
    public void setAvailableKB(long v) { this.availableKB = v; }
    public long getUsedKB() { return usedKB; }
    public void setUsedKB(long v) { this.usedKB = v; }
    public long getSwapTotalKB() { return swapTotalKB; }
    public void setSwapTotalKB(long v) { this.swapTotalKB = v; }
    public long getSwapUsedKB() { return swapUsedKB; }
    public void setSwapUsedKB(long v) { this.swapUsedKB = v; }
    public long getZramOriginalKB() { return zramOriginalKB; }
    public void setZramOriginalKB(long v) { this.zramOriginalKB = v; }
    public long getZramCompressedKB() { return zramCompressedKB; }
    public void setZramCompressedKB(long v) { this.zramCompressedKB = v; }
    public long getZramMemUsedTotalKB() { return zramMemUsedTotalKB; }
    public void setZramMemUsedTotalKB(long v) { this.zramMemUsedTotalKB = v; }
    public float getCompressionRatio() { return compressionRatio; }
    public void setCompressionRatio(float v) { this.compressionRatio = v; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long v) { this.timestamp = v; }

    @Override
    public String toString() {
        return "MemoryInfo{totalKB=" + totalKB + ", availableKB=" + availableKB + ", usedKB=" + usedKB + "}";
    }
}
