package com.example.deviceinfoviewer.fragment;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.deviceinfoviewer.DeviceApplication;
import com.example.deviceinfoviewer.FormatUtils;
import com.example.deviceinfoviewer.R;
import com.example.deviceinfoviewer.data.model.MemoryInfo;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;
import com.example.deviceinfoviewer.widget.MonitorChartView;

public class MemoryFragment extends Fragment {

    private static final String TAG = "MemoryFragment";
    private DeviceRepository repo;

    // 内存概览
    private TextView tvMemUsage, tvMemTotal, tvMemUsed, tvMemAvailable;
    private ProgressBar pbMemory;

    // ZRAM
    private TextView tvZramTitle, tvZramDetail;
    private ProgressBar pbZram;

    // Swap
    private TextView tvSwapTitle, tvSwapDetail;
    private ProgressBar pbSwap;

    // 图表
    private MonitorChartView chartMemAvailable, chartMemUsed;

    private Handler handler;
    private Runnable chartUpdater;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.fragment_memory, container, false);
        } catch (Exception e) {
            Log.e(TAG, "onCreateView failed", e);
            return new TextView(getContext());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            repo = DeviceApplication.getDeviceRepository();

            tvMemUsage = view.findViewById(R.id.tv_mem_usage);
            tvMemTotal = view.findViewById(R.id.tv_mem_total);
            tvMemUsed = view.findViewById(R.id.tv_mem_used);
            tvMemAvailable = view.findViewById(R.id.tv_mem_available);
            pbMemory = view.findViewById(R.id.pb_memory);

            tvZramTitle = view.findViewById(R.id.tv_zram_title);
            tvZramDetail = view.findViewById(R.id.tv_zram_detail);
            pbZram = view.findViewById(R.id.pb_zram);

            tvSwapTitle = view.findViewById(R.id.tv_swap_title);
            tvSwapDetail = view.findViewById(R.id.tv_swap_detail);
            pbSwap = view.findViewById(R.id.pb_swap);

            chartMemAvailable = view.findViewById(R.id.chart_mem_available);
            chartMemUsed = view.findViewById(R.id.chart_mem_used);

            if (chartMemAvailable != null) {
                chartMemAvailable.setTitle("可用");
                chartMemAvailable.setChartColor(Color.parseColor("#4CAF50"));
                chartMemAvailable.setValueFormat("%.1f", " GB");
            }
            if (chartMemUsed != null) {
                chartMemUsed.setTitle("已用");
                chartMemUsed.setChartColor(Color.parseColor("#FF9800"));
                chartMemUsed.setValueFormat("%.1f", " GB");
            }

            if (repo == null) return;

            repo.getMemoryLiveData().observe(getViewLifecycleOwner(), mem -> {
                if (mem != null && mem.getTotalKB() > 0) {
                    updateMemoryInfo(mem);
                }
            });

            handler = new Handler(Looper.getMainLooper());
            chartUpdater = new Runnable() {
                @Override
                public void run() {
                    updateCharts();
                    if (handler != null) handler.postDelayed(this, 2000);
                }
            };
        } catch (Exception e) {
            Log.e(TAG, "onViewCreated failed", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (handler != null && chartUpdater != null) {
            handler.post(chartUpdater);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (handler != null && chartUpdater != null) {
            handler.removeCallbacks(chartUpdater);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
        chartUpdater = null;
    }

    private void updateMemoryInfo(MemoryInfo mem) {
        long totalKB = mem.getTotalKB();
        long usedKB = mem.getUsedKB();
        long availKB = mem.getAvailableKB();
        long totalBytes = totalKB * 1024L;
        long usedBytes = usedKB * 1024L;
        long availBytes = availKB * 1024L;

        // 使用率百分比
        int pct = (int) ((float) usedKB / totalKB * 100);
        if (tvMemUsage != null) tvMemUsage.setText(pct + "%");
        if (tvMemTotal != null) tvMemTotal.setText(FormatUtils.formatBytes(totalBytes));
        if (tvMemUsed != null) tvMemUsed.setText(FormatUtils.formatBytes(usedBytes));
        if (tvMemAvailable != null) tvMemAvailable.setText(FormatUtils.formatBytes(availBytes));
        if (pbMemory != null) {
            pbMemory.setProgress(pct);
            pbMemory.setProgressTintList(getProgressColor(pct));
        }

        // ZRAM
        long zramOrigKB = mem.getZramOriginalKB();
        long zramUsedKB = mem.getZramMemUsedTotalKB();
        long zramCompKB = mem.getZramCompressedKB();
        float compRatio = mem.getCompressionRatio();

        if (zramOrigKB > 0) {
            int zramPct = (int) ((float) zramUsedKB / zramOrigKB * 100);
            if (tvZramTitle != null) {
                tvZramTitle.setText(FormatUtils.formatBytes(zramUsedKB * 1024L));
            }
            if (pbZram != null) {
                pbZram.setProgress(zramPct);
                pbZram.setProgressTintList(getProgressColor(zramPct));
            }
            StringBuilder zramInfo = new StringBuilder();
            zramInfo.append("原始 ").append(FormatUtils.formatBytes(zramOrigKB * 1024L));
            if (zramCompKB > 0) {
                zramInfo.append(" | 压缩 ").append(FormatUtils.formatBytes(zramCompKB * 1024L));
            }
            if (compRatio > 0) {
                zramInfo.append(" | 比 ").append(String.format("%.1f:1", compRatio));
            }
            if (tvZramDetail != null) tvZramDetail.setText(zramInfo.toString());
        } else {
            if (tvZramTitle != null) tvZramTitle.setText("无 ZRAM");
            if (pbZram != null) pbZram.setProgress(0);
            if (tvZramDetail != null) tvZramDetail.setText("");
        }

        // Swap
        long swapTotalKB = mem.getSwapTotalKB();
        long swapUsedKB = mem.getSwapUsedKB();
        if (swapTotalKB > 0) {
            int swapPct = (int) ((float) swapUsedKB / swapTotalKB * 100);
            if (tvSwapTitle != null) {
                tvSwapTitle.setText(FormatUtils.formatBytes(swapUsedKB * 1024L));
            }
            if (pbSwap != null) {
                pbSwap.setProgress(swapPct);
                pbSwap.setProgressTintList(getProgressColor(swapPct));
            }
            if (tvSwapDetail != null) {
                tvSwapDetail.setText("总量 " + FormatUtils.formatBytes(swapTotalKB * 1024L));
            }
        } else {
            if (tvSwapTitle != null) tvSwapTitle.setText("无 Swap");
            if (pbSwap != null) pbSwap.setProgress(0);
            if (tvSwapDetail != null) tvSwapDetail.setText("");
        }
    }

    private void updateCharts() {
        if (repo == null) return;
        MemoryInfo mem = repo.getMemoryLiveData().getValue();
        if (mem == null || mem.getTotalKB() <= 0) return;

        long now = System.currentTimeMillis();
        float availGB = mem.getAvailableKB() * 1024f / (1024f * 1024f * 1024f);
        float usedGB = mem.getUsedKB() * 1024f / (1024f * 1024f * 1024f);

        if (chartMemAvailable != null) {
            chartMemAvailable.addDataPoint(now, availGB);
        }
        if (chartMemUsed != null) {
            chartMemUsed.addDataPoint(now, usedGB);
        }
    }

    private ColorStateList getProgressColor(int pct) {
        int c;
        if (pct < 70) c = 0xFF4CAF50;
        else if (pct < 90) c = 0xFFFF9800;
        else c = 0xFFF44336;
        return ColorStateList.valueOf(c);
    }
}
