package net.golbarg.engtoper.ui.common;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

/**
 * Lays children out left-to-right (or right-to-left, following the layout direction) and wraps
 * to a new row when one is full. Each row is as tall as its tallest child.
 */
public class PillFlowLayout extends ViewGroup {

    private final int horizontalSpacing;
    private final int verticalSpacing;

    public PillFlowLayout(@NonNull Context context, int horizontalSpacing, int verticalSpacing) {
        super(context);
        this.horizontalSpacing = horizontalSpacing;
        this.verticalSpacing = verticalSpacing;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int childWidthSpec = MeasureSpec.makeMeasureSpec(Math.max(0, maxWidth), MeasureSpec.AT_MOST);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        int rowWidth = 0;
        int rowHeight = 0;
        int totalHeight = 0;
        int widestRow = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            child.measure(childWidthSpec, childHeightSpec);
            int width = child.getMeasuredWidth();
            if (rowWidth > 0 && rowWidth + horizontalSpacing + width > maxWidth) {
                totalHeight += rowHeight + verticalSpacing;
                widestRow = Math.max(widestRow, rowWidth);
                rowWidth = 0;
                rowHeight = 0;
            }
            rowWidth += (rowWidth > 0 ? horizontalSpacing : 0) + width;
            rowHeight = Math.max(rowHeight, child.getMeasuredHeight());
        }
        totalHeight += rowHeight;
        widestRow = Math.max(widestRow, rowWidth);

        int width = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY
                ? MeasureSpec.getSize(widthMeasureSpec)
                : widestRow + getPaddingLeft() + getPaddingRight();
        setMeasuredDimension(width, totalHeight + getPaddingTop() + getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        int maxWidth = r - l - getPaddingLeft() - getPaddingRight();
        int x = 0;
        int y = getPaddingTop();
        int rowHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            int width = child.getMeasuredWidth();
            int height = child.getMeasuredHeight();
            if (x > 0 && x + width > maxWidth) {
                x = 0;
                y += rowHeight + verticalSpacing;
                rowHeight = 0;
            }
            int left = rtl ? r - l - getPaddingRight() - x - width : getPaddingLeft() + x;
            child.layout(left, y, left + width, y + height);
            x += width + horizontalSpacing;
            rowHeight = Math.max(rowHeight, height);
        }
    }
}
