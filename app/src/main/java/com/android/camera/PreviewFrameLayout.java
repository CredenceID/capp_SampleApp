package com.android.camera;


import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.credenceid.sdkapp.SampleActivity;

public class PreviewFrameLayout extends RelativeLayout {
    private String TAG = "PreviewFrameLayout";

    private double mAspectRatio = 4.0 / 3.0;

    public PreviewFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAspectRatio(4.0 / 3.0);
    }

    public void setAspectRatio(double ratio) {
        if (ratio <= 0.0)
            throw new IllegalArgumentException();
        if (mAspectRatio != ratio) {
            mAspectRatio = ratio;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int previewWidth = MeasureSpec.getSize(widthSpec);
        int previewHeight = MeasureSpec.getSize(heightSpec);

        // Get the padding of the border background.
        int hPadding = getPaddingLeft() + getPaddingRight();
        int vPadding = getPaddingTop() + getPaddingBottom();
        // Resize the preview frame with correct aspect ratio.
        previewWidth -= hPadding;
        previewHeight -= vPadding;

        boolean widthLonger = previewWidth > previewHeight;
        int longSide = (widthLonger ? previewWidth : previewHeight);
        int shortSide = (widthLonger ? previewHeight : previewWidth);
        if (longSide > shortSide * mAspectRatio) {
            longSide = (int) ((double) shortSide * mAspectRatio);
        } else {
            shortSide = (int) ((double) longSide / mAspectRatio);
        }
        if (widthLonger) {
            previewWidth = longSide;
            previewHeight = shortSide;
        } else {
            previewWidth = shortSide;
            previewHeight = longSide;
        }

        // Add the padding of the border.
        previewWidth += hPadding;
        previewHeight += vPadding;

        // Ask children to follow the new preview dimension.
        super.onMeasure(MeasureSpec.makeMeasureSpec(previewWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(previewHeight, MeasureSpec.EXACTLY));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // If user touched SurfaceView
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Grab touch coordinates
            float x = event.getX();
            float y = event.getY();

            float touchMajor = event.getTouchMajor();
            float touchMinor = event.getTouchMinor();

            Rect touchRect = new Rect(
                    (int) (x - touchMajor / 2),
                    (int) (y - touchMinor / 2),
                    (int) (x + touchMajor / 2),
                    (int) (y + touchMinor / 2));

            Log.d(TAG, "X:" + x + "\tY:" + y);
            Log.d(TAG, "TouchRect:" + touchRect.toString());

            ((SampleActivity) getContext()).getFaceCameraPage().touchFocus(touchRect);
        }
        return true;
    }
}
