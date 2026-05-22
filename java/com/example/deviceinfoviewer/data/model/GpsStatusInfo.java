package com.example.deviceinfoviewer.data.model;

import java.util.ArrayList;
import java.util.List;

public class GpsStatusInfo {
    private boolean gpsEnabled = false;
    private boolean fixAcquired = false;
    private double latitude = Double.NaN;
    private double longitude = Double.NaN;
    private float accuracy = Float.NaN;
    private int satelliteCount = 0;
    private List<GpsSatelliteInfo> satellites = new ArrayList<>();

    public GpsStatusInfo() {}
    public boolean isGpsEnabled() { return gpsEnabled; }
    public void setGpsEnabled(boolean v) { this.gpsEnabled = v; }
    public boolean isFixAcquired() { return fixAcquired; }
    public void setFixAcquired(boolean v) { this.fixAcquired = v; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double v) { this.latitude = v; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double v) { this.longitude = v; }
    public float getAccuracy() { return accuracy; }
    public void setAccuracy(float v) { this.accuracy = v; }
    public int getSatelliteCount() { return satelliteCount; }
    public void setSatelliteCount(int v) { this.satelliteCount = v; }
    public List<GpsSatelliteInfo> getSatellites() { return satellites; }
    public void setSatellites(List<GpsSatelliteInfo> v) { this.satellites = v; }
    @Override
    public String toString() { return "GpsStatusInfo{gpsEnabled=" + gpsEnabled + ", fixAcquired=" + fixAcquired + "}"; }
}
