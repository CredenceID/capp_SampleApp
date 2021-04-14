@file:Suppress("unused")

package com.credenceid.sdkapp

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.credenceid.biometrics.DeviceFamily
import com.credenceid.sdkapp.android.camera.CallCredenceCameraApp
import com.credenceid.sdkapp.util.CredenceCameraAppLivenessResult
import com.credenceid.sdkapp.util.credenceCamera_Constant.*
import kotlinx.android.synthetic.main.act_camera.*
import java.io.File


/**
 * Used for Android Logfront_liveness_neurotech_btncat.
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

        back_liveness_neurotech_btn?.isEnabled = false
        back_liveness_neurotech_btn?.setOnClickListener {
            startLiveness(PROVIDER_NEUROTECH, CAMERA_BACK);
        }

//        front_liveness_neurotech_btn?.isEnabled = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)



        
        front_liveness_neurotech_btn?.isEnabled = false
        front_liveness_neurotech_btn?.setOnClickListener {
            startLiveness(PROVIDER_NEUROTECH, CAMERA_FRONT);
        }



        back_liveness_luna_btn?.isEnabled = true
        back_liveness_luna_btn?.setOnClickListener {
            startLiveness(PROVIDER_LUNA, CAMERA_BACK);
        }

        front_liveness_luna_btn?.isEnabled = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)


        //front_liveness_neurotech_btn?.isEnabled = true
        front_liveness_luna_btn?.setOnClickListener {
            startLiveness(PROVIDER_LUNA, CAMERA_FRONT);
        }


        back_liveness_innovatrics_btn?.isEnabled = true
        back_liveness_innovatrics_btn?.setOnClickListener {
            startLiveness(PROVIDER_INNOVATRICS, CAMERA_BACK);
        }

        front_liveness_innovatrics_btn?.isEnabled = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)

        front_liveness_innovatrics_btn?.setOnClickListener {
            startLiveness(PROVIDER_INNOVATRICS, CAMERA_FRONT);
        }

        back_liveness_nfv_btn?.isEnabled = true
        back_liveness_nfv_btn?.setOnClickListener {
            startLiveness(PROVIDER_NFV, CAMERA_BACK);
        }

        front_liveness_nfv_btn?.isEnabled = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)

        front_liveness_nfv_btn?.setOnClickListener {

            startLiveness(PROVIDER_NFV, CAMERA_FRONT);
        }

        barcode_cid_btn?.isEnabled = true
        barcode_cid_btn?.setOnClickListener {
            val startCredenceCameraApp = registerForActivityResult(CallCredenceCameraApp(
                    FEATURE_BARCODE,
                    PROVIDER_CID,
                    LIVENESS_MODE_PASSIVE,
                    CAMERA_BACK)){ result: CredenceCameraAppLivenessResult? ->
                // Handle the return
                if (result != null) {
                    if(result.sdkResult == 1) {
                        status_textview.text = "Result = OK " +
                                "\nMessage = " + result.sdkResultMessage

                        displayImage(result.faceImageUri)
                    }else {
                        status_textview.text = "Result = FAILED"
                    }
                } else {
                    Log.e(TAG, "Fail to get Live Subject template.");
                }
            }
            startCredenceCameraApp.launch(0)
        }

      //  mrz_btn?.isEnabled = true
        mrz_btn?.setOnClickListener {
            val startCredenceCameraApp = registerForActivityResult(CallCredenceCameraApp(
                    FEATURE_DOCUMENT_MRZ,
                    PROVIDER_CID,
                    LIVENESS_MODE_PASSIVE,
                    CAMERA_BACK)){ result: CredenceCameraAppLivenessResult? ->
                // Handle the return
                if (result != null) {
                    if(result.sdkResult == 1) {
                        status_textview.text = "Result = OK " +
                                "\nMessage = " + result.sdkResultMessage
                        displayImage(result.faceImageUri)
                    }else {
                        status_textview.text = "Result = FAILED"
                    }
                } else {
                    Log.e(TAG, "Fail to get Live Subject template.");
                }
            }
            startCredenceCameraApp.launch(0)
            Toast.makeText(applicationContext, "Not implemented", Toast.LENGTH_SHORT)
        }

    }

    private fun startLiveness(provider: Int, camera: Int){

        val startCredenceCameraApp = registerForActivityResult(CallCredenceCameraApp(
                FEATURE_FACE_LIVENESS,
                provider,
                LIVENESS_MODE_PASSIVE,
                camera)){ result: CredenceCameraAppLivenessResult? ->
            // Handle the return
            Log.e("result", result.toString());

            if (result != null) {
                if(result.sdkResult == 1) {
                    Log.e("result.faceImageUri", result.faceImageUri.path);

                    status_textview.text = "Result = OK " +
                            "\nLiveness Score = " + result.livenessScore +
                            "\nMessage = " + result.sdkResultMessage

                        displayImage(result.faceImageUri)

                }else {
                    Log.e("result.faceImageUri", "FAILED");
                    faceResultImageView.setImageDrawable(null)

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

    private fun displayImage(imageUri: Uri){
        val faceImageFile = File(imageUri.toString())
        Log.d(TAG, "Image file Exist!!!" + faceImageFile)

//        if (Build.MODEL.equals(DeviceFamily.CredenceTwo.toString())) {
            if(faceImageFile.exists()){
                Log.d(TAG, "Image  Build.MODEL"+faceImageFile.toString())

                var image = BitmapFactory.decodeFile(faceImageFile.toString())
//                if(image.height > 475) {
//                    Log.d(TAG, "Image file loaded: image size = " + image.byteCount)
//                    image = Bitmap.createScaledBitmap(image, (475 * image.width) / image.height, 475, false)
//                }
                faceResultImageView.setImageBitmap(image)
                Log.d(TAG, "Image displayed")
            }
        Log.d(TAG, "Image  Build.MODEL"+faceImageFile.toString())

        /* }else{
             if(faceImageFile.exists()){
                 Log.d(TAG, "Image file Exist!!!")

                 var image = BitmapFactory.decodeFile(faceImageFile.path)
                 if(image.height > 475) {
                     Log.d(TAG, "Image file loaded: image size = " + image.byteCount)
                     image = Bitmap.createScaledBitmap(image, (475 * image.width) / image.height, 475, false)
                 }
                 faceResultImageView.setImageBitmap(image)
                 Log.d(TAG, "Image displayed")
             }
         }*/

                if (Build.MODEL.equals(DeviceFamily.CredenceTwo.toString())) {
                    Log.d(TAG, DeviceFamily.CredenceTwo.toString())

                }


    }


}