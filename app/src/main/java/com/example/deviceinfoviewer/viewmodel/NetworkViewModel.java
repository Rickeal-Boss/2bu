package com.example.deviceinfoviewer.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.deviceinfoviewer.data.model.*;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;

import java.util.List;

public class NetworkViewModel extends ViewModel {
    private DeviceRepository repository;

    public void setRepository(DeviceRepository repository) { this.repository = repository; }

    public LiveData<WifiDetailInfo> getWifiInfo() { return repository != null ? repository.getWifiLiveData() : null; }
    public LiveData<MobileNetworkInfo> getMobileInfo() { return repository != null ? repository.getMobileNetworkLiveData() : null; }
    public LiveData<List<NetworkInterfaceInfo>> getNetworkInterfaces() { return repository != null ? repository.getNetworkInterfacesLiveData() : null; }
    public LiveData<GpsStatusInfo> getGpsStatus() { return repository != null ? repository.getGpsLiveData() : null; }

    @Override
    protected void onCleared() { super.onCleared(); }
}
