package com.kevy.sudoku;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class SudokuBoardView extends View {
    public interface OnCellSelectedListener {
        void onCellSelected(int index);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int[] givens = new int[81];
    private int[] values = new int[81];
    private int selectedCell = -1;
    private int warningCell = -1;
    private int highlightedValue = 0;
    private boolean inputEnabled = true;
    private OnCellSelectedListener listener;

    public SudokuBoardView(Context context) {
        super(context);
        init();
    }

    public SudokuBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setFocusable(true);
        setClickable(true);
    }

    public void setBoard(int[] givens, int[] values) {
        this.givens = copy81(givens);
        this.values = copy81(values);
        invalidate();
    }

    public void setSelectedCell(int selectedCell) {
        this.selectedCell = selectedCell;
        invalidate();
    }

    public void setWarningCell(int warningCell) {
        this.warningCell = warningCell;
        invalidate();
    }

    public void setHighlightedValue(int highlightedValue) {
        this.highlightedValue = highlightedValue;
        invalidate();
    }

    public void setInputEnabled(boolean inputEnabled) {
        this.inputEnabled = inputEnabled;
        invalidate();
    }

    public void setOnCellSelectedListener(OnCellSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int size = width;
        if (height > 0 && MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            size = Math.min(width, height);
        }
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        float cell = size / 9f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.sudoku_board_background));
        canvas.drawRect(0, 0, size, size, paint);

        drawHighlights(canvas, size, cell);
        drawNumbers(canvas, cell);
        drawGrid(canvas, size, cell);
        if (!inputEnabled) {
            drawPausedOverlay(canvas, size);
        }
    }

    private void drawHighlights(Canvas canvas, float size, float cell) {
        if (highlightedValue >= 1 && highlightedValue <= 9) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color(R.color.sudoku_board_same_value));
            for (int i = 0; i < values.length; i++) {
                if (values[i] == highlightedValue) {
                    int row = i / 9;
                    int col = i % 9;
                    rect.set(col * cell, row * cell, (col + 1) * cell, (row + 1) * cell);
                    canvas.drawRect(rect, paint);
                }
            }
        }
        if (selectedCell >= 0) {
            int row = selectedCell / 9;
            int col = selectedCell % 9;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color(R.color.sudoku_board_cross));
            canvas.drawRect(0, row * cell, size, (row + 1) * cell, paint);
            canvas.drawRect(col * cell, 0, (col + 1) * cell, size, paint);

            paint.setColor(color(R.color.sudoku_board_selected));
            rect.set(col * cell, row * cell, (col + 1) * cell, (row + 1) * cell);
            canvas.drawRect(rect, paint);
        }
        if (warningCell >= 0) {
            int row = warningCell / 9;
            int col = warningCell % 9;
            paint.setColor(color(R.color.sudoku_board_warning));
            rect.set(col * cell, row * cell, (col + 1) * cell, (row + 1) * cell);
            canvas.drawRect(rect, paint);
        }
    }

    private void drawNumbers(Canvas canvas, float cell) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(cell * 0.48f);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baselineOffset = (metrics.descent + metrics.ascent) / 2f;

        for (int i = 0; i < 81; i++) {
            int value = values[i];
            if (value == 0) {
                continue;
            }
            boolean isGiven = givens[i] != 0;
            paint.setFakeBoldText(isGiven);
            paint.setColor(isGiven ? color(R.color.sudoku_board_given_text) : color(R.color.sudoku_board_user_text));
            int row = i / 9;
            int col = i % 9;
            float x = col * cell + cell / 2f;
            float y = row * cell + cell / 2f - baselineOffset;
            canvas.drawText(String.valueOf(value), x, y, paint);
        }
        paint.setFakeBoldText(false);
    }

    private void drawGrid(Canvas canvas, float size, float cell) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color(R.color.sudoku_board_grid));
        for (int i = 0; i <= 9; i++) {
            paint.setStrokeWidth(i % 3 == 0 ? 4f : 1.4f);
            float pos = i * cell;
            canvas.drawLine(pos, 0, pos, size, paint);
            canvas.drawLine(0, pos, size, pos, paint);
        }
    }

    private void drawPausedOverlay(Canvas canvas, float size) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.sudoku_paused_overlay));
        canvas.drawRect(0, 0, size, size, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(size * 0.08f);
        paint.setColor(color(R.color.sudoku_text));
        canvas.drawText(getContext().getString(R.string.paused), size / 2f, size / 2f, paint);
        paint.setFakeBoldText(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!inputEnabled || event.getAction() != MotionEvent.ACTION_DOWN) {
            return true;
        }
        float size = Math.min(getWidth(), getHeight());
        if (event.getX() < 0 || event.getY() < 0 || event.getX() >= size || event.getY() >= size) {
            return true;
        }
        int col = Math.min(8, (int) (event.getX() / (size / 9f)));
        int row = Math.min(8, (int) (event.getY() / (size / 9f)));
        int index = row * 9 + col;
        selectedCell = index;
        warningCell = -1;
        invalidate();
        if (listener != null) {
            listener.onCellSelected(index);
        }
        return true;
    }

    private int[] copy81(int[] source) {
        int[] copy = new int[81];
        if (source != null) {
            System.arraycopy(source, 0, copy, 0, Math.min(source.length, 81));
        }
        return copy;
    }

    private int color(int resId) {
        return getContext().getColor(resId);
    }
}
