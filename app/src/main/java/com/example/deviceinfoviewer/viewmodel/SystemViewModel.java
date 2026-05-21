package com.example.deviceinfoviewer.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.deviceinfoviewer.data.model.StorageInfo;
import com.example.deviceinfoviewer.data.model.SystemInfo;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;

public class SystemViewModel extends ViewModel {
    private DeviceRepository repository;

    public void setRepository(DeviceRepository repository) { this.repository = repository; }

    public LiveData<SystemInfo> getSystemInfo() { return repository != null ? repository.getSystemLiveData() : null; }
    public LiveData<StorageInfo> getStorageInfo() { return repository != null ? repository.getStorageLiveData() : null; }

    public void loadOnce() {
        if (repository != null) repository.loadStaticData();
    }

    @Override
    protected void onCleared() { super.onCleared(); }
}
