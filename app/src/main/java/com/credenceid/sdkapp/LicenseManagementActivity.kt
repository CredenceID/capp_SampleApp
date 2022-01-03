package com.credenceid.sdkapp

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*


class LicenseManagementActivity : AppCompatActivity() {


    val fileName = "Margins_ID_Group_FingerExtractor_Mobile_10013_17994.sn"

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


            val outputPath = (applicationContext.getExternalFilesDir(null)?.absolutePath ?: "") + "/" +  fileName

            val toRead = File(outputPath)

            if(!toRead.exists()){
                createFile(outputPath)
            }

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
                        saveFile(applicationContext.getExternalFilesDir(null).absolutePath
                                + "/" + fileName + ".lic",
                        data)
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

            val registerFilename = fileName + ".lic"

            val fileData = readBytes(applicationContext.getExternalFilesDir(null).absolutePath
                    + "/" +  registerFilename);

            Log.d("CID-TEST", "Register fileName = " + applicationContext.getExternalFilesDir(null).absolutePath
                    + "/" + registerFilename )
            Log.d("CID-TEST", "Register fileData = " + String(fileData!!) )

            App.BioManager!!.manageProviderLicense(ServiceConstants.Provider.NEUROTECHNOLOGY,
                    ServiceConstants.OperationType.REGISTER_LICENSE,
                    registerFilename,
                    fileData){resultCode, apiStatus, data ->

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


            val licFileName =  fileName + ".lic"

            val outputPath = (applicationContext.getExternalFilesDir(null)?.absolutePath ?: "") + "/" +  licFileName

            val toRead = File(outputPath)

            if(!toRead.exists()){
                Log.e("CID-TEST" , "No lic file")
                return@setOnClickListener
            }

            val fileData = readBytes(outputPath);


            App.BioManager!!.manageProviderLicense(ServiceConstants.Provider.NEUROTECHNOLOGY,
                ServiceConstants.OperationType.DEACTIVATE_LICENCE,
                    licFileName,
                    fileData){resultCode, apiStatus, data ->

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
                        statusTextView.text = "GET_LICENSE_STATUS OK"
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

    @Throws(Exception::class)
    fun createFile(file: String) {

        val toWrite = File(file)
        if (!toWrite.exists()) {
            Log.d("CID-TEST", "createFile = " + file );
            if (!toWrite.createNewFile())
                throw Exception("Fail to create file")
        }
        val fOut: FileOutputStream = FileOutputStream(file)
        val data = byteArrayOf(0x41, 0x42, 0x43)
        fOut.write(data)
        fOut.flush()
        fOut.close()

    }

    fun saveFile(file: String, data: ByteArray) {
        val toWrite = File(file)
        if (!toWrite.exists()) {
            if (!toWrite.createNewFile()) throw Exception("Fail to create file")
        }
        val fOut: FileOutputStream = FileOutputStream(toWrite)

        fOut.write(data)
        fOut.flush()
        fOut.close()
    }


}