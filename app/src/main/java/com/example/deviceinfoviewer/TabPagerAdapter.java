package com.example.deviceinfoviewer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.deviceinfoviewer.fragment.CpuFragment;
import com.example.deviceinfoviewer.fragment.SafePlaceholderFragment;

/**
 * ViewPager2 的 FragmentStateAdapter — 竞品风格 Tab：CPU/GPU/内存/电池/网络
 */
public class TabPagerAdapter extends FragmentStateAdapter {

    private static final int TAB_COUNT = 5;
    private static final String[] TAB_TITLES = {
        "CPU", "GPU", "内存", "电池", "网络"
    };

    public TabPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // 🔍 逐步恢复 Fragment 定位崩溃 — 当前：仅 CPU 真实，其余占位
        if (position == 0) return new CpuFragment();
        return new SafePlaceholderFragment();
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
