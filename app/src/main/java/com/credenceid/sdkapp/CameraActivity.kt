@file:Suppress("unused")

package com.credenceid.sdkapp

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import com.credenceid.biometrics.Biometrics
import com.credenceid.face.FaceEngine
import com.developer.filepicker.controller.DialogSelectionListener
import com.developer.filepicker.model.DialogConfigs
import com.developer.filepicker.model.DialogProperties
import com.developer.filepicker.view.FilePickerDialog
import kotlinx.android.synthetic.main.act_camera.*
import java.io.File


/**
 * Used for Android Logcat.
 */
private val TAG = "CID"

private var dialog: FilePickerDialog? = null
private var videoUri : Uri? = null
private var sStartTime = System.currentTimeMillis()

class CameraActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_camera)
        this.configureLayoutComponents()



        btn_browse?.isEnabled = true
        btn_browse.setOnClickListener(View.OnClickListener {
            v: View? -> dialog?.show()
        })

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val properties = DialogProperties()
        properties.selection_mode = DialogConfigs.SINGLE_MODE
        properties.selection_type = DialogConfigs.FILE_SELECT
        properties.root = File(DialogConfigs.DEFAULT_DIR)
        properties.error_dir = File(DialogConfigs.DEFAULT_DIR)
        properties.offset = File(DialogConfigs.DEFAULT_DIR)

        dialog = FilePickerDialog(this, properties)

        dialog!!.properties = properties
        dialog!!.setTitle("Select Video")
        dialog!!.setDialogSelectionListener(DialogSelectionListener { files: Array<String?> ->
            if (files.size > 0) {
                val file =  File(files[0])
                status_textview.text = file.absolutePath
                videoUri = Uri.parse(file.absolutePath)
            }
        })
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

            Log.d(TAG, "SampleApp => Starting liveness verification" )

            sStartTime = System.currentTimeMillis()

            App.BioManager!!.verifyFaceLiveness(videoUri, object : FaceEngine.OnVerifyFaceLiveness {
                override fun onVerifyFaceLiveness(resultCode: Biometrics.ResultCode?, status: String?, sdkResult: String? , score: Int, progress: Int, blinkResult: Boolean) {
                    var displayedResult = "Verifcation completed: "+ "\n"
                    when {
                        Biometrics.ResultCode.OK == resultCode -> {
                            displayedResult =  displayedResult + "Status = " + status+ "\n"
                            displayedResult =  displayedResult + "SDK result = " + sdkResult + "\n"
                            displayedResult =  displayedResult + "Liveness score = " + score + "\n"
                            displayedResult =  displayedResult + "Subject Blink detection = " + blinkResult + "\n"
                            displayedResult =  displayedResult + "Performing time  = " + (System.currentTimeMillis() - sStartTime)

                        }
                        Biometrics.ResultCode.INTERMEDIATE == resultCode -> {
                            displayedResult = "Performing verification: "+ "\n" + status + " (" + progress + "%)"
                        }
                        Biometrics.ResultCode.FAIL == resultCode -> {
                            displayedResult =  displayedResult + "Status = " + status+ "\n"
                            displayedResult =  displayedResult + "SDK result = " + sdkResult + "\n"
                            displayedResult =  displayedResult + "Liveness score = " + score + "\n"
                            displayedResult =  displayedResult + "Subject Blink detection = " + blinkResult + "\n"
                        }
                    }
                    status_textview.text = displayedResult
                }
            })
        }


    }


}