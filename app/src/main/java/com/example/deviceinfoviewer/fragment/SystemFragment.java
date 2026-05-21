package com.example.deviceinfoviewer.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.deviceinfoviewer.DeviceApplication;
import com.example.deviceinfoviewer.FormatUtils;
import com.example.deviceinfoviewer.R;
import com.example.deviceinfoviewer.data.model.StorageInfo;
import com.example.deviceinfoviewer.data.model.SystemInfo;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 系统 Fragment — Build.* 参数 + 内核/JVM + 存储分区，直接观察 Repository LiveData
 */
public class SystemFragment extends Fragment {

    private DeviceRepository repo;

    private SearchView searchView;
    private LinearLayout buildParamsContainer;
    private TextView tvKernel, tvJvm, tvBootloader;
    private LinearLayout partitionsContainer;
    private SwipeRefreshLayout swipeRefresh;

    private final List<Map.Entry<String, String>> allBuildParams = new ArrayList<>();
    private String currentFilter = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_system, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repo = DeviceApplication.getDeviceRepository();

        searchView = view.findViewById(R.id.search_view);
        buildParamsContainer = view.findViewById(R.id.build_params_container);
        tvKernel = view.findViewById(R.id.tv_kernel);
        tvJvm = view.findViewById(R.id.tv_jvm);
        tvBootloader = view.findViewById(R.id.tv_bootloader);
        partitionsContainer = view.findViewById(R.id.partitions_container);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);

        // 搜索过滤
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentFilter = newText != null ? newText.toLowerCase().trim() : "";
                rebuildBuildParamsView();
                return true;
            }
        });

        if (repo == null) {
            return;
        }

        // 观察系统信息 LiveData
        repo.getSystemLiveData().observe(getViewLifecycleOwner(), sys -> {
            if (sys == null) return;

            // 内核版本
            String kernel = sys.getKernelVersion();
            tvKernel.setText(kernel != null && !kernel.isEmpty() ? kernel : "N/A");

            // Java VM
            String vmName = sys.getJavaRuntimeName();
            String vmVer = sys.getJavaVmVersion();
            if (vmName != null && !vmName.isEmpty()) {
                tvJvm.setText(vmName + (vmVer != null && !vmVer.isEmpty() ? " " + vmVer : ""));
            } else if (vmVer != null && !vmVer.isEmpty()) {
                tvJvm.setText(vmVer);
            } else {
                tvJvm.setText("N/A");
            }

            // Bootloader
            String bootloader = sys.getBootloader();
            tvBootloader.setText(bootloader != null && !bootloader.isEmpty() ? bootloader : "N/A");

            // Build 参数
            allBuildParams.clear();
            if (sys.getBuildFields() != null) {
                allBuildParams.addAll(sys.getBuildFields().entrySet());
            }
            rebuildBuildParamsView();
        });

        // 观察存储信息 LiveData
        repo.getStorageLiveData().observe(getViewLifecycleOwner(), sto -> {
            if (sto == null) return;
            partitionsContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            for (StorageInfo.PartitionInfo p : sto.getPartitions()) {
                View row = createDetailRow(inflater, p.getMountPoint(),
                        "总 " + FormatUtils.formatBytes(p.getTotalBytes())
                                + " / 可用 " + FormatUtils.formatBytes(p.getAvailableBytes()));
                partitionsContainer.addView(row);
            }
        });

        swipeRefresh.setOnRefreshListener(() -> {
            swipeRefresh.setRefreshing(false);
            if (repo != null) {
                repo.loadStaticData();
            }
        });
    }

    /**
     * 根据当前过滤词重建 Build 参数视图
     */
    private void rebuildBuildParamsView() {
        buildParamsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (Map.Entry<String, String> entry : allBuildParams) {
            if (!currentFilter.isEmpty()) {
                if (!entry.getKey().toLowerCase().contains(currentFilter)
                        && !entry.getValue().toLowerCase().contains(currentFilter)) {
                    continue;
                }
            }
            View row = createDetailRow(inflater, entry.getKey() + ":", entry.getValue());
            // 长按复制
            final String key = entry.getKey();
            final String value = entry.getValue();
            row.setOnLongClickListener(v -> {
                String text = key + " = " + value;
                ClipboardManager cm = (ClipboardManager) requireContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("system_info", text));
                Toast.makeText(requireContext(), R.string.copy_toast, Toast.LENGTH_SHORT).show();
                return true;
            });
            buildParamsContainer.addView(row);
        }
    }

    /**
     * 创建一个 detail_row（标签: 值）行
     */
    private View createDetailRow(LayoutInflater inflater, String label, String value) {
        LinearLayout row = new LinearLayout(getContext());
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dpToPx(6), 0, dpToPx(6));

        TextView tvLabel = new TextView(getContext());
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvLabel.setLayoutParams(labelParams);
        tvLabel.setText(label);
        tvLabel.setTextColor(getResources().getColor(R.color.text_secondary, null));
        tvLabel.setTextSize(14);

        TextView tvValue = new TextView(getContext());
        tvValue.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        tvValue.setText(value);
        tvValue.setTextColor(getResources().getColor(R.color.primary, null));
        tvValue.setTextSize(14);
        tvValue.setTypeface(tvValue.getTypeface(), android.graphics.Typeface.BOLD);

        row.addView(tvLabel);
        row.addView(tvValue);
        return row;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}
