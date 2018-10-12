package com.credenceid.sdkapp.pages;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.Biometrics.CloseReasonCode;
import com.credenceid.biometrics.Biometrics.OnIrisesCapturedListener;
import com.credenceid.biometrics.Biometrics.ResultCode;
import com.credenceid.biometrics.IrisQuality;
import com.credenceid.sdkapp.R;
import com.credenceid.sdkapp.TheApp;
import com.credenceid.sdkapp.activity.SampleActivity;
import com.credenceid.sdkapp.models.PageView;
import com.credenceid.sdkapp.utils.Beeper;

import java.io.File;
import java.util.Locale;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static com.credenceid.biometrics.Biometrics.EyeSelection.LEFT_EYE;

public class IrisPage
		extends LinearLayout
		implements PageView {
	private static final String TAG = IrisPage.class.getSimpleName();

	private Biometrics mBiometrics;
	private SampleActivity mSampleActivity;

	private Button mCaptureBtn;
	private Button mCloseBtn;
	private ImageView mCaptureRight;
	private ImageView mCaptureLeft;
	private TextView mStatusTextView;
	private TextView mInfoTextView;

	private String mLeftEyeImagePath;
	private String mRightEyeImagePath;
	private boolean mCapturing = false;

	private long mUncompressedImageSize;
	private long mCompressedImageSize;
	private long mKind7ConvertStartTime;

	// Currently selected iris-capture type.
	private Biometrics.EyeSelection mCurrentCaptureType = Biometrics.EyeSelection.BOTH_EYES;

	// Avaiable capture type.s
	private Biometrics.EyeSelection mCaptureTypes[] = {
			Biometrics.EyeSelection.BOTH_EYES,
			LEFT_EYE,
			Biometrics.EyeSelection.RIGHT_EYE};

	public IrisPage(Context context) {
		super(context);

		this.initializeLayoutComponents();
		this.configureLayoutComponents();
	}

	public IrisPage(Context context,
					AttributeSet attrs) {
		super(context, attrs);

		this.initializeLayoutComponents();
		this.configureLayoutComponents();
	}

	public IrisPage(Context context,
					AttributeSet attrs,
					int defStyle) {
		super(context, attrs, defStyle);

		this.initializeLayoutComponents();
		this.configureLayoutComponents();
	}

	public void
	setActivity(SampleActivity activity) {
		mSampleActivity = activity;
	}

	/* Loads layout out and finds all components in file.  */
	private void
	initializeLayoutComponents() {
		// Tell view which layout file to display.
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.page_iris, this, true);

		mCaptureRight = (ImageView) findViewById(R.id.capture_1_image);
		mCaptureLeft = (ImageView) findViewById(R.id.capture_2_image);
		mCaptureBtn = (Button) findViewById(R.id.capture_btn);
		mCloseBtn = (Button) findViewById(R.id.close_btn);
		mStatusTextView = (TextView) findViewById(R.id.status);
		mInfoTextView = (TextView) findViewById(R.id.info);
	}

	/* Configures each component along with its onClick action. */
	private void
	configureLayoutComponents() {
		mCaptureRight.setOnClickListener((View v) ->
				mSampleActivity.showFullScreenImage(mRightEyeImagePath));

		mCaptureLeft.setOnClickListener((View v) ->
				mSampleActivity.showFullScreenImage(mLeftEyeImagePath));

		mCaptureBtn.setOnClickListener((View v) -> this.onCapture());

		mCloseBtn.setEnabled(false);
		mCloseBtn.setOnClickListener((View v) -> this.onClose());

		// Configures Spinner which allows user to select iris capture type. This initialization is
		// only required one time, so need need to create a global object for it.
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				getContext(),
				R.array.iris_scan_type,
				android.R.layout.simple_spinner_item);

		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		Spinner irisTypeSpinner = (Spinner) findViewById(R.id.iris_type_spinner);
		irisTypeSpinner.setAdapter(adapter);

		irisTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void
			onItemSelected(AdapterView<?> parent,
						   View view,
						   int position,
						   long id) {
				mCurrentCaptureType = mCaptureTypes[position];
				if (mCapturing)
					onCapture();
			}

			@Override
			public void
			onNothingSelected(AdapterView<?> parent) {

			}
		});
	}

	@Override
	public String
	getTitle() {
		return getContext().getResources().getString(R.string.title_iris);
	}

	@Override
	public void
	activate(Biometrics biometrics) {
		this.mBiometrics = biometrics;
		this.doResume();
	}

	@Override
	public void
	doResume() {
		this.setInfoText("");
	}

	@Override
	public void
	deactivate() {
	}

	private void
	onCapture() {
		this.resetCapture();
		this.enableCapture(false);
		this.setStatusText("Initializing...");

		mBiometrics.captureIrises(mCurrentCaptureType, new OnIrisesCapturedListener() {
			@Override
			public void
			onIrisesCaptured(ResultCode result,
							 Bitmap bmLeft,
							 Bitmap bmRight,
							 String leftEyeImagePath,
							 String rightEyeImagePath,
							 String status) {
				final boolean ok = (result == ResultCode.OK);

				// Update status text so user is aware of what is going on.
				if (status != null && !status.isEmpty())
					setStatusText(status);

				if (mCurrentCaptureType != LEFT_EYE && bmRight != null) {
					if (!ok)
						mCaptureRight.setImageBitmap(bmRight);
					mRightEyeImagePath = rightEyeImagePath;
				}

				if (mCurrentCaptureType != Biometrics.EyeSelection.RIGHT_EYE && bmLeft != null) {
					if (!ok)
						mCaptureLeft.setImageBitmap(bmLeft);
					mLeftEyeImagePath = leftEyeImagePath;
				}

				// Once iris capture is complete, result code OK is returned.
				if (result == ResultCode.OK) {
					Log.i(TAG, "Iris Captured Completed");

					// On capture play a "click" sound, similar to sound when an imae is capture via
					// a camera.
					Beeper.getInstance().click();
					enableCapture(false);

					// Convert both images to NIST Kind7 format.
					convertToKind7(mLeftEyeImagePath, mRightEyeImagePath);
				}


				// If sensor is still capturing, and it suddenly stops, due to cancel/error we then
				// reset the capturing state/system.
				if (result == ResultCode.INTERMEDIATE
						&& (status != null && status.equals("Capture Stopped"))) {
					resetCapture();
					enableCapture(true);
				}

				// If sensor failed to capture, notify user, and reset.
				if (result == ResultCode.FAIL) {
					Log.e(TAG, "onIrisesCaptured - FAILED");

					// Let user know captured failed.
					setStatusText("Iris Captured Open-FAILED");
					mCloseBtn.setEnabled(false);
					enableCapture(true);
				}
			}

			@Override
			public void onCloseIrisScanner(ResultCode resultCode,
										   CloseReasonCode closeReasonCode) {
				if (resultCode == ResultCode.OK) {
					// Let uesr know why scanner reader closed
					setStatusText("Iris Scanner Closed: " + closeReasonCode.toString());

					resetCapture();
					enableCapture(true);

					// Disable close button since there is nothing to re-close.F
					mCloseBtn.setEnabled(false);
				} else if (resultCode == ResultCode.FAIL) {
					mCloseBtn.setEnabled(true);
					mCaptureBtn.setEnabled(false);

					setStatusText("Iris Scanner FAILED to close.");
				}
			}
		});

		mCloseBtn.setEnabled(true);
	}

	// Enable/disable capture by updating Capture button based on parameter.
	private void
	enableCapture(boolean enable) {
		mCaptureBtn.setEnabled(enable);
		mCapturing = !enable;
	}

	// Resets capture state.
	private void
	resetCapture() {
		mCaptureRight.setImageDrawable(null);
		mCaptureLeft.setImageDrawable(null);
		mLeftEyeImagePath = null;
		mRightEyeImagePath = null;

		this.setInfoText("");
	}

	// Closes Iris Scanner and puts state back to Close state
	private void
	onClose() {
		this.resetCapture();
		this.enableCapture(true);

		// Turn off close button since we are going to close everything
		mCloseBtn.setEnabled(false);
		// Disable capture button to avoid double clicks
		mCaptureBtn.setEnabled(false);
		// Notify user scanner is closing.
		this.setStatusText("Closing Iris scanner, Please wait...");
		// Close Iris Scanner
		mBiometrics.closeIrisScanner();
	}

	// Calls the convertToKind7 API call.
	private void
	convertToKind7(String left_pathname,
				   String right_pathname) {
		mUncompressedImageSize = 0;

		// If left path is invalid it then uses right. If right path is invalid then API call
		// will return back with FAIL.
		String pathname = (left_pathname != null ? left_pathname : right_pathname);

		if (pathname != null) {
			File uncompressed = new File(pathname);
			if (uncompressed.exists())
				mUncompressedImageSize = uncompressed.length();
		}

		mKind7ConvertStartTime = SystemClock.elapsedRealtime();

		mBiometrics.convertToKind7(left_pathname, right_pathname,
				(ResultCode result,
				 String pathnameLeft,
				 IrisQuality iqLeft,
				 String pathnameRight,
				 IrisQuality iqRight) -> {

					if (result == ResultCode.INTERMEDIATE)
						setInfoText("Algorithms Initializing...");
					else if (result == ResultCode.FAIL)
						setInfoText("Convert to Kind7 failed");
					else {
						mCompressedImageSize = 0;

						String path = (pathnameLeft != null ? pathnameLeft : pathnameRight);

						if (path != null) {
							File compressed = new File(path);
							if (compressed.exists())
								mCompressedImageSize = compressed.length();
						}

						long duration = SystemClock.elapsedRealtime() - mKind7ConvertStartTime;

						String str = String.format(Locale.US,
								"PNG: %s, Kind7: %s, Duration: %dms",
								TheApp.abbreviateNumber(mUncompressedImageSize),
								TheApp.abbreviateNumber(mCompressedImageSize),
								duration);
						setInfoText(str);
					}
				});
	}

	/* Sets StatusTextView text. */
	private void
	setStatusText(String text) {
		mStatusTextView.setText(text);
	}

	/* Sets IntoTextView text. */
	private void
	setInfoText(String text) {
		mInfoTextView.setText(text);
	}
}
