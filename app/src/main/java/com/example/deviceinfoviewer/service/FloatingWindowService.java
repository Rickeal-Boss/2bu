package com.example.deviceinfoviewer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;

import com.example.deviceinfoviewer.AppSettings;
import com.example.deviceinfoviewer.DeviceApplication;
import com.example.deviceinfoviewer.FormatUtils;
import com.example.deviceinfoviewer.MainActivity;
import com.example.deviceinfoviewer.R;
import com.example.deviceinfoviewer.data.model.BatteryInfo;
import com.example.deviceinfoviewer.data.model.CpuCoreInfo;
import com.example.deviceinfoviewer.data.model.CpuInfo;
import com.example.deviceinfoviewer.data.model.MemoryInfo;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;

/**
 * 悬浮窗前台 Service，在所有应用上方显示设备信息
 */
public class FloatingWindowService extends Service {

    private static final String CHANNEL_ID = "floating_window_channel";
    private static final int NOTIFICATION_ID = 2001;

    private WindowManager windowManager;
    private View floatingView;
    private DeviceRepository repository;
    private AppSettings settings;
    private Handler handler;
    private Runnable refreshRunnable;

    private TextView tvCpuTemp, tvCpuFreq, tvBattery, tvRam;

    // 拖拽相关
    private float initialTouchX, initialTouchY;
    private float initialWindowX, initialWindowY;
    private static final int LONG_PRESS_DURATION = 500;
    private boolean isLongPress = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Android 13+ 请求通知权限（前台服务必需）
        if (Build.VERSION.SDK_INT >= 33) {
            requestNotificationPermission();
        }

        settings = AppSettings.getInstance(this);
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        repository = DeviceApplication.getDeviceRepository();
        repository.startMonitoring(1000);
        handler = new Handler(Looper.getMainLooper());

        // 启动前台通知
        showForegroundNotification();

