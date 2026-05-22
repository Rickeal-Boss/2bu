package com.example.deviceinfoviewer.data.model;

public class HistoryDataPoint {
    private long timestampMillis;
    private float value;
    private String seriesName;

    public HistoryDataPoint() {}
    public HistoryDataPoint(long timestampMillis, float value, String seriesName) {
        this.timestampMillis = timestampMillis;
        this.value = value;
        this.seriesName = seriesName;
    }
    public long getTimestampMillis() { return timestampMillis; }
    public void setTimestampMillis(long v) { this.timestampMillis = v; }
    public float getValue() { return value; }
    public void setValue(float v) { this.value = v; }
    public String getSeriesName() { return seriesName; }
    public void setSeriesName(String v) { this.seriesName = v; }
    @Override
    public String toString() { return "HistoryDataPoint{" + seriesName + "=" + value + "@" + timestampMillis + "}"; }
}
