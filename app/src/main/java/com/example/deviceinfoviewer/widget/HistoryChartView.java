package com.example.deviceinfoviewer.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

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
 * 历史图表控件 — 深色主题适配
 */
public class HistoryChartView extends LinearLayout {

    // 深色主题色
    private static final int COLOR_TEXT_SECONDARY = 0xFF8B949E;
    private static final int COLOR_GRID = 0xFF30363D;
    private static final int COLOR_AXIS = 0xFF484F58;
    private static final int DEFAULT_LINE_COLOR = 0xFFFF7043;

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

        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDrawGridBackground(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(COLOR_AXIS);
        xAxis.setTextColor(COLOR_TEXT_SECONDARY);
        xAxis.setTextSize(10f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            @Override
            public String getFormattedValue(float value) {
                return sdf.format(new Date((long) value));
            }
        });

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(COLOR_GRID);
        leftAxis.setGridLineWidth(0.5f);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setTextColor(COLOR_TEXT_SECONDARY);
        leftAxis.setTextSize(10f);
        leftAxis.setAxisMinimum(0f);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setNoDataText("暂无数据");
        lineChart.setNoDataTextColor(COLOR_TEXT_SECONDARY);
    }

    public void addDataPoint(String label, long timestampMillis, float value) {
        LineData data = lineChart.getData();
        if (data == null) {
            data = new LineData();
            lineChart.setData(data);
        }

        LineDataSet set = (LineDataSet) data.getDataSetByLabel(label, true);
        if (set == null) {
            set = new LineDataSet(new ArrayList<>(), label);
            set.setColor(DEFAULT_LINE_COLOR);
            set.setCircleColor(DEFAULT_LINE_COLOR);
            set.setLineWidth(2.5f);
            set.setCircleRadius(3f);
            set.setDrawValues(false);
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            set.setCubicIntensity(0.05f);
            set.setDrawFilled(true);
            try {
                set.setFillDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0x60FF7043, 0x05FF7043}));
            } catch (Exception ignored) {}
            data.addDataSet(set);
        }

        data.addEntry(new Entry(timestampMillis, value), data.getIndexOfDataSet(set));
        data.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.setVisibleXRangeMaximum(60f);
        lineChart.moveViewToX(timestampMillis);
    }

    public void setData(String label, List<HistoryDataPoint> points) {
        if (points == null || points.isEmpty()) {
            lineChart.clear();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        for (HistoryDataPoint p : points) {
            entries.add(new Entry(p.getTimestampMillis(), p.getValue()));
        }

        LineDataSet set = new LineDataSet(entries, label);
        set.setColor(DEFAULT_LINE_COLOR);
        set.setCircleColor(DEFAULT_LINE_COLOR);
        set.setLineWidth(2.5f);
        set.setCircleRadius(3f);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.05f);
        set.setDrawFilled(true);
        try {
            set.setFillDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0x60FF7043, 0x05FF7043}));
        } catch (Exception ignored) {}

        LineData data = new LineData(set);
        lineChart.setData(data);
        lineChart.notifyDataSetChanged();
        lineChart.invalidate();
    }

    public void clear() {
        lineChart.clear();
    }

    public LineChart getLineChart() {
        return lineChart;
    }
}
