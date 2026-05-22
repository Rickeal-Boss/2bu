package com.example.deviceinfoviewer.data.model;

public class WifiDetailInfo {
    private String ssid = "";
    private String bssid = "";
    private int signalDbm = Integer.MIN_VALUE;
    private int linkSpeedMbps = -1;
    private String ipv4 = "";
    private String ipv6 = "";
    private String macAddress = "";
    private String gateway = "";
    private String dns = "";
    private String subnetMask = "";

    public WifiDetailInfo() {}
    public String getSsid() { return ssid; }
    public void setSsid(String v) { this.ssid = v; }
    public String getBssid() { return bssid; }
    public void setBssid(String v) { this.bssid = v; }
    public int getSignalDbm() { return signalDbm; }
    public void setSignalDbm(int v) { this.signalDbm = v; }
    public int getLinkSpeedMbps() { return linkSpeedMbps; }
    public void setLinkSpeedMbps(int v) { this.linkSpeedMbps = v; }
    public String getIpv4() { return ipv4; }
    public void setIpv4(String v) { this.ipv4 = v; }
    public String getIpv6() { return ipv6; }
    public void setIpv6(String v) { this.ipv6 = v; }
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String v) { this.macAddress = v; }
    public String getGateway() { return gateway; }
    public void setGateway(String v) { this.gateway = v; }
    public String getDns() { return dns; }
    public void setDns(String v) { this.dns = v; }
    public String getSubnetMask() { return subnetMask; }
    public void setSubnetMask(String v) { this.subnetMask = v; }
    @Override
    public String toString() { return "WifiDetailInfo{ssid='" + ssid + "', signalDbm=" + signalDbm + "}"; }
}