        // 创建悬浮窗视图
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.layout_floating_window, null);

        tvCpuTemp = floatingView.findViewById(R.id.tv_float_cpu_temp);
        tvCpuFreq = floatingView.findViewById(R.id.tv_float_cpu_freq);
        tvBattery = floatingView.findViewById(R.id.tv_float_battery);
        tvRam = floatingView.findViewById(R.id.tv_float_ram);

        // 设置透明度
        floatingView.setAlpha(settings.getFloatingWindowOpacity());

        // 拖拽处理
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private Handler longPressHandler = new Handler(Looper.getMainLooper());
            private Runnable longPressRunnable;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        WindowManager.LayoutParams paramsDown = (WindowManager.LayoutParams) floatingView.getLayoutParams();
                        initialWindowX = paramsDown.x;
                        initialWindowY = paramsDown.y;
                        isLongPress = false;

                        longPressRunnable = () -> {
                            isLongPress = true;
                            showSettingsDialog();
                        };
                        longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_DURATION);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        longPressHandler.removeCallbacks(longPressRunnable);
                        float deltaX = event.getRawX() - initialTouchX;
                        float deltaY = event.getRawY() - initialTouchY;
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            WindowManager.LayoutParams params = (WindowManager.LayoutParams) floatingView.getLayoutParams();
                            params.x = (int) (initialWindowX + deltaX);
                            params.y = (int) (initialWindowY + deltaY);
                            windowManager.updateViewLayout(floatingView, params);

                            // 保存位置
                            settings.setFloatingWindowX(params.x);
                            settings.setFloatingWindowY(params.y);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        longPressHandler.removeCallbacks(longPressRunnable);
                        return true;

                    default:
                        return false;
                }
            }
        });

        // 添加到 WindowManager
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;

        // 恢复上次位置或默认右上角
        int savedX = settings.getFloatingWindowX();
        int savedY = settings.getFloatingWindowY();
        if (savedX >= 0 && savedY >= 0) {
            params.x = savedX;
            params.y = savedY;
        } else {
            int screenWidth;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                screenWidth = windowManager.getCurrentWindowMetrics().getBounds().width();
            } else {
                screenWidth = windowManager.getDefaultDisplay().getWidth();
            }
            params.x = screenWidth - 200;
            params.y = 100;
        }

        windowManager.addView(floatingView, params);

        // 定时刷新
        startRefresh();
    }

    private int getWindowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            return WindowManager.LayoutParams.TYPE_PHONE;
        }
    }

    private void startRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshData();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(refreshRunnable);
    }

    private void refreshData() {
        if (repository == null) return;

        // CPU 温度
        CpuInfo cpu = repository.getCpuLiveData().getValue();
        if (cpu != null) {
            tvCpuTemp.setText("CPU: " + FormatUtils.formatTempCelsius(cpu.getTemperatureCelsius()));

            long maxFreq = 0;
            for (CpuCoreInfo core : cpu.getCores()) {
                if (core.getCurrentFreqKHz() > maxFreq) {
                    maxFreq = core.getCurrentFreqKHz();
                }
            }
            tvCpuFreq.setText("频率: " + FormatUtils.formatFreq(maxFreq));
        }

        // 电池（使用新字段）
        BatteryInfo battery = repository.getBatteryLiveData().getValue();
        if (battery != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("电池: ").append(FormatUtils.formatPercent(battery.getLevelPercent()));
            // 温度
            if (!Float.isNaN(battery.getTemperatureCelsius())) {
                sb.append(" ").append(FormatUtils.formatTempCelsius(battery.getTemperatureCelsius()));
            }
            // 充电功率
            long chargingMw = battery.getChargingPowerMw();
            if (chargingMw > 0) {
                sb.append(" ⚡").append(chargingMw).append("mW");
            }
            tvBattery.setText(sb.toString());
        }

        // RAM
        MemoryInfo memory = repository.getMemoryLiveData().getValue();
        if (memory != null && memory.getTotalKB() > 0) {
            int usagePct = (int) ((float) memory.getUsedKB() / memory.getTotalKB() * 100);
            tvRam.setText("RAM: " + FormatUtils.formatPercent(usagePct));
        }
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);

        SeekBar seekBar = dialogView.findViewById(R.id.seekbar_opacity);
        TextView tvOpacity = dialogView.findViewById(R.id.tv_opacity_value);
        CheckBox cbCpuTemp = dialogView.findViewById(R.id.cb_show_cpu_temp);
        CheckBox cbCpuFreq = dialogView.findViewById(R.id.cb_show_cpu_freq);
        CheckBox cbBattery = dialogView.findViewById(R.id.cb_show_battery);
        CheckBox cbRam = dialogView.findViewById(R.id.cb_show_ram);

        seekBar.setProgress((int) (settings.getFloatingWindowOpacity() * 100));
        tvOpacity.setText((int) (settings.getFloatingWindowOpacity() * 100) + "%");

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float alpha = progress / 100f;
                floatingView.setAlpha(alpha);
                settings.setFloatingWindowOpacity(alpha);
                tvOpacity.setText(progress + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        cbCpuTemp.setChecked(tvCpuTemp.getVisibility() == View.VISIBLE);
        cbCpuFreq.setChecked(tvCpuFreq.getVisibility() == View.VISIBLE);
        cbBattery.setChecked(tvBattery.getVisibility() == View.VISIBLE);
        cbRam.setChecked(tvRam.getVisibility() == View.VISIBLE);

        cbCpuTemp.setOnCheckedChangeListener((button, checked) -> tvCpuTemp.setVisibility(checked ? View.VISIBLE : View.GONE));
        cbCpuFreq.setOnCheckedChangeListener((button, checked) -> tvCpuFreq.setVisibility(checked ? View.VISIBLE : View.GONE));
        cbBattery.setOnCheckedChangeListener((button, checked) -> tvBattery.setVisibility(checked ? View.VISIBLE : View.GONE));
        cbRam.setOnCheckedChangeListener((button, checked) -> tvRam.setVisibility(checked ? View.VISIBLE : View.GONE));

        builder.setView(dialogView)
                .setTitle("悬浮窗设置")
                .setPositiveButton("确定", (dialog, which) -> dialog.dismiss())
                .setCancelable(true);

        // 使用 TYPE_APPLICATION_OVERLAY 的 WindowManager 来显示对话框
        AlertDialog dialog = builder.create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } else {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);
        }
        dialog.show();
    }

    private void showForegroundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, pendingFlags);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("设备信息悬浮窗")
                .setContentText("正在显示设备信息悬浮窗")
                .setSmallIcon(R.drawable.ic_float_window)
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshRunnable);
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
        if (repository != null) {
            repository.stopMonitoring();
        }
    }

    /**
     * Android 13+ 运行时请求通知权限
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Service 中无法直接弹出权限对话框，记录日志等待主 Activity 处理
                android.util.Log.w("FloatingWindowService",
                        "POST_NOTIFICATIONS permission not granted; foreground notification may fail");
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "悬浮窗",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("设备信息悬浮窗通知");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
