package com.example.deviceinfoviewer.data.source;

import com.example.deviceinfoviewer.data.model.MemoryInfo;

import java.util.List;

/**
 * 内存数据源，解析 /proc/meminfo 和 ZRAM 统计
 */
public class MemoryDataSource {

    public MemoryInfo getMemoryInfo() {
        MemoryInfo info = new MemoryInfo();
        info.setTimestamp(System.currentTimeMillis());

        String meminfo = SysFsReader.readAll("/proc/meminfo");
        if (meminfo.isEmpty()) {
            return info;
        }

        for (String line : meminfo.split("\n")) {
            line = line.trim();
            if (line.startsWith("MemTotal:")) {
                info.setTotalKB(parseKB(line));
            } else if (line.startsWith("MemAvailable:")) {
                info.setAvailableKB(parseKB(line));
            } else if (line.startsWith("SwapTotal:")) {
                info.setSwapTotalKB(parseKB(line));
            } else if (line.startsWith("SwapFree:")) {
                long swapFree = parseKB(line);
                if (info.getSwapTotalKB() > 0) {
                    info.setSwapUsedKB(info.getSwapTotalKB() - swapFree);
                }
            }
        }

        // 计算已用内存
        if (info.getTotalKB() > 0 && info.getAvailableKB() > 0) {
            info.setUsedKB(info.getTotalKB() - info.getAvailableKB());
        }

        // 获取 ZRAM 统计
        getZramStats(info);

        return info;
    }

    private void getZramStats(MemoryInfo info) {
        List<String> blocks = SysFsReader.listDir("/sys/block/");
        for (String block : blocks) {
            if (block.startsWith("zram")) {
                String base = "/sys/block/" + block + "/";
                // orig_data_size / compr_data_size (单位: bytes)
                long origSize = SysFsReader.readLong(base + "orig_data_size");
                long comprSize = SysFsReader.readLong(base + "compr_data_size");
                if (origSize > 0) {
                    info.setZramOriginalKB(info.getZramOriginalKB() > 0
                            ? info.getZramOriginalKB() + origSize / 1024 : origSize / 1024);
                }
                if (comprSize > 0) {
                    info.setZramCompressedKB(info.getZramCompressedKB() > 0
                            ? info.getZramCompressedKB() + comprSize / 1024 : comprSize / 1024);
                }
                // mm_stat: 更准确的 ZRAM 统计
                String mmStat = SysFsReader.readAll(base + "mm_stat");
                if (!mmStat.isEmpty()) {
                    String[] parts = mmStat.trim().split("\\s+");
                    if (parts.length >= 3) {
                        try {
                            long memUsedTotal = Long.parseLong(parts[2]); // bytes
                            info.setZramMemUsedTotalKB(info.getZramMemUsedTotalKB() > 0
                                    ? info.getZramMemUsedTotalKB() + memUsedTotal / 1024
                                    : memUsedTotal / 1024);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        // 压缩比
        if (info.getZramOriginalKB() > 0 && info.getZramCompressedKB() > 0) {
            info.setCompressionRatio((float) info.getZramCompressedKB() / info.getZramOriginalKB());
        }
    }

    private long parseKB(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length >= 2) {
            try {
                return Long.parseLong(parts[1]);
            } catch (NumberFormatException ignored) {}
        }
        return -1L;
    }
}
