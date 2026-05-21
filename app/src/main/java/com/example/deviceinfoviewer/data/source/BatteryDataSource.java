package com.example.deviceinfoviewer.data.source;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.example.deviceinfoviewer.AppSettings;
import com.example.deviceinfoviewer.data.model.BatteryInfo;

/**
 * 电池数据源 — 全网方案版
 * 1. 温度：BatteryManager (decicelsius÷10) + sysfs fallback
 * 2. 功率：区分充电/放电，使用 double 安全计算
 * 3. 循环次数：20+ 路径多级 fallback（覆盖小米/华为/三星/OPPO/vivo/索尼/一加等）
 * 4. 容量：sysfs charge_full / charge_full_design + BatteryManager
 * 5. 双电芯：读取 AppSettings 中的 dualCell 开关
 */
public class BatteryDataSource {

    private final Context context;

    public BatteryDataSource(Context context) {
        this.context = context.getApplicationContext();
    }

    public BatteryInfo getBatteryInfo() {
        BatteryInfo info = new BatteryInfo();
        info.setTimestamp(System.currentTimeMillis());

        // 双电芯开关
        info.setDualCell(AppSettings.getInstance(context).isDualCellBattery());

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        if (batteryStatus == null) return info;

        // === 电量百分比 ===
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level >= 0 && scale > 0) {
            info.setLevelPercent((int) (level * 100.0f / scale));
        }

        // === 温度 (decicelsius → celsius) ===
        int tempRaw = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        if (tempRaw > 0) {
            info.setTemperatureCelsius(tempRaw / 10.0f);
        } else {
            // fallback: sysfs power_supply
            float sysTemp = SysFsReader.readFloat("/sys/class/power_supply/battery/temp");
            if (!Float.isNaN(sysTemp) && sysTemp > 0) {
                // sysfs temp 单位可能为 decicelsius
                if (sysTemp > 100) sysTemp /= 10.0f;
                info.setTemperatureCelsius(sysTemp);
            }
        }

        // === 电压 (mV, 双电芯×2) ===
        int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        info.setVoltage(voltage);

        // === 充电状态 ===
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        info.setChargeStatus(chargeStatusToString(status));
        info.setCharging(status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL);

        // === 健康状态 ===
        int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        info.setHealth(healthToString(health));

