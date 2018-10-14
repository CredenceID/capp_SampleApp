package com.android.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class DrawingView extends View {
	@SuppressWarnings("unused")
	private final static String TAG = DrawingView.class.getSimpleName();

	// Purposely use two separate paints, one for TapToFocus and another for drawing face Rect.
	// TapToFocus is drawn with white paint and face Rect. with green. We could use the same object,
	// but change the
	private Paint mTouchPaint;
	private Paint mFacePaint;

	// Tells us if user touched view
	private boolean mHaveTouch;
	// Rect to hold our user touched area
	private Rect mTouchArea;

	// Scaling used for displaying face Rect.
	private float mBitmapScale;
	// Rect. containing found face region.
	private RectF mFaceRect;
	// If true, View will draw "mFaceRect" to surface.
	private boolean mHasFace;
	// Used for calculating "true" width of detected face Rect. We need to take into account the
	// given Bitmap image width vs. DrawingView Layout width.
	private int mFaceImageWidth;

	public DrawingView(Context context, AttributeSet attrs) {
		super(context, attrs);

		mTouchPaint = new Paint();
		mTouchPaint.setColor(Color.GREEN);
		mTouchPaint.setStyle(Paint.Style.STROKE);
		mTouchPaint.setStrokeWidth(2);
		mTouchPaint.setAntiAlias(true);

		mFacePaint = new Paint();
		mFacePaint.setColor(Color.GREEN);
		mFacePaint.setStyle(Paint.Style.STROKE);
		mFacePaint.setStrokeWidth(2);
		mFacePaint.setAntiAlias(true);

		mHaveTouch = false;
		mHasFace = false;

		mFaceRect = new RectF();
	}

	@Override
	public void
	onDraw(Canvas canvas) {
		// If we detected a touch we will draw a green circle where user touched.
		if (mHaveTouch) {
			// Grab CENTER coordinates of touch
			int x = (mTouchArea.left + mTouchArea.right) / 2;
			int y = (mTouchArea.top + mTouchArea.bottom) / 2;

			// Draw circle at x, y with radius 55 and paint Green.
			int mTapToFocusRadius = 75;
			canvas.drawCircle(x, y, mTapToFocusRadius, mTouchPaint);
		}

		// If face was found, then draw face rect.
		if (mHasFace)
			canvas.drawRoundRect(mFaceRect, 10, 10, mFacePaint);
	}

	// This function is to be called when a user touch event is detected, it tells the DrawingView
	// that the user touched "me" and now "I" need to draw some stuff
	public void
	setHaveTouch(boolean val,
				 Rect rect) {
		Log.d(TAG, "setHaveTouch(" + rect + ")");
		mHaveTouch = val;
		mTouchArea = rect;
	}

	// Updates Bitmap used to calculate where to draw facial landmarks.
	public void
	faceEngineSetBitmapDimensions(int width,
								  int height) {
		Log.d(TAG, "faceEngineSetBitmapDimensions(" + width + ", " + height + ")");
		mBitmapScale = ((float) (getLayoutParams().height)) / height;
		mFaceImageWidth = (int) (mBitmapScale * width);

		Log.i(TAG, "Scale, Bm width: " + mBitmapScale + ", " + mFaceImageWidth);
	}

	// Tell drawing view if it should draw facial landmarks on next OnDraw() call.
	public void
	faceEngineSetHasFace(boolean hasFace) {
		mHasFace = hasFace;
	}

	// Set coordinates of where to draw face detection rect on next onDraw() call.
	public void
	faceEngineSetFaceRect(float left,
						  float top,
						  float right,
						  float bottom) {
		float shift = (getLayoutParams().width - mFaceImageWidth) * 0.5f;

		mFaceRect.set(shift + left * mBitmapScale,
				(top * mBitmapScale) + 20,
				shift + right * mBitmapScale,
				(bottom * mBitmapScale) + 20);
	}
}