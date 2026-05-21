package com.example.deviceinfoviewer.fragment;

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
import com.example.deviceinfoviewer.data.model.BatteryInfo;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;
import com.example.deviceinfoviewer.widget.MonitorChartView;

/**
 * 电池 Fragment — 竞品风格：大电量百分比 + 多个图表
 */
public class BatteryFragment extends Fragment {

    private DeviceRepository repo;

    private TextView tvBatteryStatus, tvBatteryPercent, tvCycleCount, tvCapacity;
    private TextView tvVoltage, tvCurrent;
    private ProgressBar pbBattery;
    private MonitorChartView chartPower, chartTemp;

    private Handler handler;
    private Runnable chartUpdater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_battery_new, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repo = DeviceApplication.getDeviceRepository();

        tvBatteryStatus = view.findViewById(R.id.tv_battery_status);
        tvBatteryPercent = view.findViewById(R.id.tv_battery_percent);
        tvCycleCount = view.findViewById(R.id.tv_cycle_count);
        tvCapacity = view.findViewById(R.id.tv_capacity);
        tvVoltage = view.findViewById(R.id.tv_voltage);
        tvCurrent = view.findViewById(R.id.tv_current);
        pbBattery = view.findViewById(R.id.pb_battery);
        chartPower = view.findViewById(R.id.chart_power);
        chartTemp = view.findViewById(R.id.chart_temp);

        if (chartPower != null) {
            chartPower.setTitle("功率 (mW)");
            chartPower.setChartColor(Color.parseColor("#4CAF50"));
            chartPower.setValueFormat("%.0f", " mW");
        }
        if (chartTemp != null) {
            chartTemp.setTitle("温度");
            chartTemp.setChartColor(Color.parseColor("#FF9800"));
            chartTemp.setValueFormat("%.1f", "°C");
        }

        if (repo == null) return;

        repo.getBatteryLiveData().observe(getViewLifecycleOwner(), bat -> {
            if (bat == null) return;
            updateBatteryInfo(bat);
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

    private void updateBatteryInfo(BatteryInfo bat) {
        // 电量
        int level = bat.getLevelPercent();
        tvBatteryPercent.setText(level >= 0 ? level + "%" : "N/A");
        pbBattery.setProgress(level >= 0 ? level : 0);

        // 充电状态
        String status = bat.getChargeStatus();
        tvBatteryStatus.setText(status != null && !status.isEmpty() ? status : "未知");

        // 循环次数
        int cycles = bat.getCycleCount();
        tvCycleCount.setText(cycles > 0 ? cycles + " 次" : "N/A");

        // 容量
        long nowCap = bat.getChargeFullMAh() > 0 ? bat.getChargeFullMAh() : bat.getCapacityNowMAh();
        tvCapacity.setText(nowCap > 0 ? nowCap + " mAh" : "N/A");

        // 电压
        int voltage = bat.getEffectiveVoltage();
        tvVoltage.setText(voltage > 0 ? voltage + " mV" : "N/A");

        // 电流
        long currentUA = bat.getCurrentNowUA();
        if (currentUA > 0) {
            tvCurrent.setText("+" + String.format("%.0f", currentUA / 1000.0) + " mA");
        } else if (currentUA < 0) {
            tvCurrent.setText(String.format("%.0f", Math.abs(currentUA) / 1000.0) + " mA");
        } else {
            tvCurrent.setText("N/A");
        }
    }

    private void updateCharts() {
        if (repo == null) return;
        if (chartPower != null) {
            chartPower.setData(repo.getHistoryCache().getSeries("battery_power"));
        }
        if (chartTemp != null) {
            chartTemp.setData(repo.getHistoryCache().getSeries("battery_temp"));
        }
    }
}
