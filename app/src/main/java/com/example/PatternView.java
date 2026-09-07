package com.example;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PatternView extends View {
    private static final int DOT_COUNT = 3;
    private Paint dotPaint;
    private Paint linePaint;
    private Paint textPaint;
    private List<Dot> dots = new ArrayList<>();
    private List<Dot> selectedDots = new ArrayList<>();
    private float lastX, lastY;
    private OnPatternListener listener;
    private boolean displayOnly = false;

    public interface OnPatternListener {
        void onPatternEntered(String pattern);
    }

    public PatternView(Context context) {
        super(context);
        init();
    }

    public PatternView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PatternView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        int dotColor = androidx.core.content.ContextCompat.getColor(getContext(), R.color.brandBlue);
        int lineColor = androidx.core.content.ContextCompat.getColor(getContext(), R.color.brandGreen);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(dotColor);
        dotPaint.setStyle(Paint.Style.FILL);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(lineColor);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(12f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFFFFF); // White
        textPaint.setTextSize(24f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    public void setDisplayOnly(boolean displayOnly) {
        this.displayOnly = displayOnly;
        if (displayOnly) {
            linePaint.setStrokeWidth(6f);
            dotPaint.setAlpha(128);
            textPaint.setTextSize(14f);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setupDots();
    }

    private void setupDots() {
        dots.clear();
        float w = getWidth();
        float h = getHeight();
        if (w == 0 || h == 0) return;
        float widthStep = w / (DOT_COUNT + 1f);
        float heightStep = h / (DOT_COUNT + 1f);
        for (int i = 0; i < DOT_COUNT; i++) {
            for (int j = 0; j < DOT_COUNT; j++) {
                dots.add(new Dot((j + 1) * widthStep, (i + 1) * heightStep, i * DOT_COUNT + j));
            }
        }
    }

    public void setPattern(String pattern) {
        if (dots.isEmpty()) {
            post(() -> setPattern(pattern));
            return;
        }
        selectedDots.clear();
        if (pattern != null) {
            for (char c : pattern.toCharArray()) {
                int id = Character.getNumericValue(c);
                for (Dot dot : dots) {
                    if (dot.id == id) {
                        selectedDots.add(dot);
                        break;
                    }
                }
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float radius = displayOnly ? 10f : 24f;
        
        // Draw dots
        for (Dot dot : dots) {
            canvas.drawCircle(dot.x, dot.y, radius, dotPaint);
        }

        // Draw connections
        if (!selectedDots.isEmpty()) {
            Path path = new Path();
            path.moveTo(selectedDots.get(0).x, selectedDots.get(0).y);
            for (int i = 1; i < selectedDots.size(); i++) {
                path.lineTo(selectedDots.get(i).x, selectedDots.get(i).y);
            }
            canvas.drawPath(path, linePaint);
            
            // Draw temporary line while drawing
            if (!displayOnly && lastX != 0 && lastY != 0) {
                 Dot lastSelected = selectedDots.get(selectedDots.size() - 1);
                 canvas.drawLine(lastSelected.x, lastSelected.y, lastX, lastY, linePaint);
            }

            // Draw sequence numbers
            for (int i = 0; i < selectedDots.size(); i++) {
                Dot dot = selectedDots.get(i);
                float textY = dot.y - (textPaint.descent() + textPaint.ascent()) / 2;
                canvas.drawText(String.valueOf(i + 1), dot.x, textY, textPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (displayOnly) return false;

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                resetPattern();
                checkDot(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                checkDot(x, y);
                lastX = x;
                lastY = y;
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (listener != null) {
                    listener.onPatternEntered(getPatternString());
                }
                lastX = 0;
                lastY = 0;
                invalidate();
                performClick();
                break;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void checkDot(float x, float y) {
        for (Dot dot : dots) {
            if (!selectedDots.contains(dot)) {
                double distance = Math.sqrt(Math.pow(x - dot.x, 2) + Math.pow(y - dot.y, 2));
                if (distance < 70) {
                    selectedDots.add(dot);
                    invalidate();
                    break;
                }
            }
        }
    }

    public void resetPattern() {
        selectedDots.clear();
        invalidate();
    }

    public String getPatternString() {
        StringBuilder sb = new StringBuilder();
        for (Dot dot : selectedDots) {
            sb.append(dot.id);
        }
        return sb.toString();
    }

    public void setOnPatternListener(OnPatternListener listener) {
        this.listener = listener;
    }

    private static class Dot {
        float x, y;
        int id;

        Dot(float x, float y, int id) {
            this.x = x;
            this.y = y;
            this.id = id;
        }
    }
}
