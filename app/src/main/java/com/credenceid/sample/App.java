package com.credenceid.sample;

import android.annotation.SuppressLint;
import android.app.Application;

import com.credenceid.biometrics.BiometricsManager;
import com.credenceid.biometrics.DeviceFamily;
import com.credenceid.biometrics.DeviceType;

@SuppressLint("StaticFieldLeak")
public class App
        extends Application {

    /* CredenceSDK biometrics object used to interface with APIs. */
    public static BiometricsManager BioManager;
    /* Stores which Credence family of device's this app is running on. */
    public static DeviceFamily DevFamily = DeviceFamily.InvalidDevice;
    /* Stores which specific device this app is running on. */
    public static DeviceType DevType = DeviceType.InvalidDevice;
}
