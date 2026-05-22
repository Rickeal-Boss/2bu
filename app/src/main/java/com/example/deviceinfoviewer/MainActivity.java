package com.example.deviceinfoviewer;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.deviceinfoviewer.data.repository.DeviceRepository;
import com.example.deviceinfoviewer.service.FloatingWindowService;
import com.example.deviceinfoviewer.util.ExportHelper;
import com.example.deviceinfoviewer.util.PermissionHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * 主 Activity —— TabLayout + ViewPager2 主框架
 * 持有全局 DeviceRepository，管理菜单操作
 */
public class MainActivity extends AppCompatActivity {

    private DeviceRepository repository;
    private AppSettings settings;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // 必须在 super.onCreate() 之前应用主题以避免配置变更崩溃
        settings = AppSettings.getInstance(getApplicationContext());
        applyDarkMode();

        super.onCreate(savedInstanceState);

        // 启用 Edge-to-Edge（全面屏适配）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }

        setContentView(R.layout.activity_main);

        // 处理 WindowInsets — 让 Toolbar 留出状态栏空间
        Toolbar toolbar = findViewById(R.id.toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), statusBarHeight, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        setSupportActionBar(toolbar);

        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);

        // 设置 ViewPager2
        TabPagerAdapter adapter = new TabPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // 绑定 TabLayout
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(TabPagerAdapter.getTabTitle(position));
            switch (position) {
                case 0: tab.setIcon(R.drawable.ic_dashboard); break;
                case 1: tab.setIcon(R.drawable.ic_hardware); break;
                case 2: tab.setIcon(R.drawable.ic_system); break;
                case 3: tab.setIcon(R.drawable.ic_network); break;
                case 4: tab.setIcon(R.drawable.ic_battery); break;
            }
        }).attach();

        // 初始化 Repository（单例）
        repository = DeviceApplication.getDeviceRepository();
        repository.startMonitoring(settings.getRefreshIntervalMs());
        repository.loadStaticData();

        // 权限引导 — 延迟到 Window 完全就绪后执行，避免 BadTokenException
        viewPager.post(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PermissionHelper.requestPermissionsSequential(MainActivity.this, new PermissionHelper.PermissionCallback() {
                    @Override
                    public void onAllGranted() { }
                    @Override
                    public void onDenied() { }
                });
            }
        });
    }

    /**
     * 获取全局 Repository（供 Fragment 使用）
     */
    public DeviceRepository getRepository() {
        return repository;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_dark_mode) {
            // 切换深色模式
            boolean isDark = !settings.isDarkMode();
            settings.setDarkMode(isDark);
            applyDarkMode();
            Toast.makeText(this, isDark ? "深色模式已开启" : "深色模式已关闭", Toast.LENGTH_SHORT).show();
            return true;
        }

        if (id == R.id.action_export) {
            // 导出并分享
            String text = ExportHelper.exportToText(repository);
            ExportHelper.shareReport(this, text, getString(R.string.export_text_title));
            return true;
        }

        if (id == R.id.action_floating_window) {
            // 悬浮窗开关
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(intent);
                return true;
            }

            boolean enabled = settings.isFloatingWindowEnabled();
            if (enabled) {
                // 停止悬浮窗
                stopFloatingWindow();
                settings.setFloatingWindowEnabled(false);
                Toast.makeText(this, "悬浮窗已关闭", Toast.LENGTH_SHORT).show();
            } else {
                // 启动悬浮窗
                startFloatingWindow();
                settings.setFloatingWindowEnabled(true);
                Toast.makeText(this, "悬浮窗已开启", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        if (id == R.id.action_settings) {
            // 显示设置对话框
            showMainSettingsDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void applyDarkMode() {
        if (settings.isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void startFloatingWindow() {
        Intent intent = new Intent(this, FloatingWindowService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopFloatingWindow() {
        Intent intent = new Intent(this, FloatingWindowService.class);
        stopService(intent);
    }

    private void showMainSettingsDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("设置")
                .setItems(new String[]{"刷新间隔: " + (settings.getRefreshIntervalMs() / 1000) + "秒",
                        "清空历史数据"},
                        (dialog, which) -> {
                            if (which == 0) {
                                showIntervalDialog();
                            } else if (which == 1) {
                                if (repository != null) {
                                    repository.getHistoryCache().clear();
                                }
                                Toast.makeText(this, "历史数据已清空", Toast.LENGTH_SHORT).show();
                            }
                        })
                .setNegativeButton("关闭", null)
                .show();
    }

    private void showIntervalDialog() {
        final String[] intervals = {"1秒", "2秒", "3秒", "5秒", "10秒"};
        final int[] values = {1000, 2000, 3000, 5000, 10000};

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("选择刷新间隔")
                .setItems(intervals, (dialog, which) -> {
                    int ms = values[which];
                    settings.setRefreshIntervalMs(ms);
                    if (repository != null) {
                        repository.setIntervalMs(ms);
                    }
                    Toast.makeText(this, "刷新间隔设为 " + (ms / 1000) + "秒", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.stopMonitoring();
        }
        stopFloatingWindow();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionHelper.onPermissionResult(requestCode, permissions, grantResults);
    }
}
