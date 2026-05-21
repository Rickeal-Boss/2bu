package com.example.deviceinfoviewer.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.deviceinfoviewer.data.model.CpuInfo;
import com.example.deviceinfoviewer.data.model.GpuInfo;
import com.example.deviceinfoviewer.data.model.SensorItemInfo;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;

import java.util.List;

public class HardwareViewModel extends ViewModel {
    private DeviceRepository repository;

    public void setRepository(DeviceRepository repository) { this.repository = repository; }

    public LiveData<CpuInfo> getCpuInfo() { return repository != null ? repository.getCpuLiveData() : null; }
    public LiveData<GpuInfo> getGpuInfo() { return repository != null ? repository.getGpuLiveData() : null; }
    public LiveData<List<SensorItemInfo>> getSensors() { return repository != null ? repository.getSensorsLiveData() : null; }

    public void loadSensors() { if (repository != null) repository.loadStaticData(); }

    @Override
    protected void onCleared() { super.onCleared(); }
}
