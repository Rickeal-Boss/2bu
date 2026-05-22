package com.example.deviceinfoviewer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deviceinfoviewer.R;
import com.example.deviceinfoviewer.data.model.SensorItemInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 传感器列表 RecyclerView Adapter
 */
public class SensorListAdapter extends RecyclerView.Adapter<SensorListAdapter.ViewHolder> {

    private final List<SensorItemInfo> sensors = new ArrayList<>();

    public void setSensors(List<SensorItemInfo> sensors) {
        this.sensors.clear();
        if (sensors != null) {
            this.sensors.addAll(sensors);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sensor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SensorItemInfo sensor = sensors.get(position);
        holder.tvName.setText(sensor.getName());
        holder.tvType.setText("类型: " + getSensorTypeName(sensor.getType()));
        holder.tvVendor.setText("厂商: " + (sensor.getVendor() != null ? sensor.getVendor() : "未知"));

        StringBuilder detail = new StringBuilder();
        if (!Float.isNaN(sensor.getMaxRange())) {
            detail.append("最大量程: ").append(String.format("%.2f", sensor.getMaxRange()));
        }
        if (!Float.isNaN(sensor.getResolution())) {
            if (detail.length() > 0) detail.append("  ");
            detail.append("分辨率: ").append(String.format("%.4f", sensor.getResolution()));
        }
        if (!Float.isNaN(sensor.getPowerMa())) {
            if (detail.length() > 0) detail.append("  ");
            detail.append("功耗: ").append(String.format("%.2f", sensor.getPowerMa())).append("mA");
        }
        if (sensor.getMinDelay() > 0) {
            if (detail.length() > 0) detail.append("  ");
            detail.append("最小延迟: ").append(sensor.getMinDelay()).append("μs");
        }
        holder.tvDetail.setText(detail.toString());
    }

    @Override
    public int getItemCount() {
        return sensors.size();
    }

    private String getSensorTypeName(int type) {
        switch (type) {
            case 1: return "加速度计";
            case 2: return "磁力计";
            case 3: return "方向";
            case 4: return "陀螺仪";
            case 5: return "光线";
            case 6: return "压力";
            case 7: return "温度";
            case 8: return "距离";
            case 9: return "重力";
            case 10: return "线性加速度";
            case 11: return "旋转矢量";
            case 12: return "相对湿度";
            case 13: return "环境温度";
            case 14: return "磁场未校准";
            case 15: return "游戏旋转矢量";
            case 16: return "陀螺仪未校准";
            case 17: return "重要运动";
            case 18: return "步行检测";
            case 19: return "计步器";
            case 20: return "地磁旋转矢量";
            case 21: return "心率";
            default: return "传感器(" + type + ")";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType, tvVendor, tvDetail;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_sensor_name);
            tvType = itemView.findViewById(R.id.tv_sensor_type);
            tvVendor = itemView.findViewById(R.id.tv_sensor_vendor);
            tvDetail = itemView.findViewById(R.id.tv_sensor_detail);
        }
    }
}
