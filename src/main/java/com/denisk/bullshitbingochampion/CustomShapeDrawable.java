package com.denisk.bullshitbingochampion;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;

public class CustomShapeDrawable extends ShapeDrawable {
    private Paint strokepaint;

    public CustomShapeDrawable(Shape s, int strokeWidth, int strokeColor) {
        super(s);
        strokepaint = new Paint();
        strokepaint.setStyle(Paint.Style.STROKE);
        strokepaint.setStrokeWidth(strokeWidth);
        strokepaint.setColor(strokeColor);
    }

    @Override
    protected void onDraw(Shape shape, Canvas canvas, Paint fillpaint) {
        shape.draw(canvas, fillpaint);
        shape.draw(canvas, strokepaint);
    }
}