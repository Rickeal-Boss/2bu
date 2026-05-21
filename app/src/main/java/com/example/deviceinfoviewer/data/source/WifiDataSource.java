package com.example.deviceinfoviewer.data.source;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import com.example.deviceinfoviewer.data.model.WifiDetailInfo;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * WiFi 数据源，通过 WifiManager 获取 WiFi 详细信息
 */
public class WifiDataSource {

    private final Context context;

    public WifiDataSource(Context context) {
        this.context = context.getApplicationContext();
    }

    @SuppressWarnings("MissingPermission")
    public WifiDetailInfo getWifiDetail() {
        WifiDetailInfo info = new WifiDetailInfo();

        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm == null) {
            return info;
        }

        WifiInfo wifiInfo = wm.getConnectionInfo();
        if (wifiInfo == null) {
            return info;
        }

        info.setSsid(wifiInfo.getSSID().replace("\"", ""));
        info.setBssid(wifiInfo.getBSSID());
        info.setSignalDbm(wifiInfo.getRssi());
        info.setLinkSpeedMbps(wifiInfo.getLinkSpeed());
        info.setMacAddress(wifiInfo.getMacAddress());

        // IPv4 地址
        int ipInt = wifiInfo.getIpAddress();
        if (ipInt != 0) {
            info.setIpv4(formatIp(ipInt));
        }

        // DHCP 信息（网关、DNS、子网掩码）
        DhcpInfo dhcp = wm.getDhcpInfo();
        if (dhcp != null) {
            info.setGateway(formatIp(dhcp.gateway));
            info.setDns(formatIp(dhcp.dns1));
            info.setSubnetMask(formatIp(dhcp.netmask));
        }

        return info;
    }

    private String formatIp(int ip) {
        if (ip == 0) {
            return "";
        }
        try {
            byte[] bytes = new byte[]{
                (byte) (ip & 0xFF),
                (byte) ((ip >> 8) & 0xFF),
                (byte) ((ip >> 16) & 0xFF),
                (byte) ((ip >> 24) & 0xFF)
            };
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (UnknownHostException e) {
            return "";
        }
    }
}
