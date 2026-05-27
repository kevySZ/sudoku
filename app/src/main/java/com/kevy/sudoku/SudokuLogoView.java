package com.kevy.sudoku;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class SudokuLogoView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public SudokuLogoView(Context context) {
        super(context);
    }

    public SudokuLogoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        float left = (getWidth() - size) / 2f;
        float top = (getHeight() - size) / 2f;
        float radius = size * 0.22f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.sudoku_surface));
        rect.set(left + size * 0.04f, top + size * 0.04f, left + size * 0.96f, top + size * 0.96f);
        canvas.drawRoundRect(rect, radius, radius, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.035f);
        paint.setColor(color(R.color.sudoku_primary));
        rect.set(left + size * 0.18f, top + size * 0.18f, left + size * 0.82f, top + size * 0.82f);
        canvas.drawRoundRect(rect, size * 0.08f, size * 0.08f, paint);

        paint.setStrokeWidth(size * 0.018f);
        paint.setColor(color(R.color.sudoku_board_selected));
        for (int i = 1; i < 3; i++) {
            float offset = size * 0.18f + size * 0.64f * i / 3f;
            canvas.drawLine(left + offset, top + size * 0.18f, left + offset, top + size * 0.82f, paint);
            canvas.drawLine(left + size * 0.18f, top + offset, left + size * 0.82f, top + offset, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.sudoku_accent));
        float cell = size * 0.64f / 3f;
        rect.set(left + size * 0.18f + cell, top + size * 0.18f + cell,
                left + size * 0.18f + cell * 2f, top + size * 0.18f + cell * 2f);
        canvas.drawRoundRect(rect, size * 0.035f, size * 0.035f, paint);
    }

    private int color(int resId) {
        return getContext().getColor(resId);
    }
}
