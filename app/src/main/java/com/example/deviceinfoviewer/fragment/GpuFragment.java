package com.example.deviceinfoviewer.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.deviceinfoviewer.DeviceApplication;
import com.example.deviceinfoviewer.FormatUtils;
import com.example.deviceinfoviewer.R;
import com.example.deviceinfoviewer.data.model.GpuInfo;
import com.example.deviceinfoviewer.data.model.HistoryDataPoint;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;
import com.example.deviceinfoviewer.widget.MonitorChartView;

import java.util.ArrayList;
import java.util.List;

/**
 * GPU Fragment — 竞品风格：GPU 信息卡片 + 负载/温度图表
 */
public class GpuFragment extends Fragment {

    private DeviceRepository repo;

    private TextView tvGpuModel, tvGpuFreqHeader;
    private TextView tvGpuLoad, tvGpuTemp;
    private MonitorChartView chartGpuLoad, chartGpuTemp, chartGpuLoadHist;

    private Handler handler;
    private Runnable chartUpdater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gpu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repo = DeviceApplication.getDeviceRepository();

        tvGpuModel = view.findViewById(R.id.tv_gpu_model);
        tvGpuFreqHeader = view.findViewById(R.id.tv_gpu_freq_header);
        tvGpuLoad = view.findViewById(R.id.tv_gpu_load);
        tvGpuTemp = view.findViewById(R.id.tv_gpu_temp);
        chartGpuLoad = view.findViewById(R.id.chart_gpu_load);
        chartGpuTemp = view.findViewById(R.id.chart_gpu_temp);
        chartGpuLoadHist = view.findViewById(R.id.chart_gpu_load_hist);

        // 配置图表
        if (chartGpuLoad != null) {
            chartGpuLoad.setTitle("负载 (%)");
            chartGpuLoad.setChartColor(Color.parseColor("#4CAF50"));
            chartGpuLoad.setValueFormat("%.0f", "%");
        }
        if (chartGpuTemp != null) {
            chartGpuTemp.setTitle("温度");
            chartGpuTemp.setChartColor(Color.parseColor("#FF9800"));
            chartGpuTemp.setValueFormat("%.1f", "°C");
        }
        if (chartGpuLoadHist != null) {
            chartGpuLoadHist.setTitle("负载趋势");
            chartGpuLoadHist.setChartColor(Color.parseColor("#4CAF50"));
        }

        if (repo == null) return;

        // 观察 GPU LiveData
        repo.getGpuLiveData().observe(getViewLifecycleOwner(), gpu -> {
            if (gpu == null) return;
            updateGpuInfo(gpu);
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

    private void updateGpuInfo(GpuInfo gpu) {
        // GPU 型号
        String model = gpu.getModel();
        if (model == null || model.isEmpty()) {
            model = gpu.getVendor();
        }
        if (model == null || model.isEmpty()) {
            model = "未知 GPU";
        }
        tvGpuModel.setText(model);

        // GPU 频率
        if (gpu.getFrequencyKHz() > 0) {
            tvGpuFreqHeader.setText(FormatUtils.formatFreq(gpu.getFrequencyKHz()));
        } else {
            tvGpuFreqHeader.setText("");
        }

        // GPU 负载
        float load = gpu.getLoadPercentage();
        if (!Float.isNaN(load)) {
            tvGpuLoad.setText(String.format("%.0f%%", load));
        } else {
            tvGpuLoad.setText("N/A");
        }

        // GPU 温度
        float temp = gpu.getTemperatureCelsius();
        if (!Float.isNaN(temp)) {
            tvGpuTemp.setText(FormatUtils.formatTempCelsius(temp));
        } else {
            tvGpuTemp.setText("N/A");
        }
    }

    private void updateCharts() {
        if (repo == null) return;

        // GPU 负载历史（从历史缓存读取）
        List<HistoryDataPoint> gpuLoadData = repo.getHistoryCache().getSeries("gpu_load");
        if (gpuLoadData != null && !gpuLoadData.isEmpty()) {
            if (chartGpuLoad != null) chartGpuLoad.setData(gpuLoadData);
            if (chartGpuLoadHist != null) chartGpuLoadHist.setData(gpuLoadData);
        }

        // GPU 温度历史
        List<HistoryDataPoint> gpuTempData = repo.getHistoryCache().getSeries("gpu_temp");
        if (gpuTempData != null && !gpuTempData.isEmpty()) {
            if (chartGpuTemp != null) chartGpuTemp.setData(gpuTempData);
        }
    }
}
