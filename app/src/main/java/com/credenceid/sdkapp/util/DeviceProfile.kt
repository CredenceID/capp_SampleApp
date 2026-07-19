package com.credenceid.sdkapp.util

import android.util.Log
import com.credenceid.biometrics.Biometrics.FingerprintScannerType
import com.credenceid.biometrics.BiometricsManager
import com.credenceid.biometrics.DeviceFamily
import com.credenceid.biometrics.DeviceType
import com.credenceid.sdkapp.App

/**
 * A snapshot of what the device running this app can do, built once after
 * BiometricsManager.initializeBiometrics() returns OK.
 *
 * The same APK runs on every Credence ID device (C-ONE, Trident, C-TAB, ECO, ...),
 * so the app must adapt at runtime. The rule this sample demonstrates:
 *
 *   1. Prefer CAPABILITY checks (hasCardReader(), hasMRZReader(), ...) to decide
 *      which features to offer. These keep working unchanged when Credence ID
 *      ships a new device model.
 *   2. Reserve DeviceType/DeviceFamily checks for the rare cases where behaviour
 *      genuinely differs between families with the same peripheral (e.g. the
 *      CredenceECO fingerprint sensor supports host-side calibration).
 *   3. Adapt to SCREEN differences with standard Android resource qualifiers
 *      (values-sw480dp, layout-land, ...) rather than device names — see
 *      res/values-sw480dp/dimens.xml.
 */
object DeviceProfile {

    var isPopulated = false
        private set

    var deviceType: DeviceType = DeviceType.InvalidDevice
        private set
    var deviceFamily: DeviceFamily = DeviceFamily.InvalidDevice
        private set
    var productName: String = ""
        private set

    /* Peripheral capabilities as reported by CredenceSDK. */
    var hasFingerprintScanner = false
        private set
    var hasCardReader = false
        private set
    var hasSamCardReader = false
        private set
    var hasMRZReader = false
        private set
    var hasIrisScanner = false
        private set
    var fingerprintScannerType: FingerprintScannerType = FingerprintScannerType.NONE
        private set

    /**
     * Family-specific behaviour flags. Keep every name/family-based decision here,
     * in one place, so activities never need to compare device names themselves.
     */
    val supportsFingerprintCalibration: Boolean
        get() = DeviceFamily.CredenceECO == deviceFamily

    /**
     * Queries all capabilities from an initialized BiometricsManager. Call this
     * exactly once, from the initializeBiometrics() OK callback, before any
     * activity reads this profile.
     */
    fun populate(manager: BiometricsManager) {
        deviceType = manager.deviceType
        deviceFamily = manager.deviceFamily
        productName = manager.productName ?: ""

        hasFingerprintScanner = manager.hasFingerprintScanner()
        hasCardReader = manager.hasCardReader()
        hasSamCardReader = manager.hasSamCardReader()
        hasMRZReader = manager.hasMRZReader()
        hasIrisScanner = manager.hasIrisScanner()
        fingerprintScannerType = manager.fingerprintScannerType

        isPopulated = true

        Log.d(
            App.TAG, "DeviceProfile: type=${deviceType.name} family=${deviceFamily.name}" +
                    " product=\"$productName\" fingerprint=$hasFingerprintScanner" +
                    "(${fingerprintScannerType.name}) card=$hasCardReader sam=$hasSamCardReader" +
                    " mrz=$hasMRZReader iris=$hasIrisScanner" +
                    " fpCalibration=$supportsFingerprintCalibration"
        )
    }
}
