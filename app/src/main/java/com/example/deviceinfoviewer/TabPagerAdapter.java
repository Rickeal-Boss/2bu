package com.example.deviceinfoviewer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.deviceinfoviewer.fragment.CpuFragment;
import com.example.deviceinfoviewer.fragment.GpuFragment;
import com.example.deviceinfoviewer.fragment.MemoryFragment;
import com.example.deviceinfoviewer.fragment.BatteryFragment;
import com.example.deviceinfoviewer.fragment.NetworkFragment;
import com.example.deviceinfoviewer.fragment.SafePlaceholderFragment;

/**
 * ViewPager2 FragmentStateAdapter
 *   DIAG_LEVEL: -1=全占位  0=只有CPU  1=CPU+GPU  ...  5=全部
 */
public class TabPagerAdapter extends FragmentStateAdapter {

    private static final int DIAG_LEVEL = 5;

    public static final int TAB_COUNT = 5;
    private static final String[] TAB_TITLES = {"CPU", "GPU", "内存", "电池", "网络"};
    public static final int[] TAB_COLORS = {
        0xFFFF9800, 0xFFAB47BC, 0xFF42A5F5, 0xFF66BB6A, 0xFF26C6DA,
    };

    public TabPagerAdapter(@NonNull FragmentActivity fa) { super(fa); }

    @NonNull @Override
    public Fragment createFragment(int position) {
        if (position > DIAG_LEVEL) return new SafePlaceholderFragment();
        switch (position) {
            case 0: return new CpuFragment();
            case 1: return new GpuFragment();
            case 2: return new MemoryFragment();
            case 3: return new BatteryFragment();
            case 4: return new NetworkFragment();
            default: return new SafePlaceholderFragment();
        }
    }

    @Override public int getItemCount() { return TAB_COUNT; }
    public static String getTabTitle(int pos) { return TAB_TITLES[pos]; }
    public static int getTabColor(int pos) { return TAB_COLORS[pos]; }
}
