package com.example.deviceinfoviewer.fragment;

import android.content.res.ColorStateList;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.deviceinfoviewer.DeviceApplication;
import com.example.deviceinfoviewer.FormatUtils;
import com.example.deviceinfoviewer.R;
import com.example.deviceinfoviewer.data.model.BatteryInfo;
import com.example.deviceinfoviewer.data.model.CpuCoreInfo;
import com.example.deviceinfoviewer.data.model.CpuInfo;
import com.example.deviceinfoviewer.data.model.GpuInfo;
import com.example.deviceinfoviewer.data.model.MemoryInfo;
import com.example.deviceinfoviewer.data.model.StorageInfo;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;
import com.example.deviceinfoviewer.widget.HistoryChartView;

/**
 * 仪表盘 Fragment —— 2x2 卡片网格 + 历史趋势，直接观察 Repository LiveData
 */
public class DashboardFragment extends Fragment {

    private DeviceRepository repo;

    // CPU 卡片
    private TextView tvCpuTemp, tvCpuFreq;
    // 电池卡片
    private TextView tvBatteryLevel, tvBatteryTemp;
    // RAM 卡片
    private TextView tvRamUsage, tvRamDetail;
    private ProgressBar pbRam;
    private View cardRam;
    // 存储卡片
    private TextView tvStorageUsage, tvStorageDetail;
    private ProgressBar pbStorage;
    // GPU 温度（从CPU卡片区域显示）
    private TextView tvGpuTemp;
    // 历史趋势
    private HistoryChartView chartHistory;
    private SwipeRefreshLayout swipeRefresh;
    private Handler chartHandler;
    private Runnable chartUpdater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repo = DeviceApplication.getDeviceRepository();

        // 查找所有视图
        tvCpuTemp = view.findViewById(R.id.tv_cpu_temp);
        tvCpuFreq = view.findViewById(R.id.tv_cpu_freq);
        tvBatteryLevel = view.findViewById(R.id.tv_battery_level);
        tvBatteryTemp = view.findViewById(R.id.tv_battery_temp);
        tvRamUsage = view.findViewById(R.id.tv_ram_usage);
        tvRamDetail = view.findViewById(R.id.tv_ram_detail);
        pbRam = view.findViewById(R.id.pb_ram);
        cardRam = view.findViewById(R.id.card_ram);
        tvStorageUsage = view.findViewById(R.id.tv_storage_usage);
        tvStorageDetail = view.findViewById(R.id.tv_storage_detail);
        pbStorage = view.findViewById(R.id.pb_storage);
        chartHistory = view.findViewById(R.id.chart_history);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        tvGpuTemp = view.findViewById(R.id.tv_gpu_temp);

        if (repo == null) {
            return;
        }

        // 观察 CPU LiveData
        repo.getCpuLiveData().observe(getViewLifecycleOwner(), cpu -> {
            if (cpu == null) return;
            tvCpuTemp.setText(FormatUtils.formatTempCelsius(cpu.getTemperatureCelsius()));
            long maxFreq = 0;
            for (CpuCoreInfo core : cpu.getCores()) {
                if (core.getCurrentFreqKHz() > maxFreq) {
                    maxFreq = core.getCurrentFreqKHz();
                }
            }
            tvCpuFreq.setText(maxFreq > 0 ? FormatUtils.formatFreq(maxFreq) : "N/A");
        });

        // 观察 GPU LiveData（显示 GPU 温度在 Dashboard）
        repo.getGpuLiveData().observe(getViewLifecycleOwner(), gpu -> {
            if (gpu == null) return;
            String gpuText = FormatUtils.formatTempCelsius(gpu.getTemperatureCelsius());
            if (!Float.isNaN(gpu.getLoadPercentage()) && gpu.getLoadPercentage() > 0) {
                gpuText += " | " + String.format("%.0f%%", gpu.getLoadPercentage());
            }
            tvGpuTemp.setText(gpuText);
        });

        // 观察电池 LiveData
        repo.getBatteryLiveData().observe(getViewLifecycleOwner(), bat -> {
            if (bat == null) return;
            tvBatteryLevel.setText(bat.getLevelPercent() >= 0 ? bat.getLevelPercent() + "%" : "N/A");
            tvBatteryTemp.setText(FormatUtils.formatTempCelsius(bat.getTemperatureCelsius()));
        });

