package com.example.deviceinfoviewer;

import android.os.Build;
import android.os.Bundle;
import androidx.viewpager2.widget.ViewPager2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.deviceinfoviewer.data.repository.DeviceRepository;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppSettings settings = AppSettings.getInstance(this);
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

        ViewPager2 vp = findViewById(R.id.view_pager);
        vp.setOffscreenPageLimit(0);
        TabLayout tl = findViewById(R.id.tab_layout);
        vp.setAdapter(new TabPagerAdapter(this));
        for (int i = 0; i < TabPagerAdapter.TAB_COUNT; i++)
            tl.addTab(tl.newTab().setText(TabPagerAdapter.getTabTitle(i)));
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

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            DeviceRepository repo = DeviceApplication.getDeviceRepository();
            if (repo != null) { repo.startMonitoring(2000); repo.loadStaticData(); }
        }, 500);
    }
}
