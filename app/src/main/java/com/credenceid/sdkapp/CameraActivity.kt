@file:Suppress("unused")

package com.credenceid.sdkapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.credenceid.sdkapp.android.camera.CallCredenceCameraApp
import com.credenceid.sdkapp.util.BitmapUtils
import com.credenceid.sdkapp.util.CredenceCameraAppLivenessResult
import com.credenceid.sdkapp.util.credenceCamera_Constant.*
import kotlinx.android.synthetic.main.act_camera.*
import java.io.File


/**
 * Used for Android Logcat.
 */
private val TAG = "CID"


class CameraActivity : ComponentActivity() {

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

        back_liveness_neurotech_btn?.isEnabled = true
        back_liveness_neurotech_btn?.setOnClickListener {
            startLiveness(PROVIDER_NEUROTECH, CAMERA_BACK);
        }

        back_liveness_innovatrics_btn?.isEnabled = true
        back_liveness_innovatrics_btn?.setOnClickListener {
            startLiveness(PROVIDER_INNOVATRICS, CAMERA_BACK);
        }

        back_liveness_nfv_btn?.isEnabled = true
        back_liveness_nfv_btn?.setOnClickListener {
            startLiveness(PROVIDER_NFV, CAMERA_BACK);
        }

        back_liveness_luna_btn.isEnabled = true
        back_liveness_luna_btn?.setOnClickListener {

            startLiveness(PROVIDER_LUNA, CAMERA_BACK);
        }


    }

    private fun startLiveness(provider: Int, camera: Int){

        val startCredenceCameraApp = registerForActivityResult(CallCredenceCameraApp(
                FEATURE_FACE_LIVENESS,
                provider,
                LIVENESS_MODE_PASSIVE,
                camera)){ result: CredenceCameraAppLivenessResult? ->
            // Handle the return
            if (result != null) {
                if(result.sdkResult == 1) {
                    status_textview.text = "Result = OK " +
                            "\nLiveness Score = " + result.livenessScore +
                            "\nMessage = " + result.sdkResultMessage

                    displayImage(result.faceImageUri)

                }else {
                    status_textview.text = "Result = FAILED" +
                            "\nLiveness Score = " + result.livenessScore +
                            "\nMessage = " + result.sdkResultMessage
                }
            } else {
                Log.e(TAG, "Fail to get Live Subject template.");
            }
        }
        startCredenceCameraApp.launch(0)
    }

    private fun displayImage( imageUri:Uri){
        val faceImageFile = File(imageUri.path)
        if(faceImageFile.exists()){
            Log.d(TAG, "Image file Exist!!!")
            var image = BitmapFactory.decodeFile(faceImageFile.path)
            if(image.height > 475) {
                Log.d(TAG, "Image file loaded: image size = " + image.byteCount)
                image = Bitmap.createScaledBitmap(image, (475*image.width)/image.height, 475, false)
            }
            faceResultImageView.setImageBitmap(image)
            Log.d(TAG, "Image displayed")
        }
    }


}