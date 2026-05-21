package com.example.deviceinfoviewer;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.example.deviceinfoviewer.data.repository.DeviceRepository;

/**
 * Application 类，提供全局 Context、DeviceRepository 单例、崩溃日志
 */
public class DeviceApplication extends Application {

    private static final String TAG = "DeviceApp";
    private static Context appContext;
    private static DeviceRepository deviceRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();

        // 全局崩溃日志捕获
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Log.e(TAG, "FATAL CRASH in thread " + t.getName(), e);
            android.os.Process.killProcess(android.os.Process.myPid());
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
