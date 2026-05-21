package com.example.deviceinfoviewer;

import android.app.Application;
import android.content.Context;

import com.example.deviceinfoviewer.data.repository.DeviceRepository;

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
