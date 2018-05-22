package com.android.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class DrawingView extends View {
    // Tells us if we are currently drawing our red border box
    public boolean drawLatentCameraBox = false;
    public boolean drawBarcodeReaderBox = false;
    // Tells us if user touched view
    private boolean haveTouch = false;
    // Rect to hold our user touched area
    private Rect touchArea;
    // Paint color of strokes
    private Paint paint;
    private int tapToFocusRadius = 75;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        haveTouch = false;

        drawLatentCameraBox = false;
        drawBarcodeReaderBox = false;

    }

    // This function is to be called when a user touch event is detected, it tells the DrawingView
    // that the user touched "me" and now "I" need to draw some stuff
    public void setHaveTouch(boolean val, Rect rect) {
        haveTouch = val;
        touchArea = rect;
    }

    @Override
    public void onDraw(Canvas canvas) {
        // If we detected a touch we will draw a green circle where user touched
        if (haveTouch) {
            // Grab CENTER coordinates of touch
            int x = (touchArea.left + touchArea.right) / 2;
            int y = (touchArea.top + touchArea.bottom) / 2;
            // Set our PAINT parameters
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(4f);
            // Draw circle at x, y with radius 55 and paint Green
            canvas.drawCircle(x, y, tapToFocusRadius, paint);
        }
    }
}