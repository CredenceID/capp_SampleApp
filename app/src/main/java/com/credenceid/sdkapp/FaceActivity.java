package com.credenceid.sdkapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.face.FaceEngine;
import com.credenceid.sdkapp.util.BitmapUtils;
import com.credenceid.util.FileUtils;

import java.util.ArrayList;

import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;
import static com.credenceid.biometrics.DeviceFamily.CredenceTwo;

@SuppressWarnings("StatementWithEmptyBody")
public class FaceActivity
	extends Activity {

	private AlertDialog.Builder mBuilder;
	private AlertDialog mDialog;

	@Override
	protected void
	onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_face);

		/* Start by displaying a popup to let user known a background operation is taking place. */
		mBuilder = new AlertDialog.Builder(this);
		mBuilder.setCancelable(false);
		mBuilder.setIcon(R.drawable.ic_launcher);
		mBuilder.setTitle(getString(R.string.face_engine_analytics_title));
		mBuilder.setMessage(getString(R.string.face_engine_analyzing_wait));

		mDialog = mBuilder.create();
		mDialog.setCanceledOnTouchOutside(false);
		mDialog.show();

		this.readAndAnalyzeImage();
	}

	private void
	readAndAnalyzeImage() {

		Bitmap image = null;

		/* There is an Android framework's bug in Oreo where sending image data through
		 * Intent's causes an "FAILED BINDER TRANSACTION" crash to occur. This same block of
		 * code works as expected on Marshmallow, Nougat, Pie, yet fails on ALL Android Oreo
		 * devices. To get around this in camera.Activity we save image then pass image file path.
		 */
		if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
			image = FileUtils.readBitmap(getIntent().getStringExtra("image"));
		else {
			byte[] imageBytes = getIntent().getByteArrayExtra("image");
			if (null != imageBytes)
				image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
		}

		if (null != image) {
			if (CredenceTwo == App.DevFamily)
				image = BitmapUtils.rotate(image, 90);

			this.analyzeImage(image);
		} else {
			mDialog.dismiss();
			mBuilder.setTitle(getString(R.string.error));
			mBuilder.setMessage(getString(R.string.err_cant_get_image));
			mBuilder.setPositiveButton(getString(R.string.retry), (dialogInterface, i) -> {
				finish();
				startActivity(new Intent(getApplicationContext(), CameraActivity.class));
			});
			mDialog = mBuilder.create();
			mDialog.show();
		}
	}

	/* Performs a full face analysis (via CredenceSDK) on given image.
	 *
	 * @param bitmap Image to run full face analysis on.
	 */
	@SuppressWarnings("StringConcatenationInLoop")
	private void
	analyzeImage(final Bitmap bitmap) {

		if (null == bitmap)
			return;

		/* Make API call to run full face analysis. */
		App.BioManager.analyzeFace(bitmap, (Biometrics.ResultCode resultCode,
		                                    RectF rectF,
		                                    ArrayList<PointF> arrayList,
		                                    ArrayList<PointF> arrayList1,
		                                    float[] floats,
		                                    FaceEngine.HeadPoseDirection[] poseDirections,
		                                    FaceEngine.Gender gender,
		                                    int age,
		                                    FaceEngine.Emotion emotion,
		                                    boolean glasses,
		                                    int imageQuality) -> {

			String message = "";

			if (OK == resultCode) {
				String displayData = "HeadPose: ";
				for (FaceEngine.HeadPoseDirection pose : poseDirections)
					displayData += (pose.name() + " ");

				displayData += ("\nGender: " + gender.name());
				displayData += ("\nAge: " + age);
				displayData += ("\nEmotion: " + emotion.name());
				displayData += ("\nImage Quality: " + imageQuality);

				message = displayData;

			} else if (INTERMEDIATE == resultCode) {
				/* This code is never returned here. */

			} else if (FAIL == resultCode) {
				message = getString(R.string.face_engine_fail);
			}

			/* Remove popup, update its message with result from API call, and re-display. */
			mDialog.dismiss();
			mBuilder.setMessage(message);
			mBuilder.setPositiveButton(getString(R.string.retry), (dialogInterface, i) -> {
				finish();
				startActivity(new Intent(getApplicationContext(), CameraActivity.class));
			});
			mDialog = mBuilder.create();
			mDialog.show();
		});
	}
}
