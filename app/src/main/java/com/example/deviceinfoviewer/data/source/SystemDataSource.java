package com.example.deviceinfoviewer.data.source;

import android.os.Build;

import com.example.deviceinfoviewer.data.model.SystemInfo;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统信息数据源，通过反射读取 Build 字段
 */
public class SystemDataSource {

    public SystemInfo getSystemInfo() {
        SystemInfo info = new SystemInfo();

        // 反射读取 Build 全部字段
        Map<String, String> buildFields = new HashMap<>();
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                String name = field.getName();
                Object value = field.get(null);
                if (value instanceof String) {
                    buildFields.put(name, (String) value);
                } else {
                    buildFields.put(name, String.valueOf(value));
                }
            } catch (Exception ignored) {}
        }
        // 也读取 VERSION 类的字段
        Field[] versionFields = Build.VERSION.class.getDeclaredFields();
        for (Field field : versionFields) {
            try {
                String name = "VERSION." + field.getName();
                Object value = field.get(null);
                buildFields.put(name, String.valueOf(value));
            } catch (Exception ignored) {}
        }

        info.setBuildFields(buildFields);
        info.setAndroidVersion(Build.VERSION.RELEASE);
        info.setBootloader(Build.BOOTLOADER);
        info.setSecurityPatch(Build.VERSION.SECURITY_PATCH);

        // 内核版本
        info.setKernelVersion(SysFsReader.readLine("/proc/version"));

        // Java VM 版本
        info.setJavaVmVersion(System.getProperty("java.vm.version", ""));
        info.setJavaRuntimeName(System.getProperty("java.runtime.name", ""));

        return info;
    }
}
