package com.example.deviceinfoviewer.data.model;

public class MobileNetworkInfo {
    private String networkType = "";
    private String operatorName = "";
    private String mccMnc = "";
    private int signalStrengthDbm = Integer.MIN_VALUE;
    private boolean isRoaming = false;

    public MobileNetworkInfo() {}
    public String getNetworkType() { return networkType; }
    public void setNetworkType(String v) { this.networkType = v; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String v) { this.operatorName = v; }
    public String getMccMnc() { return mccMnc; }
    public void setMccMnc(String v) { this.mccMnc = v; }
    public int getSignalStrengthDbm() { return signalStrengthDbm; }
    public void setSignalStrengthDbm(int v) { this.signalStrengthDbm = v; }
    public boolean isRoaming() { return isRoaming; }
    public void setRoaming(boolean v) { this.isRoaming = v; }
    @Override
    public String toString() { return "MobileNetworkInfo{networkType='" + networkType + "', operatorName='" + operatorName + "'}"; }
}
