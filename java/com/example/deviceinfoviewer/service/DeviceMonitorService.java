package com.example.deviceinfoviewer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.deviceinfoviewer.DeviceApplication;
import com.example.deviceinfoviewer.MainActivity;
import com.example.deviceinfoviewer.R;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;

/**
 * 前台服务，后台持有 DeviceRepository 进行持续数据采集
 */
public class DeviceMonitorService extends Service {

    private static final String CHANNEL_ID = "device_monitor_channel";
    private static final int NOTIFICATION_ID = 1001;

    private DeviceRepository repository;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, pendingFlags);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("设备信息监控")
                .setContentText("正在后台监控设备信息")
                .setSmallIcon(R.drawable.ic_dashboard)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        // API 34+ 需指定 foregroundServiceType
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        // 获取全局 Repository 单例并开始采集
        repository = DeviceApplication.getDeviceRepository();
        repository.startMonitoring(2000);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.stopMonitoring();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public DeviceRepository getRepository() {
        return repository;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "设备监控",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("设备信息后台监控通知");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
