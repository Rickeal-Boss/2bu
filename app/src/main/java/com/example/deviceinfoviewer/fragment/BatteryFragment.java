package com.example.deviceinfoviewer.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.deviceinfoviewer.AppSettings;
import com.example.deviceinfoviewer.DeviceApplication;
import com.example.deviceinfoviewer.FormatUtils;
import com.example.deviceinfoviewer.R;
import com.example.deviceinfoviewer.data.model.BatteryInfo;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;
import com.example.deviceinfoviewer.widget.HistoryChartView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

/**
 * 电池详情 Fragment — 圆形进度 + 详细参数 + 历史功率图表，直接观察 Repository LiveData
 */
public class BatteryFragment extends Fragment {

    private DeviceRepository repo;
    private AppSettings settings;

    // 圆形进度
    private CircularProgressIndicator pbBatteryCircle;
    private TextView tvBatteryPercent;
    // 状态
    private TextView tvChargeStatus;
    // 电池详情
    private TextView tvBattTemp, tvVoltage, tvCurrent;
    private TextView tvChargePower, tvDischargePower;
    private TextView tvHealth, tvTechnology;
    // dumpsys 信息
    private TextView tvChargerType, tvMaxChargeCurrent, tvMaxChargeVoltage;
    // 容量
    private TextView tvCapacityNow, tvCapacityDesign, tvHealthPct;
    private CheckBox cbDualCell;
    // 循环次数
    private TextView tvCycleCount;
    // 图表
    private HistoryChartView chartPower;
    private SwipeRefreshLayout swipeRefresh;
    private Handler handler;
    private Runnable chartUpdater;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_battery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repo = DeviceApplication.getDeviceRepository();
        settings = AppSettings.getInstance(requireContext());

