package com.example.deviceinfoviewer.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.deviceinfoviewer.DeviceApplication;
import com.example.deviceinfoviewer.FormatUtils;
import com.example.deviceinfoviewer.R;
import com.example.deviceinfoviewer.data.model.CpuCoreInfo;
import com.example.deviceinfoviewer.data.model.CpuInfo;
import com.example.deviceinfoviewer.data.model.HistoryDataPoint;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;
import com.example.deviceinfoviewer.widget.MonitorChartView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CPU Fragment — 设备信息卡片 + 温度状态 + 图表 + Cluster/Per core 核心状态
 */
public class CpuFragment extends Fragment {

    private static final String TAG = "CpuFragment";
    private DeviceRepository repo;

    private TextView tvCpuModel, tvCpuSpec, tvTempStatus;
    private MonitorChartView chartCpuTemp;
    private TextView tabCluster, tabPerCore;
    private LinearLayout clusterView, perCoreView;
    private Handler handler;
    private Runnable chartUpdater;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.fragment_cpu_minimal, container, false);
        } catch (Exception e) {
            Log.e(TAG, "onCreateView failed", e);
            TextView fallback = new TextView(getContext() != null ? getContext() : inflater.getContext());
            fallback.setText("CPU 页面加载失败");
            fallback.setPadding(48, 48, 48, 48);
            return fallback;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            repo = DeviceApplication.getDeviceRepository();

            tvCpuModel = view.findViewById(R.id.tv_cpu_model);
            tvCpuSpec = view.findViewById(R.id.tv_cpu_spec);
            tvTempStatus = view.findViewById(R.id.tv_temp_status);
            chartCpuTemp = view.findViewById(R.id.chart_cpu_temp);
            tabCluster = view.findViewById(R.id.tab_cluster);
            tabPerCore = view.findViewById(R.id.tab_per_core);
            clusterView = view.findViewById(R.id.cluster_view);
            perCoreView = view.findViewById(R.id.per_core_view);

            if (chartCpuTemp != null) {
                chartCpuTemp.setTitle("CPU 温度");
                chartCpuTemp.setValueFormat("%.1f", "°C");
            }

            if (tabCluster != null) tabCluster.setOnClickListener(v -> switchToCluster());
            if (tabPerCore != null) tabPerCore.setOnClickListener(v -> switchToPerCore());

            if (repo == null) return;

            repo.getCpuLiveData().observe(getViewLifecycleOwner(), cpu -> {
                if (cpu != null) {
                    updateCpuInfo(cpu);
                    updateTempStatus(cpu);
                    updateCoreViews(cpu);
                }
            });

            handler = new Handler(Looper.getMainLooper());
            chartUpdater = () -> {
                updateCharts();
                if (handler != null) handler.postDelayed(chartUpdater, 2000);
            };
        } catch (Exception e) {
            Log.e(TAG, "onViewCreated failed", e);
        }
    }

    @Override public void onResume() {
        super.onResume();
        if (handler != null && chartUpdater != null) handler.post(chartUpdater);
    }

    @Override public void onPause() {
        super.onPause();
        if (handler != null && chartUpdater != null) handler.removeCallbacks(chartUpdater);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    private void updateCpuInfo(CpuInfo cpu) {
        String arch = cpu.getArchitecture();
        StringBuilder model = new StringBuilder();
        if (arch != null && !arch.isEmpty()) model.append(arch);
        if (model.length() == 0) model.append("未知处理器");
        if (tvCpuModel != null) tvCpuModel.setText(model.toString());

        StringBuilder spec = new StringBuilder();
        spec.append(cpu.getCoreCount()).append(" 核心");
        if (arch != null && !arch.isEmpty()) spec.append(" · ").append(arch);
        if (tvCpuSpec != null) tvCpuSpec.setText(spec.toString());
    }

    private void updateTempStatus(CpuInfo cpu) {
        if (tvTempStatus == null) return;
        float temp = cpu.getTemperatureCelsius();
        if (Float.isNaN(temp)) {
            tvTempStatus.setText("未知");
            tvTempStatus.setTextColor(Color.parseColor("#9E9E9E"));
        } else {
            tvTempStatus.setText(String.format("%.1f°C", temp));
            if (temp < 45) tvTempStatus.setTextColor(Color.parseColor("#4CAF50"));
            else if (temp < 60) tvTempStatus.setTextColor(Color.parseColor("#FF9800"));
            else tvTempStatus.setTextColor(Color.parseColor("#F44336"));
        }
    }

    private void updateCoreViews(CpuInfo cpu) {
        List<CpuCoreInfo> cores = cpu.getCores();
        if (cores == null || cores.isEmpty()) return;
        android.content.Context ctx = getContext();
        if (ctx == null) return;

        Map<Long, List<CpuCoreInfo>> clusters = groupByCluster(cores);

        if (clusterView != null) {
            clusterView.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(ctx);
            for (Map.Entry<Long, List<CpuCoreInfo>> e : clusters.entrySet()) {
                List<CpuCoreInfo> clusterCores = e.getValue();
                View item = inflater.inflate(R.layout.item_cpu_cluster, clusterView, false);

                TextView tvType = item.findViewById(R.id.tv_cluster_type);
                TextView tvCores = item.findViewById(R.id.tv_cluster_cores);
                TextView tvFreq = item.findViewById(R.id.tv_cluster_freq);

                if (tvType != null) tvType.setText(getClusterType(e.getKey()));
                if (tvCores != null) tvCores.setText(clusterCores.size() + " 核心 · 最高 " + FormatUtils.formatFreq(e.getKey()));
                if (tvFreq != null) tvFreq.setText(FormatUtils.formatFreq(getAvgFreq(clusterCores)));

                clusterView.addView(item);
            }
        }

        if (perCoreView != null) {
            perCoreView.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(ctx);
            for (CpuCoreInfo core : cores) {
                View item = inflater.inflate(R.layout.item_cpu_core_bar, perCoreView, false);

                TextView tvName = item.findViewById(R.id.tv_core_name);
                TextView tvFreq = item.findViewById(R.id.tv_core_freq);
                View barFill = item.findViewById(R.id.view_core_bar_fill);

                if (tvName != null) tvName.setText("核心 " + core.getCoreIndex());
                if (tvFreq != null) tvFreq.setText(FormatUtils.formatFreq(core.getCurrentFreqKHz()));

                if (barFill != null) {
                    float ratio = core.getMaxFreqKHz() > 0 ? (float) core.getCurrentFreqKHz() / core.getMaxFreqKHz() : 0f;
                    ratio = Math.min(ratio, 1.0f);
                    int maxW = perCoreView.getWidth() - dpToPx(160);
                    if (maxW <= 0) maxW = dpToPx(200);
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) barFill.getLayoutParams();
                    lp.width = (int) (maxW * ratio);
                    barFill.setLayoutParams(lp);
                    barFill.setBackgroundColor(getFreqColor(core.getCurrentFreqKHz()));
                }
                perCoreView.addView(item);
            }
        }
    }

    private void updateCharts() {
        if (repo == null || chartCpuTemp == null) return;
        List<HistoryDataPoint> data = repo.getHistoryCache().getSeries("cpu_temp");
        if (data != null && !data.isEmpty()) chartCpuTemp.setData(data);
    }

    private void switchToCluster() {
        if (tabCluster != null) { tabCluster.setBackgroundResource(R.drawable.bg_tab_left); tabCluster.setTextColor(Color.WHITE); }
        if (tabPerCore != null) { tabPerCore.setBackgroundResource(R.drawable.bg_tab_right); tabPerCore.setTextColor(Color.parseColor("#212121")); }
        if (clusterView != null) clusterView.setVisibility(View.VISIBLE);
        if (perCoreView != null) perCoreView.setVisibility(View.GONE);
    }

    private void switchToPerCore() {
        if (tabPerCore != null) { tabPerCore.setBackgroundResource(R.drawable.bg_tab_left); tabPerCore.setTextColor(Color.WHITE); }
        if (tabCluster != null) { tabCluster.setBackgroundResource(R.drawable.bg_tab_right); tabCluster.setTextColor(Color.parseColor("#212121")); }
        if (perCoreView != null) perCoreView.setVisibility(View.VISIBLE);
        if (clusterView != null) clusterView.setVisibility(View.GONE);
    }

    private Map<Long, List<CpuCoreInfo>> groupByCluster(List<CpuCoreInfo> cores) {
        Map<Long, List<CpuCoreInfo>> map = new HashMap<>();
        for (CpuCoreInfo c : cores) {
            Long key = null;
            for (Long k : map.keySet()) if (Math.abs(k - c.getMaxFreqKHz()) <= 100000L) { key = k; break; }
            if (key == null) { key = c.getMaxFreqKHz(); map.put(key, new ArrayList<>()); }
            map.get(key).add(c);
        }
        return map;
    }

    private long getAvgFreq(List<CpuCoreInfo> cores) {
        long sum = 0; int cnt = 0;
        for (CpuCoreInfo c : cores) { if (c.getCurrentFreqKHz() > 0) { sum += c.getCurrentFreqKHz(); cnt++; } }
        return cnt > 0 ? sum / cnt : 0;
    }

    private String getClusterType(long maxFreq) {
        if (maxFreq < 1200000L) return "Efficiency";
        if (maxFreq < 2200000L) return "Performance";
        return "Prime";
    }

    private int getFreqColor(long khz) {
        if (khz <= 0) return Color.GRAY;
        if (khz < 1500000L) return Color.parseColor("#4CAF50");
        if (khz < 2500000L) return Color.parseColor("#FFC107");
        return Color.parseColor("#F44336");
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
