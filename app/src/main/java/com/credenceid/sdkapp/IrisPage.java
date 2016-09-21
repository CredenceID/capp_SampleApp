package com.credenceid.sdkapp;

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
import com.credenceid.biometrics.Biometrics.OnConvertToKind7Listener;
import com.credenceid.biometrics.Biometrics.OnIrisesCapturedListener;
import com.credenceid.biometrics.Biometrics.ResultCode;
import com.credenceid.biometrics.IrisQuality;

import java.io.File;

public class IrisPage extends LinearLayout implements PageView {
	private static final String TAG = IrisPage.class.getName();

	private Biometrics mBiometrics;
	private SampleActivity mActivity;

	private Button mCaptureBtn;
	private Button mCloseBtn;
	private ImageView mCaptureRight;
	private ImageView mCaptureLeft;
	private TextView mStatusTextView;
	private TextView mInfoTextView;
	private Spinner mIrisTypeSpinner;

	private String mPathnameLeft;
	private String mPathnameRight;
	private boolean mCapturing = false;

	private Biometrics.EyeSelection mEyeSelection = Biometrics.EyeSelection.BOTH_EYES;

	private Biometrics.EyeSelection eye_selection_types[] = { Biometrics.EyeSelection.BOTH_EYES,
			Biometrics.EyeSelection.LEFT_EYE, Biometrics.EyeSelection.RIGHT_EYE };

	public IrisPage(Context context) {
		super(context);
		initialize();
	}

	public IrisPage(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	public IrisPage(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}

	public void setActivity(SampleActivity activity) {
		mActivity = activity;
	}

	private void initialize() {
		Log.d(TAG, "initialize");
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.page_iris, this, true);
		mCaptureRight = (ImageView) findViewById(R.id.capture_1_image);
		mCaptureRight.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mActivity.showFullScreen(mPathnameRight);
			}
		});
		mCaptureLeft = (ImageView) findViewById(R.id.capture_2_image);
		mCaptureLeft.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mActivity.showFullScreen(mPathnameLeft);
			}
		});
		mCaptureBtn = (Button) findViewById(R.id.capture_btn);
		mCaptureBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onCapture();
			}
		});
		mCloseBtn = (Button) findViewById(R.id.close_btn);
		mCloseBtn.setEnabled(false);
		mCloseBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onClose();
			}
		});
		mStatusTextView = (TextView) findViewById(R.id.status);
		mInfoTextView = (TextView) findViewById(R.id.info);

		mIrisTypeSpinner = (Spinner) findViewById(R.id.iris_type_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
				R.array.iris_scan_type, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mIrisTypeSpinner.setAdapter(adapter);
		mIrisTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mEyeSelection = eye_selection_types[position];
				if (mCapturing)
					onCapture();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});
	}

	@Override
	public String getTitle() {
		return getContext().getResources().getString(R.string.title_iris);
	}

	@Override
	public void activate(Biometrics biometrics) {
		this.mBiometrics = biometrics;
		doResume();
	}

	@Override
	public void doResume() {
		setInfoText("");
	}

	@Override
	public void deactivate() {
	}

	private void enableCapture(boolean enable) {
		// Enable capture by turning on capture button for user
		mCaptureBtn.setEnabled(enable);
		mCapturing = !enable;
	}

	private void resetCapture() {
		mCaptureRight.setImageDrawable(null);
		mCaptureLeft.setImageDrawable(null);
		mPathnameLeft = null;
		mPathnameRight = null;
		setInfoText("");
	}

	private void onCapture() {
		resetCapture();
		enableCapture(false);

		mBiometrics.captureIrises(mEyeSelection, new OnIrisesCapturedListener() {

			@Override
			public void onIrisesCaptured(Biometrics.ResultCode result, Bitmap bmLeft,
										 Bitmap bmRight, String pathnameLeft, String pathnameRight, String status) {
				boolean ok = (result == ResultCode.OK);

				if (mEyeSelection != Biometrics.EyeSelection.LEFT_EYE && bmRight != null) {
					if (!ok)
						mCaptureRight.setImageBitmap(bmRight);
					mPathnameRight = pathnameRight;
				}
				if (mEyeSelection != Biometrics.EyeSelection.RIGHT_EYE && bmLeft != null) {
					if (!ok)
						mCaptureLeft.setImageBitmap(bmLeft);
					mPathnameLeft = pathnameLeft;
				}
				if (status != null)
					setStatusText(status);
				if (result == ResultCode.OK) {
					Log.i(TAG, "Iris Captured Completed");
					enableCapture(false);
					convertToKind7(mPathnameLeft, mPathnameRight);
				}
				// If result failed
				if (result == ResultCode.FAIL) {
					Log.e(TAG, "onIrisesCaptured - FAILED");
					// Let user know captured failed
					setStatusText("Iris Captured Open-FAILED");
					mCloseBtn.setEnabled(false);
					enableCapture(true);
				}
			}

			@Override
			public void onCloseIrisScanner(Biometrics.CloseReasonCode closeReasonCode) {
				// Log output for debugging
				Log.d(TAG, "Iris Scanner closed: " + closeReasonCode.toString());
				resetCapture();
				// Let uesr know why scanner reader closed
				setStatusText("Iris Scanner closed: " + closeReasonCode.toString());
				enableCapture(true);
				// Make close button unclickable, since the scanner has just closed
				mCloseBtn.setEnabled(false);
			}
		});

		mCloseBtn.setEnabled(true);
	}

	private void onClose() {
		resetCapture();
		enableCapture(true);

		// Turn off close button since we are going to close everything
		mCloseBtn.setEnabled(false);

		// Close Iris Scanner
		mBiometrics.closeIrisScanner();
	}

	private long uncompressed_size;
	private long compressed_size;
	private long start_time;

	private void convertToKind7(String left_pathname, String right_pathname) {
		uncompressed_size = 0;
		String pathname = left_pathname != null ? left_pathname : right_pathname;
		if (pathname != null) {
			File uncompressed = new File(pathname);
			if (uncompressed.exists()) {
				uncompressed_size = uncompressed.length();
			}
		}
		start_time = SystemClock.elapsedRealtime();
		mBiometrics.convertToKind7(left_pathname, right_pathname, new OnConvertToKind7Listener() {

			@Override
			public void onConvertToKind7(ResultCode result, String pathnameLeft,
										 IrisQuality iqLeft, String pathnameRight, IrisQuality iqRight) {
				if (result == ResultCode.INTERMEDIATE) {
					setInfoText("Algorithms initializing...");
				} else if (result == ResultCode.FAIL) {
					setInfoText("Convert to Kind7 failed");
				} else {

					compressed_size = 0;
					String pathname = pathnameLeft != null ? pathnameLeft : pathnameRight;
					if (pathname != null) {
						File compressed = new File(pathname);
						if (compressed.exists()) {
							compressed_size = compressed.length();
						}
					}
					long duration = SystemClock.elapsedRealtime() - start_time;
					String str = String.format("PNG: %s, Kind7: %s, Dur: %dms",
							TheApp.abbreviateNumber(uncompressed_size),
							TheApp.abbreviateNumber(compressed_size), duration);
					setInfoText(str);
				}
			}
		});
	}

	private void setStatusText(String text) {
		if (!text.isEmpty())
			Log.d(TAG, "setStatusText: " + text);
		mStatusTextView.setText(text);
	}

	private void setInfoText(String text) {
		if (!text.isEmpty())
			Log.d(TAG, "setInfoText: " + text);
		mInfoTextView.setText(text);
	}

}
