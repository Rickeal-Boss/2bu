package com.example.deviceinfoviewer;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
 * 主 Activity — TabLayout + ViewPager2，竞品绿色主题风格
 */
public class MainActivity extends AppCompatActivity {

    private DeviceRepository repository;
    private AppSettings settings;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("MainActivity", "onCreate start, SDK=" + Build.VERSION.SDK_INT);

        settings = AppSettings.getInstance(this);
        applyDarkMode();
        Log.i("MainActivity", "step1: dark mode applied");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }

        Log.i("MainActivity", "step2: inflating layout...");
        setContentView(R.layout.activity_main);
        Log.i("MainActivity", "step3: layout inflated OK");

        // 工具栏 — 处理状态栏避让
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("System Monitor");
        }

        // 用 ViewCompat 确保 WindowInsets 正确分发
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            // 清除可能存在的主题 padding，仅保留 Insets padding
            v.setPadding(0, top, 0, 0);
            return insets;
        });

        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);
        Log.i("MainActivity", "step4: views found");

        // ViewPager2
        TabPagerAdapter adapter = new TabPagerAdapter(this);
        Log.i("MainActivity", "step5: adapter created, setting...");
        viewPager.setAdapter(adapter);
        Log.i("MainActivity", "step6: adapter set OK");

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(TabPagerAdapter.getTabTitle(position));
        }).attach();
        Log.i("MainActivity", "step7: tabs attached");

        // 初始化 Repository
        repository = DeviceApplication.getDeviceRepository();
        if (repository != null) {
            repository.startMonitoring(settings.getRefreshIntervalMs());
            repository.loadStaticData();
        }
        Log.i("MainActivity", "step8: repository ready");

        // 权限引导
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PermissionHelper.requestPermissionsSequential(this, new PermissionHelper.PermissionCallback() {
                @Override
                public void onAllGranted() { }
                @Override
                public void onDenied() { }
            });
        }
        Log.i("MainActivity", "step9: onCreate done");
    }

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
            boolean isDark = !settings.isDarkMode();
            settings.setDarkMode(isDark);
            applyDarkMode();
            Toast.makeText(this, isDark ? "深色模式已开启" : "深色模式已关闭", Toast.LENGTH_SHORT).show();
            return true;
        }

        if (id == R.id.action_export) {
            String text = ExportHelper.exportToText(repository);
            ExportHelper.shareReport(this, text, getString(R.string.export_text_title));
            return true;
        }

        if (id == R.id.action_floating_window) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivity(intent);
                return true;
            }
            boolean enabled = settings.isFloatingWindowEnabled();
            if (enabled) {
                stopFloatingWindow();
                settings.setFloatingWindowEnabled(false);
                Toast.makeText(this, "悬浮窗已关闭", Toast.LENGTH_SHORT).show();
            } else {
                startFloatingWindow();
                settings.setFloatingWindowEnabled(true);
                Toast.makeText(this, "悬浮窗已开启", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        if (id == R.id.action_settings) {
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
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("设置")
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
