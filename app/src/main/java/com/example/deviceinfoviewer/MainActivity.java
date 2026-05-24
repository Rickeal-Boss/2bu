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

    // 🔧 逐级加回功能: 1=+repo 2=+Toolbar 3=+permissions 4=完整
    private static final int LEVEL = 3;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = AppSettings.getInstance(this);
        applyDarkMode();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            getWindow().setDecorFitsSystemWindows(false);

        setContentView(R.layout.activity_step3a);
        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager.setAdapter(new SafePagerAdapter(this));
        for (int i = 0; i < 5; i++)
            tabLayout.addTab(tabLayout.newTab().setText(SafePagerAdapter.getTabTitle(i)));
        final boolean[] s = {false};
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab t) {
                if (!s[0]) { s[0] = true; viewPager.setCurrentItem(t.getPosition(), false); s[0] = false; }
            }
            @Override public void onTabUnselected(TabLayout.Tab t) {}
            @Override public void onTabReselected(TabLayout.Tab t) {}
        });
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) {
                if (!s[0]) { TabLayout.Tab t = tabLayout.getTabAt(pos); if (t != null && !t.isSelected()) t.select(); }
            }
        });

        // 🔧 逐级测试
        if (LEVEL >= 1) {
            repository = DeviceApplication.getDeviceRepository();
            if (repository != null) { repository.startMonitoring(settings.getRefreshIntervalMs()); repository.loadStaticData(); }
            Log.i(TAG, "LEVEL 1: repo ok");
        }
        if (LEVEL >= 2) {
            setContentView(R.layout.activity_main); // 用正式布局替代
            // Toolbar setup (简化的，先验证布局能否加载)
            Log.i(TAG, "LEVEL 2: activity_main layout ok");
        }
        if (LEVEL >= 3) {
            PermissionHelper.requestPermissionsSequential(this, new PermissionHelper.PermissionCallback() {
                @Override public void onAllGranted() {}
                @Override public void onDenied() {}
            });
            Log.i(TAG, "LEVEL 3: permissions ok");
        }
    }

    public DeviceRepository getRepository() { return repository; }
    @Override public boolean onCreateOptionsMenu(Menu menu) { getMenuInflater().inflate(R.menu.main_menu, menu); return true; }
    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) { return super.onOptionsItemSelected(item); }
    private void applyDarkMode() { AppCompatDelegate.setDefaultNightMode(settings.isDarkMode() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO); }
    @Override protected void onDestroy() { if (repository != null) repository.stopMonitoring(); }
    @Override public void onRequestPermissionsResult(int rq, @NonNull String[] p, @NonNull int[] g) { super.onRequestPermissionsResult(rq, p, g); }
}
