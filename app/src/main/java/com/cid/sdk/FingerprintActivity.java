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
import com.credenceid.biometrics.Biometrics.OnFingerprintGrabbedWSQListener;
import com.credenceid.biometrics.BiometricsManager;

import java.util.Arrays;

import static com.credenceid.biometrics.Biometrics.FmdFormat.ISO_19794_2_2005;
import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;

public class FingerprintActivity
		extends Activity {
	private final static String TAG = FingerprintActivity.class.getSimpleName();

	/* CredenceSDK biometrics object, used to interface with APIs. */
	@SuppressLint("StaticFieldLeak")
	private static BiometricsManager mBiometricsManager;
	/* Stores which Credence family of device's this app is running on. */
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	private static DeviceFamily mDeviceFamily = DeviceFamily.INVALID;
	/* Stores which specific device this app is running on. */
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	private static DeviceType mDeviceType = DeviceType.INVALID;

	/* List of different fingerprint scan types supported across all Credence devices. */
	private final Biometrics.ScanType mScanTypes[] = {
			Biometrics.ScanType.SINGLE_FINGER,
			/* These three scan types are only supported on Trident family of devices. */
			Biometrics.ScanType.TWO_FINGERS,
			Biometrics.ScanType.ROLL_SINGLE_FINGER,
			Biometrics.ScanType.TWO_FINGERS_SPLIT
	};

	/*
	 * Components in layout file.
	 */
	private ImageView mFingerprintOneImageView;
	private ImageView mFingerprintTwoImageView;
	private Button mOpenCloseButton;
	private Button mCaptureButton;
	private Button mMatchButton;
	private TextView mStatusTextView;
	private TextView mInfoTextView;

	/* If true, then "mOpenClose" button text is "Open" meaning we need to open fingerprint.
	 * If false, then "mOpenClose" button text is "Close" meaning we need to close fingerprint.
	 */
	private boolean mOpenFingerprint = true;
	/* We are capturing two fingerprints. If true then capture saves data as first fingerprint; if
	 * false capture saves data as second fingerprint.
	 */
	private boolean mCaptureFingerprintOne = true;
	/* Stores FMD templates (used for fingerprint matching) for each fingerprint. */
	private byte[] mFingerprintOneFMDTemplate = null;
	private byte[] mFingerprintTwoFMDTemplate = null;

	/* Callback invoked every time fingerprint sensor on device opens or closes. */
	private Biometrics.FingerprintReaderStatusListener mFingerprintOpenCloseListener =
			new Biometrics.FingerprintReaderStatusListener() {
				@Override
				public void onOpenFingerprintReader(Biometrics.ResultCode resultCode,
													String hint) {
					Log.d(TAG, "onOpenFingerprintReader(): " + resultCode.name());

					/* If hint is valid, display it. */
					if (hint != null && !hint.isEmpty())
						mStatusTextView.setText(hint);

					/* This code is returned while sensor is in middle of opening.
					 * Allow user to open sensor if it failed to open.
					 * Allow user to close sensor if it successfully opened.
					 */
					if (resultCode != INTERMEDIATE)
						mOpenCloseButton.setEnabled(true);

					if (resultCode == Biometrics.ResultCode.OK) {
						/* Now that sensor is open, if user presses "mOpenCloseButton" fingerprint
						 * sensor should now close. To achieve this we change flag which controls
						 * what action mOpenCloseButton takes.
						 */
						mOpenFingerprint = false;

						/* Only if fingerprint opened do we allow user to capture fingerprints. */
						mCaptureButton.setEnabled(true);
						/* If fingerprint opened then we change button to say "Close". */
						mOpenCloseButton.setText(getString(R.string.close));
					}
				}

				@SuppressLint("SetTextI18n")
				@Override
				public void onCloseFingerprintReader(Biometrics.ResultCode resultCode,
													 Biometrics.CloseReasonCode closeReasonCode) {
					if (resultCode == Biometrics.ResultCode.OK)
						mStatusTextView.setText("Fingerprint Closed: " + closeReasonCode.name());

					/* Now that sensor is closed, if user presses "mOpenCloseButton" fingerprint
					 * sensor should now open. To achieve this we change flag which controls
					 * what action mOpenCloseButton takes.
					 */
					mOpenFingerprint = true;

					/* Change text back to "Open" and allow button to be clickable. */
					mOpenCloseButton.setText(getString(R.string.open));
					mOpenCloseButton.setEnabled(true);
					/* Since sensor is closed, user should NOT be able to press capture or match. */
					mCaptureButton.setEnabled(false);
					mMatchButton.setEnabled(false);
				}
			};

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

		/* If back button is pressed when we want to destroy activity. */
		this.onDestroy();
	}

	/* Invoked when application is killed, either by user or system. */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		/* Tell biometrics to cancel current on-going capture. */
		mBiometricsManager.cancelCapture();
		/* Close all open peripherals. */
		mBiometricsManager.closeFingerprintReader();

		/* If user presses back button then they are exiting application. If this is the case then
		 * tell C-Service to unbind from this application.
		 */
		mBiometricsManager.finalizeBiometrics(false);
	}

	/* Initializes all objects inside layout file. */
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

	/* Configure all objects in layout file, set up listeners, views, etc. */
	private void
	configureLayoutComponents() {
		/* Only allow capture once fingerprint is open. */
		/* Only allow match once both fingerprints have been captured. */
		this.setCaptureMatchButtonEnable(false);

		mFingerprintOneImageView.setOnClickListener((View v) -> {
			/* This ImageView should turn green since it was selected. */
			mFingerprintOneImageView.setBackground(getResources().getDrawable(R.drawable.image_border_on));
			/* Other ImageView should turn black or off. */
			mFingerprintTwoImageView.setBackground(getResources().getDrawable(R.drawable.image_border_off));

			mCaptureFingerprintOne = true;
		});

		mFingerprintTwoImageView.setOnClickListener((View v) -> {
			/* This ImageView should turn green since it was selected. */
			mFingerprintTwoImageView.setBackground(getResources().getDrawable(R.drawable.image_border_on));
			/* Other ImageView should turn black or off. */
			mFingerprintOneImageView.setBackground(getResources().getDrawable(R.drawable.image_border_off));

			mCaptureFingerprintOne = false;
		});

		/* Inside onClickListeners for each button, we disable all buttons until their respective
		 * operation is complete. Once it is done, the appropriate buttons are re-enabled.
		 */
		mOpenCloseButton.setOnClickListener((View v) -> {
			/* Disable button so user does not try a second open while fingerprint it opening.
			 * Hide capture/math buttons since sensor is opening/closing.
			 */
			this.setAllComponentEnable(false);

			if (mOpenFingerprint)
				mBiometricsManager.openFingerprintReader(mFingerprintOpenCloseListener);
			else mBiometricsManager.closeFingerprintReader();
		});

		mCaptureButton.setOnClickListener((View v) -> {
			this.setAllComponentEnable(false);
			mInfoTextView.setText("");

			/* Based on which ImageView was selected, capture appropriate fingerprint. */
			if (mCaptureFingerprintOne)
				this.captureFingerprintOne();
			else this.captureFingerprintTwo();
		});

		mMatchButton.setOnClickListener((View v) -> {
			this.setAllComponentEnable(false);
			this.matchFMDTemplates(mFingerprintOneFMDTemplate, mFingerprintTwoFMDTemplate);
		});
	}

	/* Sets enable for "mCaptureButton" and "mMatchButton" components.
	 *
	 * @param enable If true components are enabled, if false they are disabled.
	 */
	@SuppressWarnings("SameParameterValue")
	private void
	setCaptureMatchButtonEnable(boolean enable) {
		mCaptureButton.setEnabled(enable);
		mMatchButton.setEnabled(enable);
	}

	/* Sets enable for all UI components in layout.
	 *
	 * @param enable If true components are enabled, if false they are disabled.
	 */
	@SuppressWarnings("SameParameterValue")
	private void
	setAllComponentEnable(boolean enable) {
		this.setCaptureMatchButtonEnable(enable);

		mOpenCloseButton.setEnabled(enable);

		mFingerprintOneImageView.setEnabled(enable);
		mFingerprintTwoImageView.setEnabled(enable);
	}

	private void
	captureFingerprintOne() {
		mFingerprintOneFMDTemplate = null;

		/* OnFingerprintGrabbedWSQListener: This listener is to be used if you wish to obtain a WSQ
		 * template, fingerprint quality score, along with captured fingerprint image. This saves
		 * from having to make separate API calls.
		 */
		mBiometricsManager.grabFingerprint(mScanTypes[0], new OnFingerprintGrabbedWSQListener() {
			@SuppressLint("SetTextI18n")
			@Override
			public void onFingerprintGrabbed(Biometrics.ResultCode resultCode,
											 Bitmap bitmap,
											 byte[] bytes,
											 String filepath,
											 String wsqFilepath,
											 String hint,
											 int nfiqScore) {
				/* If a valid hint was given then display it for user to see. */
				if (hint != null && !hint.isEmpty())
					mStatusTextView.setText(hint);

				/* On each iteration of this callback we get a Bitmap of actual fingerprint. */
				if (bitmap != null)
					mFingerprintOneImageView.setImageBitmap(bitmap);

				/* This case may occur if "cancelCapture()" or "closeFingerprint()" are called while
				 * in middle of capture.
				 */
				if (resultCode == INTERMEDIATE && hint != null && hint.equals("Capture Stopped")) {
					mOpenCloseButton.setEnabled(true);
					mCaptureButton.setEnabled(true);
				}

				/* This ResultCode is returned once fingerprint has been fully captured. */
				if (resultCode == OK) {
					mStatusTextView.setText("WSQ File: " + wsqFilepath);
					mInfoTextView.setText("Quality: " + nfiqScore);
					mCaptureButton.setEnabled(true);
					mOpenCloseButton.setEnabled(true);

					/* Create template from fingerprint image. */
					createFMDTemplate(bitmap);
				}

				/* This ResultCode is returned is fingerprint capture fails. */
				if (resultCode == FAIL) {
					mOpenCloseButton.setEnabled(true);
					mCaptureButton.setEnabled(true);
				}
			}

			@Override
			public void onCloseFingerprintReader(Biometrics.ResultCode resultCode,
												 Biometrics.CloseReasonCode closeReasonCode) {
				/* This case is already handled by "mFingerprintOpenCloseListener". */
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

				/* If a valid hint was given then display it for user to see. */
				if (hint != null && !hint.isEmpty())
					mStatusTextView.setText(hint);

				/* On each iteration of this callback we get a Bitmap of actual fingerprint. */
				if (bitmap != null)
					mFingerprintTwoImageView.setImageBitmap(bitmap);

				/* This case may occur if "cancelCapture()" or "closeFingerprint()" are called while
				 * in middle of capture.
				 */
				if (resultCode == INTERMEDIATE && hint != null && hint.equals("Capture Stopped")) {
					mOpenCloseButton.setEnabled(true);
					mCaptureButton.setEnabled(true);
				}

				/* This ResultCode is returned once fingerprint has been fully captured. */
				if (resultCode == OK) {
					mCaptureButton.setEnabled(true);
					mOpenCloseButton.setEnabled(true);

					/* Create template from fingerprint image. */
					createFMDTemplate(bitmap);
				}

				/* This ResultCode is returned is fingerprint capture fails. */
				if (resultCode == FAIL) {
					mOpenCloseButton.setEnabled(true);
					mCaptureButton.setEnabled(true);
				}
			}

			@Override
			public void onCloseFingerprintReader(Biometrics.ResultCode resultCode,
												 Biometrics.CloseReasonCode closeReasonCode) {
				/* This case is already handled by "mFingerprintOpenCloseListener". */
			}
		});
	}

	/* Attempts to create a FMD template from given Bitmap image. If successful it saves FMD to
	 * respective fingerprint template array.
	 *
	 * @param bitmap
	 */
	@SuppressLint("SetTextI18n")
	private void
	createFMDTemplate(Bitmap bitmap) {
		/* Keep a track of how long it takes for FMD creation. */
		final long startTime = SystemClock.elapsedRealtime();

		mBiometricsManager.convertToFmd(bitmap, ISO_19794_2_2005, (Biometrics.ResultCode resultCode,
																   byte[] bytes) -> {
			if (resultCode != OK) {
				mStatusTextView.setText("Failed to create FMD template.");
				return;
			}

			/* Display how long it took for FMD template to be created. */
			final double durationInSeconds = (SystemClock.elapsedRealtime() - startTime) / 1000.0;
			mInfoTextView.setText("Created FMD template in: " + durationInSeconds + " seconds.");

			if (mCaptureFingerprintOne)
				mFingerprintOneFMDTemplate = Arrays.copyOf(bytes, bytes.length);
			else mFingerprintTwoFMDTemplate = Arrays.copyOf(bytes, bytes.length);

			/* If both templates have been created then enable Match button. */
			if (mFingerprintOneFMDTemplate != null && mFingerprintTwoFMDTemplate != null)
				mMatchButton.setEnabled(true);
		});
	}

	/* Matches two FMD templates and displays score.
	 *
	 * @param templateOne FMD template.
	 * @param templateTwo FMD template to match against.
	 */
	@SuppressLint("SetTextI18n")
	private void
	matchFMDTemplates(byte[] templateOne,
					  byte[] templateTwo) {
		/* Normally one would handle parameter checking, but this API handles it for us. Meaning
		 * that if any FMD is invalid it will return the proper score of 0, etc.
		 */

		mBiometricsManager.compareFmd(templateOne, templateTwo, ISO_19794_2_2005,
				(Biometrics.ResultCode resultCode, float dissimilarity) -> {
					/* Re-enable all components since operation is now complete. */
					this.setAllComponentEnable(true);

					if (resultCode != OK) {
						mStatusTextView.setText("Failed to compare templates.");
						mInfoTextView.setText("");
						return;
					}
					/* Code reaches here if templates were matched and a score calculated. */

					String matchDecision = "No Match";
					/* This is how to properly determine a match or not. */
					if (dissimilarity < (Integer.MAX_VALUE / 1000000))
						matchDecision = "Match";

					mStatusTextView.setText("Matching complete.");
					mInfoTextView.setText("Match outcome: " + matchDecision);
				});
	}
}