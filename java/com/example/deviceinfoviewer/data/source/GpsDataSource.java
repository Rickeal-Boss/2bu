package com.example.deviceinfoviewer.data.source;

import android.content.Context;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;

import com.example.deviceinfoviewer.data.model.GpsSatelliteInfo;
import com.example.deviceinfoviewer.data.model.GpsStatusInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * GPS 数据源，兼容 API 21-23 GpsStatus.Listener 和 API 24+ GnssStatus.Callback
 */
public class GpsDataSource {

    private final Context context;
    private LocationManager locationManager;
    private boolean listening = false;
    private LocationListener locationListener;
    private GnssStatus.Callback gnssCallback;
    private GpsStatus.Listener gpsListener;

    public GpsDataSource(Context context) {
        this.context = context.getApplicationContext();
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public interface GpsCallback {
        void onGpsStatusUpdate(GpsStatusInfo statusInfo);
    }

    /**
     * 开始监听 GPS 状态
     */
    @SuppressWarnings("MissingPermission")
    public void startListening(final GpsCallback callback) {
        if (locationManager == null || listening) {
            return;
        }
        listening = true;

        try {
            // 创建共享的 LocationListener（保存为成员变量以便 stopListening 移除）
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    GpsStatusInfo info = new GpsStatusInfo();
                    info.setGpsEnabled(true);
                    info.setFixAcquired(true);
                    info.setLatitude(location.getLatitude());
                    info.setLongitude(location.getLongitude());
                    info.setAccuracy(location.getAccuracy());
                    callback.onGpsStatusUpdate(info);
                }
                @Override public void onStatusChanged(String p, int s, Bundle b) {}
                @Override public void onProviderEnabled(String p) {}
                @Override public void onProviderDisabled(String p) {}
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // API 24+ 使用 GnssStatus.Callback
                gnssCallback = new GnssStatus.Callback() {
                    @Override
                    public void onSatelliteStatusChanged(GnssStatus status) {
                        GpsStatusInfo info = new GpsStatusInfo();
                        info.setGpsEnabled(true);
                        info.setSatelliteCount(status.getSatelliteCount());
                        List<GpsSatelliteInfo> satellites = new ArrayList<>();
                        int usedCount = 0;
                        for (int i = 0; i < status.getSatelliteCount(); i++) {
                            GpsSatelliteInfo sat = new GpsSatelliteInfo();
                            int svid = status.getSvid(i);
                            int constellation = status.getConstellationType(i);
                            sat.setPrn(getStandardPrn(svid, constellation));
                            sat.setConstellation(getConstellationName(constellation));
                            sat.setSnr(status.getCn0DbHz(i));
                            sat.setElevation(status.getElevationDegrees(i));
                            sat.setAzimuth(status.getAzimuthDegrees(i));
                            sat.setUsedInFix(status.usedInFix(i));
                            if (status.usedInFix(i)) usedCount++;
                            satellites.add(sat);
                        }
                        info.setSatellites(satellites);
                        info.setFixAcquired(usedCount > 0);
                        callback.onGpsStatusUpdate(info);
                    }
                };
                locationManager.registerGnssStatusCallback(gnssCallback, null);
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 1000, 0,
                        locationListener, Looper.getMainLooper());

            } else {
                // API 21-23 使用 GpsStatus.Listener
                gpsListener = new GpsStatus.Listener() {
                    @Override
                    public void onGpsStatusChanged(int event) {
                        try {
                            @SuppressWarnings("deprecation")
                            GpsStatus gpsStatus = locationManager.getGpsStatus(null);
                            if (gpsStatus == null) return;

                            GpsStatusInfo info = new GpsStatusInfo();
                            info.setGpsEnabled(true);
                            info.setSatelliteCount(gpsStatus.getMaxSatellites());

                            List<GpsSatelliteInfo> satellites = new ArrayList<>();
                            int usedCount = 0;
                            @SuppressWarnings("deprecation")
                            Iterable<GpsSatellite> iterable = gpsStatus.getSatellites();
                            for (GpsSatellite sat : iterable) {
                                GpsSatelliteInfo si = new GpsSatelliteInfo();
                                si.setPrn(sat.getPrn());
                                si.setSnr(sat.getSnr());
                                si.setElevation(sat.getElevation());
                                si.setAzimuth(sat.getAzimuth());
                                si.setUsedInFix(sat.usedInFix());
                                if (sat.usedInFix()) usedCount++;
                                satellites.add(si);
                            }
                            info.setSatellites(satellites);
                            info.setFixAcquired(usedCount > 0);
                            callback.onGpsStatusUpdate(info);
                        } catch (Exception ignored) {}
                    }
                };
                locationManager.addGpsStatusListener(gpsListener);
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 1000, 0,
                        locationListener, Looper.getMainLooper());
            }
        } catch (SecurityException e) {
            // 权限未授予
            GpsStatusInfo info = new GpsStatusInfo();
            info.setGpsEnabled(false);
            callback.onGpsStatusUpdate(info);
        }
    }

    public void stopListening() {
        listening = false;
        if (locationManager != null) {
            try {
                // 移除 GnssStatus.Callback
                if (gnssCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    locationManager.unregisterGnssStatusCallback(gnssCallback);
                    gnssCallback = null;
                }
                // 移除 GpsStatus.Listener
                if (gpsListener != null) {
                    locationManager.removeGpsStatusListener(gpsListener);
                    gpsListener = null;
                }
                // 移除 LocationListener
                if (locationListener != null) {
                    locationManager.removeUpdates(locationListener);
                    locationListener = null;
                }
            } catch (Exception ignored) {}
        }
    }

    private String getConstellationName(int type) {
        switch (type) {
            case GnssStatus.CONSTELLATION_GPS: return "GPS";
            case GnssStatus.CONSTELLATION_SBAS: return "SBAS";
            case GnssStatus.CONSTELLATION_GLONASS: return "GLONASS";
            case GnssStatus.CONSTELLATION_QZSS: return "QZSS";
            case GnssStatus.CONSTELLATION_BEIDOU: return "BEIDOU";
            case GnssStatus.CONSTELLATION_GALILEO: return "GALILEO";
            case GnssStatus.CONSTELLATION_IRNSS: return "IRNSS";
            default: return "UNKNOWN";
        }
    }

    /** SVID → 标准 PRN 映射 */
    private int getStandardPrn(int svid, int constellationType) {
        switch (constellationType) {
            case GnssStatus.CONSTELLATION_BEIDOU:   return svid + 200;
            case GnssStatus.CONSTELLATION_GLONASS:   return svid + 64;
            case GnssStatus.CONSTELLATION_QZSS:      return svid + 192;
            default: return svid; // GPS/SBAS/Galileo/IRNSS: SVID = PRN
        }
    }
}
