package com.example.deviceinfoviewer.fragment;

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
import com.example.deviceinfoviewer.data.model.BatteryInfo;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;
import com.example.deviceinfoviewer.widget.MonitorChartView;

public class BatteryFragment extends Fragment {

    private static final String TAG = "BatteryFragment";
    private DeviceRepository repo;
    private TextView tvBatteryStatus, tvBatteryPercent, tvCycleCount, tvCapacity, tvVoltage, tvCurrent;
    private ProgressBar pbBattery;
    private MonitorChartView chartPower, chartTemp;
    private Handler handler;
    private Runnable chartUpdater;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try { return inflater.inflate(R.layout.fragment_battery_new, container, false); }
        catch (Exception e) { Log.e(TAG, "onCreateView failed", e); TextView fb = new TextView(getContext() != null ? getContext() : inflater.getContext()); fb.setText("页面加载失败"); fb.setPadding(48,48,48,48); return fb; }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
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

            if (chartPower != null) { chartPower.setTitle("功率 (mW)"); chartPower.setValueFormat("%.0f", " mW"); }
            if (chartTemp != null) { chartTemp.setTitle("温度"); chartTemp.setChartColor(Color.parseColor("#FF9800")); chartTemp.setValueFormat("%.1f", "°C"); }

            if (repo == null) return;

            repo.getBatteryLiveData().observe(getViewLifecycleOwner(), bat -> {
                if (bat != null) updateBatteryInfo(bat);
            });

            handler = new Handler(Looper.getMainLooper());
            chartUpdater = () -> { updateCharts(); if (handler != null) handler.postDelayed(chartUpdater, 2000); };
        } catch (Exception e) { Log.e(TAG, "onViewCreated failed", e); }
    }

    @Override public void onResume() { super.onResume(); if (handler != null && chartUpdater != null) handler.post(chartUpdater); }
    @Override public void onPause() { super.onPause(); if (handler != null && chartUpdater != null) handler.removeCallbacks(chartUpdater); }
    @Override public void onDestroyView() { super.onDestroyView(); if (handler != null) { handler.removeCallbacksAndMessages(null); handler = null; } }

    private void updateBatteryInfo(BatteryInfo bat) {
        int level = bat.getLevelPercent();
        if (tvBatteryPercent != null) tvBatteryPercent.setText(level >= 0 ? level + "%" : "N/A");
        if (pbBattery != null) pbBattery.setProgress(Math.max(0, level));

        String status = bat.getChargeStatus();
        if (tvBatteryStatus != null) tvBatteryStatus.setText((status != null && !status.isEmpty()) ? status : "未知");

        int cycles = bat.getCycleCount();
        if (tvCycleCount != null) tvCycleCount.setText(cycles > 0 ? cycles + " 次" : "N/A");

        long nowCap = bat.getChargeFullMAh() > 0 ? bat.getChargeFullMAh() : bat.getCapacityNowMAh();
        if (tvCapacity != null) tvCapacity.setText(nowCap > 0 ? nowCap + " mAh" : "N/A");

        int voltage = bat.getEffectiveVoltage();
        if (tvVoltage != null) tvVoltage.setText(voltage > 0 ? voltage + " mV" : "N/A");

        long currentUA = bat.getCurrentNowUA();
        if (tvCurrent != null) {
            if (currentUA > 0) tvCurrent.setText(String.format("+%.0f mA", currentUA / 1000.0));
            else if (currentUA < 0) tvCurrent.setText(String.format("%.0f mA", Math.abs(currentUA) / 1000.0));
            else tvCurrent.setText("N/A");
        }
    }

    private void updateCharts() {
        if (repo == null) return;
        if (chartPower != null) chartPower.setData(repo.getHistoryCache().getSeries("battery_power"));
        if (chartTemp != null) chartTemp.setData(repo.getHistoryCache().getSeries("battery_temp"));
    }
}
