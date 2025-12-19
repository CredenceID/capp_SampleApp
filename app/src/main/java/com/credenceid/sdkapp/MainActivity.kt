package com.credenceid.sdkapp

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.credenceid.biometrics.Biometrics.ResultCode
import com.credenceid.biometrics.Biometrics.ResultCode.FAIL
import com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE
import com.credenceid.biometrics.Biometrics.ResultCode.OK
import com.credenceid.biometrics.BiometricsManager
import com.credenceid.sdkapp.databinding.ActMainBinding

/**
 * When requested for permissions you must specify a number which sort of links the permissions
 * request to a "key". This way when you get back a permissions event you can tell from where
 * that permission was requested from.
 */
private const val REQUEST_ALL_PERMISSIONS = 0
private var sdkInitializationTime: Long = 0
private var sdkStartingTime: Long = 0

/**
 * List of all permissions we will request.
 */
private val PERMISSIONS = arrayOf(
    permission.WRITE_EXTERNAL_STORAGE,
    permission.READ_EXTERNAL_STORAGE,
    permission.CAMERA,
    permission.READ_CONTACTS,
    permission.READ_PHONE_STATE
)

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        this.requestPermissions()
        this.configureLayoutComponents()
        this.initializeBiometrics()
    }

    /**
     * Configure all objects in layout file, set up listeners, views, etc.
     */
    private fun configureLayoutComponents() {
        binding.appVersionTextView.text = packageVersion
        binding.fpBtn.setOnClickListener { startActivity(Intent(this, FingerprintActivity::class.java)) }
        binding.cardBtn.setOnClickListener { startActivity(Intent(this, CardReaderActivity::class.java)) }
        binding.samCardBtn.setOnClickListener { startActivity(Intent(this, SamCardReaderActivity::class.java)) }
        binding.mrzBtn.setOnClickListener { startActivity(Intent(this, MRZActivity::class.java)) }
        binding.faceBtn.setOnClickListener { startActivity(Intent(this, CameraActivity::class.java)) }
        binding.deviceInfoBtn.setOnClickListener { startActivity(Intent(this, DeviceInfoActivity::class.java)) }
        setBiometricButtonsVisibility(View.GONE)
    }

    /**
     * Initializes CredenceSDK biometrics object.
     */
    private fun initializeBiometrics() {
        /*  Create new biometrics object. */
        App.BioManager = BiometricsManager(this)

        /* Initialize object, meaning tell CredenceService to bind to this application. */
        sdkStartingTime = SystemClock.elapsedRealtime()
        App.BioManager!!.initializeBiometrics { rc: ResultCode,
            _: String?,
            _: String? ->

            Log.d("CID-TEST", "Init time = " + (SystemClock.elapsedRealtime() - sdkStartingTime))
            when (rc) {
                OK -> {
                    Toast.makeText(this, getString(R.string.bio_init), Toast.LENGTH_LONG).show()

                    App.DevFamily = App.BioManager!!.deviceFamily
                    App.DevType = App.BioManager!!.deviceType

                    /* Populate text fields which display device/App information. */
                    binding.productNameTextView.text = App.BioManager!!.productName
                    binding.deviceIDTextView.text = App.BioManager!!.deviceType.name
                    binding.serviceVersionTextView.text = App.BioManager!!.serviceVersion
                    binding.jarVersionTextView.text = App.BioManager!!.sdkJarVersion

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
                else -> {
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
        binding.fpBtn.visibility = View.VISIBLE
        binding.faceBtn.visibility = View.VISIBLE

        if (App.BioManager!!.hasCardReader()) {
            binding.cardBtn.visibility = View.VISIBLE
        }

        if (App.BioManager!!.hasSamCardReader()) {
            binding.samCardBtn.visibility = View.VISIBLE
        }

        if (App.BioManager!!.hasMRZReader()) {
            binding.mrzBtn.visibility = View.VISIBLE
        }
    }

    /**
     * Sets visibility for all biometrics buttons.
     *
     * @param visibility View.VISIBLE, View.INVISIBLE, View.GONE
     */
    private fun setBiometricButtonsVisibility(@Suppress("SameParameterValue") visibility: Int) {
        binding.fpBtn.visibility = visibility
        binding.cardBtn.visibility = visibility
        binding.faceBtn.visibility = visibility
        binding.mrzBtn.visibility = visibility
        binding.samCardBtn.visibility = visibility
        binding.deviceInfoBtn.visibility = visibility
    }

    /**
     * Checks if permissions stated in manifest have been granted, if not it then requests them.
     */
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED ||
                checkSelfPermission(permission.READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED ||
                checkSelfPermission(permission.CAMERA) != PERMISSION_GRANTED ||
                checkSelfPermission(permission.READ_CONTACTS) != PERMISSION_GRANTED ||
                checkSelfPermission(permission.READ_PHONE_STATE) != PERMISSION_GRANTED
            ) {
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
}
