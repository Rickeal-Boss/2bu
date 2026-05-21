package com.example.deviceinfoviewer.data.repository;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.example.deviceinfoviewer.data.model.*;
import com.example.deviceinfoviewer.data.source.*;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 核心数据仓库，持有所有 DataSource，通过定时任务采集数据，通过 LiveData 推送更新
 */
public class DeviceRepository {

    public static final int DEFAULT_INTERVAL_MS = 2000;

    // 数据源
    private final CpuDataSource cpuDataSource;
    private final GpuDataSource gpuDataSource;
    private final BatteryDataSource batteryDataSource;
    private final MemoryDataSource memoryDataSource;
    private final StorageDataSource storageDataSource;
    private final WifiDataSource wifiDataSource;
    private final MobileNetworkDataSource mobileNetworkDataSource;
    private final NetworkInterfaceDataSource networkInterfaceDataSource;
    private final GpsDataSource gpsDataSource;
    private final SensorDataSource sensorDataSource;
    private final SystemDataSource systemDataSource;

    // 历史缓存
    private final HistoryCache historyCache;

    // LiveData
    private final MutableLiveData<CpuInfo> cpuLiveData = new MutableLiveData<>();
    private final MutableLiveData<GpuInfo> gpuLiveData = new MutableLiveData<>();
    private final MutableLiveData<BatteryInfo> batteryLiveData = new MutableLiveData<>();
    private final MutableLiveData<MemoryInfo> memoryLiveData = new MutableLiveData<>();
    private final MutableLiveData<StorageInfo> storageLiveData = new MutableLiveData<>();
    private final MutableLiveData<WifiDetailInfo> wifiLiveData = new MutableLiveData<>();
    private final MutableLiveData<MobileNetworkInfo> mobileNetworkLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<NetworkInterfaceInfo>> networkInterfacesLiveData = new MutableLiveData<>();
    private final MutableLiveData<GpsStatusInfo> gpsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<SensorItemInfo>> sensorsLiveData = new MutableLiveData<>();
    private final MutableLiveData<SystemInfo> systemLiveData = new MutableLiveData<>();

    private ScheduledExecutorService scheduler;
    private int intervalMs = DEFAULT_INTERVAL_MS;
    private boolean monitoring = false;

    public DeviceRepository(Context context) {
        Context appContext = context.getApplicationContext();
        this.cpuDataSource = new CpuDataSource();
        this.gpuDataSource = new GpuDataSource();
        this.batteryDataSource = new BatteryDataSource(appContext);
        this.memoryDataSource = new MemoryDataSource();
        this.storageDataSource = new StorageDataSource();
        this.wifiDataSource = new WifiDataSource(appContext);
        this.mobileNetworkDataSource = new MobileNetworkDataSource(appContext);
        this.networkInterfaceDataSource = new NetworkInterfaceDataSource();
        this.gpsDataSource = new GpsDataSource(appContext);
        this.sensorDataSource = new SensorDataSource(appContext);
        this.systemDataSource = new SystemDataSource();
        this.historyCache = new HistoryCache();
    }

    /**
     * 启动后台数据采集（幂等：已在运行时忽略）
     */
    public void startMonitoring(int intervalMs) {
        if (monitoring) return;
        monitoring = true;
        this.intervalMs = intervalMs;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(this::collectData, 0, intervalMs, TimeUnit.MILLISECONDS);

        // GPS 独立监听
        gpsDataSource.startListening(statusInfo -> gpsLiveData.postValue(statusInfo));
    }

    /**
     * 停止后台采集
     */
    public void stopMonitoring() {
        monitoring = false;
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        gpsDataSource.stopListening();
        historyCache.shutdown();
    }

