package com.example.deviceinfoviewer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.deviceinfoviewer.fragment.BatteryFragment;
import com.example.deviceinfoviewer.fragment.DashboardFragment;
import com.example.deviceinfoviewer.fragment.HardwareFragment;
import com.example.deviceinfoviewer.fragment.NetworkFragment;
import com.example.deviceinfoviewer.fragment.SystemFragment;

/**
 * ViewPager2 的 FragmentStateAdapter，管理5个Tab
 */
public class TabPagerAdapter extends FragmentStateAdapter {

    private static final int TAB_COUNT = 5;
    private static final String[] TAB_TITLES = {
        "仪表盘", "硬件", "系统", "网络", "电池"
    };

    public TabPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new DashboardFragment();
            case 1: return new HardwareFragment();
            case 2: return new SystemFragment();
            case 3: return new NetworkFragment();
            case 4: return new BatteryFragment();
            default: return new DashboardFragment();
        }
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }

    /**
     * 获取 Tab 标题
     */
    public static String getTabTitle(int position) {
        if (position >= 0 && position < TAB_TITLES.length) {
            return TAB_TITLES[position];
        }
        return "";
    }
}
