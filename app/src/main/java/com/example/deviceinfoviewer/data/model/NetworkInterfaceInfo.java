package com.example.deviceinfoviewer.data.model;

public class NetworkInterfaceInfo {
    private String name = "";
    private String ipAddress = "";
    private String macAddress = "";
    private int mtu = -1;
    private long rxBytes = -1L;
    private long txBytes = -1L;

    public NetworkInterfaceInfo() {}
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String v) { this.ipAddress = v; }
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String v) { this.macAddress = v; }
    public int getMtu() { return mtu; }
    public void setMtu(int v) { this.mtu = v; }
    public long getRxBytes() { return rxBytes; }
    public void setRxBytes(long v) { this.rxBytes = v; }
    public long getTxBytes() { return txBytes; }
    public void setTxBytes(long v) { this.txBytes = v; }
    @Override
    public String toString() { return "NetworkInterfaceInfo{name='" + name + "', ipAddress='" + ipAddress + "'}"; }
}
