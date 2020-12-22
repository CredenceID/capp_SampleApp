package com.credenceid.sdkapp

import android.Manifest.permission
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.credenceid.biometrics.Biometrics.ResultCode
import com.credenceid.biometrics.Biometrics.ResultCode.*
import com.credenceid.biometrics.BiometricsManager
import kotlinx.android.synthetic.main.act_main.*

/**
 * When requested for permissions you must specify a number which sort of links the permissions
 * request to a "key". This way when you get back a permissions event you can tell from where
 * that permission was requested from.
 */
private const val REQUEST_ALL_PERMISSIONS = 0
/**
 * List of all permissions we will request.
 */
private val PERMISSIONS = arrayOf(
        permission.WRITE_EXTERNAL_STORAGE,
        permission.READ_EXTERNAL_STORAGE,
        permission.CAMERA,
        permission.READ_CONTACTS,
        permission.READ_PHONE_STATE)

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_main)

        this.requestPermissions()
        this.configureLayoutComponents()
        this.initializeBiometrics()
    }

    /**
     * Configure all objects in layout file, set up listeners, views, etc.
     */
    private fun configureLayoutComponents() {

        appVersionTextView.text = packageVersion
        fpBtn.setOnClickListener { startActivity(Intent(this, FingerprintActivity::class.java)) }
        cardBtn.setOnClickListener { startActivity(Intent(this, CardReaderActivity::class.java)) }
        mrzBtn.setOnClickListener { startActivity(Intent(this, MRZActivity::class.java)) }
        faceBtn.setOnClickListener { startActivity(Intent(this, CameraActivity::class.java)) }
        setBiometricButtonsVisibility(View.GONE)

    }


    /**
     * Initializes CredenceSDK biometrics object.
     */
    private fun initializeBiometrics() {

        /*  Create new biometrics object. */
        App.BioManager = BiometricsManager(this)

        /* Initialize object, meaning tell CredenceService to bind to this application. */
        App.BioManager!!.initializeBiometrics { rc: ResultCode,
                                                _: String?,
                                                _: String? ->

            when (rc) {
                OK -> {
                    Toast.makeText(this, getString(R.string.bio_init), Toast.LENGTH_LONG).show()

                    App.DevFamily = App.BioManager!!.deviceFamily
                    App.DevType = App.BioManager!!.deviceType

                    /* Populate text fields which display device/App information. */
                    productNameTextView.text = App.BioManager!!.productName
                    deviceIDTextView.text = App.BioManager!!.deviceID
                    serviceVersionTextView.text = App.BioManager!!.serviceVersion
                    jarVersionTextView.text = App.BioManager!!.sdkJarVersion

                    /* Configure which buttons user is allowed to see to based on current device this
                     * application is running on.
                     */
                    configureButtons()
                }
                INTERMEDIATE -> {
                    /* This ResultCode is never returned for this API. */
                }
                FAIL -> {
                    Toast.makeText(this, getString(R.string.bio_init_fail), Toast.LENGTH_LONG).show()
                    /* If biometrics failed to initialize then all API calls will return FAIL.
                     * Application should not proceed & close itself.
                     */
                    finish()
                }
            }
        }
    }

    /**
     * Configures which biometrics buttons should be visible to user based on device type this
     * application is running on.
     */
    private fun configureButtons() {

        /* By default all Credence device's face a fingerprint sensor and camera. */
        fpBtn.visibility = View.VISIBLE
        faceBtn.visibility = View.VISIBLE

        if (App.BioManager!!.hasCardReader())
            cardBtn.visibility = View.VISIBLE

        if (App.BioManager!!.hasMRZReader())
            mrzBtn.visibility = View.VISIBLE
    }

    /**
     * Sets visibility for all biometrics buttons.
     *
     * @param visibility View.VISIBLE, View.INVISIBLE, View.GONE
     */
    private fun setBiometricButtonsVisibility(@Suppress("SameParameterValue") visibility: Int) {

        fpBtn.visibility = visibility
        cardBtn.visibility = visibility
        faceBtn.visibility = visibility
        mrzBtn.visibility = visibility
    }

    /**
     * Checks if permissions stated in manifest have been granted, if not it then requests them.
     */
    private fun requestPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED
                    || checkSelfPermission(permission.READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED
                    || checkSelfPermission(permission.CAMERA) != PERMISSION_GRANTED
                    || checkSelfPermission(permission.READ_CONTACTS) != PERMISSION_GRANTED
                    ||checkSelfPermission(permission.READ_PHONE_STATE) != PERMISSION_GRANTED) {
                requestPermissions(PERMISSIONS, REQUEST_ALL_PERMISSIONS)
            }
        }
    }

    /**
     * Get this application's version number through manifest file.
     */
    private val packageVersion: String
        get() {
            var version = "Unknown"
            try {
                val pInfo = packageManager.getPackageInfo(packageName, 0)
                version = pInfo.versionName
            } catch (ignore: Exception) {
            }
            return version
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==100){
            //            if(resultCode == 1)
            //            {
            Toast.makeText(this,"Get Result",Toast.LENGTH_SHORT).show();
            //            }
        }
    }
}