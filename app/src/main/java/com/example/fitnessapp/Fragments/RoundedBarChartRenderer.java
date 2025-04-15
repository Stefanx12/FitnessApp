package com.example.fitnessapp.Fragments;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.buffer.BarBuffer;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

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
            float barWidth = mChart.getBarData().getBarWidth();
            float halfBarWidth = barWidth / 2f;

            float[] yVals = entry.getYVals();

            if (yVals == null) {
                // Not stacked bar
                float y = entry.getY();
                float top = Math.max(y, 0);
                float bottom = Math.min(y, 0);

                mBarRect.set(x - halfBarWidth, bottom, x + halfBarWidth, top);
                trans.rectToPixelPhase(mBarRect, mAnimator.getPhaseY());

                drawFullyRoundedBar(c, mBarRect, mRenderPaint, radius);
            } else {
                // Stacked bar
                float posY = 0f;
                float negY = 0f;

                for (int j = 0; j < yVals.length; j++) {
                    float y = yVals[j];

                    float startY = y >= 0 ? posY : negY;
                    float endY = startY + y;

                    if (y >= 0)
                        posY = endY;
                    else
                        negY = endY;

                    float top = Math.max(startY, endY);
                    float bottom = Math.min(startY, endY);

                    mBarRect.set(x - halfBarWidth, bottom, x + halfBarWidth, top);
                    trans.rectToPixelPhase(mBarRect, mAnimator.getPhaseY());

                    mRenderPaint.setColor(dataSet.getColor(j));

                    boolean isFirst = j == 0;
                    boolean isLast = j == yVals.length - 1;

                    if (isFirst && isLast) {
                        drawFullyRoundedBar(c, mBarRect, mRenderPaint, radius);
                    } else if (isFirst) {
                        drawBottomRoundedBar(c, mBarRect, mRenderPaint, radius);
                    } else if (isLast) {
                        drawTopRoundedBar(c, mBarRect, mRenderPaint, radius);
                    } else {
                        c.drawRect(mBarRect, mRenderPaint);
                    }
                }
            }
        }
    }

    private void drawFullyRoundedBar(Canvas c, RectF barRect, Paint paint, float radius) {
        Path path = new Path();
        path.addRoundRect(barRect, new float[]{
                radius, radius,   // top-left, top-right
                radius, radius,   // bottom-right, bottom-left
                radius, radius,   // not used
                radius, radius    // not used
        }, Path.Direction.CW);
        c.drawPath(path, paint);
    }

    private void drawTopRoundedBar(Canvas c, RectF barRect, Paint paint, float radius) {
        Path path = new Path();
        path.addRoundRect(barRect, new float[]{
                radius, radius,  // top-left, top-right
                radius, radius,  // bottom-right, bottom-left (set to 0 if flat bottom desired)
                0, 0, 0, 0
        }, Path.Direction.CW);
        c.drawPath(path, paint);
    }

    private void drawBottomRoundedBar(Canvas c, RectF barRect, Paint paint, float radius) {
        Path path = new Path();
        path.addRoundRect(barRect, new float[]{
                0, 0,          // top-left, top-right
                0, 0,          // bottom-right, bottom-left
                radius, radius,
                radius, radius
        }, Path.Direction.CW);
        c.drawPath(path, paint);
    }

    @Override
    public void initBuffers() {
        BarData barData = mChart.getBarData();
        mBarBuffers = new BarBuffer[barData.getDataSetCount()];

        for (int i = 0; i < mBarBuffers.length; i++) {
            IBarDataSet set = barData.getDataSetByIndex(i);
            mBarBuffers[i] = new BarBuffer(
                    set.getEntryCount() * (set.isStacked() ? 4 * set.getStackSize() : 4),
                    barData.getDataSetCount(),
                    set.isStacked()
            );
        }
    }
}
