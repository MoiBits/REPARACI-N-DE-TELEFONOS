package com.github.mikephil.charting.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class BarChart extends View {

    private Paint barPaint;
    private Paint barAccentPaint;
    private Paint textPaint;
    private Paint gridPaint;

    private static final String[] DAYS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private static final float[] VALUES = {4f, 7f, 5f, 9f, 8f, 11f, 3f};
    private static final float MAX_VAL = 12f;

    public BarChart(Context context) {
        super(context);
        init();
    }

    public BarChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BarChart(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(0xFF16556B); // Deep Teal Brand

        barAccentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barAccentPaint.setColor(0xFF10B981); // Emerald Accent

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF64748B); // Slate text
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(0xFFE2E8F0);
        gridPaint.setStrokeWidth(2f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        if (w == 0 || h == 0) return;

        float bottomPadding = 60f;
        float topPadding = 40f;
        float chartHeight = h - bottomPadding - topPadding;

        // Draw horizontal baseline
        float baselineY = h - bottomPadding;
        canvas.drawLine(30f, baselineY, w - 30f, baselineY, gridPaint);

        int count = DAYS.length;
        float slotWidth = (w - 60f) / count;
        float barWidth = slotWidth * 0.45f;

        for (int i = 0; i < count; i++) {
            float slotCenterX = 30f + (i + 0.5f) * slotWidth;
            float barH = (VALUES[i] / MAX_VAL) * chartHeight;
            float left = slotCenterX - (barWidth / 2f);
            float right = slotCenterX + (barWidth / 2f);
            float top = baselineY - barH;

            RectF rect = new RectF(left, top, right, baselineY);
            Paint p = (i == 5) ? barAccentPaint : barPaint; // Peak day accent
            canvas.drawRoundRect(rect, 12f, 12f, p);

            // Draw Day Label
            canvas.drawText(DAYS[i], slotCenterX, baselineY + 40f, textPaint);
        }
    }
}
