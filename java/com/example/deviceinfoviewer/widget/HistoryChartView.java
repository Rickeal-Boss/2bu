package com.example.deviceinfoviewer.widget;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.example.deviceinfoviewer.R;
import com.example.deviceinfoviewer.data.model.HistoryDataPoint;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 历史图表控件，封装 MPAndroidChart LineChart
 */
public class HistoryChartView extends LinearLayout {

    private LineChart lineChart;

    public HistoryChartView(Context context) {
        super(context);
        init(context);
    }

    public HistoryChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HistoryChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);

        lineChart = new LineChart(context);
        lineChart.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(lineChart);

        // 配置图表样式
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDrawGridBackground(false);

        // X 轴
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            @Override
            public String getFormattedValue(float value) {
                return sdf.format(new Date((long) value));
            }
        });

        // 左 Y 轴
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        // 右 Y 轴关闭
        lineChart.getAxisRight().setEnabled(false);

        lineChart.getLegend().setEnabled(true);
        lineChart.setNoDataText("暂无数据");
    }

    /**
     * 添加单个数据点
     */
    public void addDataPoint(String label, long timestampMillis, float value) {
        LineData data = lineChart.getData();
        if (data == null) {
            data = new LineData();
            lineChart.setData(data);
        }

        LineDataSet set = (LineDataSet) data.getDataSetByLabel(label, true);
        if (set == null) {
            set = new LineDataSet(new ArrayList<>(), label);
            set.setColor(Color.parseColor("#1565C0"));
            set.setCircleColor(Color.parseColor("#1565C0"));
            set.setLineWidth(2f);
            set.setCircleRadius(3f);
            set.setDrawValues(false);
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            data.addDataSet(set);
        }

        data.addEntry(new Entry(timestampMillis, value), data.getIndexOfDataSet(set));
        data.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.setVisibleXRangeMaximum(60f);
        lineChart.moveViewToX(timestampMillis);
    }

    /**
     * 设置完整数据
     */
    public void setData(String label, List<HistoryDataPoint> points) {
        if (points == null || points.isEmpty()) {
            lineChart.clear();
            lineChart.setNoDataText("暂无数据");
            return;
        }

        List<Entry> entries = new ArrayList<>();
        for (HistoryDataPoint p : points) {
            entries.add(new Entry(p.getTimestampMillis(), p.getValue()));
        }

        LineDataSet set = new LineDataSet(entries, label);
        set.setColor(Color.parseColor("#1565C0"));
        set.setCircleColor(Color.parseColor("#1565C0"));
        set.setLineWidth(2f);
        set.setCircleRadius(3f);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData data = new LineData(set);
        lineChart.setData(data);
        lineChart.notifyDataSetChanged();
        lineChart.invalidate();
    }

    /**
     * 清除图表
     */
    public void clear() {
        lineChart.clear();
        lineChart.setNoDataText("暂无数据");
    }

    public LineChart getLineChart() {
        return lineChart;
    }
}
