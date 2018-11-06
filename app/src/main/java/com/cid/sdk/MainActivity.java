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

public class MainActivity extends AppCompatActivity {
	private static final String TAG = MainActivity.class.getSimpleName();
	private static final int REQUEST_ALL_PERMISSIONS = 0;
	private static final String[] PERMISSIONS = new String[]{
			WRITE_EXTERNAL_STORAGE,
			READ_EXTERNAL_STORAGE,
			CAMERA,
			READ_CONTACTS,
			READ_PHONE_STATE
	};

	@SuppressLint("StaticFieldLeak")
	private static BiometricsManager mBiometricsManager;
	private static DeviceFamily mDeviceFamily = DeviceFamily.INVALID;
	private static DeviceType mDeviceType = DeviceType.INVALID;
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
	@SuppressWarnings("IfCanBeSwitch")
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
			mDeviceType = DeviceType.CTWO_V2;
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
			mDeviceType = DeviceType.CTAB_V4;
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

		// Create new biometrics object.
		mBiometricsManager = new BiometricsManager(this);

		// Initialize object, meaning tell CredenceService to bind to this application.
		mBiometricsManager.initializeBiometrics((Biometrics.ResultCode resultCode,
												 String sdk_version,
												 String required_version) -> {
			if (resultCode != Biometrics.ResultCode.OK)
				Toast.makeText(this, "Biometric initialization FAILED.", LENGTH_LONG).show();
			else {
				Toast.makeText(this, "Biometrics initialized.", LENGTH_LONG).show();

				// Save DeviceType/DeviceFamily so other activities may more easily identify on
				// what devices they are running on. This is used for activities to set up their
				// layouts, etc.
				this.setDeviceType(mBiometricsManager.getProductName());

				// Populate text fields which display device/app information.
				mProductNameTextView.setText(mBiometricsManager.getProductName());
				mDeviceIDTextView.setText(mBiometricsManager.getDeviceId());
				mServiceVersionTextView.setText(mBiometricsManager.getSDKVersion());
				mDeviceLibVersionTextView.setText(mBiometricsManager.getDeviceLibraryVersion());

				// Now display buttons which allow user to actually try out different biometrics.
				this.configureButtons();
			}
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
		// Display this apps version number.
		mSDKAppVersionTextView.setText(this.getPackageVersion());

		// Hide all buttons until Biometrics initializes.
		this.setGlobalButtonVisibility(View.GONE);

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

	/* Sets biometric buttons visibility, this is used since on start of app we want all buttons to
	 * be hidden until biometrics initialized.
	 *
	 * @param visibility View.VISIBLE, View.INVISIBLE, View.GONE
	 */
	private void
	setGlobalButtonVisibility(@SuppressWarnings("SameParameterValue") int visibility) {
		mFingerprintButton.setVisibility(visibility);
		mCardReaderButton.setVisibility(visibility);
		mFaceButton.setVisibility(visibility);
		mMRZButton.setVisibility(visibility);
		mIrisButton.setVisibility(visibility);
	}

	/* For each button, either hides/shows based on DeviceType. This is because not all device's
	 * support all biometrics.
	 */
	private void
	configureButtons() {
		mFingerprintButton.setVisibility(View.VISIBLE);
		mFaceButton.setVisibility(View.VISIBLE);

		if (mDeviceType == DeviceType.TWIZZLER ||
				mDeviceType == DeviceType.CONE_V2 || mDeviceType == DeviceType.CONE_V3 ||
				mDeviceType == DeviceType.CTWO_V2 ||
				mDeviceType == DeviceType.CTAB_V2 || mDeviceType == DeviceType.CTAB_V4)
			mCardReaderButton.setVisibility(View.VISIBLE);

		if (mDeviceType == DeviceType.CONE_V3 ||
				mDeviceType == DeviceType.CTAB_V3 || mDeviceType == DeviceType.CTAB_V4)
			mMRZButton.setVisibility(View.VISIBLE);

		if (mDeviceFamily == DeviceFamily.TRIDENT)
			mIrisButton.setVisibility(View.VISIBLE);
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