        pbBatteryCircle = view.findViewById(R.id.pb_battery_circle);
        tvBatteryPercent = view.findViewById(R.id.tv_battery_percent);
        tvChargeStatus = view.findViewById(R.id.tv_charge_status);
        tvBattTemp = view.findViewById(R.id.tv_batt_temp);
        tvVoltage = view.findViewById(R.id.tv_voltage);
        tvCurrent = view.findViewById(R.id.tv_current);
        tvChargePower = view.findViewById(R.id.tv_charge_power);
        tvDischargePower = view.findViewById(R.id.tv_discharge_power);
        tvHealth = view.findViewById(R.id.tv_health);
        tvTechnology = view.findViewById(R.id.tv_technology);
        tvChargerType = view.findViewById(R.id.tv_charger_type);
        tvMaxChargeCurrent = view.findViewById(R.id.tv_max_charge_current);
        tvMaxChargeVoltage = view.findViewById(R.id.tv_max_charge_voltage);
        tvCapacityNow = view.findViewById(R.id.tv_capacity_now);
        tvCapacityDesign = view.findViewById(R.id.tv_capacity_design);
        tvHealthPct = view.findViewById(R.id.tv_health_pct);
        cbDualCell = view.findViewById(R.id.cb_dual_cell);
        tvCycleCount = view.findViewById(R.id.tv_cycle_count);
        chartPower = view.findViewById(R.id.chart_power);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);

        if (repo == null) {
            return;
        }

        // 双电芯切换
        if (settings != null) {
            cbDualCell.setChecked(settings.isDualCellBattery());
        }
        cbDualCell.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (settings != null) {
                    settings.setDualCellBattery(isChecked);
                }
                // 刷新电压显示
                BatteryInfo battery = repo.getBatteryLiveData().getValue();
                if (battery != null) {
                    updateVoltageDisplay(battery);
                }
            }
        });

        // 观察电池 LiveData
        repo.getBatteryLiveData().observe(getViewLifecycleOwner(), bat -> {
            if (bat == null) return;

            // 圆形进度
            int level = bat.getLevelPercent();
            pbBatteryCircle.setProgress(level >= 0 ? level : 0);
            tvBatteryPercent.setText(level >= 0 ? level + "%" : "N/A");

            // 充电/放电状态
            String statusText = bat.getChargeStatus();
            if (statusText == null || statusText.isEmpty()) {
                statusText = "N/A";
            }
            tvChargeStatus.setText(statusText);

            // 温度
            tvBattTemp.setText(FormatUtils.formatTempCelsius(bat.getTemperatureCelsius()));

            // 电压（考虑双电芯）
            updateVoltageDisplay(bat);

            // 电流
            long currentUA = bat.getCurrentNowUA();
            if (currentUA > 0) {
                tvCurrent.setText("充电 +" + currentUA + " µA");
            } else if (currentUA < 0) {
                tvCurrent.setText("放电 " + Math.abs(currentUA) + " µA");
            } else {
                tvCurrent.setText("N/A");
            }

            // 充电功率
            long chargingPowerMw = bat.getChargingPowerMw();
            tvChargePower.setText(chargingPowerMw > 0 ? chargingPowerMw + " mW" : "N/A");

            // 放电功率
            long dischargingPowerMw = bat.getDischargingPowerMw();
            tvDischargePower.setText(dischargingPowerMw > 0 ? dischargingPowerMw + " mW" : "N/A");

            // 健康
            String health = bat.getHealth();
            tvHealth.setText(health != null && !health.isEmpty() ? health : "N/A");

            // 技术
            String tech = bat.getTechnology();
            tvTechnology.setText(tech != null && !tech.isEmpty() ? tech : "N/A");

            // 当前容量（优先使用 chargeFull）
            long nowCap = bat.getChargeFullMAh() > 0 ? bat.getChargeFullMAh() : bat.getCapacityNowMAh();
            tvCapacityNow.setText(nowCap > 0 ? nowCap + " mAh" : "N/A");

            // 设计容量
            long designCap = bat.getCapacityDesignMAh();
            tvCapacityDesign.setText(designCap > 0 ? designCap + " mAh" : "N/A");

            // 健康度
            long chargeFull = bat.getChargeFullMAh();
            long chargeFullDesign = bat.getChargeFullDesignMAh();
            if (chargeFull > 0 && chargeFullDesign > 0) {
                int healthPct = (int) (chargeFull * 100 / chargeFullDesign);
                tvHealthPct.setText(healthPct + "%");
            } else {
                tvHealthPct.setText("N/A");
            }

            // 循环次数
            int cycles = bat.getCycleCount();
            tvCycleCount.setText(cycles > 0 ? cycles + " 次" : "N/A");

            // dumpsys 信息
            String chargerType = bat.getChargerType();
            tvChargerType.setText(chargerType != null && !chargerType.isEmpty() ? chargerType : "N/A");

            long maxCurrentUA = bat.getMaxChargingCurrentUA();
            tvMaxChargeCurrent.setText(maxCurrentUA > 0
                    ? String.format("%.0f mA", maxCurrentUA / 1000.0) : "N/A");

            long maxVoltageUV = bat.getMaxChargingVoltageUV();
            tvMaxChargeVoltage.setText(maxVoltageUV > 0
                    ? String.format("%.1f V", maxVoltageUV / 1_000_000.0) : "N/A");
        });

        // 图表定时更新（含崩溃保护）
        handler = new Handler(Looper.getMainLooper());
        chartUpdater = new Runnable() {
            @Override
            public void run() {
                try {
                    updateChart();
                } catch (Exception ignored) {
                    // 防止图表渲染异常导致崩溃
                }
                if (handler != null) {
                    handler.postDelayed(this, 3000);
                }
            }
        };

        swipeRefresh.setOnRefreshListener(() -> {
            swipeRefresh.setRefreshing(false);
            updateChart();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (handler != null && chartUpdater != null) {
            handler.postDelayed(chartUpdater, 200);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (handler != null && chartUpdater != null) {
            handler.removeCallbacks(chartUpdater);
        }
    }

    private void updateVoltageDisplay(BatteryInfo battery) {
        int effectiveVoltage = battery.getEffectiveVoltage();
        if (effectiveVoltage > 0) {
            tvVoltage.setText(effectiveVoltage + " mV");
        } else {
            tvVoltage.setText("N/A");
        }
    }

    private void updateChart() {
        if (repo == null || chartPower == null) return;
        // 从 HistoryCache 获取功率历史
        chartPower.setData("功率",
                repo.getHistoryCache().getSeries("battery_power"));
    }
}
