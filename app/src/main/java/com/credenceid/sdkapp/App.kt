package com.credenceid.sdkapp

import android.app.Application
import com.credenceid.biometrics.BiometricsManager
import com.credenceid.biometrics.DeviceFamily
import com.credenceid.biometrics.DeviceType


class App : Application() {
    companion object {
        const val TAG = "Credence ID Sample App"
        /**
         * CredenceSDK biometrics object used to interface with APIs.
         */
        var BioManager: BiometricsManager? = null
        /**
         * Stores which Credence family of device's this app is running on.
         */
        var DevFamily = DeviceFamily.InvalidDevice
        /**
         * Stores which specific device this app is running on.
         */
        var DevType = DeviceType.InvalidDevice
    }
}