package com.cid.sdk;

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

import com.cid.sdk.models.DeviceFamily;
import com.cid.sdk.models.DeviceType;
import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.BiometricsManager;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.widget.Toast.LENGTH_LONG;
import static com.cid.sdk.models.DeviceType.CTAB_V2;
import static com.cid.sdk.models.DeviceType.CTAB_V3;
import static com.cid.sdk.models.DeviceType.CTAB_V4;
import static com.cid.sdk.models.DeviceType.CTWO_V2;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = MainActivity.class.getSimpleName();
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

	/* CredenceSDK biometrics object, used to interface with APIs. */
	@SuppressLint("StaticFieldLeak")
	private static BiometricsManager mBiometricsManager;
	/* Stores which Credence family of device's this app is running on. */
	private static DeviceFamily mDeviceFamily = DeviceFamily.CID_PRODUCT;
	/* Stores which specific device this app is running on. */
	private static DeviceType mDeviceType = DeviceType.CID_PRODUCT;

	/*
	 * Components in layout file.
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

	public static BiometricsManager
	getBiometricsManager() {
		return mBiometricsManager;
	}

	public static DeviceFamily
	getDeviceFamily() {
		return mDeviceFamily;
	}

	public static DeviceType
	getDeviceType() {
		return mDeviceType;
	}

	/* Based on given product name, defines DeviceType and DeviceFamily.
	 *
	 * @param productName Product name returned via BiometricsManager.getProductName()
	 */
	@SuppressWarnings({"IfCanBeSwitch", "SpellCheckingInspection"})
	private void
	setDeviceType(String productName) {
		Log.d(TAG, "setDeviceType(" + productName + ")");

		if (productName == null || productName.length() == 0) {
			Log.e(TAG, "Invalid product name!");
			return;
		}

		if (productName.equals("Twizzler")) {
			mDeviceType = DeviceType.TWIZZLER;
			mDeviceFamily = DeviceFamily.TWIZZLER;
		} else if (productName.equals("Trident-1")) {
			mDeviceType = DeviceType.TRIDENT_1;
			mDeviceFamily = DeviceFamily.TRIDENT;
		} else if (productName.equals("Trident-2")) {
			mDeviceType = DeviceType.TRIDENT_2;
			mDeviceFamily = DeviceFamily.TRIDENT;
		} else if (productName.equals("Credence One V1")) {
			mDeviceType = DeviceType.CONE_V1;
			mDeviceFamily = DeviceFamily.CONE;
		} else if (productName.equals("Credence One V2")) {
			mDeviceType = DeviceType.CONE_V2;
			mDeviceFamily = DeviceFamily.CONE;
		} else if (productName.equals("Credence One V3")) {
			mDeviceType = DeviceType.CONE_V3;
			mDeviceFamily = DeviceFamily.CONE;
		} else if (productName.equals("Credence Two V1")) {
			mDeviceType = DeviceType.CTWO_V1;
			mDeviceFamily = DeviceFamily.CTWO;
		} else if (productName.equals("Credence Two V2")) {
			mDeviceType = CTWO_V2;
			mDeviceFamily = DeviceFamily.CTWO;
		} else if (productName.equals("Credence TAB V1")) {
			mDeviceType = DeviceType.CTAB_V1;
			mDeviceFamily = DeviceFamily.CTAB;
		} else if (productName.equals("Credence TAB V2")) {
			mDeviceType = DeviceType.CTAB_V2;
			mDeviceFamily = DeviceFamily.CTAB;
		} else if (productName.equals("Credence TAB V3")) {
			mDeviceType = DeviceType.CTAB_V3;
			mDeviceFamily = DeviceFamily.CTAB;
		} else if (productName.equals("Credence TAB V4")) {
			mDeviceType = CTAB_V4;
			mDeviceFamily = DeviceFamily.CTAB;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		this.requestPermissions();

		this.initializeLayoutComponents();
		this.configureLayoutComponents();
		this.setBiometricButtonsVisibility(View.GONE);

		/*  Create new biometrics object. */
		mBiometricsManager = new BiometricsManager(this);

		/* Initialize object, meaning tell CredenceService to bind to this application. */
		mBiometricsManager.initializeBiometrics((Biometrics.ResultCode resultCode,
												 String sdk_version,
												 String required_version) -> {

			if (Biometrics.ResultCode.OK != resultCode) {
				Toast.makeText(this, "Biometric initialization FAILED. Exiting application.", LENGTH_LONG).show();
				finish();
				return;
			}

			Toast.makeText(this, "Biometrics initialized.", LENGTH_LONG).show();

			/* Save DeviceType/DeviceFamily so other activities may more easily identify on
			 * what devices they are running on. This is used for activities to set up their
			 * layouts, etc.
			 */
			this.setDeviceType(mBiometricsManager.getProductName());

			/* Populate text fields which display device/app information. */
			mProductNameTextView.setText(mBiometricsManager.getProductName());
			mDeviceIDTextView.setText(mBiometricsManager.getDeviceId());
			mServiceVersionTextView.setText(mBiometricsManager.getSDKVersion());
			mDeviceLibVersionTextView.setText(mBiometricsManager.getDeviceLibraryVersion());

			/* Configure which buttons user is allowed to see to try out different biometrics based on
			 * current device this application is running on.
			 */
			this.configureButtons(mDeviceFamily, mDeviceType);
		});
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
	}

	private void
	configureButtons(@SuppressWarnings("unused") DeviceFamily deviceFamily,
					 DeviceType deviceType) {

		/* By default all Credence device's face a fingerprint sensor and camera. */
		mFingerprintButton.setVisibility(View.VISIBLE);
		mFaceButton.setVisibility(View.VISIBLE);

		/* Only these devices contain a card reader. */
		if (CTWO_V2 == deviceType || CTAB_V2 == deviceType || CTAB_V4 == deviceType)
			mCardReaderButton.setVisibility(View.VISIBLE);

		/* Only these devices contain a MRZ reader. */
		if (CTAB_V3 == deviceType || CTAB_V4 == deviceType)
			mMRZButton.setVisibility(View.VISIBLE);
	}

	private void
	setBiometricButtonsVisibility(@SuppressWarnings("SameParameterValue") int visibility) {
		mFingerprintButton.setVisibility(visibility);
		mCardReaderButton.setVisibility(visibility);
		mFaceButton.setVisibility(visibility);
		mMRZButton.setVisibility(visibility);
	}

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
		Log.d(TAG, "getPackageVersion()");

		String version = "???";
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			version = pInfo.versionName;
		} catch (Exception e) {
			Log.w(TAG, e.getMessage());
		}
		return version;
	}
}
