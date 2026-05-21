package com.example.deviceinfoviewer.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import com.example.deviceinfoviewer.adapter.NetworkInterfaceAdapter;
import com.example.deviceinfoviewer.data.model.GpsSatelliteInfo;
import com.example.deviceinfoviewer.data.model.GpsStatusInfo;
import com.example.deviceinfoviewer.data.model.MobileNetworkInfo;
import com.example.deviceinfoviewer.data.model.WifiDetailInfo;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;
import com.example.deviceinfoviewer.widget.MonitorChartView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NetworkFragment extends Fragment {

    private static final String TAG = "NetworkFragment";
    private DeviceRepository repo;
    private TextView tvWifiSsid, tvWifiSignal, tvWifiSpeed, tvWifiIp;
    private TextView tvMobileType, tvMobileOperator, tvMobileSignal, tvMobileRoaming;
    private TextView tvGpsEnabled, tvGpsSatellites, tvGpsCoord;
    private MonitorChartView chartNetActivity;
    private RecyclerView recyclerSatellites, recyclerNetInterfaces;
    private SatelliteAdapter satelliteAdapter;
    private NetworkInterfaceAdapter netInterfaceAdapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        try { return inflater.inflate(R.layout.fragment_network_new, container, false); }
        catch (Exception e) { Log.e(TAG, "onCreateView failed", e); return new TextView(getContext()); }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            repo = DeviceApplication.getDeviceRepository();
            tvWifiSsid = view.findViewById(R.id.tv_wifi_ssid);
            tvWifiSignal = view.findViewById(R.id.tv_wifi_signal);
            tvWifiSpeed = view.findViewById(R.id.tv_wifi_speed);
            tvWifiIp = view.findViewById(R.id.tv_wifi_ip);
            tvMobileType = view.findViewById(R.id.tv_mobile_type);
            tvMobileOperator = view.findViewById(R.id.tv_mobile_operator);
            tvMobileSignal = view.findViewById(R.id.tv_mobile_signal);
            tvMobileRoaming = view.findViewById(R.id.tv_mobile_roaming);
            tvGpsEnabled = view.findViewById(R.id.tv_gps_enabled);
            tvGpsSatellites = view.findViewById(R.id.tv_gps_satellites);
            tvGpsCoord = view.findViewById(R.id.tv_gps_coord);
            chartNetActivity = view.findViewById(R.id.chart_net_activity);
            recyclerSatellites = view.findViewById(R.id.recycler_satellites);
            recyclerNetInterfaces = view.findViewById(R.id.recycler_net_interfaces);

            if (chartNetActivity != null) { chartNetActivity.setTitle("网络活动"); chartNetActivity.setValueFormat("%.0f", " KB/s"); }

            if (recyclerSatellites != null) {
                recyclerSatellites.setLayoutManager(new LinearLayoutManager(getContext()));
                satelliteAdapter = new SatelliteAdapter();
                recyclerSatellites.setAdapter(satelliteAdapter);
            }
            if (recyclerNetInterfaces != null) {
                recyclerNetInterfaces.setLayoutManager(new LinearLayoutManager(getContext()));
                netInterfaceAdapter = new NetworkInterfaceAdapter();
                recyclerNetInterfaces.setAdapter(netInterfaceAdapter);
            }

            if (repo == null) return;
            repo.getWifiLiveData().observe(getViewLifecycleOwner(), this::updateWifi);
            repo.getMobileNetworkLiveData().observe(getViewLifecycleOwner(), this::updateMobile);
            repo.getGpsLiveData().observe(getViewLifecycleOwner(), this::updateGps);
            repo.getNetworkInterfacesLiveData().observe(getViewLifecycleOwner(), interfaces -> {
                if (netInterfaceAdapter != null && interfaces != null) netInterfaceAdapter.setInterfaces(interfaces);
            });
        } catch (Exception e) { Log.e(TAG, "onViewCreated failed", e); }
    }

    private void updateWifi(WifiDetailInfo wifi) {
        if (wifi == null) return;
        String ssid = wifi.getSsid();
        if (tvWifiSsid != null) tvWifiSsid.setText((ssid != null && !ssid.isEmpty()) ? ssid : "未连接 WiFi");
        if (tvWifiSignal != null) tvWifiSignal.setText(FormatUtils.formatDbm(wifi.getSignalDbm()));
        if (tvWifiSpeed != null) tvWifiSpeed.setText(wifi.getLinkSpeedMbps() > 0 ? wifi.getLinkSpeedMbps() + " Mbps" : "N/A");
        String ipv4 = wifi.getIpv4();
        if (tvWifiIp != null) tvWifiIp.setText((ipv4 != null && !ipv4.isEmpty()) ? ipv4 : "");
    }

    private void updateMobile(MobileNetworkInfo mobile) {
        if (mobile == null) return;
        if (tvMobileType != null) {
            String t = mobile.getNetworkType();
            tvMobileType.setText((t != null && !t.isEmpty()) ? t : "N/A");
        }
        if (tvMobileOperator != null) {
            String op = mobile.getOperatorName();
            tvMobileOperator.setText((op != null && !op.isEmpty()) ? op : "N/A");
        }
        if (tvMobileSignal != null) tvMobileSignal.setText(FormatUtils.formatDbm(mobile.getSignalStrengthDbm()));
        if (tvMobileRoaming != null) tvMobileRoaming.setText(mobile.isRoaming() ? "是" : "否");
    }

    private void updateGps(GpsStatusInfo gps) {
        if (gps == null) return;
        if (tvGpsEnabled != null) tvGpsEnabled.setText(gps.isGpsEnabled() ? (gps.isFixAcquired() ? "已定位" : "未定位") : "未启用");
        if (tvGpsSatellites != null) tvGpsSatellites.setText(String.valueOf(gps.getSatelliteCount()));
        if (tvGpsCoord != null && !Double.isNaN(gps.getLatitude()) && !Double.isNaN(gps.getLongitude()))
            tvGpsCoord.setText(String.format(Locale.US, "%.6f, %.6f", gps.getLatitude(), gps.getLongitude()));
        if (satelliteAdapter != null) satelliteAdapter.setSatellites(gps.getSatellites());
    }

    private static class SatelliteAdapter extends RecyclerView.Adapter<SatelliteAdapter.VH> {
        private final List<GpsSatelliteInfo> satellites = new ArrayList<>();
        void setSatellites(List<GpsSatelliteInfo> s) { satellites.clear(); if (s != null) satellites.addAll(s); notifyDataSetChanged(); }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_satellite, p, false)); }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            GpsSatelliteInfo sat = satellites.get(pos);
            String con = sat.getConstellation();
            h.tvSatFlag.setText(con != null ? getSymbol(con) : "\uD83D\uDEF0");
            h.tvSatName.setText((sat.getPrn() >= 0 ? "PRN " + sat.getPrn() : "") + (con != null ? " | " + con : ""));
            StringBuilder d = new StringBuilder();
            if (!Float.isNaN(sat.getSnr())) d.append("SNR ").append(String.format(Locale.US, "%.0f", sat.getSnr())).append("dB");
            if (!Float.isNaN(sat.getElevation())) d.append(" ").append(String.format(Locale.US, "%.0f", sat.getElevation())).append("°");
            h.tvSatDetail.setText(d.length() > 0 ? d.toString() : "N/A");
            if (!Float.isNaN(sat.getSnr())) { h.tvSatSnr.setText(String.format(Locale.US, "%.0fdB", sat.getSnr()));
                float snr = sat.getSnr(); int cr = snr >= 35 ? R.color.status_good : snr >= 25 ? R.color.status_warning : R.color.text_secondary;
                h.tvSatSnr.setTextColor(h.itemView.getResources().getColor(cr, null)); }
            else h.tvSatSnr.setText("N/A");
        }
        @Override public int getItemCount() { return satellites.size(); }
        private static String getSymbol(String c) {
            if (c == null) return "\uD83D\uDEF0";
            switch (c.toUpperCase()) { case "GPS": return "\uD83C\uDDFA\uD83C\uDDF8"; case "GLONASS": return "\uD83C\uDDF7\uD83C\uDDFA"; case "BEIDOU": return "\uD83C\uDDE8\uD83C\uDDF3"; case "GALILEO": return "\uD83C\uDDEA\uD83C\uDDFA"; default: return "\uD83D\uDEF0"; }
        }
        static class VH extends RecyclerView.ViewHolder {
            TextView tvSatFlag, tvSatName, tvSatDetail, tvSatSnr;
            VH(View v) { super(v); tvSatFlag = v.findViewById(R.id.tv_sat_flag); tvSatName = v.findViewById(R.id.tv_sat_name); tvSatDetail = v.findViewById(R.id.tv_sat_detail); tvSatSnr = v.findViewById(R.id.tv_sat_snr); }
        }
    }
}
