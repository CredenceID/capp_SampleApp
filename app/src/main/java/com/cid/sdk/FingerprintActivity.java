package com.cid.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cid.sdk.models.DeviceFamily;
import com.cid.sdk.models.DeviceType;
import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.BiometricsManager;

import java.util.Arrays;

import static com.credenceid.biometrics.Biometrics.FmdFormat.ISO_19794_2_2005;
import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;

public class FingerprintActivity
		extends Activity {

	private final static String TAG = FingerprintActivity.class.getSimpleName();

	@SuppressLint("StaticFieldLeak")
	private static BiometricsManager mBiometricsManager;
	private static DeviceFamily mDeviceFamily;
	private static DeviceType mDeviceType;

	private final Biometrics.ScanType mScanTypes[] = {
			Biometrics.ScanType.SINGLE_FINGER,
			// These three scan types are only supported on Trident family of devices.
			Biometrics.ScanType.TWO_FINGERS,
			Biometrics.ScanType.ROLL_SINGLE_FINGER,
			Biometrics.ScanType.TWO_FINGERS_SPLIT
	};

	private ImageView mFingerprintOneImageView;
	private ImageView mFingerprintTwoImageView;
	private Button mOpenCloseButton;
	private Button mCaptureButton;
	private Button mMatchButton;
	private TextView mStatusTextView;
	private TextView mInfoTextView;

	// If true, then "mOpenClose" button text is "Open" meaning we need to open fingerprint.
	// If false, then "mOpenClose" button text is "Close" meaning we need to cloes fingerprint.
	private boolean mOpenFingerprint = true;
	// We are capturing two fingerprints. If true then capture saves data as first fingerprint; if
	// false capture saves data as second fingerprint.
	private boolean mCaptureFingerprintOne = true;
	// Stores FMD templates (used for fingerprint matching) for each fingerprint.
	private byte[] mFingerprintOneFMDTemplate = null;
	private byte[] mFingerprintTwoFMDTemplate = null;

	private Biometrics.FingerprintReaderStatusListener mFingerprintOpenCloseListener =
			new Biometrics.FingerprintReaderStatusListener() {
				@Override
				public void onOpenFingerprintReader(Biometrics.ResultCode resultCode,
													String hint) {
					Log.d(TAG, "onOpenFingerprintReader(): " + resultCode.name());

					// If hint is valid, display it.
					if (hint != null && !hint.isEmpty())
						mStatusTextView.setText(hint);

					// This code is returned while sensor is in middle of opening.
					// Allow user to open sensor is if it failed to open.
					// Alow user to close sensor if it successfully opened.
					if (resultCode != INTERMEDIATE)
						mOpenCloseButton.setEnabled(true);

					if (resultCode == Biometrics.ResultCode.OK) {
						// Now that sensor is open, if user presses "mOpenCloseButton" fingerprint
						// sensor should now close.
						mOpenFingerprint = false;

						// Only if fingerprint opened do we now allow user to capture fingerprints.
						mCaptureButton.setEnabled(true);
						// If fingerprint opened then we change button to say "Close".
						mOpenCloseButton.setText(getString(R.string.close));
					}
				}

				@SuppressLint("SetTextI18n")
				@Override
				public void onCloseFingerprintReader(Biometrics.ResultCode resultCode,
													 Biometrics.CloseReasonCode closeReasonCode) {
					if (resultCode == Biometrics.ResultCode.OK)
						mStatusTextView.setText("Fingerprint Closed: " + closeReasonCode.name());

					mOpenFingerprint = true;

					// Change text back to "Open" and allow button to be clickable.
					mOpenCloseButton.setText(getString(R.string.open));
					mOpenCloseButton.setEnabled(true);
					// Since sensor is closed, user should NOT be able to press capture or match.
					mCaptureButton.setEnabled(false);
					mMatchButton.setEnabled(false);
				}
			};

	// --------------------------------------------------------------------------------------------
	//
	// Methods called on Activity lifecycle.
	//
	// --------------------------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fingerprint);
		Log.d(TAG, "onCreate(Bundle)");

		mBiometricsManager = MainActivity.getBiometricsManager();
		mDeviceFamily = MainActivity.getDeviceFamily();
		mDeviceType = MainActivity.getDeviceType();

		this.initializeLayoutComponents();
		this.configureLayoutComponents();
	}

	/* Invoked when user pressed back menu button. */
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		Log.d(TAG, "onBackPressed()");

		// Tell biometrics to cancel current on-going capture.
		mBiometricsManager.cancelCapture();
		// Close all open peripherals.
		mBiometricsManager.closeFingerprintReader();

		// If user presses back button then they are exiting application. If this is the case then
		// tell C-Service to unbind from this application.
		mBiometricsManager.finalizeBiometrics(false);
	}

	/* Invoked when application is killed, either by user or system. */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Tell biometrics to cancel current on-going capture.
		mBiometricsManager.cancelCapture();
		// Close all open peripherals.
		mBiometricsManager.closeFingerprintReader();

		// If user presses back button then they are exiting application. If this is the case then
		// tell C-Service to unbind from this application.
		mBiometricsManager.finalizeBiometrics(false);
	}

	// --------------------------------------------------------------------------------------------
	//
	// Methods used for initialization of layout components.
	//
	// --------------------------------------------------------------------------------------------
	private void
	initializeLayoutComponents() {
		mFingerprintOneImageView = findViewById(R.id.finger_one_imageview);
		mFingerprintTwoImageView = findViewById(R.id.finger_two_imageview);

		mOpenCloseButton = findViewById(R.id.open_close_button);
		mCaptureButton = findViewById(R.id.capture_button);
		mMatchButton = findViewById(R.id.match_button);

		mStatusTextView = findViewById(R.id.status_textview);
		mInfoTextView = findViewById(R.id.fingeprint_info_textview);
	}

	private void
	configureLayoutComponents() {
		// Only allow capture once fingerprint is open.
		// Only allow match once both fingerprints have been captured.
		this.setCaptureMatchButtonEnable(false);

		mFingerprintOneImageView.setOnClickListener((View v) -> {
			mFingerprintOneImageView.setBackground(getResources().getDrawable(R.drawable.image_border_on));
			mFingerprintTwoImageView.setBackground(getResources().getDrawable(R.drawable.image_border_off));

			mCaptureFingerprintOne = true;
		});

		mFingerprintTwoImageView.setOnClickListener((View v) -> {
			mFingerprintOneImageView.setBackground(getResources().getDrawable(R.drawable.image_border_off));
			mFingerprintTwoImageView.setBackground(getResources().getDrawable(R.drawable.image_border_on));

			mCaptureFingerprintOne = false;
		});

		// Inside onClickListeners for each button, we disable all buttons until their respective
		// operation is complete. Once it is done, the appropriate buttons are re-enabled.
		mOpenCloseButton.setOnClickListener((View v) -> {
			// Disable button so user does not try a second open while fingerprint it opening.
			// Hide capture/math buttons since sensor is opening/closing.
			this.setAllButtonEnable(false);

			if (mOpenFingerprint)
				mBiometricsManager.openFingerprintReader(mFingerprintOpenCloseListener);
			else mBiometricsManager.closeFingerprintReader();
		});

		mCaptureButton.setOnClickListener((View v) -> {
			this.setAllButtonEnable(false);
			mInfoTextView.setText("");

			if (mCaptureFingerprintOne)
				this.captureFingerprintOne();
			else this.captureFingerprintTwo();
		});

		mMatchButton.setOnClickListener((View v) -> {
			this.setAllButtonEnable(false);
			this.matchFMDTemplates(mFingerprintOneFMDTemplate, mFingerprintTwoFMDTemplate);
		});
	}

	// --------------------------------------------------------------------------------------------
	//
	// Methods related to UI control.
	//
	// --------------------------------------------------------------------------------------------
	@SuppressWarnings("SameParameterValue")
	private void
	setCaptureMatchButtonEnable(boolean enable) {
		mCaptureButton.setEnabled(enable);
		mMatchButton.setEnabled(enable);
	}

	@SuppressWarnings("SameParameterValue")
	private void
	setAllButtonEnable(boolean enable) {
		mOpenCloseButton.setEnabled(enable);
		this.setCaptureMatchButtonEnable(enable);
	}

	// --------------------------------------------------------------------------------------------
	//
	// Methods used for performing some type of operation/processing.
	//
	// --------------------------------------------------------------------------------------------
	private void
	captureFingerprintOne() {
		mFingerprintOneFMDTemplate = null;

		// OnFingerprintGrabbedWSQListener: This listener is to be used if you wish to obtain a WSQ
		// template, fingerprint quality score, along with captured fingerprint image. This saves
		// from having to make separate API calls.
		mBiometricsManager.grabFingerprint(mScanTypes[0], new Biometrics.OnFingerprintGrabbedWSQListener() {
			@SuppressLint("SetTextI18n")
			@Override
			public void onFingerprintGrabbed(Biometrics.ResultCode resultCode,
											 Bitmap bitmap,
											 byte[] bytes,
											 String filepath,
											 String wsqFilepath,
											 String hint,
											 int nfiqScore) {
				// If a valid hint was given then display it for user to see.
				if (hint != null && !hint.isEmpty())
					mStatusTextView.setText(hint);

				// On each iteration of this callback we get a Bitmap of actual fingerprint.
				if (bitmap != null)
					mFingerprintOneImageView.setImageBitmap(bitmap);

				// This case may occur if "cancelCapture()" or "closeFingerprint()" are called while
				// in middle of capture.
				if (resultCode == INTERMEDIATE && hint != null && hint.equals("Capture Stopped")) {
					mOpenCloseButton.setEnabled(true);
					mCaptureButton.setEnabled(true);
				}

				// This ResultCode is returned once fingerprint has been fully captured.
				if (resultCode == OK) {
					mStatusTextView.setText("WSQ File: " + wsqFilepath);
					mInfoTextView.setText("Quality: " + nfiqScore);
					mCaptureButton.setEnabled(true);
					mOpenCloseButton.setEnabled(true);
					// Create template from fingerprint image.
					createFMDTemplate(bitmap);
				}

				// This ResultCode is returned is fingerprint capture fails.
				if (resultCode == FAIL) {
					mOpenCloseButton.setEnabled(true);
					mCaptureButton.setEnabled(true);
				}
			}

			@Override
			public void onCloseFingerprintReader(Biometrics.ResultCode resultCode,
												 Biometrics.CloseReasonCode closeReasonCode) {
				// This case is already handled by "mFingerprintOpenCloseListener".
			}
		});
	}

	private void
	captureFingerprintTwo() {
		mFingerprintTwoFMDTemplate = null;

		mBiometricsManager.grabFingerprint(mScanTypes[0], new Biometrics.OnFingerprintGrabbedListener() {
			@Override
			public void onFingerprintGrabbed(Biometrics.ResultCode resultCode,
											 Bitmap bitmap,
											 byte[] bytes,
											 String s,
											 String hint) {

				// If a valid hint was given then display it for user to see.
				if (hint != null && !hint.isEmpty())
					mStatusTextView.setText(hint);

				// On each iteration of this callback we get a Bitmap of actual fingerprint.
				if (bitmap != null)
					mFingerprintTwoImageView.setImageBitmap(bitmap);

				// This case may occur if "cancelCapture()" or "closeFingerprint()" are called while
				// in middle of capture.
				if (resultCode == INTERMEDIATE && hint != null && hint.equals("Capture Stopped")) {
					mOpenCloseButton.setEnabled(true);
					mCaptureButton.setEnabled(true);
				}

				// This ResultCode is returned once fingerprint has been fully captured.
				if (resultCode == OK) {
					mCaptureButton.setEnabled(true);
					mOpenCloseButton.setEnabled(true);
					// Create template from fingerprint image.
					createFMDTemplate(bitmap);
				}

				// This ResultCode is returned is fingerprint capture fails.
				if (resultCode == FAIL) {
					mOpenCloseButton.setEnabled(true);
					mCaptureButton.setEnabled(true);
				}
			}

			@Override
			public void onCloseFingerprintReader(Biometrics.ResultCode resultCode,
												 Biometrics.CloseReasonCode closeReasonCode) {
				// This case is already handled by "mFingerprintOpenCloseListener".
			}
		});
	}

	@SuppressLint("SetTextI18n")
	private void
	createFMDTemplate(Bitmap bitmap) {
		final long startTime = SystemClock.elapsedRealtime();

		mBiometricsManager.convertToFmd(bitmap, ISO_19794_2_2005, (Biometrics.ResultCode resultCode,
																   byte[] bytes) -> {
			if (resultCode != OK) {
				mStatusTextView.setText("Failed to create FMD template.");
				return;
			}

			// Display how long it took for FMD template to be created.
			final double durationInSeconds = (SystemClock.elapsedRealtime() - startTime) / 1000.0;
			mInfoTextView.setText("Created FMD template in: " + durationInSeconds + " seconds.");

			if (mCaptureFingerprintOne)
				mFingerprintOneFMDTemplate = Arrays.copyOf(bytes, bytes.length);
			else mFingerprintTwoFMDTemplate = Arrays.copyOf(bytes, bytes.length);

			// If both templates have been created then enable Match button.
			if (mFingerprintOneFMDTemplate != null && mFingerprintTwoFMDTemplate != null)
				mMatchButton.setEnabled(true);
		});
	}

	@SuppressLint("SetTextI18n")
	private void
	matchFMDTemplates(byte[] templateOne,
					  byte[] templateTwo) {
		mBiometricsManager.compareFmd(templateOne, templateTwo, ISO_19794_2_2005,
				(Biometrics.ResultCode resultCode, float dissimilarity) -> {
					// Re-enable all buttons since operation is now complete.
					this.setAllButtonEnable(true);

					if (resultCode != OK) {
						mStatusTextView.setText("Failed to compare templates.");
						mInfoTextView.setText("");
						return;
					}

					String matchDecision = "No Match";
					if (dissimilarity < (Integer.MAX_VALUE / 1000000))
						matchDecision = "Match";

					mStatusTextView.setText("Matching complete.");
					mInfoTextView.setText("Match outcome: " + matchDecision);
				});
	}
}