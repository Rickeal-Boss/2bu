package com.example.deviceinfoviewer;

import android.app.Application;
import android.content.Context;
import android.os.Process;

import com.example.deviceinfoviewer.data.repository.DeviceRepository;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Application 类，提供全局 Context 访问和 DeviceRepository 单例
 */
public class DeviceApplication extends Application {

    private static Context appContext;
    private static DeviceRepository deviceRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        setupCrashHandler();
    }

    /**
     * 安装全局崩溃处理器，将堆栈写入文件便于排查
     */
    private void setupCrashHandler() {
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                File crashDir = new File(getFilesDir(), "crashes");
                crashDir.mkdirs();
                File crashFile = new File(crashDir, "crash_" + System.currentTimeMillis() + ".log");
                try (PrintWriter pw = new PrintWriter(crashFile)) {
                    pw.println("Time: " + new java.util.Date());
                    pw.println("Thread: " + thread.getName());
                    pw.println();
                    throwable.printStackTrace(pw);

                    // 同时输出 Throwable 的 cause 链
                    Throwable cause = throwable.getCause();
                    while (cause != null) {
                        pw.println("\n--- Caused by ---");
                        cause.printStackTrace(pw);
                        cause = cause.getCause();
                    }
                }
            } catch (Exception ignored) {
                // 崩溃日志写入失败，不阻塞进程退出
            }
            // 交给系统默认处理器（显示 ANR/崩溃 对话框）
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            } else {
                Process.killProcess(Process.myPid());
            }
        });
    }

    /**
     * 获取全局 Application Context
     */
    public static Context getContext() {
        return appContext;
    }

    /**
     * 获取全局 DeviceRepository 单例
     */
    public static synchronized DeviceRepository getDeviceRepository() {
        if (deviceRepository == null && appContext != null) {
            deviceRepository = new DeviceRepository(appContext);
        }
        return deviceRepository;
    }
}
