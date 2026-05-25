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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        settings = AppSettings.getInstance(this);
        AppCompatDelegate.setDefaultNightMode(
                settings.isDarkMode() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            getWindow().setDecorFitsSystemWindows(false);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("System Monitor");
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, top, 0, 0);
            return insets;
        });

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

        repository = DeviceApplication.getDeviceRepository();
        if (repository != null) { repository.startMonitoring(settings.getRefreshIntervalMs()); repository.loadStaticData(); }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PermissionHelper.requestPermissionsSequential(this, new PermissionHelper.PermissionCallback() {
                @Override public void onAllGranted() {} @Override public void onDenied() {}
            });
        }
        Log.i(TAG, "init OK, dark=" + settings.isDarkMode());
    }

    public DeviceRepository getRepository() { return repository; }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu); return true;
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_dark_mode) {
            boolean isDark = !settings.isDarkMode(); settings.setDarkMode(isDark);
            AppCompatDelegate.setDefaultNightMode(isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            recreate();
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

    @Override protected void onDestroy() {
        super.onDestroy();
        if (repository != null) repository.stopMonitoring();
        stopService(new Intent(this, FloatingWindowService.class));
    }
    @Override public void onRequestPermissionsResult(int rq, @NonNull String[] p, @NonNull int[] g) { super.onRequestPermissionsResult(rq, p, g); }
}
