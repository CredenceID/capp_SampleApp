package com.cid.sdk.android.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;


public class DrawingView extends View {
	@SuppressWarnings("unused")
	private final static String TAG = DrawingView.class.getSimpleName();

	// Purposely use two separate paints, one for TapToFocus and another for drawing face Rect.
	// TapToFocus is drawn with white paint and face Rect. with green. We could use same object
	// and change paint settings, but this reduces performance. In face detection we want processing
	// and visualization to be very quick, so use two separate objects.
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

	public DrawingView(Context context,
					   AttributeSet attrs) {
		super(context, attrs);

		mTouchPaint = new Paint();
		mTouchPaint.setColor(Color.WHITE);
		mTouchPaint.setStyle(Paint.Style.STROKE);
		mTouchPaint.setStrokeWidth(4);
		mTouchPaint.setAntiAlias(true);

		mFacePaint = new Paint();
		mFacePaint.setColor(Color.GREEN);
		mFacePaint.setStyle(Paint.Style.STROKE);
		mFacePaint.setStrokeWidth(4);
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
			// Grab CENTER coordinates of touch rectangle.
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

	/* This function is to be called when a user touch event is detected, it tells the DrawingView
	 * that the user touched "me" and now "I" need to draw some stuff.
	 *
	 * @param hasTouch If true this view will draw auto-focus circle, else will not.
	 * @param rect Region where tap-to-focus was initiated. This is used to calculare center point
	 * 			   from where to draw circle.
	 */
	public void
	setHasTouch(boolean hasTouch,
				Rect rect) {
		mHaveTouch = hasTouch;
		mTouchArea = rect;
	}

	/* Tells this view dimensions of Bitmap on which face detections are ran. This view and actual
	 * camera preview images are different sizes, so in order for this View to know how to map a
	 * point from camera preview image onto its dimensions it needs to be told what camera preview
	 * image size it. This way it can determine proper scaling, etc.
	 *
	 * @param width Width of camera preview frames.
	 * @param height Height of camera preview frames.
	 */
	public void
	setBitmapDimensions(int width,
						int height) {
		mBitmapScale = ((float) (getLayoutParams().height)) / height;
		mFaceImageWidth = (int) (mBitmapScale * width);
	}

	/* Tell this View if it should draw facial Rect. on next OnDraw() call. */
	public void
	setHasFace(boolean hasFace) {
		mHasFace = hasFace;
	}

	/* Set coordinates of where to draw face detection rect on next onDraw() call.
	 *
	 * @param left Top left point of detected face Rect.
	 * @param top Top point of detected face Rect.
	 * @param right Bottom right point of detected face Rect.
	 * @param bottom Bottom point of detected face Rect.
	 */
	public void
	setFaceRect(float left,
				float top,
				float right,
				float bottom) {
		float shift = (getLayoutParams().width - mFaceImageWidth) * 0.5f;

		mFaceRect.set(shift + left * mBitmapScale,
				(top * mBitmapScale),
				shift + right * mBitmapScale,
				(bottom * mBitmapScale));
	}
}