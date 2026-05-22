package com.example.deviceinfoviewer.data.model;

public class GpsSatelliteInfo {
    private int prn = -1;
    private String constellation = "";
    private float snr = Float.NaN;
    private float elevation = Float.NaN;
    private float azimuth = Float.NaN;
    private boolean usedInFix = false;

    public GpsSatelliteInfo() {}
    public int getPrn() { return prn; }
    public void setPrn(int v) { this.prn = v; }
    public String getConstellation() { return constellation; }
    public void setConstellation(String v) { this.constellation = v; }
    public float getSnr() { return snr; }
    public void setSnr(float v) { this.snr = v; }
    public float getElevation() { return elevation; }
    public void setElevation(float v) { this.elevation = v; }
    public float getAzimuth() { return azimuth; }
    public void setAzimuth(float v) { this.azimuth = v; }
    public boolean isUsedInFix() { return usedInFix; }
    public void setUsedInFix(boolean v) { this.usedInFix = v; }
    @Override
    public String toString() { return "GpsSatelliteInfo{prn=" + prn + ", constellation='" + constellation + "', snr=" + snr + "}"; }
}
