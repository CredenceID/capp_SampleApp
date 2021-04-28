@file:Suppress("unused")

package com.credenceid.sdkapp

import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import com.credenceid.sdkapp.android.camera.CallCredenceCameraApp
import com.credenceid.sdkapp.util.CredenceCameraAppLivenessResult
import com.credenceid.sdkapp.util.credenceCamera_Constant.*
import kotlinx.android.synthetic.main.act_camera.*
import java.io.FileInputStream
import java.io.InputStream


/**
 * Used for Android Logfront_liveness_neurotech_btncat.
 */
private val TAG = "CID"


class CameraActivity : ComponentActivity() {

    val startCCameraAppBarcodeCidBack = registerForActivityResult(CallCredenceCameraApp(
            FEATURE_BARCODE,
            PROVIDER_CID,
            CAMERA_BACK)){ result: CredenceCameraAppLivenessResult? ->
        // Handle the return
        result?.let { handleCreenceCameraResult(it) }
    }

    val startCCameraAppLivenessNfvBack = registerForActivityResult(CallCredenceCameraApp(
            FEATURE_FACE_LIVENESS,
            PROVIDER_NFV,
            CAMERA_BACK)){ result: CredenceCameraAppLivenessResult? ->
        // Handle the return
        result?.let { handleCreenceCameraResult(it) }
    }

    val startCCameraAppLivenessNfvFront = registerForActivityResult(CallCredenceCameraApp(
            FEATURE_FACE_LIVENESS,
            PROVIDER_NFV,
            CAMERA_FRONT)){ result: CredenceCameraAppLivenessResult? ->
        // Handle the return
        result?.let { handleCreenceCameraResult(it) }
    }

    val startCCameraAppLivenessInnoBack = registerForActivityResult(CallCredenceCameraApp(
            FEATURE_FACE_LIVENESS,
            PROVIDER_INNOVATRICS,
            CAMERA_BACK)){ result: CredenceCameraAppLivenessResult? ->
        // Handle the return
        result?.let { handleCreenceCameraResult(it) }
    }

    val startCCameraAppLivenessInnoFront = registerForActivityResult(CallCredenceCameraApp(
            FEATURE_FACE_LIVENESS,
            PROVIDER_INNOVATRICS,
            CAMERA_FRONT)){ result: CredenceCameraAppLivenessResult? ->
        // Handle the return
        result?.let { handleCreenceCameraResult(it) }
    }

    val startCCameraAppLivenessLunaBack = registerForActivityResult(CallCredenceCameraApp(
            FEATURE_FACE_LIVENESS,
            PROVIDER_LUNA,
            CAMERA_BACK)){ result: CredenceCameraAppLivenessResult? ->
        // Handle the return
        result?.let { handleCreenceCameraResult(it) }
    }

    val startCCameraAppLivenessLunaFront = registerForActivityResult(CallCredenceCameraApp(
            FEATURE_FACE_LIVENESS,
            PROVIDER_LUNA,
            CAMERA_FRONT)){ result: CredenceCameraAppLivenessResult? ->
        // Handle the return
        result?.let { handleCreenceCameraResult(it) }
    }

    val startCCameraAppLivenessTech5Back = registerForActivityResult(CallCredenceCameraApp(
            FEATURE_FACE_LIVENESS,
            PROVIDER_TECH5,
            CAMERA_BACK)){ result: CredenceCameraAppLivenessResult? ->
        // Handle the return
        result?.let { handleCreenceCameraResult(it) }
    }

    val startCCameraAppLivenessTech5Front = registerForActivityResult(CallCredenceCameraApp(
            FEATURE_FACE_LIVENESS,
            PROVIDER_TECH5,
            CAMERA_FRONT)){ result: CredenceCameraAppLivenessResult? ->
        // Handle the return
        result?.let { handleCreenceCameraResult(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_camera)
        this.configureLayoutComponents()
    }

    override fun onDestroy() {

        super.onDestroy()

    }

    /**
     * Configure all objects in layout file, set up listeners, views, etc.
     */
    private fun configureLayoutComponents() {


        back_liveness_luna_btn?.isEnabled = true
        back_liveness_luna_btn?.setOnClickListener {
            startCCameraAppLivenessLunaBack.launch(0)
        }

        front_liveness_luna_btn?.isEnabled = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
        front_liveness_luna_btn?.setOnClickListener {
            startCCameraAppLivenessLunaFront.launch(0)
        }


        back_liveness_innovatrics_btn?.isEnabled = true
        back_liveness_innovatrics_btn?.setOnClickListener {
            startCCameraAppLivenessInnoBack.launch(0)
        }

        front_liveness_innovatrics_btn?.isEnabled = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
        front_liveness_innovatrics_btn?.setOnClickListener {
            startCCameraAppLivenessInnoFront.launch(0)
        }

        back_liveness_nfv_btn?.isEnabled = true
        back_liveness_nfv_btn?.setOnClickListener {
            startCCameraAppLivenessNfvBack.launch(0)
        }

        front_liveness_nfv_btn?.isEnabled = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
        front_liveness_nfv_btn?.setOnClickListener {
            startCCameraAppLivenessNfvFront.launch(0)
        }

        back_liveness_tech5_btn?.isEnabled = true
        back_liveness_tech5_btn?.setOnClickListener {
            startCCameraAppLivenessTech5Back.launch(0)
        }

        front_liveness_tech5_btn?.isEnabled = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
        front_liveness_tech5_btn?.setOnClickListener {
            startCCameraAppLivenessTech5Front.launch(0)
        }

        barcode_btn?.isEnabled = true
        barcode_btn?.setOnClickListener {
            startCCameraAppBarcodeCidBack.launch(0)
        }

    }

    private fun handleCreenceCameraResult(result: CredenceCameraAppLivenessResult){
        if (result != null) {
            if(result.sdkResult == 1) {
                status_textview.text = "Result = OK " +
                        "\nMessage = " + result.sdkResultMessage

                displayImage(result.resultImage)
            }else {
                status_textview.text = "Result = FAILED"
            }
        } else {
            Log.e(TAG, "Fail to get Live Subject template.");
        }

    }

    private fun displayImage(imageUri: Uri){
        try {
            val inputPFD = applicationContext.contentResolver.openFileDescriptor(imageUri, "r")
            val fD = inputPFD.getFileDescriptor()
            Log.d(TAG, "fD = " + fD.sync().toString());
            val fileStream: InputStream = FileInputStream(fD)
            val bitmap = BitmapFactory.decodeStream(fileStream)
            faceResultImageView.setImageBitmap(bitmap)
            Log.d(TAG, "Image displayed")
        }catch (e: Exception){
            Log.e(TAG, "Error: " + e.toString());
        }
    }


}