package com.credenceid.sdkapp;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.BiometricsManager;

import static android.widget.Toast.LENGTH_LONG;

/* This application demonstrates using CredenceID SDK via two different ways.
 *
 * The first way is through "BiometricsManager" object. A user may create an instance of this
 * object, initialize it, and then call its methods. This method is traditional way for using custom
 * created objects in Java.
 *
 * The second way is to your have class extend the "BiometricsActivity" class. By doing this all
 * methods contained inside BiometricsActivity become visible to your class.
 *
 * You will see both methods being used throughout the application.
 */
public class TheApp
		extends Application {
	private static final String TAG = TheApp.class.getSimpleName();

	@SuppressLint("StaticFieldLeak")
	private static Context mContext;
	@SuppressLint("StaticFieldLeak")
	private static TheApp mTheAppInstance;

	private static DeviceFamily mDeviceFamily = DeviceFamily.INVALID;
	private static DeviceType mDeviceType = DeviceType.INVALID;

	// First method where an object is used.
	private BiometricsManager mBiometricsManager;

	public TheApp() {
		mTheAppInstance = this;
		mContext = this;

		// Create new instance of our biometrics object, make sure to pass it a context.
		mBiometricsManager = new BiometricsManager(this);
	}

	@Override
	public void
	onCreate() {
		super.onCreate();

		// Once our object has been create we will initialize the biometrics. What this does is tell
		// CredenceService to bind to this application.
		mBiometricsManager.initializeBiometrics((Biometrics.ResultCode resultCode,
												 String sdk_version,
												 String required_version) -> {
			this.setDeviceType(mBiometricsManager.getProductName());

			if (resultCode != Biometrics.ResultCode.OK) {
				Toast.makeText(this, "Biometric initialization FAILED.", LENGTH_LONG).show();
				Log.w(TAG, "Biometric initialization FAILED.");
			}
		});
	}

	public static Context
	getAppContext() {
		return mContext;
	}

	public static TheApp
	getInstance() {
		return mTheAppInstance;
	}

	public static String
	abbreviateNumber(long value) {
		if (value < 1024)
			return String.valueOf(value);

		value /= 1024;
		if (value < 1024)
			return String.valueOf(value) + "K";

		value /= 1024;
		return String.valueOf(value) + "M";
	}

	public BiometricsManager
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
		}else if (productName.equals("Credence TAB V3")) {
			mDeviceType = DeviceType.CTAB_V3;
			mDeviceFamily = DeviceFamily.CTAB;
		}else if (productName.equals("Credence TAB V4")) {
			mDeviceType = DeviceType.CTAB_V4;
			mDeviceFamily = DeviceFamily.CTAB;
		}
	}
}
