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
import androidx.core.content.ContextCompat;
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
 * 主 Activity — DevCheck Pro 风格深色主题
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private DeviceRepository repository;
    private AppSettings settings;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    // 🔧 诊断模式：true = 最简布局
    private static final boolean DIAG_MINIMAL = true;
    // 🔧 诊断步骤：1=Toolbar 2=+TabLayout 3=+ViewPager 4=完整
    private static final int DIAG_STEP = 34;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate start, SDK=" + Build.VERSION.SDK_INT);

        settings = AppSettings.getInstance(this);
        applyDarkMode();
        Log.i(TAG, "dark mode applied, isDark=" + settings.isDarkMode());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }

        // 🔧 诊断分支
        if (DIAG_MINIMAL) {
            switch (DIAG_STEP) {
                case 1: setContentView(R.layout.activity_step1); break;
                case 2: setContentView(R.layout.activity_step2); break;
                case 3:
                    setContentView(R.layout.activity_step3);
                    viewPager = findViewById(R.id.view_pager);
                    tabLayout = findViewById(R.id.tab_layout);
                    viewPager.setOffscreenPageLimit(0);
                    viewPager.setAdapter(new SafePagerAdapter(this));
                    new com.google.android.material.tabs.TabLayoutMediator(
                            tabLayout, viewPager,
                            (tab, p) -> tab.setText(SafePagerAdapter.getTabTitle(p))
                    ).attach();
                    break;
                case 31:
                    // 3a: ViewPager2 无 CoordinatorLayout
                    setContentView(R.layout.activity_step3a);
                    viewPager = findViewById(R.id.view_pager);
                    tabLayout = findViewById(R.id.tab_layout);
                    viewPager.setOffscreenPageLimit(0);
                    viewPager.setAdapter(new SafePagerAdapter(this));
                    new com.google.android.material.tabs.TabLayoutMediator(
                            tabLayout, viewPager,
                            (tab, p) -> tab.setText(SafePagerAdapter.getTabTitle(p))
                    ).attach();
                    break;
                case 32:
                    // 3b: ViewPager2 单独，无 adapter 无 TabLayout
                    setContentView(R.layout.activity_step3b);
                    break;
                case 33:
                    // 3c: ViewPager2 + adapter，无 TabLayoutMediator
                    setContentView(R.layout.activity_step3c);
                    viewPager = findViewById(R.id.view_pager);
                    viewPager.setAdapter(new SafePagerAdapter(this));
                    break;
                case 34:
                    // 3d: 手动绑定 TabLayout ↔ ViewPager2（绕开 TabLayoutMediator）
                    setContentView(R.layout.activity_step3a);
                    viewPager = findViewById(R.id.view_pager);
                    tabLayout = findViewById(R.id.tab_layout);
                    viewPager.setOffscreenPageLimit(0);
                    viewPager.setAdapter(new SafePagerAdapter(this));
                    // 手动添加 Tab
                    for (int i = 0; i < 5; i++) {
                        tabLayout.addTab(tabLayout.newTab().setText(SafePagerAdapter.getTabTitle(i)));
                    }
                    tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                        @Override public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                            viewPager.setCurrentItem(tab.getPosition());
                        }
                        @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
                        @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
                    });
                    viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                        @Override public void onPageSelected(int position) {
                            com.google.android.material.tabs.TabLayout.Tab t = tabLayout.getTabAt(position);
                            if (t != null) t.select();
                        }
                    });
                    break;
                default: setContentView(R.layout.activity_minimal); break;
            }
            Log.i(TAG, "diag step " + DIAG_STEP + " layout OK");
            return;
        }

        setContentView(R.layout.activity_main);

        // 工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("System Monitor");
        }

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, top, 0, 0);
            return insets;
        });

        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);

        // ViewPager2 + TabLayout — offscreenPageLimit=0 避免启动时并行创建多个 MonitorChartView
        TabPagerAdapter adapter = new TabPagerAdapter(this);
        viewPager.setOffscreenPageLimit(0);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(TabPagerAdapter.getTabTitle(position));
        }).attach();

        // Tab 切换时改变指示器颜色
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int color = TabPagerAdapter.getTabColor(position);
                tabLayout.setSelectedTabIndicatorColor(color);
                tabLayout.setTabTextColors(
                        ContextCompat.getColor(MainActivity.this, R.color.text_on_dark_secondary),
                        color);
            }
        });

        // 初始化第一个 Tab 的颜色
        tabLayout.setSelectedTabIndicatorColor(TabPagerAdapter.getTabColor(0));
        tabLayout.setTabTextColors(
                ContextCompat.getColor(this, R.color.text_on_dark_secondary),
                TabPagerAdapter.getTabColor(0));

        // 初始化 Repository
        repository = DeviceApplication.getDeviceRepository();
        if (repository != null) {
            repository.startMonitoring(settings.getRefreshIntervalMs());
            repository.loadStaticData();
        }
        Log.i(TAG, "repository ready");

        // 权限引导
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PermissionHelper.requestPermissionsSequential(this, new PermissionHelper.PermissionCallback() {
                @Override public void onAllGranted() { }
                @Override public void onDenied() { }
            });
        }
        Log.i(TAG, "onCreate done");
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
            Toast.makeText(this, isDark ? "深色模式已开启" : "浅色模式已开启", Toast.LENGTH_SHORT).show();
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
