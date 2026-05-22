package com.example.deviceinfoviewer.data.source;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统文件系统读取工具类，统一所有 /proc 和 /sys 文件读取操作
 */
public final class SysFsReader {

    private SysFsReader() {}

    /**
     * 读取文件第一行
     */
    public static String readLine(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line = br.readLine();
            return line != null ? line.trim() : "";
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * 读取文件内容并解析为 Long
     */
    public static long readLong(String path) {
        String content = readLine(path);
        if (content.isEmpty()) {
            return -1L;
        }
        try {
            return Long.parseLong(content);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    /**
     * 读取文件内容并解析为 Float
     */
    public static float readFloat(String path) {
        String content = readLine(path);
        if (content.isEmpty()) {
            return Float.NaN;
        }
        try {
            return Float.parseFloat(content);
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }

    /**
     * 检查文件是否存在
     */
    public static boolean fileExists(String path) {
        return new File(path).exists();
    }

    /**
     * 列出目录下所有文件名
     */
    public static List<String> listDir(String path) {
        List<String> result = new ArrayList<>();
        File dir = new File(path);
        if (dir.exists() && dir.isDirectory()) {
            String[] files = dir.list();
            if (files != null) {
                for (String f : files) {
                    result.add(f);
                }
            }
        }
        return result;
    }

    /**
     * 通过反射读取 SystemProperties
     */
    public static String readProp(String key) {
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            Method method = cls.getMethod("get", String.class);
            Object result = method.invoke(null, key);
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 读取文件全部内容
     */
    public static String readAll(String path) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            return "";
        }
        return sb.toString();
    }
}
