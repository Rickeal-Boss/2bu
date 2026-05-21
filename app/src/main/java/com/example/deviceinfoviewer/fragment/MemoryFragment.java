package com.example.deviceinfoviewer.fragment;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

/**
 * 内存 Fragment — 竞品风格：进度条 + 多个图表
 */
public class MemoryFragment extends Fragment {

    private DeviceRepository repo;

    private TextView tvMemUsage, tvMemDetail, tvZramDetail;
    private ProgressBar pbMemory, pbZram;
    private MonitorChartView chartMemAvailable, chartSwap;

    private Handler handler;
    private Runnable chartUpdater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_memory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repo = DeviceApplication.getDeviceRepository();

        tvMemUsage = view.findViewById(R.id.tv_mem_usage);
        tvMemDetail = view.findViewById(R.id.tv_mem_detail);
        tvZramDetail = view.findViewById(R.id.tv_zram_detail);
        pbMemory = view.findViewById(R.id.pb_memory);
        pbZram = view.findViewById(R.id.pb_zram);
        chartMemAvailable = view.findViewById(R.id.chart_mem_available);
        chartSwap = view.findViewById(R.id.chart_swap);

        if (chartMemAvailable != null) {
            chartMemAvailable.setTitle("可用内存");
            chartMemAvailable.setChartColor(Color.parseColor("#4CAF50"));
            chartMemAvailable.setValueFormat("%.1f", " GB");
        }
        if (chartSwap != null) {
            chartSwap.setTitle("Swap 使用");
            chartSwap.setChartColor(Color.parseColor("#FF9800"));
            chartSwap.setValueFormat("%.1f", " MB");
        }

        if (repo == null) return;

        repo.getMemoryLiveData().observe(getViewLifecycleOwner(), mem -> {
            if (mem == null) return;
            updateMemoryInfo(mem);
        });

        handler = new Handler(Looper.getMainLooper());
        chartUpdater = () -> {
            updateCharts();
            handler.postDelayed(chartUpdater, 2000);
        };
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

    private void updateMemoryInfo(MemoryInfo mem) {
        if (mem.getTotalKB() <= 0) return;

        int pct = (int) ((float) mem.getUsedKB() / mem.getTotalKB() * 100);
        tvMemUsage.setText(pct + "%");
        tvMemDetail.setText(FormatUtils.formatBytes(mem.getUsedKB() * 1024L)
                + " / " + FormatUtils.formatBytes(mem.getTotalKB() * 1024L));
        pbMemory.setProgress(pct);
        pbMemory.setProgressTintList(getProgressColor(pct));

        // ZRAM
        long zramUsed = mem.getZramMemUsedTotalKB();
        long zramOrig = mem.getZramOriginalKB();
        if (zramOrig > 0) {
            int zramPct = (int) ((float) zramUsed / zramOrig * 100);
            pbZram.setProgress(zramPct);
            pbZram.setProgressTintList(getProgressColor(zramPct));
            tvZramDetail.setText(FormatUtils.formatBytes(zramUsed * 1024L)
                    + " / " + FormatUtils.formatBytes(zramOrig * 1024L)
                    + " | 压缩比 " + (mem.getCompressionRatio() > 0
                    ? String.format("%.1f:1", mem.getCompressionRatio()) : "N/A"));
        } else {
            pbZram.setProgress(0);
            tvZramDetail.setText("无 ZRAM");
        }
    }

    private void updateCharts() {
        if (repo == null) return;

        // 可用内存历史
        if (chartMemAvailable != null && repo.getMemoryLiveData().getValue() != null) {
            MemoryInfo mem = repo.getMemoryLiveData().getValue();
            float availGB = (float) (mem.getAvailableKB() * 1024L) / (1024 * 1024 * 1024);
            chartMemAvailable.addDataPoint(System.currentTimeMillis(), availGB);
        }

        // Swap 历史 - 从 HistoryCache 获取
        if (chartSwap != null) {
            chartSwap.setData(repo.getHistoryCache().getSeries("swap_used"));
        }
    }

    private ColorStateList getProgressColor(int pct) {
        int color;
        if (pct < 70) color = 0xFF4CAF50;
        else if (pct < 90) color = 0xFFFF9800;
        else color = 0xFFF44336;
        return ColorStateList.valueOf(color);
    }
}
