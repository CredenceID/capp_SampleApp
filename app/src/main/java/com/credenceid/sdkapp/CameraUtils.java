package com.credenceid.sdkapp;


import android.hardware.Camera;
import android.util.Log;

import java.util.List;

public class CameraUtils {
	private static String TAG = CameraUtils.class.getSimpleName();

	/* Gets best camera frame preview size based on display metrics. */
	@SuppressWarnings({"unused"})
	public static Camera.Size
	getBestPreviewSize(int width,
					   int height,
					   Camera.Parameters parameters) {
		Camera.Size result = null;

		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			if (size.width <= width && size.height <= height) {
				if (result == null) result = size;
				else {
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;
					if (newArea > resultArea)
						result = size;
				}
			}
		}
		return (result);
	}

	/* Gets optimal camera frame preview size based on display metrics. */
	public static Camera.Size
	getOptimalPreviewSize(int w,
						  int h,
						  Camera.Parameters params) {
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double) w / h;
		List<Camera.Size> sizes = params.getSupportedPictureSizes();
		if (sizes == null) return null;

		Camera.Size optimalSize = null;

		double minDiff = Double.MAX_VALUE;

		for (Camera.Size size : sizes) {
			double ratio = (double) size.width / size.height;

			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;

			if (Math.abs(size.height - h) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - h);
			}
		}

		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Camera.Size size : sizes) {
				if (Math.abs(size.height - h) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - h);
				}
			}
		}
		return optimalSize;
	}

	/* Attempts to open front facing camera on device. */
	public static Camera
	openFrontFacingCamera() {
		int cameraCount;
		Camera cam = null;
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		cameraCount = Camera.getNumberOfCameras();
		for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
			Camera.getCameraInfo(camIdx, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				try {
					cam = Camera.open(camIdx);
				} catch (RuntimeException e) {
					Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
				}
			}
		}

		if (cam != null) {
			Log.i(TAG, "front camera1: " + cam.getParameters().getPreviewSize().width
					+ " " + cam.getParameters().getPreviewSize().height);
		}

		return cam;
	}

	@SuppressWarnings({"unused"})
	public static Camera.Size
	getLargestPictureSize(Camera.Parameters parameters) {
		List<Camera.Size> sizes = parameters.getSupportedPictureSizes();

		Camera.Size bestDimens = null;
		for (Camera.Size dimens : sizes) {
			if (dimens.width <= 1024 && dimens.height <= 768) {
				if (bestDimens == null || (dimens.width > bestDimens.width && dimens.height > bestDimens.height)) {
					Log.i(TAG, "Dimens: " + dimens.width + ", " + dimens.height);
					bestDimens = dimens;
				}
			}
		}
		parameters.setPictureSize(bestDimens.width, bestDimens.height);

		return bestDimens;
	}
}