        // === 电池技术 ===
        info.setTechnology(batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY));

        // === 容量 (BatteryManager + sysfs) ===
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (bm != null) {
            long capacity = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            if (capacity != Long.MIN_VALUE && capacity > 0) {
                info.setCapacityDesignMAh(capacity);
            }
        }
        // sysfs 容量（更准确）
        long chargeFull = SysFsReader.readLong("/sys/class/power_supply/battery/charge_full");
        if (chargeFull > 0) info.setChargeFullMAh(chargeFull / 1000);
        long chargeFullDesign = SysFsReader.readLong("/sys/class/power_supply/battery/charge_full_design");
        if (chargeFullDesign > 0) info.setChargeFullDesignMAh(chargeFullDesign / 1000);
        // 如果 BatteryManager 没有容量，用 sysfs
        if (info.getCapacityDesignMAh() <= 0 && chargeFullDesign > 0) {
            info.setCapacityDesignMAh(chargeFullDesign / 1000);
        }
        if (info.getCapacityNowMAh() <= 0 && chargeFull > 0) {
            info.setCapacityNowMAh(chargeFull / 1000);
        }

        // === 电流 (µA, 带符号) ===
        long currentNow = getCurrentNow();
        info.setCurrentNowUA(currentNow);

        // === 功率 = |电压(V) × 电流(A)| = |电压(mV)/1000 × 电流(µA)/1000000| = |电压×电流|/1e9 (W) → ×1000 = mW
        // 简化：功率(mW) = |电压(mV) × 电流(µA)| / 1000000
        int effVoltage = info.getEffectiveVoltage();
        if (effVoltage > 0 && currentNow != 0) {
            double powerMw = Math.abs((double) effVoltage * (double) currentNow) / 1_000_000.0;
            if (currentNow > 0) {
                info.setChargingPowerMw((long) powerMw);
                info.setCharging(true);
            } else {
                info.setDischargingPowerMw((long) powerMw);
            }
        }

        // === 循环次数 ===
        info.setCycleCount(getBatteryCycleCount());

        // === dumpsys battery 附加信息 ===
        try {
            String dumpsysBattery = ShellCommandDataSource.getDumpsysBattery();
            if (!dumpsysBattery.isEmpty()) {
                // 最大充电电流
                long maxCurrent = ShellCommandDataSource.extractLong(dumpsysBattery, "Max charging current");
                if (maxCurrent > 0) info.setMaxChargingCurrentUA(maxCurrent);

                // 最大充电电压
                long maxVoltage = ShellCommandDataSource.extractLong(dumpsysBattery, "Max charging voltage");
                if (maxVoltage > 0) info.setMaxChargingVoltageUV(maxVoltage);

                // Charge counter (已充电量)
                long chargeCounter = ShellCommandDataSource.extractLong(dumpsysBattery, "Charge counter");
                if (chargeCounter > 0) info.setChargeCounterUAh(chargeCounter);

                // 充电类型
                String acOnline = ShellCommandDataSource.extractDumpsysValue(dumpsysBattery, "AC powered");
                String usbOnline = ShellCommandDataSource.extractDumpsysValue(dumpsysBattery, "USB powered");
                String wirelessOnline = ShellCommandDataSource.extractDumpsysValue(dumpsysBattery, "Wireless powered");
                String dockOnline = ShellCommandDataSource.extractDumpsysValue(dumpsysBattery, "Dock powered");
                StringBuilder chargerType = new StringBuilder();
                if ("true".equalsIgnoreCase(acOnline)) chargerType.append("AC");
                if ("true".equalsIgnoreCase(usbOnline)) {
                    if (chargerType.length() > 0) chargerType.append(" + ");
                    chargerType.append("USB");
                }
                if ("true".equalsIgnoreCase(wirelessOnline)) {
                    if (chargerType.length() > 0) chargerType.append(" + ");
                    chargerType.append("无线");
                }
                if ("true".equalsIgnoreCase(dockOnline)) {
                    if (chargerType.length() > 0) chargerType.append(" + ");
                    chargerType.append("底座");
                }
                if (chargerType.length() > 0) {
                    info.setChargerType(chargerType.toString());
                }
            }
        } catch (Exception ignored) {}

        return info;
    }

    // ========== 全网循环次数方案 ==========

    private int getBatteryCycleCount() {
        // Level 1: 直接读取 cycle_count
        long cnt = SysFsReader.readLong("/sys/class/power_supply/battery/cycle_count");
        if (cnt > 0) return (int) cnt;

        // Level 2: 小米方案 — charge_counter ÷ 设计容量
        long counter = SysFsReader.readLong("/sys/class/power_supply/battery/charge_counter");
        long designCap = SysFsReader.readLong("/sys/class/power_supply/battery/charge_full_design");
        if (designCap <= 0) designCap = SysFsReader.readLong("/sys/class/power_supply/bms/charge_full_design");
        if (counter > 0 && designCap > 0) return (int) (counter / designCap);

        // Level 3: 三星方案
        cnt = SysFsReader.readLong("/sys/class/power_supply/battery/batt_cycle");
        if (cnt > 0) return (int) cnt;

        // Level 4: 各厂商系统属性
        String[] props = {
            "ro.vendor.battery.cycle_count",      // 通用厂商
            "persist.vendor.battery.cycle_count",
            "ro.battery.cycle_count",
            "persist.battery.cycle_count",
            "ro.vendor.battery.cycle",             // OPPO/Realme
            "persist.vendor.battery.cycle",
            "ro.vendor.battery.charge_cycle",      // vivo/iQOO
            "persist.vendor.battery.charge_cycle",
            "ro.batt.cycle_count",                 // 华为/荣耀
            "persist.batt.cycle_count",
            "ro.battery_cycle",                    // 索尼
            "persist.battery_cycle",
            "ro.vendor.power.battery_cycle",       // 一加
            "persist.vendor.power.battery_cycle",
            "ro.boot.battery_cycle",               // bootloader传入
        };
        for (String prop : props) {
            String val = SysFsReader.readProp(prop);
            if (!val.isEmpty()) {
                try { int v = Integer.parseInt(val.trim()); if (v > 0) return v; }
                catch (NumberFormatException ignored) {}
            }
        }

        // Level 5: 三星 healthd 属性
        String[] samsungProps = {
            "ro.vendor.battery.healthd_cycle",
            "persist.vendor.battery.healthd_cycle",
        };
        for (String prop : samsungProps) {
            String val = SysFsReader.readProp(prop);
            if (!val.isEmpty()) {
                try { int v = Integer.parseInt(val.trim()); if (v > 0) return v; }
                catch (NumberFormatException ignored) {}
            }
        }

        return -1;
    }

    /**
     * 获取电流 (µA)，按优先级多路径尝试
     */
    private long getCurrentNow() {
        // 主要路径
        long val = SysFsReader.readLong("/sys/class/power_supply/battery/current_now");
        if (val != -1 && val != Long.MIN_VALUE) return val;

        // 备选
        val = SysFsReader.readLong("/sys/class/power_supply/battery/battery_current");
        if (val != -1 && val != Long.MIN_VALUE) return val;

        // 三星方案
        val = SysFsReader.readLong("/sys/class/power_supply/battery/current_avg");
        if (val != -1 && val != Long.MIN_VALUE) return val;

        // 高通 BMS
        val = SysFsReader.readLong("/sys/class/power_supply/bms/current_now");
        if (val != -1 && val != Long.MIN_VALUE) return val;

        // MediaTek
        val = SysFsReader.readLong("/sys/class/power_supply/battery/Charger_Current");
        if (val != -1 && val != Long.MIN_VALUE) return val;

        return 0;
    }

    private String chargeStatusToString(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING: return "充电中";
            case BatteryManager.BATTERY_STATUS_DISCHARGING: return "放电中";
            case BatteryManager.BATTERY_STATUS_FULL: return "已充满";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "未充电";
            default: return "未知";
        }
    }

    private String healthToString(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "良好";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "过热";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "损坏";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "过压";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: return "故障";
            case BatteryManager.BATTERY_HEALTH_COLD: return "过冷";
            default: return "未知";
        }
    }
}
