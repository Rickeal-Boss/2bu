package com.example.deviceinfoviewer.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
 * CPU Fragment — 竞品风格：设备信息卡片 + 温度状态 + 图表 + Cluster/Per core 切换
 */
public class CpuFragment extends Fragment {

    private DeviceRepository repo;

    // 设备信息
    private TextView tvCpuModel, tvCpuSpec, tvTempStatus;
    private MonitorChartView chartCpuTemp;

    // 切换按钮
    private TextView tabCluster, tabPerCore;
    private LinearLayout clusterView, perCoreView;
    private boolean showPerCore = false;

    private Handler handler;
    private Runnable chartUpdater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cpu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repo = DeviceApplication.getDeviceRepository();

        tvCpuModel = view.findViewById(R.id.tv_cpu_model);
        tvCpuSpec = view.findViewById(R.id.tv_cpu_spec);
        tvTempStatus = view.findViewById(R.id.tv_temp_status);
        chartCpuTemp = view.findViewById(R.id.chart_cpu_temp);
        tabCluster = view.findViewById(R.id.tab_cluster);
        tabPerCore = view.findViewById(R.id.tab_per_core);
        clusterView = view.findViewById(R.id.cluster_view);
        perCoreView = view.findViewById(R.id.per_core_view);

        // 配置温度图表
        if (chartCpuTemp != null) {
            chartCpuTemp.setTitle("CPU 温度");
            chartCpuTemp.setChartColor(Color.parseColor("#4CAF50"));
            chartCpuTemp.setValueFormat("%.1f", "°C");
        }

        // Cluster / Per core 切换
        tabCluster.setOnClickListener(v -> switchToCluster());
        tabPerCore.setOnClickListener(v -> switchToPerCore());

        if (repo == null) return;

        // 观察 CPU LiveData
        repo.getCpuLiveData().observe(getViewLifecycleOwner(), cpu -> {
            if (cpu == null) return;
            updateCpuInfo(cpu);
            updateTempStatus(cpu);
            updateCoreViews(cpu);
        });

        // 图表定时更新
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

    private void updateCpuInfo(CpuInfo cpu) {
        // 构建 CPU 型号信息
        StringBuilder model = new StringBuilder();
        String arch = cpu.getArchitecture();
        if (arch != null && !arch.isEmpty()) {
            model.append(arch);
        }
        if (model.length() == 0) {
            model.append("未知处理器");
        }
        tvCpuModel.setText(model.toString());

        // 规格信息：核心数 | 架构
        StringBuilder spec = new StringBuilder();
        spec.append(cpu.getCoreCount()).append(" 核心");
        if (arch != null && !arch.isEmpty()) {
            spec.append(" · ").append(arch);
        }
        tvCpuSpec.setText(spec.toString());
    }

    private void updateTempStatus(CpuInfo cpu) {
        float temp = cpu.getTemperatureCelsius();
        String status;
        int color;
        if (Float.isNaN(temp)) {
            status = "未知";
            color = Color.parseColor("#9E9E9E");
        } else if (temp < 45) {
            status = "正常";
            color = Color.parseColor("#4CAF50");
        } else if (temp < 60) {
            status = "偏高";
            color = Color.parseColor("#FF9800");
        } else {
            status = "过热";
            color = Color.parseColor("#F44336");
        }
        tvTempStatus.setText(status);
        tvTempStatus.setTextColor(color);
    }

