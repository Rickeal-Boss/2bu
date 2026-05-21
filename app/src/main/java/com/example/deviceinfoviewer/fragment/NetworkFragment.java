package com.example.deviceinfoviewer.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deviceinfoviewer.DeviceApplication;
import com.example.deviceinfoviewer.FormatUtils;
import com.example.deviceinfoviewer.R;
import com.example.deviceinfoviewer.data.model.GpsSatelliteInfo;
import com.example.deviceinfoviewer.data.model.GpsStatusInfo;
import com.example.deviceinfoviewer.data.model.WifiDetailInfo;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;
import com.example.deviceinfoviewer.widget.MonitorChartView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 网络 Fragment — 竞品风格：WiFi 卡片 + 网络活动图表 + GPS + 卫星列表
 */
public class NetworkFragment extends Fragment {

    private DeviceRepository repo;

    private TextView tvWifiSsid, tvWifiSignal, tvWifiSpeed, tvWifiIp;
    private TextView tvGpsEnabled, tvGpsSatellites, tvGpsCoord;
    private MonitorChartView chartNetActivity;
    private RecyclerView recyclerSatellites;
    private SatelliteAdapter satelliteAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_network_new, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repo = DeviceApplication.getDeviceRepository();

        tvWifiSsid = view.findViewById(R.id.tv_wifi_ssid);
        tvWifiSignal = view.findViewById(R.id.tv_wifi_signal);
        tvWifiSpeed = view.findViewById(R.id.tv_wifi_speed);
        tvWifiIp = view.findViewById(R.id.tv_wifi_ip);
        tvGpsEnabled = view.findViewById(R.id.tv_gps_enabled);
        tvGpsSatellites = view.findViewById(R.id.tv_gps_satellites);
        tvGpsCoord = view.findViewById(R.id.tv_gps_coord);
        chartNetActivity = view.findViewById(R.id.chart_net_activity);
        recyclerSatellites = view.findViewById(R.id.recycler_satellites);

        if (chartNetActivity != null) {
            chartNetActivity.setTitle("网络活动");
            chartNetActivity.setChartColor(Color.parseColor("#4CAF50"));
            chartNetActivity.setValueFormat("%.0f", " KB/s");
        }

        recyclerSatellites.setLayoutManager(new LinearLayoutManager(getContext()));
        satelliteAdapter = new SatelliteAdapter();
        recyclerSatellites.setAdapter(satelliteAdapter);

        if (repo == null) return;

        repo.getWifiLiveData().observe(getViewLifecycleOwner(), this::updateWifiInfo);
        repo.getGpsLiveData().observe(getViewLifecycleOwner(), this::updateGpsInfo);
    }

    private void updateWifiInfo(WifiDetailInfo wifi) {
        if (wifi == null) return;
        String ssid = wifi.getSsid();
        tvWifiSsid.setText(ssid != null && !ssid.isEmpty() ? ssid : "未连接 WiFi");
        tvWifiSignal.setText(FormatUtils.formatDbm(wifi.getSignalDbm()));
        tvWifiSpeed.setText(wifi.getLinkSpeedMbps() > 0 ? wifi.getLinkSpeedMbps() + " Mbps" : "N/A");
        String ipv4 = wifi.getIpv4();
        tvWifiIp.setText(ipv4 != null && !ipv4.isEmpty() ? ipv4 : "");
    }

    private void updateGpsInfo(GpsStatusInfo gps) {
        if (gps == null) return;
        tvGpsEnabled.setText(gps.isGpsEnabled()
                ? (gps.isFixAcquired() ? "已定位" : "未定位")
                : "未启用");
        tvGpsSatellites.setText(String.valueOf(gps.getSatelliteCount()));

        if (!Double.isNaN(gps.getLatitude()) && !Double.isNaN(gps.getLongitude())) {
            tvGpsCoord.setText(String.format(Locale.US, "%.6f, %.6f", gps.getLatitude(), gps.getLongitude()));
        } else {
            tvGpsCoord.setText("");
        }

        satelliteAdapter.setSatellites(gps.getSatellites());
    }

    // ---- Satellite Adapter ----
    private static class SatelliteAdapter extends RecyclerView.Adapter<SatelliteAdapter.VH> {
        private final List<GpsSatelliteInfo> satellites = new ArrayList<>();

        void setSatellites(List<GpsSatelliteInfo> satellites) {
            this.satellites.clear();
            if (satellites != null) this.satellites.addAll(satellites);
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_satellite, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            GpsSatelliteInfo sat = satellites.get(position);
            String con = sat.getConstellation();
            holder.tvSatFlag.setText(getConstellationSymbol(con));
            String name = sat.getPrn() >= 0 ? "PRN " + sat.getPrn() : "未知";
            if (con != null && !con.isEmpty()) name += " | " + con;
            holder.tvSatName.setText(name);

            StringBuilder detail = new StringBuilder();
            if (!Float.isNaN(sat.getSnr()))
                detail.append("SNR ").append(String.format(Locale.US, "%.0f", sat.getSnr())).append("dB");
            if (!Float.isNaN(sat.getElevation()))
                detail.append(" 仰角").append(String.format(Locale.US, "%.0f", sat.getElevation())).append("°");
            holder.tvSatDetail.setText(detail.length() > 0 ? detail.toString() : "N/A");

            if (!Float.isNaN(sat.getSnr())) {
                holder.tvSatSnr.setText(String.format(Locale.US, "%.0fdB", sat.getSnr()));
                float snr = sat.getSnr();
                int colorRes = snr >= 35 ? R.color.status_good : snr >= 25 ? R.color.status_warning : R.color.text_secondary;
                holder.tvSatSnr.setTextColor(holder.itemView.getResources().getColor(colorRes, null));
            } else {
                holder.tvSatSnr.setText("N/A");
            }
        }

        @Override public int getItemCount() { return satellites.size(); }

        private static String getConstellationSymbol(String c) {
            if (c == null) return "\uD83D\uDEF0";
            switch (c.toUpperCase()) {
                case "GPS": return "\uD83C\uDDFA\uD83C\uDDF8";
                case "GLONASS": return "\uD83C\uDDF7\uD83C\uDDFA";
                case "BEIDOU": return "\uD83C\uDDE8\uD83C\uDDF3";
                case "GALILEO": return "\uD83C\uDDEA\uD83C\uDDFA";
                case "QZSS": return "\uD83C\uDDEF\uD83C\uDDF5";
                default: return "\uD83D\uDEF0";
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvSatFlag, tvSatName, tvSatDetail, tvSatSnr;
            VH(View v) {
                super(v);
                tvSatFlag = v.findViewById(R.id.tv_sat_flag);
                tvSatName = v.findViewById(R.id.tv_sat_name);
                tvSatDetail = v.findViewById(R.id.tv_sat_detail);
                tvSatSnr = v.findViewById(R.id.tv_sat_snr);
            }
        }
    }
}
