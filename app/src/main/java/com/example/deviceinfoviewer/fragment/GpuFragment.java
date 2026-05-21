package com.example.deviceinfoviewer.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

import java.util.List;

public class GpuFragment extends Fragment {

    private static final String TAG = "GpuFragment";
    private DeviceRepository repo;
    private TextView tvGpuModel, tvGpuFreqHeader, tvGpuLoad, tvGpuTemp;
    private TextView tvGpuVendor, tvGpuRenderer, tvGpuGovernor, tvGpuFreq, tvGpuFreqRange;
    private MonitorChartView chartGpuLoad, chartGpuTemp;
    private Handler handler;
    private Runnable chartUpdater;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try { return inflater.inflate(R.layout.fragment_gpu, container, false); }
        catch (Exception e) { Log.e(TAG, "onCreateView failed", e); return new TextView(getContext()); }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            repo = DeviceApplication.getDeviceRepository();
            tvGpuModel = view.findViewById(R.id.tv_gpu_model);
            tvGpuFreqHeader = view.findViewById(R.id.tv_gpu_freq_header);
            tvGpuLoad = view.findViewById(R.id.tv_gpu_load);
            tvGpuTemp = view.findViewById(R.id.tv_gpu_temp);
            tvGpuVendor = view.findViewById(R.id.tv_gpu_vendor);
            tvGpuRenderer = view.findViewById(R.id.tv_gpu_renderer);
            tvGpuGovernor = view.findViewById(R.id.tv_gpu_governor);
            tvGpuFreq = view.findViewById(R.id.tv_gpu_freq);
            tvGpuFreqRange = view.findViewById(R.id.tv_gpu_freq_range);
            chartGpuLoad = view.findViewById(R.id.chart_gpu_load);
            chartGpuTemp = view.findViewById(R.id.chart_gpu_temp);

            if (chartGpuLoad != null) { chartGpuLoad.setTitle("负载 (%)"); chartGpuLoad.setValueFormat("%.0f", "%"); }
            if (chartGpuTemp != null) { chartGpuTemp.setTitle("温度"); chartGpuTemp.setChartColor(Color.parseColor("#FF9800")); chartGpuTemp.setValueFormat("%.1f", "°C"); }

            if (repo == null) return;

            repo.getGpuLiveData().observe(getViewLifecycleOwner(), gpu -> {
                if (gpu != null) updateGpuInfo(gpu);
            });

            handler = new Handler(Looper.getMainLooper());
            chartUpdater = () -> { updateCharts(); if (handler != null) handler.postDelayed(chartUpdater, 2000); };
        } catch (Exception e) { Log.e(TAG, "onViewCreated failed", e); }
    }

    @Override public void onResume() { super.onResume(); if (handler != null && chartUpdater != null) handler.post(chartUpdater); }
    @Override public void onPause() { super.onPause(); if (handler != null && chartUpdater != null) handler.removeCallbacks(chartUpdater); }
    @Override public void onDestroyView() { super.onDestroyView(); if (handler != null) { handler.removeCallbacksAndMessages(null); handler = null; } }

    private void updateGpuInfo(GpuInfo gpu) {
        // 型号（优先 GPU model，其次 vendor）
        String model = gpu.getModel();
        if (model == null || model.isEmpty()) model = gpu.getVendor();
        if (model == null || model.isEmpty()) model = "未知 GPU";
        if (tvGpuModel != null) tvGpuModel.setText(model);

        // 头部信息行：频率 + 负载简要
        StringBuilder headerInfo = new StringBuilder();
        if (gpu.getFrequencyKHz() > 0) headerInfo.append(FormatUtils.formatFreq(gpu.getFrequencyKHz()));
        float load = gpu.getLoadPercentage();
        if (!Float.isNaN(load)) {
            if (headerInfo.length() > 0) headerInfo.append(" · ");
            headerInfo.append(String.format("%.0f%%", load));
        }
        if (tvGpuFreqHeader != null) tvGpuFreqHeader.setText(headerInfo.toString());

        // 负载大字
        if (tvGpuLoad != null) tvGpuLoad.setText(Float.isNaN(load) ? "N/A" : String.format("%.0f%%", load));

        // 温度大字
        float temp = gpu.getTemperatureCelsius();
        if (tvGpuTemp != null) tvGpuTemp.setText(Float.isNaN(temp) ? "N/A" : FormatUtils.formatTempCelsius(temp));

        // 详细信息
        String vendor = gpu.getVendor();
        if (tvGpuVendor != null) tvGpuVendor.setText((vendor != null && !vendor.isEmpty()) ? vendor : "N/A");

        String renderer = gpu.getRenderer();
        if (tvGpuRenderer != null) tvGpuRenderer.setText((renderer != null && !renderer.isEmpty()) ? renderer : "N/A");

        String governor = gpu.getGovernor();
        if (tvGpuGovernor != null) tvGpuGovernor.setText((governor != null && !governor.isEmpty()) ? governor : "N/A");

        if (tvGpuFreq != null) tvGpuFreq.setText(gpu.getFrequencyKHz() > 0 ? FormatUtils.formatFreq(gpu.getFrequencyKHz()) : "N/A");

        String range = "";
        if (gpu.getMinFreqKHz() > 0 && gpu.getMaxFreqKHz() > 0) {
            range = FormatUtils.formatFreq(gpu.getMinFreqKHz()) + " - " + FormatUtils.formatFreq(gpu.getMaxFreqKHz());
        }
        if (tvGpuFreqRange != null) tvGpuFreqRange.setText(!range.isEmpty() ? range : "N/A");
    }

    private void updateCharts() {
        if (repo == null) return;
        List<HistoryDataPoint> loadData = repo.getHistoryCache().getSeries("gpu_load");
        if (loadData != null && !loadData.isEmpty() && chartGpuLoad != null) chartGpuLoad.setData(loadData);
        List<HistoryDataPoint> tempData = repo.getHistoryCache().getSeries("gpu_temp");
        if (tempData != null && !tempData.isEmpty() && chartGpuTemp != null) chartGpuTemp.setData(tempData);
    }
}
