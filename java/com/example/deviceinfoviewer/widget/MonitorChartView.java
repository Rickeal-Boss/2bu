package com.example.deviceinfoviewer.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

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
 * 竞品风格监控图表 — 构造函数全 try-catch 防护
 */
public class MonitorChartView extends LinearLayout {

    private static final String TAG = "MonitorChart";
    private TextView tvTitle;
    private TextView tvCurrentValue;
    private LineChart lineChart;

    private int chartColor = Color.parseColor("#4CAF50");
    private String valueFormat = "%.1f";
    private String valueSuffix = "";
    private String seriesName = "";

    public MonitorChartView(Context context) {
        super(context);
        safeInit(context);
    }

    public MonitorChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        safeInit(context);
    }

    public MonitorChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        safeInit(context);
    }

    /** 安全初始化 — 任何步骤失败都不崩溃 */
    private void safeInit(Context ctx) {
        try {
            setOrientation(VERTICAL);
            tvTitle = buildHeaderRow(ctx);
            try {
                lineChart = buildChart(ctx);
                if (lineChart != null) configureChart(lineChart);
            } catch (Throwable t) {
                Log.e(TAG, "LineChart init failed", t);
                lineChart = null;
                // 降级：显示纯文本标题
                if (tvTitle != null) tvTitle.setText(tvTitle.getText() + " (图表加载失败)");
            }
            tvCurrentValue = findCurrentValueView();
        } catch (Throwable t) {
            Log.e(TAG, "safeInit failed", t);
            // 即使全部失败，至少显示错误文本
            try {
                TextView err = new TextView(ctx);
                err.setText("图表加载失败");
                err.setTextColor(0xFF757575);
                err.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                err.setPadding(dp(16, ctx), dp(16, ctx), dp(16, ctx), dp(16, ctx));
                addView(err);
            } catch (Throwable ignored) {}
        }
    }

    private TextView buildHeaderRow(Context ctx) {
        setOrientation(VERTICAL);
        LinearLayout headerRow = new LinearLayout(ctx);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        int px4 = dp(4, ctx);
        headerRow.setPadding(px4, px4, px4, dp(2, ctx));

        TextView title = new TextView(ctx);
        title.setId(View.generateViewId());
        title.setTextColor(0xFF757575);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        title.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView current = new TextView(ctx);
        current.setId(View.generateViewId());
        current.setTextColor(0xFF212121);
        current.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        current.setTypeface(null, Typeface.BOLD);
        current.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        headerRow.addView(title);
        headerRow.addView(current);
        addView(headerRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return title;
    }

    private LineChart buildChart(Context ctx) {
        LineChart chart = new LineChart(ctx);
        chart.setId(View.generateViewId());
        chart.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(120, ctx)));
        addView(chart);
        return chart;
    }

    private TextView findCurrentValueView() {
        try {
            if (getChildCount() > 0) {
                View header = getChildAt(0);
                if (header instanceof LinearLayout && ((LinearLayout) header).getChildCount() >= 2) {
                    View v = ((LinearLayout) header).getChildAt(1);
                    if (v instanceof TextView) return (TextView) v;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private void configureChart(LineChart chart) {
        try {
            chart.getDescription().setEnabled(false);
            chart.setTouchEnabled(true);
            chart.setDragEnabled(true);
            chart.setScaleEnabled(true);
            chart.setPinchZoom(true);
            chart.setDrawGridBackground(false);
            chart.setExtraOffsets(0, 4, 0, 8);
            chart.getAxisRight().setEnabled(false);
            chart.getLegend().setEnabled(false);

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

            YAxis leftAxis = chart.getAxisLeft();
            leftAxis.setDrawGridLines(true);
            leftAxis.setGridColor(0xFFE8E8E8);
            leftAxis.setGridLineWidth(0.5f);
            leftAxis.setDrawAxisLine(false);
            leftAxis.setTextColor(0xFF9E9E9E);
            leftAxis.setTextSize(10f);
            leftAxis.setAxisMinimum(0f);
            leftAxis.setLabelCount(3, true);
        } catch (Throwable t) {
            Log.e(TAG, "configureChart failed", t);
        }
    }

    // ---- 公开 API (全部 null-safe) ----

    public void setTitle(String title) {
        this.seriesName = title;
        if (tvTitle != null) tvTitle.setText(title);
    }

    public void setCurrentValue(float value) {
        if (tvCurrentValue != null) {
            try {
                tvCurrentValue.setText(String.format(Locale.US, valueFormat, value) + valueSuffix);
            } catch (Exception ignored) {}
        }
    }

    public void setChartColor(int color) { this.chartColor = color; }

    public void setValueFormat(String format, String suffix) {
        this.valueFormat = format;
        this.valueSuffix = suffix;
    }

    public void setData(List<HistoryDataPoint> points) {
        if (lineChart == null || points == null || points.isEmpty()) { if (lineChart != null) lineChart.clear(); return; }
        try {
            List<Entry> entries = new ArrayList<>();
            for (HistoryDataPoint p : points) entries.add(new Entry(p.getTimestampMillis(), p.getValue()));
            LineDataSet set = new LineDataSet(entries, seriesName);
            safeStyleDataSet(set);
            lineChart.setData(new LineData(set));
            setCurrentValue(points.get(points.size() - 1).getValue());
            lineChart.notifyDataSetChanged();
            lineChart.invalidate();
        } catch (Exception e) { Log.e(TAG, "setData failed", e); }
    }

    public void addDataPoint(long timestampMillis, float value) {
        if (lineChart == null) return;
        try {
            LineData data = lineChart.getData();
            if (data == null) { data = new LineData(); lineChart.setData(data); }
            ILineDataSet es = data.getDataSetByIndex(0);
            LineDataSet set;
            if (es instanceof LineDataSet) set = (LineDataSet) es;
            else { set = new LineDataSet(new ArrayList<>(), seriesName); safeStyleDataSet(set); data.addDataSet(set); }
            data.addEntry(new Entry(timestampMillis, value), 0);
            setCurrentValue(value);
            data.notifyDataChanged();
            lineChart.notifyDataSetChanged();
            lineChart.setVisibleXRangeMaximum(60f);
            lineChart.moveViewToX(timestampMillis);
        } catch (Exception e) { /* silent */ }
    }

    private void safeStyleDataSet(LineDataSet set) {
        try {
            set.setColor(chartColor);
            set.setCircleColor(chartColor);
            set.setLineWidth(2f);
            set.setCircleRadius(2f);
            set.setDrawCircleHole(false);
            set.setDrawValues(false);
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            set.setCubicIntensity(0.1f);
            set.setDrawFilled(true);
            try {
                int r = Color.red(chartColor), g = Color.green(chartColor), b = Color.blue(chartColor);
                set.setFillDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{Color.argb(80, r, g, b), Color.argb(10, r, g, b)}));
            } catch (Exception e) {
                set.setFillColor(chartColor & 0x00FFFFFF | 0x30000000);
            }
        } catch (Exception e) { Log.e(TAG, "styleDataSet failed", e); }
    }

    public void clear() { if (lineChart != null) lineChart.clear(); }
    public LineChart getLineChart() { return lineChart; }

    private static int dp(float dp, Context ctx) {
        return (int) (dp * ctx.getResources().getDisplayMetrics().density);
    }
}
