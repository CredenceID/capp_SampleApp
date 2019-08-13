package com.credenceid.sdkapp.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.io.ByteArrayOutputStream;

@SuppressWarnings("unused")
public class BitmapUtils {
	private static byte[] mColorPalette = new byte[1024];
	private static byte[] mBMPFileHeader = new byte[14];
	private static byte[] mDIBHeader = new byte[40];

	/* Rotates given Bitmap by "angle" degrees.
	 *
	 * @param in Bitmap to rotate.
	 * @param angle Degrees to rotate Bitmap by.
	 *
	 * @return Rotated Bitmap.
	 */
	public static Bitmap
	rotate(Bitmap bitmap,
	       int angle) {

		Matrix mat = new Matrix();
		mat.postRotate(angle);
		return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);
	}

	/* Converts a given Bitmap to byte array.
	 *
	 * @param bitmap Bitmap to convert to byte array.
	 *
	 * @return If Bitmap is null empty byte array is returned, else converted Bitmap in byte form.
	 */
	public static byte[]
	toBytes(Bitmap bitmap) {

		if (null == bitmap)
			return new byte[]{};

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
		return stream.toByteArray();
	}

	/* Convert byte array to Bitmap.
	 *
	 * @param data Byte array to convert to Bitmap.
	 *
	 * @return If data is not Bitmap then NULL is returned.
	 */
	public static Bitmap
	decode(byte[] data) {

		try {
			return BitmapFactory.decodeByteArray(data, 0, data.length);
		} catch (Exception ignore) {
			return null;
		}
	}

	/* Convert Bitmap to gray scale color format.
	 *
	 * @param bmpOriginal Bitmap to convert.
	 * @return
	 */
	public static Bitmap
	toGrayScale(Bitmap bitmap) {

		/* Create a new Bitmap with same dimensions as given one. */
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		/* Set up objects to change Bitmap "paint" colors. */
		Canvas c = new Canvas(bmp);
		Paint paint = new Paint();
		ColorMatrix cm = new ColorMatrix();
		cm.setSaturation(0);
		ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
		paint.setColorFilter(f);

		/* Create new Bitmap to copy with new paint scheme. */
		c.drawBitmap(bitmap, 0, 0, paint);

		return bmp;
	}

	public static Bitmap
	cropToSquare(Bitmap bitmap) {

		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int newWidth = (height > width) ? width : height;
		int newHeight = (height > width) ? height - (height - width) : height;
		int cropW = (width - height) / 2;
		cropW = (cropW < 0) ? 0 : cropW;
		int cropH = (height - width) / 2;
		cropH = (cropH < 0) ? 0 : cropH;

		return Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight);
	}
}

