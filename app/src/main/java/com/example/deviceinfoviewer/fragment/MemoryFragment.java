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
    private TextView tvMemUsage, tvMemDetail, tvZramDetail;
    private ProgressBar pbMemory, pbZram;
    private MonitorChartView chartMemAvailable, chartSwap;
    private Handler handler;
    private Runnable chartUpdater;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try { return inflater.inflate(R.layout.fragment_memory, container, false); }
        catch (Exception e) { Log.e(TAG, "onCreateView failed", e); return new TextView(getContext()); }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            repo = DeviceApplication.getDeviceRepository();
            tvMemUsage = view.findViewById(R.id.tv_mem_usage);
            tvMemDetail = view.findViewById(R.id.tv_mem_detail);
            tvZramDetail = view.findViewById(R.id.tv_zram_detail);
            pbMemory = view.findViewById(R.id.pb_memory);
            pbZram = view.findViewById(R.id.pb_zram);
            chartMemAvailable = view.findViewById(R.id.chart_mem_available);
            chartSwap = view.findViewById(R.id.chart_swap);

            if (chartMemAvailable != null) { chartMemAvailable.setTitle("可用内存"); chartMemAvailable.setValueFormat("%.1f", " GB"); }
            if (chartSwap != null) { chartSwap.setTitle("Swap"); chartSwap.setChartColor(Color.parseColor("#FF9800")); chartSwap.setValueFormat("%.0f", " MB"); }

            if (repo == null) return;

            repo.getMemoryLiveData().observe(getViewLifecycleOwner(), mem -> {
                if (mem != null) updateMemoryInfo(mem);
            });

            handler = new Handler(Looper.getMainLooper());
            chartUpdater = () -> { updateCharts(); if (handler != null) handler.postDelayed(chartUpdater, 2000); };
        } catch (Exception e) { Log.e(TAG, "onViewCreated failed", e); }
    }

    @Override public void onResume() { super.onResume(); if (handler != null && chartUpdater != null) handler.post(chartUpdater); }
    @Override public void onPause() { super.onPause(); if (handler != null && chartUpdater != null) handler.removeCallbacks(chartUpdater); }
    @Override public void onDestroyView() { super.onDestroyView(); if (handler != null) { handler.removeCallbacksAndMessages(null); handler = null; } }

    private void updateMemoryInfo(MemoryInfo mem) {
        if (mem.getTotalKB() <= 0) return;
        int pct = (int) ((float) mem.getUsedKB() / mem.getTotalKB() * 100);
        if (tvMemUsage != null) tvMemUsage.setText(pct + "%");
        if (tvMemDetail != null) tvMemDetail.setText(FormatUtils.formatBytes(mem.getUsedKB() * 1024L)
                + " / " + FormatUtils.formatBytes(mem.getTotalKB() * 1024L)
                + " | 可用 " + FormatUtils.formatBytes(mem.getAvailableKB() * 1024L));
        if (pbMemory != null) { pbMemory.setProgress(pct); pbMemory.setProgressTintList(getProgressColor(pct)); }

        long zramUsed = mem.getZramMemUsedTotalKB();
        long zramOrig = mem.getZramOriginalKB();
        if (zramOrig > 0) {
            int zramPct = (int) ((float) zramUsed / zramOrig * 100);
            if (pbZram != null) { pbZram.setProgress(zramPct); pbZram.setProgressTintList(getProgressColor(zramPct)); }
            if (tvZramDetail != null) tvZramDetail.setText(FormatUtils.formatBytes(zramUsed * 1024L)
                    + " / " + FormatUtils.formatBytes(zramOrig * 1024L)
                    + (mem.getCompressionRatio() > 0 ? " | 压缩 " + String.format("%.1f:1", mem.getCompressionRatio()) : ""));
        }
    }

    private void updateCharts() {
        if (repo == null) return;
        MemoryInfo mem = repo.getMemoryLiveData().getValue();
        if (mem != null && chartMemAvailable != null) {
            float availGB = mem.getAvailableKB() * 1024f / (1024 * 1024 * 1024);
            chartMemAvailable.addDataPoint(System.currentTimeMillis(), availGB);
        }
    }

    private ColorStateList getProgressColor(int pct) {
        int c = pct < 70 ? 0xFF4CAF50 : pct < 90 ? 0xFFFF9800 : 0xFFF44336;
        return ColorStateList.valueOf(c);
    }
}
