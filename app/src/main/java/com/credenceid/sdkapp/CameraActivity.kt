@file:Suppress("unused")

package com.credenceid.sdkapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.credenceid.sdkapp.android.camera.CallCredenceCameraApp
import com.credenceid.sdkapp.util.CredenceCameraAppLivenessResult
import com.credenceid.sdkapp.util.credenceCamera_Constant.FEATURE_FACE_LIVENESS
import com.credenceid.sdkapp.util.credenceCamera_Constant.PROVIDER_NEUROTECH
import kotlinx.android.synthetic.main.act_camera.*

/**
 * Used for Android Logcat.
 */
private val TAG = CameraActivity::class.java.simpleName


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

        liveness_neurotech_btn?.isEnabled = true
        liveness_neurotech_btn?.setOnClickListener {
            val startCredenceCameraApp = registerForActivityResult(CallCredenceCameraApp(
                    FEATURE_FACE_LIVENESS,
                    PROVIDER_NEUROTECH)){ result: CredenceCameraAppLivenessResult? ->
                // Handle the return
                if (result != null) {
                    if(result.sdkResult == 1) {
                        status_textview.text = "Result = " + result.sdkResult + "\nLiveness Score = " + result.livenessScore

                    }else {
                        status_textview.text = "ERROR = " + result.sdkResult
                    }
                } else {
                    Log.e(TAG, "Fail to get Live Subject template.");
                }
            }
            startCredenceCameraApp.launch(0)
        }


        liveness_innovatrics_btn?.isEnabled = true
        liveness_innovatrics_btn?.setOnClickListener {
            val launchIntent = packageManager.getLaunchIntentForPackage("com.credenceid.capp_credencecamera")
            launchIntent.flags = 0
            launchIntent?.let { startActivityForResult(it, 100) }
        }

        barcode_cid_btn?.isEnabled = true
        barcode_cid_btn?.setOnClickListener {
            val launchIntent = packageManager.getLaunchIntentForPackage("com.credenceid.capp_credencecamera")
            launchIntent.flags = 0
            launchIntent?.let { startActivityForResult(it, 100) }
        }

    }


}