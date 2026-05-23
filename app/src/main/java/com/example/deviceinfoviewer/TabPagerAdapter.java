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
 * ViewPager2 FragmentStateAdapter — 诊断模式
 *
 * 逐级启用 Fragment，定位闪退根因：
 *   LEVEL 0: 全部 SafePlaceholder — 验证主题/布局无问题
 *   LEVEL 1: 启用 CPU Fragment
 *   LEVEL 2: 启用 GPU Fragment
 *   ...
 */
public class TabPagerAdapter extends FragmentStateAdapter {

    // 🔧 诊断级别：0=全部占位 1=只有CPU 2=CPU+GPU ...
    private static final int DIAG_LEVEL = 0;

    private static final int TAB_COUNT = 5;
    private static final String[] TAB_TITLES = {
        "CPU", "GPU", "内存", "电池", "网络"
    };

    public static final int[] TAB_COLORS = {
        0xFFFF9800, 0xFFAB47BC, 0xFF42A5F5, 0xFF66BB6A, 0xFF26C6DA,
    };

    public TabPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position > DIAG_LEVEL) {
            return new SafePlaceholderFragment();
        }
        switch (position) {
            case 0: return new CpuFragment();
            case 1: return new GpuFragment();
            case 2: return new MemoryFragment();
            case 3: return new BatteryFragment();
            case 4: return new NetworkFragment();
            default: return new SafePlaceholderFragment();
        }
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }

    public static String getTabTitle(int position) {
        if (position >= 0 && position < TAB_TITLES.length) {
            return TAB_TITLES[position];
        }
        return "";
    }

    public static int getTabColor(int position) {
        if (position >= 0 && position < TAB_COLORS.length) {
            return TAB_COLORS[position];
        }
        return 0xFFFF9800;
    }
}
