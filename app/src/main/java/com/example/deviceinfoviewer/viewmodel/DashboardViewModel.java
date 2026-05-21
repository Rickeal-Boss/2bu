package com.example.deviceinfoviewer.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.deviceinfoviewer.data.model.BatteryInfo;
import com.example.deviceinfoviewer.data.model.CpuInfo;
import com.example.deviceinfoviewer.data.model.MemoryInfo;
import com.example.deviceinfoviewer.data.model.StorageInfo;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;

/**
 * 仪表盘 ViewModel，聚合 CPU/电池/内存/存储 LiveData
 */
public class DashboardViewModel extends ViewModel {

    private DeviceRepository repository;

    public void setRepository(DeviceRepository repository) {
        this.repository = repository;
    }

    public LiveData<CpuInfo> getCpuInfo() {
        return repository != null ? repository.getCpuLiveData() : null;
    }

    public LiveData<BatteryInfo> getBatteryInfo() {
        return repository != null ? repository.getBatteryLiveData() : null;
    }

    public LiveData<MemoryInfo> getMemoryInfo() {
        return repository != null ? repository.getMemoryLiveData() : null;
    }

    public LiveData<StorageInfo> getStorageInfo() {
        return repository != null ? repository.getStorageLiveData() : null;
    }

    public void startRefresh() {
        if (repository != null) {
            repository.startMonitoring(DeviceRepository.DEFAULT_INTERVAL_MS);
        }
    }

    public void stopRefresh() {
        if (repository != null) {
            repository.stopMonitoring();
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopRefresh();
    }
}
