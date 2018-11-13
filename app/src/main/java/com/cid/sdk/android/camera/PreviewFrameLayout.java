package com.cid.sdk.android.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.cid.sdk.FaceActivity;

public class PreviewFrameLayout extends RelativeLayout {
	private double mAspectRatio = 4.0 / 3.0;

	public
	PreviewFrameLayout(Context context,
					   AttributeSet attrs) {
		super(context, attrs);
		setAspectRatio(4.0 / 3.0);
	}

	public void
	setAspectRatio(double ratio) {
		if (ratio <= 0.0)
			throw new IllegalArgumentException();

		if (mAspectRatio != ratio) {
			mAspectRatio = ratio;
			requestLayout();
		}
	}

	@Override
	protected void
	onMeasure(int widthSpec,
			  int heightSpec) {
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

		if (longSide > shortSide * mAspectRatio)
			longSide = (int) ((double) shortSide * mAspectRatio);
		else shortSide = (int) ((double) longSide / mAspectRatio);

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
		int width = MeasureSpec.makeMeasureSpec(previewWidth, MeasureSpec.EXACTLY);
		int height = MeasureSpec.makeMeasureSpec(previewHeight, MeasureSpec.EXACTLY);
		super.onMeasure(width, height);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean
	onTouchEvent(MotionEvent event) {
		// If user touched SurfaceView.
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

			((FaceActivity) getContext()).performTapToFocus(touchRect);
		}
		return true;
	}
}