    private void updateCoreViews(CpuInfo cpu) {
        List<CpuCoreInfo> cores = cpu.getCores();
        if (cores == null || cores.isEmpty()) return;

        android.content.Context ctx = getContext();
        if (ctx == null) return;

        // 按 cluster 分组（基于 maxFreqKHz）
        Map<Long, List<CpuCoreInfo>> clusters = groupByCluster(cores);

        // 更新 Cluster 视图
        clusterView.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(ctx);

        for (Map.Entry<Long, List<CpuCoreInfo>> entry : clusters.entrySet()) {
            long maxFreq = entry.getKey();
            List<CpuCoreInfo> clusterCores = entry.getValue();

            // 确定 cluster 类型
            CpuCoreInfo firstCore = clusterCores.get(0);
            long curFreq = getAvgFreq(clusterCores);

            View clusterItem = inflater.inflate(R.layout.item_cpu_cluster, clusterView, false);

            TextView tvClusterType = clusterItem.findViewById(R.id.tv_cluster_type);
            TextView tvClusterCores = clusterItem.findViewById(R.id.tv_cluster_cores);
            TextView tvClusterFreq = clusterItem.findViewById(R.id.tv_cluster_freq);
            MonitorChartView miniChart = clusterItem.findViewById(R.id.chart_cluster_mini);

            // 设置 cluster 类型标签
            String type = getClusterType(firstCore, maxFreq);
            tvClusterType.setText(type);
            tvClusterCores.setText(clusterCores.size() + " 核心 @" + FormatUtils.formatFreq(maxFreq));
            tvClusterFreq.setText(FormatUtils.formatFreq(curFreq));

            clusterView.addView(clusterItem);
        }

        // 更新 Per Core 视图
        perCoreView.removeAllViews();
        for (CpuCoreInfo core : cores) {
            View coreItem = inflater.inflate(R.layout.item_cpu_core_bar, perCoreView, false);

            TextView tvCoreName = coreItem.findViewById(R.id.tv_core_name);
            TextView tvCoreFreq = coreItem.findViewById(R.id.tv_core_freq);
            View barFill = coreItem.findViewById(R.id.view_core_bar_fill);

            tvCoreName.setText("核心 " + core.getCoreIndex());
            tvCoreFreq.setText(FormatUtils.formatFreq(core.getCurrentFreqKHz()));

            // 设置条形图
            float ratio = core.getMaxFreqKHz() > 0
                    ? (float) core.getCurrentFreqKHz() / core.getMaxFreqKHz() : 0f;
            ratio = Math.min(ratio, 1.0f);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) barFill.getLayoutParams();
            int maxWidth = perCoreView.getWidth() - dpToPx(160);
            if (maxWidth <= 0) maxWidth = dpToPx(200);
            params.width = (int) (maxWidth * ratio);
            barFill.setLayoutParams(params);

            // 颜色根据频率
            int barColor = getFreqColor(core.getCurrentFreqKHz());
            barFill.setBackgroundColor(barColor);

            perCoreView.addView(coreItem);
        }
    }

    private void updateCharts() {
        if (repo == null) return;
        if (chartCpuTemp != null) {
            List<HistoryDataPoint> tempData = repo.getHistoryCache().getSeries("cpu_temp");
            if (tempData != null && !tempData.isEmpty()) {
                chartCpuTemp.setData(tempData);
            }
        }
    }

    private void switchToCluster() {
        showPerCore = false;
        tabCluster.setBackgroundResource(R.drawable.bg_tab_left);
        tabCluster.setTextColor(Color.WHITE);
        tabPerCore.setBackgroundResource(R.drawable.bg_tab_right);
        tabPerCore.setTextColor(Color.parseColor("#212121"));
        clusterView.setVisibility(View.VISIBLE);
        perCoreView.setVisibility(View.GONE);
    }

    private void switchToPerCore() {
        showPerCore = true;
        tabPerCore.setBackgroundResource(R.drawable.bg_tab_left);
        tabPerCore.setTextColor(Color.WHITE);
        tabCluster.setBackgroundResource(R.drawable.bg_tab_right);
        tabCluster.setTextColor(Color.parseColor("#212121"));
        perCoreView.setVisibility(View.VISIBLE);
        clusterView.setVisibility(View.GONE);
    }

    // ---- 工具方法 ----

    private Map<Long, List<CpuCoreInfo>> groupByCluster(List<CpuCoreInfo> cores) {
        Map<Long, List<CpuCoreInfo>> map = new HashMap<>();
        for (CpuCoreInfo core : cores) {
            long maxFreq = core.getMaxFreqKHz();
            // 将相近频率归为同一 cluster（误差范围内）
            Long key = findClusterKey(map.keySet(), maxFreq);
            if (key == null) {
                key = maxFreq;
                map.put(key, new ArrayList<>());
            }
            map.get(key).add(core);
        }
        return map;
    }

    private Long findClusterKey(Iterable<Long> keys, long freq) {
        for (Long key : keys) {
            if (Math.abs(key - freq) <= 100000L) { // 100MHz 容差
                return key;
            }
        }
        return null;
    }

    private long getAvgFreq(List<CpuCoreInfo> cores) {
        long sum = 0;
        int count = 0;
        for (CpuCoreInfo c : cores) {
            if (c.getCurrentFreqKHz() > 0) {
                sum += c.getCurrentFreqKHz();
                count++;
            }
        }
        return count > 0 ? sum / count : 0;
    }

    private String getClusterType(CpuCoreInfo core, long maxFreq) {
        // 基于频率判断类型
        if (maxFreq < 1_200_000L) return "Efficiency";
        if (maxFreq < 2_200_000L) return "Performance";
        return "Prime";
    }

    private int getFreqColor(long freqKHz) {
        if (freqKHz <= 0) return Color.GRAY;
        if (freqKHz < 1_500_000L) return Color.parseColor("#4CAF50");
        if (freqKHz < 2_500_000L) return Color.parseColor("#FFC107");
        return Color.parseColor("#F44336");
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density);
    }
}
