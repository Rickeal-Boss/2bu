package com.example.deviceinfoviewer.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.example.deviceinfoviewer.data.model.CpuCoreInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义 CPU 核心频率条形图，Canvas 绘制水平条形图
 * 颜色按频率渐变：绿(<1.5GHz) 黄(1.5-2.5GHz) 红(>2.5GHz)
 */
public class CpuBarChartView extends View {

    private static final float BAR_HEIGHT_DP = 36f;
    private static final float TEXT_SIZE_SP = 12f;
    private static final float PADDING_DP = 8f;
    private static final float GREEN_THRESHOLD_KHZ = 1_500_000L;  // 1.5 GHz
    private static final float YELLOW_THRESHOLD_KHZ = 2_500_000L; // 2.5 GHz

    private final List<CpuCoreInfo> cores = new ArrayList<>();
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint();
    private final Rect textBounds = new Rect();

    private float density;
    private float barHeight;
    private float textSize;
    private float padding;
    private long maxFreqKHz = 1;

    public CpuBarChartView(Context context) {
        super(context);
        init();
    }

    public CpuBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CpuBarChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;
        barHeight = BAR_HEIGHT_DP * density;
        textSize = TEXT_SIZE_SP * density;
        padding = PADDING_DP * density;
        textPaint.setTextSize(textSize);
        textPaint.setColor(Color.BLACK);
        textPaint.setFakeBoldText(false);
        bgPaint.setColor(Color.parseColor("#F5F5F5"));
    }

    /**
     * 更新 CPU 核心数据
     */
    public void setCores(List<CpuCoreInfo> cores) {
        this.cores.clear();
        if (cores != null) {
            this.cores.addAll(cores);
        }
        // 计算最大频率以确定条形图比例
        maxFreqKHz = 1;
        for (CpuCoreInfo core : this.cores) {
            if (core.getMaxFreqKHz() > maxFreqKHz) {
                maxFreqKHz = core.getMaxFreqKHz();
            }
        }
        if (maxFreqKHz <= 0) {
            maxFreqKHz = 1;
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = (int) (cores.size() * (barHeight + padding * 2) + padding * 2);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, Math.max(desiredHeight, (int) (barHeight + padding * 3)));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        if (width <= 0 || cores.isEmpty()) {
            textPaint.setColor(Color.GRAY);
            canvas.drawText("无数据", padding, barHeight, textPaint);
            return;
        }

        float y = padding;
        // 标签区域宽度
        float labelWidth = width * 0.25f;
        float barAreaWidth = width - labelWidth - padding;

        for (CpuCoreInfo core : cores) {
            // 背景
            bgPaint.setColor(Color.parseColor("#F5F5F5"));
            canvas.drawRect(padding, y, width, y + barHeight, bgPaint);

            // 标签
            textPaint.setColor(Color.DKGRAY);
            String label = "核心" + core.getCoreIndex();
            canvas.drawText(label, padding, y + barHeight * 0.65f, textPaint);

            // 条形图
            float ratio = core.getCurrentFreqKHz() > 0
                    ? (float) core.getCurrentFreqKHz() / maxFreqKHz
                    : 0f;
            float barWidth = ratio * barAreaWidth;
            float barX = labelWidth + padding;

            // 颜色根据频率比例
            int color = getFreqColor(core.getCurrentFreqKHz());
            barPaint.setColor(color);
            canvas.drawRoundRect(barX, y + padding, barX + barWidth, y + barHeight - padding,
                    density * 4, density * 4, barPaint);

            // 频率文字
            textPaint.setColor(Color.WHITE);
            String freqText = formatFreq(core.getCurrentFreqKHz());
            textPaint.getTextBounds(freqText, 0, freqText.length(), textBounds);
            float textY = y + barHeight / 2f + textBounds.height() / 2f;
            if (barWidth > textBounds.width() + padding) {
                canvas.drawText(freqText, barX + padding, textY, textPaint);
            } else {
                textPaint.setColor(Color.DKGRAY);
                canvas.drawText(freqText, barX + barWidth + padding, textY, textPaint);
            }

            y += barHeight + padding;
        }
    }

    private int getFreqColor(long freqKHz) {
        if (freqKHz <= 0) return Color.GRAY;
        if (freqKHz < GREEN_THRESHOLD_KHZ) return Color.rgb(76, 175, 80);    // 绿
        if (freqKHz < YELLOW_THRESHOLD_KHZ) return Color.rgb(255, 193, 7);   // 黄
        return Color.rgb(244, 67, 54); // 红
    }

    private String formatFreq(long khz) {
        if (khz <= 0) return "N/A";
        if (khz >= 1_000_000L) return String.format("%.2f GHz", khz / 1_000_000.0);
        if (khz >= 1_000L) return String.format("%.0f MHz", khz / 1_000.0);
        return khz + " KHz";
    }
}