    /**
     * 单次采集所有数据
     */
    private void collectData() {
        try {
            CpuInfo cpuInfo = cpuDataSource.getCpuInfo();
            cpuLiveData.postValue(cpuInfo);

            // 记录温度历史
            if (!Float.isNaN(cpuInfo.getTemperatureCelsius())) {
                historyCache.addPoint("cpu_temp", cpuInfo.getTemperatureCelsius());
            }
            // 记录频率历史（取最高频率核心）
            long maxCpuFreq = 0;
            for (CpuCoreInfo core : cpuInfo.getCores()) {
                if (core.getCurrentFreqKHz() > maxCpuFreq) {
                    maxCpuFreq = core.getCurrentFreqKHz();
                }
            }
            if (maxCpuFreq > 0) {
                historyCache.addPoint("cpu_freq", (float) maxCpuFreq);
            }
        } catch (Exception ignored) {}

        try {
            GpuInfo gpuInfo = gpuDataSource.getGpuInfo();
            gpuLiveData.postValue(gpuInfo);
        } catch (Exception ignored) {}

        try {
            BatteryInfo batteryInfo = batteryDataSource.getBatteryInfo();
            batteryLiveData.postValue(batteryInfo);
            if (!Float.isNaN(batteryInfo.getTemperatureCelsius())) {
                historyCache.addPoint("battery_temp", batteryInfo.getTemperatureCelsius());
            }
            if (batteryInfo.getPowerMilliwatts() >= 0) {
                historyCache.addPoint("battery_power", (float) batteryInfo.getPowerMilliwatts());
            }
            if (batteryInfo.getLevelPercent() >= 0) {
                historyCache.addPoint("battery_level", (float) batteryInfo.getLevelPercent());
            }
        } catch (Exception ignored) {}

        try {
            MemoryInfo memoryInfo = memoryDataSource.getMemoryInfo();
            memoryLiveData.postValue(memoryInfo);
            if (memoryInfo.getTotalKB() > 0) {
                float usagePct = (float) memoryInfo.getUsedKB() / memoryInfo.getTotalKB() * 100f;
                historyCache.addPoint("ram_usage", usagePct);
            }
        } catch (Exception ignored) {}

        try {
            StorageInfo storageInfo = storageDataSource.getStorageInfo();
            storageLiveData.postValue(storageInfo);
        } catch (Exception ignored) {}

        try {
            WifiDetailInfo wifiInfo = wifiDataSource.getWifiDetail();
            wifiLiveData.postValue(wifiInfo);
        } catch (Exception ignored) {}

        try {
            MobileNetworkInfo mobileInfo = mobileNetworkDataSource.getMobileNetworkInfo();
            mobileNetworkLiveData.postValue(mobileInfo);
        } catch (Exception ignored) {}

        try {
            List<NetworkInterfaceInfo> interfaces = networkInterfaceDataSource.getNetworkInterfaces();
            networkInterfacesLiveData.postValue(interfaces);
        } catch (Exception ignored) {}
    }

    /**
     * 一次性加载静态数据
     */
    public void loadStaticData() {
        try {
            systemLiveData.postValue(systemDataSource.getSystemInfo());
        } catch (Exception ignored) {}
        try {
            storageLiveData.postValue(storageDataSource.getStorageInfo());
        } catch (Exception ignored) {}
        try {
            sensorsLiveData.postValue(sensorDataSource.getAllSensors());
        } catch (Exception ignored) {}
    }

    public void setIntervalMs(int ms) {
        this.intervalMs = ms;
        if (scheduler != null && !scheduler.isShutdown()) {
            stopMonitoring();
            startMonitoring(ms);
        }
    }

    public int getIntervalMs() { return intervalMs; }
    public HistoryCache getHistoryCache() { return historyCache; }

    public MutableLiveData<CpuInfo> getCpuLiveData() { return cpuLiveData; }
    public MutableLiveData<GpuInfo> getGpuLiveData() { return gpuLiveData; }
    public MutableLiveData<BatteryInfo> getBatteryLiveData() { return batteryLiveData; }
    public MutableLiveData<MemoryInfo> getMemoryLiveData() { return memoryLiveData; }
    public MutableLiveData<StorageInfo> getStorageLiveData() { return storageLiveData; }
    public MutableLiveData<WifiDetailInfo> getWifiLiveData() { return wifiLiveData; }
    public MutableLiveData<MobileNetworkInfo> getMobileNetworkLiveData() { return mobileNetworkLiveData; }
    public MutableLiveData<List<NetworkInterfaceInfo>> getNetworkInterfacesLiveData() { return networkInterfacesLiveData; }
    public MutableLiveData<GpsStatusInfo> getGpsLiveData() { return gpsLiveData; }
    public MutableLiveData<List<SensorItemInfo>> getSensorsLiveData() { return sensorsLiveData; }
    public MutableLiveData<SystemInfo> getSystemLiveData() { return systemLiveData; }
}
