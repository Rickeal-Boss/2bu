package com.example.deviceinfoviewer.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.deviceinfoviewer.R;
import com.example.deviceinfoviewer.data.model.HistoryDataPoint;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 竞品风格监控图表 — 绿色系折线图，带填充效果
 * 基类改用 FrameLayout，配合 merge 标签避免嵌套布局冲突
 */
public class MonitorChartView extends FrameLayout {

    private TextView tvTitle;
    private TextView tvCurrentValue;
    private LineChart lineChart;
    private int chartColor = Color.parseColor("#4CAF50");
    private String valueFormat = "%.1f";
    private String valueSuffix = "";
    private String seriesName = "";

    public MonitorChartView(Context context) {
        super(context);
        init(context);
    }

    public MonitorChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MonitorChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // 使用 merge 标签的布局，子 View 直接添加到 MonitorChartView（FrameLayout）
        LayoutInflater.from(context).inflate(R.layout.widget_monitor_chart, this, true);

        tvTitle = findViewById(R.id.tv_chart_title);
        tvCurrentValue = findViewById(R.id.tv_chart_current);
        lineChart = findViewById(R.id.chart_line);

        if (lineChart != null) {
            configureChart(lineChart);
        }
    }

    private void configureChart(LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.setExtraOffsets(0, 4, 0, 8);

        // X 轴
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(0xFFE0E0E0);
        xAxis.setTextColor(0xFF9E9E9E);
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(3, true);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            @Override
            public String getFormattedValue(float value) {
                return sdf.format(new Date((long) value));
            }
        });

        // 左 Y 轴
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(0xFFE8E8E8);
        leftAxis.setGridLineWidth(0.5f);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setTextColor(0xFF9E9E9E);
        leftAxis.setTextSize(10f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setLabelCount(3, true);

        // 右 Y 轴关闭
        chart.getAxisRight().setEnabled(false);

        // 图例关闭
        Legend legend = chart.getLegend();
        legend.setEnabled(false);
    }

    public void setTitle(String title) {
        this.seriesName = title;
        if (tvTitle != null) {
            tvTitle.setText(title);
        }
    }

    public void setCurrentValue(float value) {
        if (tvCurrentValue != null) {
            tvCurrentValue.setText(String.format(Locale.US, valueFormat, value) + valueSuffix);
        }
    }

    public void setChartColor(int color) {
        this.chartColor = color;
    }

    public void setValueFormat(String format, String suffix) {
        this.valueFormat = format;
        this.valueSuffix = suffix;
    }

    public void setData(List<HistoryDataPoint> points) {
        if (lineChart == null) return;

        if (points == null || points.isEmpty()) {
            lineChart.clear();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        for (HistoryDataPoint p : points) {
            entries.add(new Entry(p.getTimestampMillis(), p.getValue()));
        }

        LineDataSet set = new LineDataSet(entries, seriesName);
        set.setColor(chartColor);
        set.setCircleColor(chartColor);
        set.setLineWidth(2f);
        set.setCircleRadius(2f);
        set.setDrawCircleHole(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.1f);

        // 填充效果
        set.setDrawFilled(true);
        Drawable fill = makeFillDrawable(chartColor);
        if (fill != null) {
            set.setFillDrawable(fill);
        }

        LineData data = new LineData(set);
        lineChart.setData(data);

        if (!points.isEmpty()) {
            HistoryDataPoint last = points.get(points.size() - 1);
            setCurrentValue(last.getValue());
        }

        lineChart.notifyDataSetChanged();
        lineChart.invalidate();
    }

    public void addDataPoint(long timestampMillis, float value) {
        if (lineChart == null) return;

        LineData data = lineChart.getData();
        if (data == null) {
            data = new LineData();
            lineChart.setData(data);
        }

        ILineDataSet existingSet = data.getDataSetByIndex(0);
        LineDataSet set;
        if (existingSet instanceof LineDataSet) {
            set = (LineDataSet) existingSet;
        } else {
            ArrayList<Entry> empty = new ArrayList<>();
            set = new LineDataSet(empty, seriesName);
            set.setColor(chartColor);
            set.setCircleColor(chartColor);
            set.setLineWidth(2f);
            set.setCircleRadius(2f);
            set.setDrawCircleHole(false);
            set.setDrawValues(false);
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            set.setCubicIntensity(0.1f);
            set.setDrawFilled(true);
            Drawable fill = makeFillDrawable(chartColor);
            if (fill != null) {
                set.setFillDrawable(fill);
            }
            data.addDataSet(set);
        }

        data.addEntry(new Entry(timestampMillis, value), 0);
        setCurrentValue(value);
        data.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.setVisibleXRangeMaximum(60f);
        lineChart.moveViewToX(timestampMillis);
    }

    private Drawable makeFillDrawable(int baseColor) {
        int r = Color.red(baseColor);
        int g = Color.green(baseColor);
        int b = Color.blue(baseColor);
        int[] colors = {
                Color.argb(80, r, g, b),
                Color.argb(10, r, g, b)
        };
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
    }

    public void clear() {
        if (lineChart != null) {
            lineChart.clear();
        }
    }

    public LineChart getLineChart() {
        return lineChart;
    }
}
