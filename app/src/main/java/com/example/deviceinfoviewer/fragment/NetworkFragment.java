package com.example.deviceinfoviewer.fragment;

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.deviceinfoviewer.DeviceApplication;
import com.example.deviceinfoviewer.FormatUtils;
import com.example.deviceinfoviewer.R;
import com.example.deviceinfoviewer.adapter.NetworkInterfaceAdapter;
import com.example.deviceinfoviewer.data.model.GpsSatelliteInfo;
import com.example.deviceinfoviewer.data.model.GpsStatusInfo;
import com.example.deviceinfoviewer.data.model.MobileNetworkInfo;
import com.example.deviceinfoviewer.data.model.NetworkInterfaceInfo;
import com.example.deviceinfoviewer.data.model.WifiDetailInfo;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 网络 Fragment — WiFi / 移动网络 / 网络接口 / GPS 卫星，直接观察 Repository LiveData
 */
public class NetworkFragment extends Fragment {

    private DeviceRepository repo;

    // WiFi
    private TextView tvWifiSsid, tvWifiBssid, tvWifiSignal, tvWifiSpeed;
    private TextView tvWifiIpv4, tvWifiIpv6, tvWifiMac, tvWifiGateway, tvWifiDns;
    // 移动网络
    private TextView tvMobileType, tvMobileOperator, tvMobileSignal, tvMobileRoaming;
    // GPS
    private TextView tvGpsEnabled, tvGpsLongitude, tvGpsLatitude, tvGpsAccuracy, tvGpsSatellites;
    // 列表
    private RecyclerView recyclerNetInterfaces, recyclerSatellites;
    private NetworkInterfaceAdapter netInterfaceAdapter;
    private SatelliteAdapter satelliteAdapter;
    private SwipeRefreshLayout swipeRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_network, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repo = DeviceApplication.getDeviceRepository();

        // WiFi
        tvWifiSsid = view.findViewById(R.id.tv_wifi_ssid);
        tvWifiBssid = view.findViewById(R.id.tv_wifi_bssid);
        tvWifiSignal = view.findViewById(R.id.tv_wifi_signal);
        tvWifiSpeed = view.findViewById(R.id.tv_wifi_speed);
        tvWifiIpv4 = view.findViewById(R.id.tv_wifi_ipv4);
        tvWifiIpv6 = view.findViewById(R.id.tv_wifi_ipv6);
        tvWifiMac = view.findViewById(R.id.tv_wifi_mac);
        tvWifiGateway = view.findViewById(R.id.tv_wifi_gateway);
        tvWifiDns = view.findViewById(R.id.tv_wifi_dns);
        // 移动网络
        tvMobileType = view.findViewById(R.id.tv_mobile_type);
        tvMobileOperator = view.findViewById(R.id.tv_mobile_operator);
        tvMobileSignal = view.findViewById(R.id.tv_mobile_signal);
        tvMobileRoaming = view.findViewById(R.id.tv_mobile_roaming);
        // GPS
        tvGpsEnabled = view.findViewById(R.id.tv_gps_enabled);
        tvGpsLongitude = view.findViewById(R.id.tv_gps_longitude);
        tvGpsLatitude = view.findViewById(R.id.tv_gps_latitude);
        tvGpsAccuracy = view.findViewById(R.id.tv_gps_accuracy);
        tvGpsSatellites = view.findViewById(R.id.tv_gps_satellites);

        // 网络接口 RecyclerView
        recyclerNetInterfaces = view.findViewById(R.id.recycler_net_interfaces);
        recyclerNetInterfaces.setLayoutManager(new LinearLayoutManager(getContext()));
        netInterfaceAdapter = new NetworkInterfaceAdapter();
        recyclerNetInterfaces.setAdapter(netInterfaceAdapter);

        // 卫星 RecyclerView
        recyclerSatellites = view.findViewById(R.id.recycler_satellites);
        recyclerSatellites.setLayoutManager(new LinearLayoutManager(getContext()));
        satelliteAdapter = new SatelliteAdapter();
        recyclerSatellites.setAdapter(satelliteAdapter);

        swipeRefresh = view.findViewById(R.id.swipe_refresh);

        if (repo == null) {
            return;
        }

        // 观察 WiFi LiveData
        repo.getWifiLiveData().observe(getViewLifecycleOwner(), wifi -> {
            if (wifi == null) return;
            tvWifiSsid.setText(wifi.getSsid() != null && !wifi.getSsid().isEmpty()
                    ? wifi.getSsid() : "N/A");
            tvWifiBssid.setText(wifi.getBssid() != null && !wifi.getBssid().isEmpty()
                    ? wifi.getBssid() : "N/A");
            tvWifiSignal.setText(FormatUtils.formatDbm(wifi.getSignalDbm()));
            tvWifiSpeed.setText(wifi.getLinkSpeedMbps() > 0
                    ? wifi.getLinkSpeedMbps() + " Mbps" : "N/A");
            tvWifiIpv4.setText(wifi.getIpv4() != null && !wifi.getIpv4().isEmpty()
                    ? wifi.getIpv4() : "N/A");
            tvWifiIpv6.setText(wifi.getIpv6() != null && !wifi.getIpv6().isEmpty()
                    ? wifi.getIpv6() : "N/A");
            tvWifiMac.setText(wifi.getMacAddress() != null && !wifi.getMacAddress().isEmpty()
                    ? wifi.getMacAddress() : "N/A");
            tvWifiGateway.setText(wifi.getGateway() != null && !wifi.getGateway().isEmpty()
                    ? wifi.getGateway() : "N/A");
            tvWifiDns.setText(wifi.getDns() != null && !wifi.getDns().isEmpty()
                    ? wifi.getDns() : "N/A");
        });

