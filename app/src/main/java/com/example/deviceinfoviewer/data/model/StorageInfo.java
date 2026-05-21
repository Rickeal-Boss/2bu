package com.example.deviceinfoviewer.data.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 存储信息
 */
public class StorageInfo {
    private long internalTotalBytes = -1L;
    private long internalUsedBytes = -1L;
    private long internalAvailableBytes = -1L;
    private List<PartitionInfo> partitions = new ArrayList<>();

    public StorageInfo() {}

    public long getInternalTotalBytes() { return internalTotalBytes; }
    public void setInternalTotalBytes(long v) { this.internalTotalBytes = v; }
    public long getInternalUsedBytes() { return internalUsedBytes; }
    public void setInternalUsedBytes(long v) { this.internalUsedBytes = v; }
    public long getInternalAvailableBytes() { return internalAvailableBytes; }
    public void setInternalAvailableBytes(long v) { this.internalAvailableBytes = v; }
    public List<PartitionInfo> getPartitions() { return partitions; }
    public void setPartitions(List<PartitionInfo> v) { this.partitions = v; }

    /**
     * 分区信息内部类
     */
    public static class PartitionInfo {
        private String mountPoint = "";
        private long totalBytes = -1L;
        private long usedBytes = -1L;
        private long availableBytes = -1L;

        public PartitionInfo() {}

        public PartitionInfo(String mountPoint, long totalBytes, long usedBytes, long availableBytes) {
            this.mountPoint = mountPoint;
            this.totalBytes = totalBytes;
            this.usedBytes = usedBytes;
            this.availableBytes = availableBytes;
        }

        public String getMountPoint() { return mountPoint; }
        public void setMountPoint(String v) { this.mountPoint = v; }
        public long getTotalBytes() { return totalBytes; }
        public void setTotalBytes(long v) { this.totalBytes = v; }
        public long getUsedBytes() { return usedBytes; }
        public void setUsedBytes(long v) { this.usedBytes = v; }
        public long getAvailableBytes() { return availableBytes; }
        public void setAvailableBytes(long v) { this.availableBytes = v; }

        @Override
        public String toString() {
            return "PartitionInfo{mountPoint='" + mountPoint + "', totalBytes=" + totalBytes + "}";
        }
    }

    @Override
    public String toString() {
        return "StorageInfo{internalTotalBytes=" + internalTotalBytes + ", partitions=" + partitions.size() + "}";
    }
}
