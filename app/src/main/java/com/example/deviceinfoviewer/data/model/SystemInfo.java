package com.example.deviceinfoviewer.data.model;

import java.util.HashMap;
import java.util.Map;

public class SystemInfo {
    private Map<String, String> buildFields = new HashMap<>();
    private String androidVersion = "";
    private String kernelVersion = "";
    private String javaVmVersion = "";
    private String javaRuntimeName = "";
    private String bootloader = "";
    private String securityPatch = "";

    public SystemInfo() {}
    public Map<String, String> getBuildFields() { return buildFields; }
    public void setBuildFields(Map<String, String> v) { this.buildFields = v; }
    public String getAndroidVersion() { return androidVersion; }
    public void setAndroidVersion(String v) { this.androidVersion = v; }
    public String getKernelVersion() { return kernelVersion; }
    public void setKernelVersion(String v) { this.kernelVersion = v; }
    public String getJavaVmVersion() { return javaVmVersion; }
    public void setJavaVmVersion(String v) { this.javaVmVersion = v; }
    public String getJavaRuntimeName() { return javaRuntimeName; }
    public void setJavaRuntimeName(String v) { this.javaRuntimeName = v; }
    public String getBootloader() { return bootloader; }
    public void setBootloader(String v) { this.bootloader = v; }
    public String getSecurityPatch() { return securityPatch; }
    public void setSecurityPatch(String v) { this.securityPatch = v; }
    @Override
    public String toString() { return "SystemInfo{androidVersion='" + androidVersion + "'}"; }
}
