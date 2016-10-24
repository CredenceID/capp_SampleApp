package com.credenceid.sdkapp;

import com.credenceid.biometrics.BiometricsManager;

/**
 * Created by markevans on 10/24/16.
 */

public class BiometricsManagerInstance {
    private static BiometricsManagerInstance mInstance = null;

    private static BiometricsManager biometrics_manager;

    // private constructor prevents instantiation from other classes
    private BiometricsManagerInstance() {

    }

    /**
     * Creates a new instance of BiometricsManagerInstance.
     */
    public static BiometricsManagerInstance getInstance() {

        if (mInstance == null) {
            mInstance = new BiometricsManagerInstance();
        }

        if (biometrics_manager == null) {
            biometrics_manager = new BiometricsManager(TheApp.getContext());
        }

        return mInstance;
    }

    public static BiometricsManager getBiometricsManager() {
        return biometrics_manager;
    }
}
