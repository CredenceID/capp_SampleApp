package com.credenceid.sdkapp.utils;

import android.util.Log;
import android.widget.Toast;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.BiometricsManager;
import com.credenceid.sdkapp.TheApp;

/* Singleton allowing use of CredenceSDK through a BiometricsManager object. Multiple pages make use
 * our API calls so instead of creating/destroying multiple objects we can just stick with a
 * globally shared object. This is essentially a wrapper for BiometricsManager.
 */
public class BiometricsManagerInstance {
	private static final String TAG = BiometricsManagerInstance.class.getSimpleName();
	private static BiometricsManagerInstance mInstance = null;
	private BiometricsManager mBiometricsManager;

	private BiometricsManagerInstance() {
	}

	/* Creates a new instance of this class. */
	@SuppressWarnings("UnusedReturnValue")
	public static BiometricsManagerInstance
	getInstance() {
		Log.d(TAG, "getInstance()");

		// If we already have a previously running instance then do not create a new one. Simply
		// return previously made instance.
		if (mInstance == null) {
			mInstance = new BiometricsManagerInstance();
			mInstance.initializeBiometrics();
		}
		return mInstance;
	}

	/* Method to initialize CredenceService to bind to this application. */
	private void
	initializeBiometrics() {
		// Create a new BiometricsManager object using instance of this application, context.
		mBiometricsManager = new BiometricsManager(TheApp.getInstance());

		// Now tell our object to initialize and bind to service.
		mBiometricsManager.initializeBiometrics(new Biometrics.OnInitializedListener() {
			@Override
			public void onInitialized(Biometrics.ResultCode resultCode, String sdk_version, String required_version) {
				Log.d(TAG, "Test App product name is " + mBiometricsManager.getProductName());

				if (resultCode != Biometrics.ResultCode.OK) {
					Toast.makeText(TheApp.getInstance(), "Biometrics initialization failed.", Toast.LENGTH_LONG).show();
					Log.d(TAG, "Initaliation failed");
				} else {
					Toast.makeText(TheApp.getInstance(), "Biometrics initialized.", Toast.LENGTH_LONG).show();
					Log.d(TAG, "Initaliation success ");
				}
			}
		});
	}
}
