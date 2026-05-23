package com.example.deviceinfoviewer.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.deviceinfoviewer.DeviceApplication;
import com.example.deviceinfoviewer.FormatUtils;
import com.example.deviceinfoviewer.R;
import com.example.deviceinfoviewer.adapter.SensorListAdapter;
import com.example.deviceinfoviewer.data.model.CpuCoreInfo;
import com.example.deviceinfoviewer.data.model.CpuInfo;
import com.example.deviceinfoviewer.data.model.GpuInfo;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;

/**
 * 硬件 Fragment —— CPU 核心动态列表 + GPU 详情 + 传感器列表，直接观察 Repository LiveData
 */
public class HardwareFragment extends Fragment {

    private DeviceRepository repo;

    private LinearLayout cpuCoresContainer;
    private TextView tvGpuModel, tvGpuVendor, tvGpuFreq, tvGpuTemp;
    private TextView tvGpuLoad, tvGpuFreqRange, tvGpuGovernor, tvGpuRenderer;
    private RecyclerView recyclerSensors;
    private SensorListAdapter sensorAdapter;
    private SwipeRefreshLayout swipeRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hardware, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repo = DeviceApplication.getDeviceRepository();

        cpuCoresContainer = view.findViewById(R.id.cpu_cores_container);
        tvGpuModel = view.findViewById(R.id.tv_gpu_model);
        tvGpuVendor = view.findViewById(R.id.tv_gpu_vendor);
        tvGpuFreq = view.findViewById(R.id.tv_gpu_freq);
        tvGpuTemp = view.findViewById(R.id.tv_gpu_temp);
        tvGpuLoad = view.findViewById(R.id.tv_gpu_load);
        tvGpuFreqRange = view.findViewById(R.id.tv_gpu_freq_range);
        tvGpuGovernor = view.findViewById(R.id.tv_gpu_governor);
        tvGpuRenderer = view.findViewById(R.id.tv_gpu_renderer);
        recyclerSensors = view.findViewById(R.id.recycler_sensors);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);

        // 传感器 RecyclerView
        sensorAdapter = new SensorListAdapter();
        recyclerSensors.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerSensors.setAdapter(sensorAdapter);

        if (repo == null) {
            return;
        }

        // 观察 CPU LiveData
        repo.getCpuLiveData().observe(getViewLifecycleOwner(), cpu -> {
            if (cpu == null) return;
            cpuCoresContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            for (CpuCoreInfo core : cpu.getCores()) {
                View row = inflater.inflate(R.layout.item_cpu_core, cpuCoresContainer, false);
                TextView tvName = row.findViewById(R.id.tv_core_name);
                TextView tvFreq = row.findViewById(R.id.tv_core_freq);
                TextView tvGov = row.findViewById(R.id.tv_core_gov);
                ProgressBar pbFreq = row.findViewById(R.id.pb_core_freq);

                tvName.setText("核心 " + core.getCoreIndex());

                String freqText = core.getCurrentFreqKHz() > 0
                        ? FormatUtils.formatFreq(core.getCurrentFreqKHz()) : "N/A";
                tvFreq.setText(freqText);

                tvGov.setText(core.getGovernor() != null && !core.getGovernor().isEmpty()
                        ? core.getGovernor() : "N/A");

                if (core.getMaxFreqKHz() > 0 && core.getCurrentFreqKHz() > 0) {
                    int pct = (int) (core.getCurrentFreqKHz() * 100 / core.getMaxFreqKHz());
                    pct = Math.min(pct, 100);
                    pbFreq.setProgress(pct);
                    pbFreq.setProgressTintList(
                            android.content.res.ColorStateList.valueOf(getFreqColor(pct)));
                } else {
                    pbFreq.setProgress(0);
                }

                cpuCoresContainer.addView(row);
            }
        });

        // 观察 GPU LiveData
        repo.getGpuLiveData().observe(getViewLifecycleOwner(), gpu -> {
            if (gpu == null) return;
            tvGpuModel.setText(gpu.getModel() != null && !gpu.getModel().isEmpty()
                    ? gpu.getModel() : "N/A");
            tvGpuVendor.setText(gpu.getVendor() != null && !gpu.getVendor().isEmpty()
                    ? gpu.getVendor() : "N/A");
            tvGpuFreq.setText(gpu.getFrequencyKHz() > 0
                    ? FormatUtils.formatFreq(gpu.getFrequencyKHz()) : "N/A");
            tvGpuTemp.setText(FormatUtils.formatTempCelsius(gpu.getTemperatureCelsius()));

            // 负载
            if (!Float.isNaN(gpu.getLoadPercentage()) && gpu.getLoadPercentage() > 0) {
                tvGpuLoad.setText(String.format("%.1f%%", gpu.getLoadPercentage()));
            } else {
                tvGpuLoad.setText("N/A");
            }

            // 频率范围
            StringBuilder freqRange = new StringBuilder();
            if (gpu.getMinFreqKHz() > 0) {
                freqRange.append(FormatUtils.formatFreq(gpu.getMinFreqKHz()));
                freqRange.append(" ~ ");
            } else {
                freqRange.append("? ~ ");
            }
            if (gpu.getMaxFreqKHz() > 0) {
                freqRange.append(FormatUtils.formatFreq(gpu.getMaxFreqKHz()));
            } else {
                freqRange.append("?");
            }
            tvGpuFreqRange.setText(freqRange.toString());

            // 调速器
            tvGpuGovernor.setText(gpu.getGovernor() != null && !gpu.getGovernor().isEmpty()
                    ? gpu.getGovernor() : "N/A");

            // 渲染器
            tvGpuRenderer.setText(gpu.getRenderer() != null && !gpu.getRenderer().isEmpty()
                    ? gpu.getRenderer() : "N/A");
        });

        // 观察传感器 LiveData（一次性加载）
        repo.getSensorsLiveData().observe(getViewLifecycleOwner(), sensors -> {
            if (sensors != null) {
                sensorAdapter.setSensors(sensors);
            }
        });

        swipeRefresh.setOnRefreshListener(() -> {
            swipeRefresh.setRefreshing(false);
            if (repo != null) {
                repo.loadStaticData();
            }
        });
    }

    /**
     * 根据频率百分比返回颜色
     */
    private int getFreqColor(int pct) {
        android.content.Context ctx = getContext();
        if (ctx == null) return 0xFF4CAF50;
        if (pct >= 90) return androidx.core.content.ContextCompat.getColor(ctx, R.color.status_critical);
        if (pct >= 50) return androidx.core.content.ContextCompat.getColor(ctx, R.color.status_warning);
        return androidx.core.content.ContextCompat.getColor(ctx, R.color.status_good);
    }
}
