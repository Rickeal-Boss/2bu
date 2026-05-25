package com.example.deviceinfoviewer;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.viewpager2.widget.ViewPager2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.deviceinfoviewer.data.repository.DeviceRepository;
import com.example.deviceinfoviewer.util.PermissionHelper;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {

    // 🔧 STEP: 4=+repo  5=+perm
    private static final int STEP = 5;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppSettings settings = AppSettings.getInstance(this);
        AppCompatDelegate.setDefaultNightMode(
                settings.isDarkMode() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            getWindow().setDecorFitsSystemWindows(false);

        // 布局
        if (STEP >= 1) setContentView(R.layout.activity_main);
        else setContentView(R.layout.activity_step3a);

        // Toolbar
        if (STEP >= 2) {
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("System Monitor");
            ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(0, top, 0, 0);
                return insets;
            });
        }

        ViewPager2 vp = findViewById(R.id.view_pager);
        TabLayout tl = findViewById(R.id.tab_layout);
        vp.setOffscreenPageLimit(0);

        // Adapter
        if (STEP >= 3) vp.setAdapter(new TabPagerAdapter(this));
        else vp.setAdapter(new SafePagerAdapter(this));

        // Tabs + binding
        for (int i = 0; i < 5; i++) {
            String title = (STEP >= 3) ? TabPagerAdapter.getTabTitle(i) : SafePagerAdapter.getTabTitle(i);
            tl.addTab(tl.newTab().setText(title));
        }
        final boolean[] s = {false};
        tl.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab t) {
                if (!s[0]) { s[0] = true; vp.setCurrentItem(t.getPosition(), false); s[0] = false; }
            }
            @Override public void onTabUnselected(TabLayout.Tab t) {}
            @Override public void onTabReselected(TabLayout.Tab t) {}
        });
        vp.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) {
                if (!s[0]) { TabLayout.Tab t = tl.getTabAt(pos); if (t != null && !t.isSelected()) t.select(); }
            }
        });

        // Repository — 延迟启动，避免在 Fragment 未就绪时推送数据
        if (STEP >= 4) {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                DeviceRepository repo = DeviceApplication.getDeviceRepository();
                if (repo != null) { repo.startMonitoring(2000); repo.loadStaticData(); }
                Log.i("Main", "repo started");
            }, 500);
        }

        // Permissions
        if (STEP >= 5 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PermissionHelper.requestPermissionsSequential(this, new PermissionHelper.PermissionCallback() {
                @Override public void onAllGranted() {} @Override public void onDenied() {}
            });
        }

        Log.i("Main", "STEP " + STEP + " OK");
    }
}
