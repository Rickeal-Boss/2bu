package com.example.deviceinfoviewer;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewPager2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // V1: setDefaultNightMode 放在 super.onCreate 之前
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            getWindow().setDecorFitsSystemWindows(false);

        setContentView(R.layout.activity_step3a);
        ViewPager2 vp = findViewById(R.id.view_pager);
        TabLayout tl = findViewById(R.id.tab_layout);
        vp.setAdapter(new SafePagerAdapter(this));
        for (int i = 0; i < 5; i++)
            tl.addTab(tl.newTab().setText(SafePagerAdapter.getTabTitle(i)));
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
        Log.i("Main", "V1 OK");
    }
}
