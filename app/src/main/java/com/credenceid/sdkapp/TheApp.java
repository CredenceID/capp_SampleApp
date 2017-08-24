package com.credenceid.sdkapp;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.BiometricsManager;

public class TheApp extends Application {
    private static final String TAG = TheApp.class.getName();

    public static boolean DEBUG = true;
    private static Context mContext;
    private static TheApp mInstance;

    // Example on how to use BiometricsManger in a Application class that can be used globally
    private BiometricsManager mBiometricsManager;
    private Toast mToast;

    public TheApp() {
        mInstance = this;
        mContext = this;
        mBiometricsManager = new BiometricsManager(this);
    }

    public static Context getAppContext() {
        return mContext;
    }

    public static TheApp getInstance() {
        return mInstance;
    }

    public static String abbreviateNumber(long value) {
        if (value < 1024)
            return String.valueOf(value);
        value /= 1024;
        if (value < 1024)
            return String.valueOf(value) + "K";
        value /= 1024;
        return String.valueOf(value) + "M";
    }

    public static String nullTerminatedByteArrayToString(byte[] buffer) {
        if (buffer == null)
            return null;
        int length = buffer.length;
        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] == 0) {
                length = i;
                break;
            }
        }
        return new String(buffer, 0, length);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Need to initialize Biometrics manually as BiometricsManager does not do it automatically
        mBiometricsManager.initializeBiometrics(new Biometrics.OnInitializedListener() {
            @Override
            public void onInitialized(Biometrics.ResultCode resultCode, String sdk_version, String required_version) {
                Log.d(TAG, "Test App product name is " + mBiometricsManager.getProductName());
                if (resultCode != Biometrics.ResultCode.OK) {
                    //					String str = String.format("Biometric initialization failed\nSDK version: %s\nRequired_version: %s", sdk_version, required_version);
                    //					Toast.makeText(TheApp.this, str, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Initaliation failed");
                } else {
                    Log.d(TAG, "Initaliation success ");
                }
            }
        });
    }

    public BiometricsManager getBiometricsManager() {
        return mBiometricsManager;
    }

    public void showToast(CharSequence cs) {
        if (DEBUG)
            Log.d(TAG, "showToast: " + cs);
        if (mToast == null) {
            mToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
        }
        mToast.setText(cs);
        mToast.show();
    }

}
