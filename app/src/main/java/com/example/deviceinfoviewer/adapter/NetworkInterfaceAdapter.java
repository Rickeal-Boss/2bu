package com.example.deviceinfoviewer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deviceinfoviewer.R;
import com.example.deviceinfoviewer.data.model.NetworkInterfaceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 网络接口列表 RecyclerView Adapter
 */
public class NetworkInterfaceAdapter extends RecyclerView.Adapter<NetworkInterfaceAdapter.ViewHolder> {

    private final List<NetworkInterfaceInfo> interfaces = new ArrayList<>();

    public void setInterfaces(List<NetworkInterfaceInfo> interfaces) {
        this.interfaces.clear();
        if (interfaces != null) {
            this.interfaces.addAll(interfaces);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_network_interface, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NetworkInterfaceInfo iface = interfaces.get(position);
        holder.tvName.setText(iface.getName());
        holder.tvIp.setText("IP: " + (iface.getIpAddress().isEmpty() ? "N/A" : iface.getIpAddress()));
        holder.tvMac.setText("MAC: " + (iface.getMacAddress().isEmpty() ? "N/A" : iface.getMacAddress()));

        StringBuilder traffic = new StringBuilder();
        traffic.append("MTU: ").append(iface.getMtu() > 0 ? iface.getMtu() : "N/A");
        if (iface.getRxBytes() >= 0) {
            traffic.append("  RX: ").append(formatBytes(iface.getRxBytes()));
        }
        if (iface.getTxBytes() >= 0) {
            traffic.append("  TX: ").append(formatBytes(iface.getTxBytes()));
        }
        holder.tvTraffic.setText(traffic.toString());
    }

    @Override
    public int getItemCount() {
        return interfaces.size();
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1_073_741_824L) return String.format("%.2f GB", bytes / 1_073_741_824.0);
        if (bytes >= 1_048_576L) return String.format("%.1f MB", bytes / 1_048_576.0);
        if (bytes >= 1_024L) return String.format("%.0f KB", bytes / 1_024.0);
        return bytes + " B";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvIp, tvMac, tvTraffic;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_iface_name);
            tvIp = itemView.findViewById(R.id.tv_iface_ip);
            tvMac = itemView.findViewById(R.id.tv_iface_mac);
            tvTraffic = itemView.findViewById(R.id.tv_iface_traffic);
        }
    }
}
