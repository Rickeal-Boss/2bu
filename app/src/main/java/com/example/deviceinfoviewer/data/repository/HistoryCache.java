package com.example.deviceinfoviewer.data.repository;

import com.example.deviceinfoviewer.data.model.HistoryDataPoint;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 历史数据缓存，按序列名存储数据点，自动裁剪超过1小时的数据
 */
public class HistoryCache {

    private static final long MAX_AGE_MS = 60 * 60 * 1000L; // 1小时
    private final ConcurrentHashMap<String, LinkedList<HistoryDataPoint>> cache = new ConcurrentHashMap<>();
    private ScheduledExecutorService pruneExecutor;

    public HistoryCache() {
        pruneExecutor = Executors.newSingleThreadScheduledExecutor();
        pruneExecutor.scheduleWithFixedDelay(this::prune, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * 添加数据点
     */
    public void addPoint(String seriesName, float value) {
        HistoryDataPoint point = new HistoryDataPoint(System.currentTimeMillis(), value, seriesName);
        cache.computeIfAbsent(seriesName, k -> new LinkedList<>()).add(point);
    }

    /**
     * 获取指定序列的所有数据点
     */
    public List<HistoryDataPoint> getSeries(String seriesName) {
        LinkedList<HistoryDataPoint> series = cache.get(seriesName);
        if (series == null) {
            return new LinkedList<>();
        }
        synchronized (series) {
            return new LinkedList<>(series);
        }
    }

    /**
     * 清理超过1小时的旧数据
     */
    private void prune() {
        long cutoff = System.currentTimeMillis() - MAX_AGE_MS;
        for (LinkedList<HistoryDataPoint> series : cache.values()) {
            synchronized (series) {
                Iterator<HistoryDataPoint> it = series.iterator();
                while (it.hasNext()) {
                    if (it.next().getTimestampMillis() < cutoff) {
                        it.remove();
                    } else {
                        break;
                    }
                }
            }
        }
    }

    /**
     * 清除所有数据
     */
    public void clear() {
        cache.clear();
    }

    /**
     * 关闭裁剪线程
     */
    public void shutdown() {
        if (pruneExecutor != null && !pruneExecutor.isShutdown()) {
            pruneExecutor.shutdown();
        }
    }
}
