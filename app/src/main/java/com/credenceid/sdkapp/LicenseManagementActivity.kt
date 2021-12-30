package com.credenceid.sdkapp

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.credenceid.biometrics.Biometrics.ResultCode
import com.credenceid.biometrics.Biometrics.ResultCode.*
import com.credenceid.biometrics.BiometricsManager
import com.credenceid.constants.ServiceConstants
import com.util.HexUtils
import kotlinx.android.synthetic.main.act_device_info.*
import kotlinx.android.synthetic.main.act_device_info.statusTextView
import kotlinx.android.synthetic.main.act_licence_management.*
import kotlinx.android.synthetic.main.act_main.*
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.lang.Exception
import java.util.*


class LicenseManagementActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_licence_management)

        this.configureLayoutComponents()
    }

    /**
     * Configure all objects in layout file, set up listeners, views, etc.
     */
    private fun configureLayoutComponents() {

        generateLicenseBtn.setOnClickListener {

            val fileName = "Margins_ID_Group_FingerExtractor_Mobile_10016_27820.sn"

            val fileData = readBytes(applicationContext.getExternalFilesDir(null).absolutePath
                    + "/" +  fileName);

            Log.d("CID-TEST", "fileName = " + applicationContext.getExternalFilesDir(null).absolutePath
                    + "/" + fileName )
            Log.d("CID-TEST", "fileData = " + String(fileData!!) )

            App.BioManager!!.manageProviderLicense(ServiceConstants.Provider.NEUROTECHNOLOGY,
            ServiceConstants.OperationType.GENERATE_LICENSE_REQUEST,
                    fileName,
                    fileData){resultCode, apiStatus, data ->

                when (resultCode) {
                    OK -> {
                        statusTextView.text = "GENERATE_LICENSE_REQUEST generation OK"
                        Log.d("CID-TEST", "data = " + String(data) )
                    }
                    /* This code is returned while sensor is in the middle of opening. */
                    API_UNAVAILABLE -> {
                        statusTextView.text = "GENERATE_LICENSE_REQUEST - API_UNAVAILABLE"
                    }
                    INVALID_INPUT_PARAMETERS -> {
                        statusTextView.text = "GENERATE_LICENSE_REQUEST - INVALID_INPUT_PARAMETERS"
                    }
                    /* This code is returned if sensor fails to open. */
                    FAIL -> {
                        statusTextView.text = "GENERATE_LICENSE_REQUEST generation failed"
                    }
                }
            }
        }

        registerLicenseBtn.setOnClickListener {
            App.BioManager!!.manageProviderLicense(ServiceConstants.Provider.NEUROTECHNOLOGY,
                    ServiceConstants.OperationType.REGISTER_LICENSE,
                    null,
                    null){resultCode, apiStatus, data ->

                when (resultCode) {
                    OK -> {
                        statusTextView.text = "REGISTER_LICENSE generation OK"
                    }
                    /* This code is returned while sensor is in the middle of opening. */
                    API_UNAVAILABLE -> {
                        statusTextView.text = "REGISTER_LICENSE - API_UNAVAILABLE"
                    }
                    INVALID_INPUT_PARAMETERS -> {
                        statusTextView.text = "REGISTER_LICENSE - INVALID_INPUT_PARAMETERS"
                    }
                    /* This code is returned if sensor fails to open. */
                    FAIL -> {
                        statusTextView.text = "REGISTER_LICENSE generation failed"
                    }
                }
            }
        }

        deactivateLicenseBtn.setOnClickListener {
            App.BioManager!!.manageProviderLicense(ServiceConstants.Provider.NEUROTECHNOLOGY,
                ServiceConstants.OperationType.DEACTIVATE_LICENCE,
                    null,
                    null){resultCode, apiStatus, data ->

                when (resultCode) {
                    OK -> {
                        statusTextView.text = "DEACTIVATE_LICENCE generation OK"
                    }
                    /* This code is returned while sensor is in the middle of opening. */
                    API_UNAVAILABLE -> {
                        statusTextView.text = "DEACTIVATE_LICENCE - API_UNAVAILABLE"
                    }
                    INVALID_INPUT_PARAMETERS -> {
                        statusTextView.text = "DEACTIVATE_LICENCE - INVALID_INPUT_PARAMETERS"
                    }
                    /* This code is returned if sensor fails to open. */
                    FAIL -> {
                        statusTextView.text = "DEACTIVATE_LICENCE generation failed"
                    }
                }
            }
        }

        getLicenseStatusBtn.setOnClickListener {
            App.BioManager!!.manageProviderLicense(ServiceConstants.Provider.NEUROTECHNOLOGY,
                    ServiceConstants.OperationType.GET_LICENSE_STATUS,
                    null,
                    null){resultCode, apiStatus, data ->

                Log.d("CID-TEST","result = " + resultCode.name )

                when (resultCode) {
                    OK -> {
                        statusTextView.text = "GET_LICENSE_STATUS generation OK"
                        Log.d("CID-TEST","Fingerprint provider = " + apiStatus.fingerprintAPIProvider.name )
                        Log.d("CID-TEST","Fingerprint status = " + apiStatus.fingerprintAPIProviderLicenseStatus.name)
                        Log.d("CID-TEST","Face provider = " + apiStatus.faceAPIProviderType.name )
                        Log.d("CID-TEST","Face status = " + apiStatus.faceAPIProviderLicenseStatus.name)
                    }
                    /* This code is returned while sensor is in the middle of opening. */
                    API_UNAVAILABLE -> {
                        statusTextView.text = "GET_LICENSE_STATUS - API_UNAVAILABLE"
                    }
                    INVALID_INPUT_PARAMETERS -> {
                        statusTextView.text = "GET_LICENSE_STATUS - INVALID_INPUT_PARAMETERS"
                    }
                    /* This code is returned if sensor fails to open. */
                    FAIL -> {
                        statusTextView.text = "GET_LICENSE_STATUS generation failed"
                    }
                }
            }
        }

    }

    fun readBytes(absFilePath: String?): ByteArray? {
        try {
            FileInputStream(absFilePath).use { fis ->
                val bos = ByteArrayOutputStream(0x20000)
                val buf = ByteArray(1024)
                var readNum: Int
                while (fis.read(buf).also { readNum = it } != -1) {
                    bos.write(buf, 0, readNum)
                }
                return bos.toByteArray()
            }
        } catch (e: Exception) {
            Log.w(App.TAG, "readBytes(String): Unable to read byes from file.")
            return byteArrayOf()
        }
    }


}