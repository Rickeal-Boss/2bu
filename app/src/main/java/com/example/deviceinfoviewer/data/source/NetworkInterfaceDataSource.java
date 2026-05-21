package com.example.deviceinfoviewer.data.source;

import com.example.deviceinfoviewer.data.model.NetworkInterfaceInfo;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 网络接口数据源，通过 Java NetworkInterface API 获取信息
 */
public class NetworkInterfaceDataSource {

    public List<NetworkInterfaceInfo> getNetworkInterfaces() {
        List<NetworkInterfaceInfo> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return result;
            }
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                NetworkInterfaceInfo info = new NetworkInterfaceInfo();
                info.setName(ni.getName());
                info.setMtu(ni.getMTU());

                // MAC 地址
                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    StringBuilder macStr = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        macStr.append(String.format("%02X", mac[i]));
                        if (i < mac.length - 1) {
                            macStr.append(":");
                        }
                    }
                    info.setMacAddress(macStr.toString());
                }

                // IP 地址
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip != null) {
                            // 移除 % 后的 scope id
                            int pct = ip.indexOf('%');
                            if (pct > 0) {
                                ip = ip.substring(0, pct);
                            }
                            if (ip.contains(":")) {
                                info.setIpAddress(ip);
                            } else if (info.getIpAddress().isEmpty()) {
                                info.setIpAddress(ip);
                            }
                        }
                    }
                }

                result.add(info);
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return result;
    }
}
