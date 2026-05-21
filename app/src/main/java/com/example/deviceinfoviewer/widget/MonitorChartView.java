package com.example.deviceinfoviewer.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
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
 * 竞品风格监控图表 — 纯代码构建，零 XML inflate，安全可靠
 */
public class MonitorChartView extends LinearLayout {

    private final TextView tvTitle;
    private final TextView tvCurrentValue;
    private final LineChart lineChart;

    private int chartColor = Color.parseColor("#4CAF50");
    private String valueFormat = "%.1f";
    private String valueSuffix = "";
    private String seriesName = "";

    public MonitorChartView(Context context) {
        super(context);
        tvTitle = buildHeaderRow(context);
        lineChart = buildChart(context);
        tvCurrentValue = findCurrentValueView();
        configureChart(lineChart);
    }

    public MonitorChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        tvTitle = buildHeaderRow(context);
        lineChart = buildChart(context);
        tvCurrentValue = findCurrentValueView();
        configureChart(lineChart);
    }

    public MonitorChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        tvTitle = buildHeaderRow(context);
        lineChart = buildChart(context);
        tvCurrentValue = findCurrentValueView();
        configureChart(lineChart);
    }

    /** 构建标题行：标题 | 当前值 */
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
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleLp);

        TextView current = new TextView(ctx);
        current.setId(View.generateViewId());
        current.setTextColor(0xFF212121);
        current.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
        current.setTypeface(null, Typeface.BOLD);
        current.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        headerRow.addView(title);
        headerRow.addView(current);

        addView(headerRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        return title;
    }

    /** 构建 LineChart */
    private LineChart buildChart(Context ctx) {
        LineChart chart = new LineChart(ctx);
        chart.setId(View.generateViewId());
        LineChart.LayoutParams lp = new LineChart.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(120, ctx));
        chart.setLayoutParams(lp);
        addView(chart);
        return chart;
    }

    /** 找到当前值 TextView（它是 headerRow 的第二个子 View） */
    private TextView findCurrentValueView() {
        if (getChildCount() > 0) {
            View header = getChildAt(0);
            if (header instanceof LinearLayout && ((LinearLayout) header).getChildCount() >= 2) {
                View v = ((LinearLayout) header).getChildAt(1);
                if (v instanceof TextView) return (TextView) v;
            }
        }
        return null;
    }

    private void configureChart(LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.setExtraOffsets(0, 4, 0, 8);

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

        chart.getAxisRight().setEnabled(false);
        Legend legend = chart.getLegend();
        legend.setEnabled(false);
    }

    // ---- 公开 API ----

    public void setTitle(String title) {
        this.seriesName = title;
        if (tvTitle != null) tvTitle.setText(title);
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
        try {
            List<Entry> entries = new ArrayList<>();
            for (HistoryDataPoint p : points) {
                entries.add(new Entry(p.getTimestampMillis(), p.getValue()));
            }
            LineDataSet set = new LineDataSet(entries, seriesName);
            styleDataSet(set);
            LineData data = new LineData(set);
            lineChart.setData(data);
            HistoryDataPoint last = points.get(points.size() - 1);
            setCurrentValue(last.getValue());
            lineChart.notifyDataSetChanged();
            lineChart.invalidate();
        } catch (Exception e) {
            lineChart.clear();
        }
    }

    public void addDataPoint(long timestampMillis, float value) {
        if (lineChart == null) return;
        try {
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
                styleDataSet(set);
                data.addDataSet(set);
            }
            data.addEntry(new Entry(timestampMillis, value), 0);
            setCurrentValue(value);
            data.notifyDataChanged();
            lineChart.notifyDataSetChanged();
            lineChart.setVisibleXRangeMaximum(60f);
            lineChart.moveViewToX(timestampMillis);
        } catch (Exception e) {
            // silently ignore chart update failures
        }
    }

    private void styleDataSet(LineDataSet set) {
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
            GradientDrawable fill = makeFill();
            if (fill != null) set.setFillDrawable(fill);
        } catch (Exception e) {
            // fallback: use fill color instead
            set.setFillColor(chartColor & 0x00FFFFFF | 0x30000000);
        }
    }

    private GradientDrawable makeFill() {
        try {
            int r = Color.red(chartColor);
            int g = Color.green(chartColor);
            int b = Color.blue(chartColor);
            return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.argb(80, r, g, b), Color.argb(10, r, g, b)});
        } catch (Exception e) {
            return null;
        }
    }

    public void clear() {
        if (lineChart != null) lineChart.clear();
    }

    public LineChart getLineChart() { return lineChart; }

    private static int dp(float dp, Context ctx) {
        return (int) (dp * ctx.getResources().getDisplayMetrics().density);
    }
}
