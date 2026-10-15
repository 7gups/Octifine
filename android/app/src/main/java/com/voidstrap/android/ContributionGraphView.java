package com.voidstrap.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

/**
 * GitHub-style commit calendar: 52 week columns of day cells with month labels and Mon/Wed/Fri labels.
 * Empty cells and label text follow the active app theme; the green activity levels stay fixed.
 */
public final class ContributionGraphView extends View {
    static final int WEEKS = 53; // a calendar year can span 53 Sunday-start weeks
    private static final int[] LEVELS = {0xFF0E4429, 0xFF006D32, 0xFF26A641, 0xFF39D353};
    private static final float ROW_STRETCH = 1.6f; // cells are taller than they are wide
    private static final String[] DAY_LABELS = {"", "Mon", "", "Wed", "", "Fri", ""};

    private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private final float labelWidth;
    private final float topPad;
    private final float gap;
    private final float corner;
    private final int emptyColor;

    private int[][] counts = new int[WEEKS][7];
    private String[] months = new String[WEEKS];
    private int max;

    public ContributionGraphView(Context context) {
        this(context, null);
    }

    public ContributionGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        labelWidth = 36 * density;
        topPad = 18 * density;
        gap = 3 * density;
        corner = 2 * density;
        emptyColor = themeColor(context, R.attr.vsInput, 0xFF161B22);
        textPaint.setColor(themeColor(context, R.attr.vsTextSecondary, 0xFF8B949E));
        textPaint.setTextSize(11 * density);
    }

    private static int themeColor(Context c, int attr, int fallback) {
        TypedValue tv = new TypedValue();
        if (c.getTheme().resolveAttribute(attr, tv, true)
                && tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return tv.data;
        }
        return fallback;
    }

    /** counts[week][day] with day 0 = Sunday, and a month label where a new month starts (otherwise null). */
    public void setData(int[][] weekCounts, String[] monthLabels) {
        counts = weekCounts;
        months = monthLabels;
        max = 0;
        for (int[] week : counts) for (int c : week) max = Math.max(max, c);
        requestLayout();
        invalidate();
    }

    private float pitchX(int width) {
        return (width - labelWidth) / WEEKS;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        float px = pitchX(width);
        setMeasuredDimension(width, Math.round(topPad + 7 * px * ROW_STRETCH));
    }

    private int colorFor(int count) {
        if (count <= 0 || max <= 0) return emptyColor;
        int level = (int) Math.ceil(4.0 * count / max);
        return LEVELS[Math.max(1, Math.min(4, level)) - 1];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float px = pitchX(getWidth());
        float py = px * ROW_STRETCH;
        float cellW = px - gap;
        float cellH = py - gap;

        float lastLabelEnd = 0;
        for (int w = 0; w < WEEKS; w++) {
            if (months[w] != null) {
                float x = labelWidth + w * px;
                float width = textPaint.measureText(months[w]);
                if (x >= lastLabelEnd) {
                    canvas.drawText(months[w], x, topPad - 6 * density, textPaint);
                    lastLabelEnd = x + width + 8 * density;
                }
            }
            for (int d = 0; d < 7; d++) {
                float left = labelWidth + w * px;
                float top = topPad + d * py;
                rect.set(left, top, left + cellW, top + cellH);
                cellPaint.setColor(colorFor(counts[w][d]));
                canvas.drawRoundRect(rect, corner, corner, cellPaint);
            }
        }
        for (int d = 0; d < 7; d++) {
            if (DAY_LABELS[d].isEmpty()) continue;
            canvas.drawText(DAY_LABELS[d], 0, topPad + d * py + cellH * 0.85f, textPaint);
        }
    }
}
