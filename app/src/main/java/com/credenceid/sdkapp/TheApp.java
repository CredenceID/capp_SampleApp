package com.credenceid.sdkapp;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.BiometricsManager;

public class TheApp extends Application {
    private static final String TAG = TheApp.class.getName();

    private static Context context;
    private static TheApp theAppIntance;

    /* Example on how to use BiometricsManger in a Application. This object can now be used
     * globally throughout the application.
     */
    private BiometricsManager biometricsManager;

    public TheApp() {
        theAppIntance = this;
        context = this;
        this.biometricsManager = new BiometricsManager(this);
    }

    public static Context getAppContext() {
        return context;
    }

    public static TheApp getInstance() {
        return theAppIntance;
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

    public BiometricsManager getBiometricsManager() {
        return this.biometricsManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        /* Need to initialize Biometrics manually as BiometricsManager does not do so
         * automatically.
         */
        this.biometricsManager.initializeBiometrics(new Biometrics.OnInitializedListener() {
            @Override
            public void onInitialized(Biometrics.ResultCode resultCode,
                                      String sdk_version,
                                      String required_version) {
                Log.d(TAG, "Test App product name is " + biometricsManager.getProductName());

                if (resultCode != Biometrics.ResultCode.OK) {
                    Toast.makeText(TheApp.this,
                            "Biometric's failed to initialize.",
                            Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Initaliation failed");
                }
            }
        });
    }
}
