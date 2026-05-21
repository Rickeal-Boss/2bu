package com.example.deviceinfoviewer.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.deviceinfoviewer.data.model.BatteryInfo;
import com.example.deviceinfoviewer.data.model.HistoryDataPoint;
import com.example.deviceinfoviewer.data.repository.DeviceRepository;

import java.util.List;

public class BatteryViewModel extends ViewModel {
    private DeviceRepository repository;

    public void setRepository(DeviceRepository repository) { this.repository = repository; }

    public LiveData<BatteryInfo> getBatteryInfo() { return repository != null ? repository.getBatteryLiveData() : null; }

    public List<HistoryDataPoint> getTempHistory() {
        return repository != null ? repository.getHistoryCache().getSeries("battery_temp") : null;
    }

    public List<HistoryDataPoint> getPowerHistory() {
        return repository != null ? repository.getHistoryCache().getSeries("battery_power") : null;
    }

    public List<HistoryDataPoint> getLevelHistory() {
        return repository != null ? repository.getHistoryCache().getSeries("battery_level") : null;
    }

    @Override
    protected void onCleared() { super.onCleared(); }
}
