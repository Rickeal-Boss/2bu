package com.example.deviceinfoviewer.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * 安全占位 Fragment — 用于崩溃定位
 * 如果这个能显示，说明 CpuFragment/GpuFragment 有问题
 */
public class SafePlaceholderFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        TextView tv = new TextView(getContext());
        tv.setText("App is alive! Crash is in other fragments.");
        tv.setTextSize(20);
        tv.setPadding(48, 48, 48, 48);
        return tv;
    }
}
