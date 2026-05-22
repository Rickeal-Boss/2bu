package com.example.deviceinfoviewer.data.model;

public class SensorItemInfo {
    private String name = "";
    private int type = -1;
    private String vendor = "";
    private float powerMa = Float.NaN;
    private float maxRange = Float.NaN;
    private float resolution = Float.NaN;
    private int minDelay = -1;

    public SensorItemInfo() {}
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public int getType() { return type; }
    public void setType(int v) { this.type = v; }
    public String getVendor() { return vendor; }
    public void setVendor(String v) { this.vendor = v; }
    public float getPowerMa() { return powerMa; }
    public void setPowerMa(float v) { this.powerMa = v; }
    public float getMaxRange() { return maxRange; }
    public void setMaxRange(float v) { this.maxRange = v; }
    public float getResolution() { return resolution; }
    public void setResolution(float v) { this.resolution = v; }
    public int getMinDelay() { return minDelay; }
    public void setMinDelay(int v) { this.minDelay = v; }
    @Override
    public String toString() { return "SensorItemInfo{name='" + name + "', type=" + type + "}"; }
}
