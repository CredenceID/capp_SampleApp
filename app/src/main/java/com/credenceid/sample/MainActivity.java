package com.credenceid.sample;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.BiometricsManager;
import com.credenceid.biometrics.DeviceFamily;
import com.credenceid.biometrics.DeviceType;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.view.View.INVISIBLE;
import static android.widget.Toast.LENGTH_LONG;
import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;
import static com.credenceid.biometrics.DeviceType.CredenceTabV2_FC;
import static com.credenceid.biometrics.DeviceType.CredenceTabV3_FM;
import static com.credenceid.biometrics.DeviceType.CredenceTabV4_FCM;
import static com.credenceid.biometrics.DeviceType.CredenceTwoV2_FC;

@SuppressWarnings("StatementWithEmptyBody")
public class MainActivity
		extends AppCompatActivity {

	/* When requested for permissions you must specify a number which sort of links the permissions
	 * request to a "key". This way when you get back a permissions event you can tell from where
	 * that permission was requested from.
	 */
	private static final int REQUEST_ALL_PERMISSIONS = 0;
	/* List of all permissions we will request. */
	private static final String[] PERMISSIONS = new String[]{
			WRITE_EXTERNAL_STORAGE,
			READ_EXTERNAL_STORAGE,
			CAMERA,
			READ_CONTACTS,
			READ_PHONE_STATE
	};

	/* --------------------------------------------------------------------------------------------
	 *
	 * Components in layout file.
	 *
	 * --------------------------------------------------------------------------------------------
	 */
	private TextView mProductNameTextView;
	private TextView mDeviceIDTextView;
	private TextView mServiceVersionTextView;
	private TextView mDeviceLibVersionTextView;
	private TextView mSDKAppVersionTextView;
	private ImageButton mFingerprintButton;
	private ImageButton mCardReaderButton;
	private ImageButton mFaceButton;
	private ImageButton mMRZButton;
	private ImageButton mIrisButton;

	/* --------------------------------------------------------------------------------------------
	 *
	 * Android activity lifecycle event methods.
	 *
	 * --------------------------------------------------------------------------------------------
	 */

	@Override
	protected void
	onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		this.requestPermissions();

		this.initializeLayoutComponents();
		this.configureLayoutComponents();

		this.initializeBiometrics();
	}

	/* Initializes all objects inside layout file. */
	private void
	initializeLayoutComponents() {

		mProductNameTextView = findViewById(R.id.textview_product_name);
		mDeviceIDTextView = findViewById(R.id.textview_device_id);
		mServiceVersionTextView = findViewById(R.id.textview_cid_service_version);
		mDeviceLibVersionTextView = findViewById(R.id.textview_device_lib_version);
		mSDKAppVersionTextView = findViewById(R.id.textview_app_version);

		mFingerprintButton = findViewById(R.id.fingerprint_button);
		mCardReaderButton = findViewById(R.id.cardreader_button);
		mFaceButton = findViewById(R.id.face_button);
		mMRZButton = findViewById(R.id.mrz_button);
		mIrisButton = findViewById(R.id.iris_button);
	}

	/* Configure all objects in layout file, set up listeners, views, etc. */
	private void
	configureLayoutComponents() {

		mSDKAppVersionTextView.setText(this.getPackageVersion());

		mFingerprintButton.setOnClickListener((View v) ->
				startActivity(new Intent(this, FingerprintActivity.class)));

		mCardReaderButton.setOnClickListener((View v) ->
				startActivity(new Intent(this, CardReaderActivity.class)));

		mFaceButton.setOnClickListener((View v) ->
				startActivity(new Intent(this, FaceActivity.class)));

		mMRZButton.setOnClickListener((View v) ->
				startActivity(new Intent(this, MRZActivity.class)));

		mIrisButton.setOnClickListener((View v) ->
				startActivity(new Intent(this, IrisActivity.class)));

		this.setBiometricButtonsVisibility(View.GONE);
	}

	/* --------------------------------------------------------------------------------------------
	 *
	 * Private helpers.
	 *
	 * --------------------------------------------------------------------------------------------
	 */

	/* Initializes CredenceSDK biometrics object. */
	private void
	initializeBiometrics() {

		/*  Create new biometrics object. */
		App.BioManager= new BiometricsManager(this);

		/* Initialize object, meaning tell CredenceService to bind to this application. */
		App.BioManager.initializeBiometrics((Biometrics.ResultCode resultCode,
												 String sdk_version,
												 String required_version) -> {

			if (OK == resultCode) {
				Toast.makeText(this, getString(R.string.bio_init_pass), LENGTH_LONG).show();

				App.DevFamily = App.BioManager.getDeviceFamily();
				App.DevType = App.BioManager.getDeviceType();

				/* Populate text fields which display device/App information. */
				mProductNameTextView.setText(App.BioManager.getProductName());
				mDeviceIDTextView.setText(App.BioManager.getDeviceID());
				mServiceVersionTextView.setText(App.BioManager.getServiceVersion());
				mDeviceLibVersionTextView.setText(App.BioManager.getSDKJarVersion());

				/* Configure which buttons user is allowed to see to based on current device this
				 * application is running on.
				 */
				this.configureButtons(App.DevFamily, App.DevType);

			} else if (INTERMEDIATE == resultCode) {
				/* This ResultCode is never returned for this API. */

			} else if (FAIL == resultCode) {
				Toast.makeText(this, getString(R.string.bio_init_failed), LENGTH_LONG).show();

				/* If biometrics failed to initialize then all API calls will return FAIL.
				 * Application should not proceed & close itself.
				 */
				finish();
			}
		});
	}

	/* Configures which biometrics buttons should be visible to user based on device type this
	 * application is running on.
	 *
	 * @param deviceFamily Family of device's to check against.
	 * @param deviceType Type of device to check against.
	 */
	private void
	configureButtons(@SuppressWarnings("unused") DeviceFamily deviceFamily,
					 DeviceType deviceType) {

		/* By default all Credence device's face a fingerprint sensor and camera. */
		mFingerprintButton.setVisibility(View.VISIBLE);
		mFaceButton.setVisibility(View.VISIBLE);

		/* Only these devices contain a card reader. */
		if (CredenceTwoV2_FC == deviceType ||
				CredenceTabV2_FC == deviceType ||
				CredenceTabV4_FCM == deviceType) {

			mCardReaderButton.setVisibility(View.VISIBLE);
		}

		/* Only these devices contain a MRZ reader. */
		if (CredenceTabV3_FM == deviceType || CredenceTabV4_FCM == deviceType)
			mMRZButton.setVisibility(View.VISIBLE);
	}

	/* Sets visibility for all biometrics buttons.
	 *
	 * @param visibility View.VISIBLE, View.INVISIBLE, View.GONE
	 */
	private void
	setBiometricButtonsVisibility(@SuppressWarnings("SameParameterValue") int visibility) {

		mFingerprintButton.setVisibility(visibility);
		mCardReaderButton.setVisibility(visibility);
		mFaceButton.setVisibility(visibility);
		mMRZButton.setVisibility(visibility);
	}

	/* Checks if permissions stated in manifest have been granted, if not it then requests them. */
	private void
	requestPermissions() {

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED
					|| checkSelfPermission(READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED
					|| checkSelfPermission(CAMERA) != PERMISSION_GRANTED
					|| checkSelfPermission(READ_CONTACTS) != PERMISSION_GRANTED
					|| checkSelfPermission(READ_PHONE_STATE) != PERMISSION_GRANTED) {

				requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSIONS);
			}
		}
	}

	/* Get this application's version number through manifest file. */
	private String
	getPackageVersion() {

		String version = "Unknown";
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			version = pInfo.versionName;
		} catch (Exception ignore) {
		}
		return version;
	}
}