        // 观察内存 LiveData
        repo.getMemoryLiveData().observe(getViewLifecycleOwner(), mem -> {
            if (mem == null || mem.getTotalKB() <= 0) {
                tvRamUsage.setText("N/A");
                tvRamDetail.setText("N/A");
                pbRam.setProgress(0);
                return;
            }
            int pct = (int) ((float) mem.getUsedKB() / mem.getTotalKB() * 100);
            tvRamUsage.setText(FormatUtils.formatBytes(mem.getUsedKB() * 1024L));
            tvRamDetail.setText(pct + "% | " + FormatUtils.formatBytes(mem.getTotalKB() * 1024L) + " 总");
            pbRam.setProgress(pct);
            pbRam.setProgressTintList(getProgressColor(pct));
        });

        // 观察存储 LiveData
        repo.getStorageLiveData().observe(getViewLifecycleOwner(), sto -> {
            if (sto == null || sto.getInternalTotalBytes() <= 0) {
                tvStorageUsage.setText("N/A");
                tvStorageDetail.setText("N/A");
                pbStorage.setProgress(0);
                return;
            }
            int pct = (int) ((float) sto.getInternalUsedBytes() / sto.getInternalTotalBytes() * 100);
            tvStorageUsage.setText(FormatUtils.formatBytes(sto.getInternalUsedBytes()));
            tvStorageDetail.setText(pct + "% | " + FormatUtils.formatBytes(sto.getInternalTotalBytes()) + " 总");
            pbStorage.setProgress(pct);
            pbStorage.setProgressTintList(getStorageColor(pct));
        });

        // 历史趋势（如果存在）
        if (chartHistory != null) {
            chartHistory.setData("CPU温度", repo.getHistoryCache().getSeries("cpu_temp"));
        }

        // RAM 卡片点击弹出 ZRAM 详情
        cardRam.setOnClickListener(v -> showZramDialog());

        // 下拉刷新
        swipeRefresh.setOnRefreshListener(() -> {
            swipeRefresh.setRefreshing(false);
            if (repo != null) {
                repo.loadStaticData();
                updateChart();
            }
        });

        // 图表定时更新 (CPU温度)
        chartHandler = new Handler(Looper.getMainLooper());
        chartUpdater = () -> {
            updateChart();
            chartHandler.postDelayed(chartUpdater, 3000);
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        if (chartHandler != null && chartUpdater != null) {
            chartHandler.post(chartUpdater);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (chartHandler != null && chartUpdater != null) {
            chartHandler.removeCallbacks(chartUpdater);
        }
    }

    private void updateChart() {
        if (repo == null || chartHistory == null) return;
        chartHistory.setData("CPU温度",
                repo.getHistoryCache().getSeries("cpu_temp"));
    }

    private ColorStateList getProgressColor(int pct) {
        int color;
        if (pct < 70) {
            color = 0xFF4CAF50;
        } else if (pct < 90) {
            color = 0xFFFF9800;
        } else {
            color = 0xFFF44336;
        }
        return ColorStateList.valueOf(color);
    }

    private ColorStateList getStorageColor(int pct) {
        int color;
        if (pct < 75) {
            color = 0xFF4CAF50;
        } else if (pct < 90) {
            color = 0xFFFF9800;
        } else {
            color = 0xFFF44336;
        }
        return ColorStateList.valueOf(color);
    }

    /**
     * 弹出 ZRAM 详情对话框
     */
    private void showZramDialog() {
        MemoryInfo memory = repo.getMemoryLiveData().getValue();
        if (memory == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("ZRAM 详情");

        StringBuilder msg = new StringBuilder();
        msg.append("原始数据: ").append(memory.getZramOriginalKB() > 0
                ? FormatUtils.formatBytes(memory.getZramOriginalKB() * 1024L) : "N/A").append("\n");
        msg.append("压缩后: ").append(memory.getZramCompressedKB() > 0
                ? FormatUtils.formatBytes(memory.getZramCompressedKB() * 1024L) : "N/A").append("\n");
        msg.append("实际占用: ").append(memory.getZramMemUsedTotalKB() > 0
                ? FormatUtils.formatBytes(memory.getZramMemUsedTotalKB() * 1024L) : "N/A").append("\n");
        msg.append("压缩比: ").append(memory.getCompressionRatio() > 0
                ? String.format("%.2f:1", memory.getCompressionRatio()) : "N/A").append("\n");
        msg.append("\n");
        msg.append("Swap 总量: ").append(memory.getSwapTotalKB() > 0
                ? FormatUtils.formatBytes(memory.getSwapTotalKB() * 1024L) : "N/A").append("\n");
        msg.append("Swap 已用: ").append(memory.getSwapUsedKB() > 0
                ? FormatUtils.formatBytes(memory.getSwapUsedKB() * 1024L) : "N/A");

        builder.setMessage(msg.toString());
        builder.setPositiveButton("确定", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
