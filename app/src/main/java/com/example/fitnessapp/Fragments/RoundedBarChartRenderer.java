package com.example.fitnessapp.Fragments;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import com.example.fitnessapp.R;
import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.buffer.BarBuffer;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.Calendar;

public class RoundedBarChartRenderer extends BarChartRenderer {

    private final float radius = 20f;
    private final RectF mBarRect = new RectF();

    public RoundedBarChartRenderer(BarDataProvider chart, ChartAnimator animator, ViewPortHandler viewPortHandler) {
        super(chart, animator, viewPortHandler);
    }

    @Override
    public void drawDataSet(Canvas c, IBarDataSet dataSet, int index) {
        mRenderPaint.setStyle(Paint.Style.FILL);
        Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

        for (int i = 0; i < dataSet.getEntryCount(); i++) {
            BarEntry entry = dataSet.getEntryForIndex(i);
            if (entry == null) continue;

            float x = entry.getX();
            float halfBarWidth = mChart.getBarData().getBarWidth() / 2f;

            float[] yVals = entry.getYVals();

            if (yVals == null) {
                // Single-value bar
                mBarRect.set(x - halfBarWidth, Math.min(0, entry.getY()), x + halfBarWidth, Math.max(0, entry.getY()));
                trans.rectToPixelPhase(mBarRect, mAnimator.getPhaseY());
                drawRoundedRect(c, mBarRect, mRenderPaint, CornerStyle.FULL);
            } else {
                float posY = 0f, negY = 0f;
                for (int j = 0; j < yVals.length; j++) {
                    float y = yVals[j];
                    float startY = y >= 0 ? posY : negY;
                    float endY = startY + y;
                    if (y >= 0) posY = endY; else negY = endY;

                    mBarRect.set(x - halfBarWidth, Math.min(startY, endY), x + halfBarWidth, Math.max(startY, endY));
                    trans.rectToPixelPhase(mBarRect, mAnimator.getPhaseY());
                    mRenderPaint.setColor(dataSet.getColor(j));

                    if (yVals.length == 1) {
                        drawRoundedRect(c, mBarRect, mRenderPaint, CornerStyle.FULL);
                    } else if (j == 0) {
                        drawRoundedRect(c, mBarRect, mRenderPaint, CornerStyle.BOTTOM);
                    } else if (j == yVals.length - 1) {
                        drawRoundedRect(c, mBarRect, mRenderPaint, CornerStyle.TOP);
                    } else {
                        c.drawRect(mBarRect, mRenderPaint);
                    }
                }
            }
        }
    }

    private enum CornerStyle { FULL, TOP, BOTTOM }

    private void drawRoundedRect(Canvas c, RectF rect, Paint paint, CornerStyle style) {
        float[] radii;
        switch (style) {
            case TOP:
                radii = new float[]{radius, radius, radius, radius, 0, 0, 0, 0};
                break;
            case BOTTOM:
                radii = new float[]{0, 0, 0, 0, radius, radius, radius, radius};
                break;
            case FULL:
            default:
                radii = new float[]{radius, radius, radius, radius, radius, radius, radius, radius};
                break;
        }
        Path path = new Path();
        path.addRoundRect(rect, radii, Path.Direction.CW);
        c.drawPath(path, paint);
    }

    @Override
    public void initBuffers() {
        BarData barData = mChart.getBarData();
        int count = barData.getDataSetCount();
        mBarBuffers = new BarBuffer[count];

        for (int i = 0; i < count; i++) {
            IBarDataSet set = barData.getDataSetByIndex(i);
            int size = set.getEntryCount() * (set.isStacked() ? 4 * set.getStackSize() : 4);
            mBarBuffers[i] = new BarBuffer(size, count, set.isStacked());
        }
    }

    @Override
    public void drawExtras(Canvas c) {
        super.drawExtras(c);

        if (mChart instanceof View && ((View) mChart).getId() == R.id.calories_bar_chart) {
            BarData barData = mChart.getBarData();
            if (barData == null) return;

            float contentTop = mViewPortHandler.contentTop();
            float contentBottom = mViewPortHandler.contentBottom();
            float lineY = contentBottom - (contentBottom - contentTop) * (100f / 120f);

            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setStrokeWidth(6f); // thicker so it looks nice
            paint.setAntiAlias(true);

            int dataSetCount = barData.getDataSetCount();

            for (int i = 0; i < dataSetCount; i++) {
                IBarDataSet dataSet = barData.getDataSetByIndex(i);

                for (int j = 0; j < dataSet.getEntryCount(); j++) {
                    BarEntry entry = dataSet.getEntryForIndex(j);

                    if (entry == null) continue;

                    float x = entry.getX();
                    Transformer transformer = mChart.getTransformer(dataSet.getAxisDependency());
                    float[] pts = new float[]{x, 0};
                    transformer.pointValuesToPixel(pts);

                    float barWidth = barData.getBarWidth();
                    float halfBarWidth = (barWidth / 2f) * mViewPortHandler.getContentRect().width() / 7f; // 7 days

                    // Draw a short green line only above this bar
                    c.drawLine(
                            pts[0] - halfBarWidth + 10f, // left side (a little inside)
                            lineY,
                            pts[0] + halfBarWidth - 10f, // right side (a little inside)
                            lineY,
                            paint
                    );
                }
            }
        }
    }
}