        // 观察移动网络 LiveData
        repo.getMobileNetworkLiveData().observe(getViewLifecycleOwner(), mobile -> {
            if (mobile == null) return;
            tvMobileType.setText(mobile.getNetworkType() != null
                    && !mobile.getNetworkType().isEmpty() ? mobile.getNetworkType() : "N/A");
            tvMobileOperator.setText(mobile.getOperatorName() != null
                    && !mobile.getOperatorName().isEmpty() ? mobile.getOperatorName() : "N/A");
            tvMobileSignal.setText(FormatUtils.formatDbm(mobile.getSignalStrengthDbm()));
            tvMobileRoaming.setText(mobile.isRoaming() ? "是" : "否");
        });

        // 观察网络接口列表 LiveData
        repo.getNetworkInterfacesLiveData().observe(getViewLifecycleOwner(), interfaces -> {
            if (interfaces != null) {
                netInterfaceAdapter.setInterfaces(interfaces);
            }
        });

        // 观察 GPS LiveData
        repo.getGpsLiveData().observe(getViewLifecycleOwner(), gps -> {
            if (gps == null) return;
            tvGpsEnabled.setText(gps.isGpsEnabled()
                    ? (gps.isFixAcquired() ? "已定位" : "未定位")
                    : "未启用/无权限");
            tvGpsLatitude.setText(!Double.isNaN(gps.getLatitude())
                    ? String.format(Locale.US, "%.6f", gps.getLatitude()) : "N/A");
            tvGpsLongitude.setText(!Double.isNaN(gps.getLongitude())
                    ? String.format(Locale.US, "%.6f", gps.getLongitude()) : "N/A");
            tvGpsAccuracy.setText(!Float.isNaN(gps.getAccuracy())
                    ? String.format(Locale.US, "%.1fm", gps.getAccuracy()) : "N/A");
            tvGpsSatellites.setText(String.valueOf(gps.getSatelliteCount()));
            satelliteAdapter.setSatellites(gps.getSatellites());
        });

        swipeRefresh.setOnRefreshListener(() -> {
            swipeRefresh.setRefreshing(false);
            if (repo != null) {
                repo.loadStaticData();
            }
        });
    }

    // ---- Satellite Adapter (内部类) ----
    private static class SatelliteAdapter extends RecyclerView.Adapter<SatelliteAdapter.VH> {

        private final List<GpsSatelliteInfo> satellites = new ArrayList<>();

        void setSatellites(List<GpsSatelliteInfo> satellites) {
            this.satellites.clear();
            if (satellites != null) {
                this.satellites.addAll(satellites);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_satellite, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            GpsSatelliteInfo sat = satellites.get(position);

            String constellation = sat.getConstellation();
            holder.tvSatFlag.setText(getConstellationSymbol(constellation));

            String name;
            if (sat.getPrn() >= 0) {
                name = "PRN " + sat.getPrn();
                if (constellation != null && !constellation.isEmpty()) {
                    name += " | " + constellation;
                }
            } else {
                name = constellation != null && !constellation.isEmpty() ? constellation : "未知";
            }
            holder.tvSatName.setText(name);

            StringBuilder detail = new StringBuilder();
            if (!Float.isNaN(sat.getSnr())) {
                detail.append("SNR ").append(String.format(Locale.US, "%.0f", sat.getSnr())).append("dB");
            }
            if (!Float.isNaN(sat.getElevation())) {
                detail.append(" 仰角").append(String.format(Locale.US, "%.0f", sat.getElevation())).append("°");
            }
            if (!Float.isNaN(sat.getAzimuth())) {
                detail.append(" 方位").append(String.format(Locale.US, "%.0f", sat.getAzimuth())).append("°");
            }
            holder.tvSatDetail.setText(detail.length() > 0 ? detail.toString() : "N/A");

            if (!Float.isNaN(sat.getSnr())) {
                holder.tvSatSnr.setText(String.format(Locale.US, "%.0fdB", sat.getSnr()));
                float snr = sat.getSnr();
                int colorRes = snr >= 35 ? R.color.status_good
                        : snr >= 25 ? R.color.status_warning
                        : R.color.text_secondary;
                holder.tvSatSnr.setTextColor(
                        holder.itemView.getResources().getColor(colorRes, null));
            } else {
                holder.tvSatSnr.setText("N/A");
                holder.tvSatSnr.setTextColor(
                        holder.itemView.getResources().getColor(R.color.text_secondary, null));
            }
        }

        @Override
        public int getItemCount() {
            return satellites.size();
        }

        private static String getConstellationSymbol(String constellation) {
            if (constellation == null) return "\uD83D\uDEF0";
            switch (constellation.toUpperCase()) {
                case "GPS":
                    return "\uD83C\uDDFA\uD83C\uDDF8";
                case "GLONASS":
                    return "\uD83C\uDDF7\uD83C\uDDFA";
                case "BEIDOU":
                    return "\uD83C\uDDE8\uD83C\uDDF3";
                case "GALILEO":
                    return "\uD83C\uDDEA\uD83C\uDDFA";
                case "QZSS":
                    return "\uD83C\uDDEF\uD83C\uDDF5";
                case "IRNSS":
                    return "\uD83C\uDDEE\uD83C\uDDF3";
                case "SBAS":
                    return "\uD83D\uDEF0";
                default:
                    return "\uD83D\uDEF0";
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
