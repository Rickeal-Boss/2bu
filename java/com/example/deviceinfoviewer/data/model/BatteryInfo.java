package com.example.deviceinfoviewer.data.model;

/**
 * 电池信息 - 增强版：区分充放电、双电芯支持、循环次数全网方案
 */
public class BatteryInfo {
    private float temperatureCelsius = Float.NaN;
    private long chargingPowerMw = -1L;
    private long dischargingPowerMw = -1L;
    private boolean isCharging = false;
    private long currentNowUA = 0L;      // 带符号的电流(µA), 正=充电 负=放电
    private int cycleCount = -1;
    private long capacityNowMAh = -1L;
    private long capacityDesignMAh = -1L;
    private long chargeFullMAh = -1L;     // sysfs charge_full (当前满电容量)
    private long chargeFullDesignMAh = -1L; // sysfs charge_full_design
    private int levelPercent = -1;
    private String chargeStatus = "";
    private String health = "";
    private String technology = "";
    private int voltage = -1;
    private long timestamp = 0L;
    private boolean dualCell = false;      // 双电芯模式

    // dumpsys battery 附加信息
    private long maxChargingCurrentUA = -1L;   // 最大充电电流
    private long maxChargingVoltageUV = -1L;   // 最大充电电压
    private long chargeCounterUAh = -1L;       // 已充电量计数器
    private String chargerType = "";            // USB / AC / Wireless / Dock

    public BatteryInfo() {}

    // -- Getters and Setters --
    public float getTemperatureCelsius() { return temperatureCelsius; }
    public void setTemperatureCelsius(float t) { this.temperatureCelsius = t; }

    public long getChargingPowerMw() { return chargingPowerMw; }
    public void setChargingPowerMw(long p) { this.chargingPowerMw = p; }

    public long getDischargingPowerMw() { return dischargingPowerMw; }
    public void setDischargingPowerMw(long p) { this.dischargingPowerMw = p; }

    @Deprecated
    public long getPowerMilliwatts() { return isCharging ? chargingPowerMw : dischargingPowerMw; }
    @Deprecated
    public void setPowerMilliwatts(long p) {}

    public boolean isCharging() { return isCharging; }
    public void setCharging(boolean c) { this.isCharging = c; }

    public long getCurrentNowUA() { return currentNowUA; }
    public void setCurrentNowUA(long c) { this.currentNowUA = c; }

    public int getCycleCount() { return cycleCount; }
    public void setCycleCount(int c) { this.cycleCount = c; }

    public long getCapacityNowMAh() { return capacityNowMAh; }
    public void setCapacityNowMAh(long c) { this.capacityNowMAh = c; }

    public long getCapacityDesignMAh() { return capacityDesignMAh; }
    public void setCapacityDesignMAh(long c) { this.capacityDesignMAh = c; }

    public long getChargeFullMAh() { return chargeFullMAh; }
    public void setChargeFullMAh(long c) { this.chargeFullMAh = c; }

    public long getChargeFullDesignMAh() { return chargeFullDesignMAh; }
    public void setChargeFullDesignMAh(long c) { this.chargeFullDesignMAh = c; }

    public int getLevelPercent() { return levelPercent; }
    public void setLevelPercent(int l) { this.levelPercent = l; }

    public String getChargeStatus() { return chargeStatus; }
    public void setChargeStatus(String s) { this.chargeStatus = s; }

    public String getHealth() { return health; }
    public void setHealth(String h) { this.health = h; }

    public String getTechnology() { return technology; }
    public void setTechnology(String t) { this.technology = t; }

    public int getVoltage() { return voltage; }
    public void setVoltage(int v) { this.voltage = v; }

    public boolean isDualCell() { return dualCell; }
    public void setDualCell(boolean d) { this.dualCell = d; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long t) { this.timestamp = t; }

    // -- dumpsys 附加信息 --
    public long getMaxChargingCurrentUA() { return maxChargingCurrentUA; }
    public void setMaxChargingCurrentUA(long v) { this.maxChargingCurrentUA = v; }
    public long getMaxChargingVoltageUV() { return maxChargingVoltageUV; }
    public void setMaxChargingVoltageUV(long v) { this.maxChargingVoltageUV = v; }
    public long getChargeCounterUAh() { return chargeCounterUAh; }
    public void setChargeCounterUAh(long v) { this.chargeCounterUAh = v; }
    public String getChargerType() { return chargerType; }
    public void setChargerType(String v) { this.chargerType = v; }

    /** 获取有效电压（双电芯×2） */
    public int getEffectiveVoltage() {
        return dualCell && voltage > 0 ? voltage * 2 : voltage;
    }

    @Override
    public String toString() {
        return "BatteryInfo{level=" + levelPercent + "%, temp=" + temperatureCelsius
                + "°C, charge=" + chargeStatus + ", health=" + health + "}";
    }
}
