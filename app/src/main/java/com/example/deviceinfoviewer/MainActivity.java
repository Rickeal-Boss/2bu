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

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private DeviceRepository repository;
    private AppSettings settings;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    // 🔧 逐级加回: 1=repo 2=Toolbar+menu 3=permissions 4=完整
    private static final int LEVEL = 4;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = AppSettings.getInstance(this);
        applyDarkMode();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            getWindow().setDecorFitsSystemWindows(false);

        // 先用 step3a 布局测试，排除布局问题
        setContentView(R.layout.activity_step3a);

        if (LEVEL >= 2) {
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("System Monitor");
            ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(0, top, 0, 0);
                return insets;
            });
        }

        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager.setOffscreenPageLimit(0);
        viewPager.setAdapter(new TabPagerAdapter(this));
        for (int i = 0; i < TabPagerAdapter.TAB_COUNT; i++)
            tabLayout.addTab(tabLayout.newTab().setText(TabPagerAdapter.getTabTitle(i)));
        final boolean[] s = {false};
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab t) {
                if (!s[0]) { s[0] = true; viewPager.setCurrentItem(t.getPosition(), false); s[0] = false; }
                int c = TabPagerAdapter.getTabColor(t.getPosition());
                tabLayout.setSelectedTabIndicatorColor(c);
                tabLayout.setTabTextColors(ContextCompat.getColor(MainActivity.this, R.color.text_on_dark_secondary), c);
            }
            @Override public void onTabUnselected(TabLayout.Tab t) {}
            @Override public void onTabReselected(TabLayout.Tab t) {}
        });
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) {
                if (!s[0]) { TabLayout.Tab t = tabLayout.getTabAt(pos); if (t != null && !t.isSelected()) t.select(); }
            }
        });
        // 延迟初始选中，避免 Fragment 事务在 onCreate 中崩溃
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            TabLayout.Tab first = tabLayout.getTabAt(0);
            if (first != null && !first.isSelected()) first.select();
        });

        if (LEVEL >= 1) {
            repository = DeviceApplication.getDeviceRepository();
            if (repository != null) { repository.startMonitoring(settings.getRefreshIntervalMs()); repository.loadStaticData(); }
        }
        if (LEVEL >= 3) {
            PermissionHelper.requestPermissionsSequential(this, new PermissionHelper.PermissionCallback() {
                @Override public void onAllGranted() {} @Override public void onDenied() {}
            });
        }
        Log.i(TAG, "LEVEL " + LEVEL + " init OK");
    }

    public DeviceRepository getRepository() { return repository; }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        if (LEVEL >= 2) { getMenuInflater().inflate(R.menu.main_menu, menu); return true; }
        return false;
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (LEVEL < 4) return super.onOptionsItemSelected(item);
        int id = item.getItemId();
        if (id == R.id.action_dark_mode) {
            boolean isDark = !settings.isDarkMode(); settings.setDarkMode(isDark);
            applyDarkMode();
            Toast.makeText(this, isDark ? "深色模式" : "浅色模式", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.action_export) {
            String t = ExportHelper.exportToText(repository);
            ExportHelper.shareReport(this, t, getString(R.string.export_text_title));
        } else if (id == R.id.action_floating_window) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请授予悬浮窗权限", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
            } else {
                boolean e = settings.isFloatingWindowEnabled();
                if (e) { stopService(new Intent(this, FloatingWindowService.class)); settings.setFloatingWindowEnabled(false); }
                else { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(new Intent(this, FloatingWindowService.class));
                       else startService(new Intent(this, FloatingWindowService.class));
                       settings.setFloatingWindowEnabled(true); }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void applyDarkMode() {
        AppCompatDelegate.setDefaultNightMode(settings.isDarkMode() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (repository != null) repository.stopMonitoring();
        stopService(new Intent(this, FloatingWindowService.class));
    }
    @Override public void onRequestPermissionsResult(int rq, @NonNull String[] p, @NonNull int[] g) { super.onRequestPermissionsResult(rq, p, g); }
}
