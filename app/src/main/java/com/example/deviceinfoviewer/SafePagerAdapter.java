package com.example.deviceinfoviewer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.deviceinfoviewer.fragment.SafePlaceholderFragment;

/**
 * 诊断用安全适配器 — 仅 SafePlaceholderFragment，不碰任何图表组件
 */
public class SafePagerAdapter extends FragmentStateAdapter {

    private static final int TAB_COUNT = 5;
    private static final String[] TAB_TITLES = {"CPU","GPU","内存","电池","网络"};

    public SafePagerAdapter(@NonNull FragmentActivity fa) { super(fa); }

    @NonNull @Override
    public Fragment createFragment(int position) {
        return new SafePlaceholderFragment();
    }
    @Override public int getItemCount() { return TAB_COUNT; }
    public static String getTabTitle(int pos) { return TAB_TITLES[pos]; }
}
