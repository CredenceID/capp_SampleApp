package com.credenceid.sdkapp;

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
	private static final String TAG = TheApp.class.getName();

	@SuppressWarnings({"all"})
	private static Context mContext;
	@SuppressWarnings({"all"})
	private static TheApp mTheAppInstance;

	// First method where an object is used.
	private BiometricsManager mBiometricsManager;

	public TheApp() {
		mTheAppInstance = this;
		mContext = this;

		// Create new instance of our biometrics object, make sure to pass it a context.
		mBiometricsManager = new BiometricsManager(this);
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

	@Override
	public void
	onCreate() {
		super.onCreate();

		// Once our object has been create we will initialize the biometrics. What this does is tell
		// CredenceService to bind to this application.
		mBiometricsManager.initializeBiometrics((Biometrics.ResultCode resultCode,
												 String sdk_version,
												 String required_version) -> {
			Log.d(TAG, "Product Name: " + mBiometricsManager.getProductName());

			if (resultCode != Biometrics.ResultCode.OK) {
				Toast.makeText(this, "Biometric initialization FAILED.", LENGTH_LONG).show();
				Log.w(TAG, "Biometric initialization FAILED.");
			}
		});
	}

	public BiometricsManager
	getBiometricsManager() {
		return mBiometricsManager;
	}
}
