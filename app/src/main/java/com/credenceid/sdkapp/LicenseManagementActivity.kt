package com.credenceid.sdkapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
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
import com.credenceid.biometrics.LicensedApiStatus

import com.credenceid.biometrics.Biometrics

import android.os.Environment
import com.credenceid.biometrics.Biometrics.OnLicenseOperationListener
import com.credenceid.biometrics.LicenseOperationResult
import com.credenceid.constants.ServiceConstants.OperationType
import com.util.FileToShare
import com.util.FileUtils


class LicenseManagementActivity : AppCompatActivity() {


    val fileName = "Margins_ID_Group_FingerExtractor_Mobile_10013_17994.sn"
    val fileName2 = "Margins_ID_Group_FingerMatcher_Mobile_10013_19720.sn"

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

            pushLicenceToSDK(applicationContext.getExternalFilesDir(null).absolutePath)
        }

        registerLicenseBtn.setOnClickListener {

            repushLicenceToSDK(applicationContext.getExternalFilesDir(null).absolutePath)
        }

        deactivateLicenseBtn.setOnClickListener {
            deleteLicenceFromSDK(applicationContext.getExternalFilesDir(null).absolutePath)
        }

        getLicenseStatusBtn.setOnClickListener {
            App.BioManager!!.manageProviderLicense(ServiceConstants.Provider.NEUROTECHNOLOGY,
                    ServiceConstants.OperationType.GET_LICENSE_STATUS,
                    null,
                    null){resultCode, resultDetails, apiStatus, data ->

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

    fun pushLicenceToSDK(folderPath: String?) {
        Log.d("CID-TEST", "folderPath = $folderPath")

        var folder = File(folderPath)

        val fileList: Array<String> =  folder.list()
        if (fileList != null) {
            val fileToShareList = arrayListOf<FileToShare>()
            for (f in fileList) {
                fileToShareList.add(FileToShare(f, readBytes(folderPath+"/"+f)))
            }

            App.BioManager!!.manageProviderMultipleLicense(
                    ServiceConstants.Provider.NEUROTECHNOLOGY,
                    ServiceConstants.OperationType.GENERATE_LICENSE_REQUEST,
                    fileToShareList) { resultCode, operationResults ->

                Log.d("CID-TEST", "manageProviderMultipleLicense result = " + resultCode.name)

                when (resultCode) {
                    OK -> {
                        statusTextView.text = "manageProviderMultipleLicense OK"
                        for (res: LicenseOperationResult in operationResults) {
                            Log.d("CID-TEST", "manageProviderMultipleLicense result - result Code = " + res.getmResultCode())
                            Log.d("CID-TEST", "manageProviderMultipleLicense result - result Details = " + res.getmDetailedResult())
                            Log.d("CID-TEST", "manageProviderMultipleLicense result - result File name = " + res.getmFileName())
                            Log.d("CID-TEST", "manageProviderMultipleLicense result - result Data = " + res.getmResultData())
                        }

                    }
                    /* This code is returned while sensor is in the middle of opening. */
                    API_UNAVAILABLE -> {
                        statusTextView.text = "manageProviderMultipleLicense - API_UNAVAILABLE"
                    }
                    INVALID_INPUT_PARAMETERS -> {
                        statusTextView.text = "manageProviderMultipleLicense - INVALID_INPUT_PARAMETERS"
                    }
                    /* This code is returned if sensor fails to open. */
                    FAIL -> {
                        statusTextView.text = "manageProviderMultipleLicense generation failed"


                    }
                }
            }
        }

    }

    fun repushLicenceToSDK(folderPath: String?) {
        Log.d("CID-TEST", "folderPath = $folderPath")

        var folder = File(folderPath)

        val fileList: Array<String> =  folder.list()
        if (fileList != null) {
            val fileToShareList = arrayListOf<FileToShare>()
            for (f in fileList) {
                fileToShareList.add(FileToShare(f, readBytes(folderPath+"/"+f)))
            }

            App.BioManager!!.manageProviderMultipleLicense(
                    ServiceConstants.Provider.NEUROTECHNOLOGY,
                    ServiceConstants.OperationType.REGISTER_LICENSE,
                    fileToShareList) { resultCode, operationResults ->

                Log.d("CID-TEST", "manageProviderMultipleLicense result = " + resultCode.name)

                when (resultCode) {
                    OK -> {
                        statusTextView.text = "manageProviderMultipleLicense OK"
                        for (res: LicenseOperationResult in operationResults) {
                            Log.d("CID-TEST", "manageProviderMultipleLicense result - result Code = " + res.getmResultCode())
                            Log.d("CID-TEST", "manageProviderMultipleLicense result - result Details = " + res.getmDetailedResult())
                            Log.d("CID-TEST", "manageProviderMultipleLicense result - result File name = " + res.getmFileName())
                            Log.d("CID-TEST", "manageProviderMultipleLicense result - result Data = " + res.getmResultData())
                        }

                    }
                    /* This code is returned while sensor is in the middle of opening. */
                    API_UNAVAILABLE -> {
                        statusTextView.text = "manageProviderMultipleLicense - API_UNAVAILABLE"
                    }
                    INVALID_INPUT_PARAMETERS -> {
                        statusTextView.text = "manageProviderMultipleLicense - INVALID_INPUT_PARAMETERS"
                    }
                    /* This code is returned if sensor fails to open. */
                    FAIL -> {
                        statusTextView.text = "manageProviderMultipleLicense generation failed"


                    }
                }
            }
        }

    }

    fun deleteLicenceFromSDK(folderPath: String?) {
        Log.d("CID-TEST", "folderPath = $folderPath")

        var folder = File(folderPath)

        val fileList: Array<String> =  folder.list()
        if (fileList != null) {
            val fileToShareList = arrayListOf<FileToShare>()
            for (f in fileList) {
                fileToShareList.add(FileToShare(f, readBytes(folderPath+"/"+f)))
            }

            App.BioManager!!.manageProviderMultipleLicense(
                    ServiceConstants.Provider.NEUROTECHNOLOGY,
                    ServiceConstants.OperationType.DEACTIVATE_LICENCE,
                    fileToShareList) { resultCode, operationResults ->

                Log.d("CID-TEST", "manageProviderMultipleLicense result = " + resultCode.name)

                when (resultCode) {
                    OK -> {
                        statusTextView.text = "manageProviderMultipleLicense OK"
                        for (res: LicenseOperationResult in operationResults) {
                            Log.d("CID-TEST", "manageProviderMultipleLicense result - result Code = " + res.getmResultCode())
                            Log.d("CID-TEST", "manageProviderMultipleLicense result - result Details = " + res.getmDetailedResult())
                            Log.d("CID-TEST", "manageProviderMultipleLicense result - result File name = " + res.getmFileName())
                            Log.d("CID-TEST", "manageProviderMultipleLicense result - result Data = " + res.getmResultData())
                        }

                    }
                    /* This code is returned while sensor is in the middle of opening. */
                    API_UNAVAILABLE -> {
                        statusTextView.text = "manageProviderMultipleLicense - API_UNAVAILABLE"
                    }
                    INVALID_INPUT_PARAMETERS -> {
                        statusTextView.text = "manageProviderMultipleLicense - INVALID_INPUT_PARAMETERS"
                    }
                    /* This code is returned if sensor fails to open. */
                    FAIL -> {
                        statusTextView.text = "manageProviderMultipleLicense generation failed"


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

