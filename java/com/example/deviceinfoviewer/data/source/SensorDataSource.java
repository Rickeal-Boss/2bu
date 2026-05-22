package com.example.deviceinfoviewer.data.source;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.example.deviceinfoviewer.data.model.SensorItemInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 传感器数据源，通过 SensorManager 获取所有传感器信息
 */
public class SensorDataSource {

    private final Context context;

    public SensorDataSource(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<SensorItemInfo> getAllSensors() {
        List<SensorItemInfo> result = new ArrayList<>();
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sm == null) {
            return result;
        }

        List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensors) {
            SensorItemInfo item = new SensorItemInfo();
            item.setName(sensor.getName());
            item.setType(sensor.getType());
            item.setVendor(sensor.getVendor());
            item.setPowerMa(sensor.getPower());
            item.setMaxRange(sensor.getMaximumRange());
            item.setResolution(sensor.getResolution());
            item.setMinDelay(sensor.getMinDelay());
            result.add(item);
        }

        return result;
    }
}
