package com.credenceid.sdkapp;

import android.util.Log;
import android.widget.Toast;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.BiometricsManager;

/**
 * Created by markevans on 10/24/16.
 */

public class BiometricsManagerInstance {
    private static final String TAG = BiometricsManagerInstance.class.getName();
    private static BiometricsManagerInstance mInstance = null;

    private BiometricsManager biometrics_manager;

    // private constructor prevents instantiation from other classes
    private BiometricsManagerInstance() {

    }

    /**
     * Creates a new instance of BiometricsManagerInstance.
     */
    public static BiometricsManagerInstance getInstance() {

        if (mInstance == null) {
            mInstance = new BiometricsManagerInstance();
            mInstance.init();
        }

        return mInstance;
    }

    private void init() {
        biometrics_manager = new BiometricsManager(TheApp.getInstance());
        biometrics_manager.initializeBiometrics(new Biometrics.OnInitializedListener() {
            @Override
            public void onInitialized(Biometrics.ResultCode resultCode, String sdk_version, String required_version) {
                Log.d(TAG, "Test App product name is " + biometrics_manager.getProductName());
                if (resultCode != Biometrics.ResultCode.OK) {
                    String str = String.format("Biometric initialization failed\nSDK version: %s\nRequired_version: %s", sdk_version, required_version);
                    Toast.makeText(TheApp.getInstance(), str, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Initaliation failed");
                } else {
                    Toast.makeText(TheApp.getInstance(), "Biometrics Initialized in Singleton", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Initaliation success ");
                }
            }
        });
    }

    BiometricsManager getBiometricsManager() {
        return biometrics_manager;
    }
}
