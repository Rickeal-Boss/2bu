package com.example.deviceinfoviewer.util;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 权限引导工具类，三步引导流程
 * 通过 pendingCallback/pendingRequestCode 机制在 MainActivity.onRequestPermissionsResult 中接收结果
 */
public final class PermissionHelper {

    private PermissionHelper() {}

    public interface PermissionCallback {
        void onAllGranted();
        void onDenied();
    }

    private static final String[] LOCATION_PERMS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    // 待处理的权限回调
    private static PermissionCallback pendingCallback;
    private static int pendingRequestCode = -1;

    /**
     * 三步引导：定位权限 → 悬浮窗权限 → 电话状态权限
     */
    public static void requestPermissionsSequential(Activity activity, PermissionCallback callback) {
        requestLocationPermission(activity, () -> {
            requestOverlayPermission(activity, () -> {
                requestPhonePermission(activity, callback);
            }, callback);
        }, callback);
    }

    /**
     * 处理权限请求结果 —— 由 Activity 的 onRequestPermissionsResult 调用
     */
    public static void onPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (pendingCallback != null && requestCode == pendingRequestCode) {
            PermissionCallback cb = pendingCallback;
            pendingCallback = null;
            pendingRequestCode = -1;

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cb.onAllGranted();
            } else {
                cb.onDenied();
            }
        }
    }

    private static void requestLocationPermission(Activity activity, Runnable onGranted, PermissionCallback callback) {
        if (hasLocationPermission(activity)) {
            onGranted.run();
            return;
        }

        new AlertDialog.Builder(activity)
                .setTitle("需要定位权限")
                .setMessage("用于获取 GPS 和网络位置信息，以便展示定位状态。")
                .setPositiveButton("授予", (dialog, which) -> {
                    pendingCallback = new PermissionCallback() {
                        @Override public void onAllGranted() { onGranted.run(); }
                        @Override public void onDenied() { onGranted.run(); }
                    };
                    pendingRequestCode = 100;
                    ActivityCompat.requestPermissions(activity, LOCATION_PERMS, 100);
                })
                .setNegativeButton("跳过", (dialog, which) -> onGranted.run())
                .show();
    }

    private static void requestOverlayPermission(Activity activity, Runnable onGranted, PermissionCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(activity)) {
            onGranted.run();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            new AlertDialog.Builder(activity)
                    .setTitle("需要悬浮窗权限")
                    .setMessage("用于在其他应用上方显示设备信息悬浮窗。")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivityForResult(intent, 200);
                    })
                    .setNegativeButton("跳过", (dialog, which) -> {})
                    .show();
        }
        // 悬浮窗权限不阻塞后续流程
        onGranted.run();
    }

    private static void requestPhonePermission(Activity activity, PermissionCallback callback) {
        if (hasPhonePermission(activity)) {
            callback.onAllGranted();
            return;
        }

        new AlertDialog.Builder(activity)
                .setTitle("需要电话权限")
                .setMessage("用于获取移动网络类型和运营商信息。")
                .setPositiveButton("授予", (dialog, which) -> {
                    pendingCallback = callback;
                    pendingRequestCode = 300;
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.READ_PHONE_STATE}, 300);
                })
                .setNegativeButton("跳过", (dialog, which) -> callback.onAllGranted())
                .show();
    }

    public static boolean hasLocationPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasPhonePermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
    }
}
