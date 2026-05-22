package com.example.deviceinfoviewer.data.source;

import android.content.Context;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import androidx.core.content.ContextCompat;

import com.example.deviceinfoviewer.data.model.MobileNetworkInfo;

import java.lang.reflect.Method;

/**
 * 移动网络数据源，通过 TelephonyManager 获取网络信息
 */
public class MobileNetworkDataSource {

    private final Context context;

    public MobileNetworkDataSource(Context context) {
        this.context = context.getApplicationContext();
    }

    @SuppressWarnings("MissingPermission")
    public MobileNetworkInfo getMobileNetworkInfo() {
        MobileNetworkInfo info = new MobileNetworkInfo();

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) {
            return info;
        }

        // 网络类型
        info.setNetworkType(networkTypeToString(tm.getNetworkType()));

        // 运营商名称（需要权限 READ_PHONE_STATE）
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            info.setOperatorName(tm.getNetworkOperatorName());
            info.setMccMnc(tm.getNetworkOperator());
            info.setRoaming(tm.isNetworkRoaming());
        }

        // 信号强度（通过反射或其他方式）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            SignalStrength ss = tm.getSignalStrength();
            if (ss != null) {
                try {
                    Method method = SignalStrength.class.getMethod("getDbm");
                    int dbm = (int) method.invoke(ss);
                    info.setSignalStrengthDbm(dbm);
                } catch (Exception e) {
                    info.setSignalStrengthDbm(Integer.MIN_VALUE);
                }
            }
        } else {
            // API < 29 使用反射
            try {
                SignalStrength ss = tm.getSignalStrength();
                if (ss != null) {
                    Method method = SignalStrength.class.getMethod("getDbm");
                    int dbm = (int) method.invoke(ss);
                    info.setSignalStrengthDbm(dbm);
                }
            } catch (Exception e) {
                info.setSignalStrengthDbm(Integer.MIN_VALUE);
            }
        }

        return info;
    }

    private String networkTypeToString(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_LTE: return "LTE (4G)";
            case TelephonyManager.NETWORK_TYPE_NR: return "NR (5G)";
            case TelephonyManager.NETWORK_TYPE_HSPAP: return "HSPA+";
            case TelephonyManager.NETWORK_TYPE_HSDPA: return "HSDPA (3G)";
            case TelephonyManager.NETWORK_TYPE_HSUPA: return "HSUPA (3G)";
            case TelephonyManager.NETWORK_TYPE_UMTS: return "UMTS (3G)";
            case TelephonyManager.NETWORK_TYPE_EVDO_0: return "EVDO Rev 0";
            case TelephonyManager.NETWORK_TYPE_EVDO_A: return "EVDO Rev A";
            case TelephonyManager.NETWORK_TYPE_EVDO_B: return "EVDO Rev B";
            case TelephonyManager.NETWORK_TYPE_CDMA: return "CDMA";
            case TelephonyManager.NETWORK_TYPE_1xRTT: return "1xRTT";
            case TelephonyManager.NETWORK_TYPE_EDGE: return "EDGE (2G)";
            case TelephonyManager.NETWORK_TYPE_GPRS: return "GPRS (2G)";
            case TelephonyManager.NETWORK_TYPE_GSM: return "GSM (2G)";
            case TelephonyManager.NETWORK_TYPE_IDEN: return "iDEN";
            case TelephonyManager.NETWORK_TYPE_IWLAN: return "IWLAN";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
            default: return "未知";
        }
    }
}
