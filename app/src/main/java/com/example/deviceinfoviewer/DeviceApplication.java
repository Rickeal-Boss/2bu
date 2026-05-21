package com.example.deviceinfoviewer;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.example.deviceinfoviewer.data.repository.DeviceRepository;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Application 类 — 全局 Context、DeviceRepository 单例、崩溃日志
 */
public class DeviceApplication extends Application {

    private static final String TAG = "DeviceApp";
    private static Context appContext;
    private static DeviceRepository deviceRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();

        // 全局未捕获异常处理器 — 写文件 + Logcat
        final Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            // 写 Logcat
            Log.e(TAG, "=== FATAL CRASH ===", e);
            Log.e(TAG, "Thread: " + t.getName());
            Log.e(TAG, "SDK: " + android.os.Build.VERSION.SDK_INT);

            // 写文件到 /data/data/.../files/crash.log
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println("=== CRASH " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + " ===");
                pw.println("SDK=" + android.os.Build.VERSION.SDK_INT);
                pw.println("Device=" + android.os.Build.MODEL);
                e.printStackTrace(pw);
                pw.flush();

                File f = new File(appContext.getFilesDir(), "crash.log");
                FileWriter fw = new FileWriter(f, true);
                fw.write(sw.toString());
                fw.flush();
                fw.close();
            } catch (Exception ignored) {}

            // 调用旧 handler（显示系统崩溃对话框）
            if (oldHandler != null) {
                oldHandler.uncaughtException(t, e);
            } else {
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
    }

    public static Context getContext() {
        return appContext;
    }

    public static synchronized DeviceRepository getDeviceRepository() {
        if (deviceRepository == null && appContext != null) {
            try {
                deviceRepository = new DeviceRepository(appContext);
            } catch (Exception e) {
                Log.e(TAG, "Failed to create DeviceRepository", e);
            }
        }
        return deviceRepository;
    }
}
