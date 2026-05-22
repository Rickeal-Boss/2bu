package com.example.deviceinfoviewer.data.source;

import android.os.StatFs;

import com.example.deviceinfoviewer.data.model.StorageInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 存储数据源，使用 StatFs 获取存储信息
 */
public class StorageDataSource {

    public StorageInfo getStorageInfo() {
        StorageInfo info = new StorageInfo();

        // 内部存储
        StatFs statFs = new StatFs("/data");
        long totalBytes = statFs.getBlockCountLong() * statFs.getBlockSizeLong();
        long availableBytes = statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
        long usedBytes = totalBytes - availableBytes;

        info.setInternalTotalBytes(totalBytes);
        info.setInternalUsedBytes(usedBytes);
        info.setInternalAvailableBytes(availableBytes);

        info.setPartitions(getPartitions());

        return info;
    }

    public List<StorageInfo.PartitionInfo> getPartitions() {
        List<StorageInfo.PartitionInfo> partitions = new ArrayList<>();

        String[] paths = {"/data", "/system", "/cache", "/vendor"};
        for (String path : paths) {
            try {
                StatFs sf = new StatFs(path);
                long total = sf.getBlockCountLong() * sf.getBlockSizeLong();
                long avail = sf.getAvailableBlocksLong() * sf.getBlockSizeLong();
                long used = total - avail;
                partitions.add(new StorageInfo.PartitionInfo(path, total, used, avail));
            } catch (Exception ignored) {}
        }
        return partitions;
    }
}
