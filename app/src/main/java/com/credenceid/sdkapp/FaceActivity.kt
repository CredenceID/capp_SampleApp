@file:Suppress("DEPRECATION")

package com.credenceid.sdkapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import com.credenceid.biometrics.Biometrics
import com.credenceid.biometrics.DeviceFamily.CredenceTwo
import com.credenceid.sdkapp.android.camera.Utils
import com.developer.filepicker.controller.DialogSelectionListener
import com.developer.filepicker.model.DialogConfigs
import com.developer.filepicker.model.DialogProperties
import com.developer.filepicker.view.FilePickerDialog
import kotlinx.android.synthetic.main.act_face.*
import java.io.File


@Suppress("DEPRECATION")
class FaceActivity : Activity() {

    private val TAG = "CID_Sample"
    private lateinit var dialog: ProgressDialog
    var dialog1FilePicker: FilePickerDialog? = null
    var dialog2FilePicker: FilePickerDialog? = null
    var properties: DialogProperties? = null
    var faceImage1: Bitmap? = null
    var faceImage2: Bitmap? = null
    var face1Template: ByteArray? = null
    var face2Template: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_face)

        this.configureLayoutComponents()

        /* If bytes were given to this activity, perform face analysis. */
        val imageBytes = intent.getByteArrayExtra(getString(R.string.camera_image))
        if (null != imageBytes) {
            var image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (CredenceTwo == App.DevFamily)
                image = Utils.rotateBitmap(image, 90f)
            this.updateFace1(image)
        } else {
            this.onBackPressed()

            Handler().postDelayed({
                Toast.makeText(this, "Failed to detect face.", LENGTH_LONG).show()
            }, 1000)
        }

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val properties = DialogProperties()
        properties.selection_mode = DialogConfigs.SINGLE_MODE
        properties.selection_type = DialogConfigs.FILE_SELECT
        properties.root = File(DialogConfigs.DEFAULT_DIR)
        properties.error_dir = File(DialogConfigs.DEFAULT_DIR)
        properties.offset = File(DialogConfigs.DEFAULT_DIR)

        dialog1FilePicker = FilePickerDialog(this, properties)
        dialog1FilePicker!!.properties = properties
        dialog1FilePicker!!.setTitle("Select Photo")
        dialog1FilePicker!!.setDialogSelectionListener(DialogSelectionListener { files: Array<String?> ->
            if (files.size > 0) {
                val file =  File(files[0])
                faceImage1 = BitmapFactory.decodeFile(file.absolutePath)
                faceImgView1.setImageBitmap(faceImage1)
            } else {
                statusTextView.text = "No File found"
            }
        })

        dialog2FilePicker = FilePickerDialog(this, properties)
        dialog2FilePicker!!.properties = properties
        dialog2FilePicker!!.setTitle("Select Photo")
        dialog2FilePicker!!.setDialogSelectionListener(DialogSelectionListener { files: Array<String?> ->
            if (files.size > 0) {
                val file =  File(files[0])
                faceImage2 = BitmapFactory.decodeFile(file.absolutePath)
                faceImgView2.setImageBitmap(faceImage2)
            } else {
                statusTextView.text = "No File found"
            }
        })

    }

    override fun onBackPressed() {

        super.onBackPressed()
        this.finish()
    }

    private fun configureLayoutComponents() {

        finishBtn.setOnClickListener { this.finish() }

        selectImg1Btn.setOnClickListener {
            v: View? -> dialog1FilePicker?.show()

        }

        selectImg2Btn.setOnClickListener {
            v: View? -> dialog2FilePicker?.show()

        }

        createTemplateImg1Btn.setOnClickListener {
            App.BioManager!!.createFaceTemplate(faceImage1 ) {rc, template ->
                when (rc) {
                    Biometrics.ResultCode.OK -> {
                        Log.d(TAG, "createFaceTemplateAsync(Bitmap1): Template created.")
                        face1Template = template
                        statusTextView.text = "Template Image1 created"
                    }
                    Biometrics.ResultCode.INTERMEDIATE -> {
                        /* This code is never returned for this API. */
                    }
                    Biometrics.ResultCode.FAIL -> {
                        statusTextView.text = "Template creation Failed"
                    }
                }
            }
        }

        createTemplateImg2Btn.setOnClickListener {
            App.BioManager!!.createFaceTemplate(faceImage2 ) {rc, template ->
                when (rc) {
                    Biometrics.ResultCode.OK -> {
                        Log.d(TAG, "createFaceTemplateAsync(Bitmap2): Template created.")
                        face2Template = template
                        statusTextView.text = "Template Image2 created"
                    }
                    Biometrics.ResultCode.INTERMEDIATE -> {
                        /* This code is never returned for this API. */
                    }
                    Biometrics.ResultCode.FAIL -> {
                        statusTextView.text = "Template creation Failed"
                    }
                }
            }
        }

        matchBtn.setOnClickListener {
            if ((null == face1Template)||(face1Template!!.isEmpty())) {
                statusTextView.text = "Face template 1 is null"
            } else if ((null == face2Template)||(face2Template!!.isEmpty())) {
                statusTextView.text = "Face template 2 is null"
            } else {
                App.BioManager!!.compareFace(face1Template, face2Template) { rc, i ->
                    when (rc) {
                        Biometrics.ResultCode.OK -> {
                            statusTextView.text = "Matching Result OK\n Matcing score =" + i
                            Log.d(TAG, "matchFacesAsync(byte[], byte[]): Score = $i")
                        }
                        Biometrics.ResultCode.INTERMEDIATE -> {
                            /* This code is never returned for this API. */
                        }
                        Biometrics.ResultCode.FAIL -> {
                            statusTextView.text = "Matching Task Failed"
                            Log.d(TAG, "matchFacesAsync(byte[], byte[]): Failed to compare templates.")
                        }
                    }
                }
            }
        }

    }

    @SuppressLint("SetTextI18n")
    fun updateFace1(bitmap: Bitmap?) {

        /* If invalid Bitmap, display Toast and exit out.*/
        if (null == bitmap) {
            Toast.makeText(this, getString(R.string.no_image_found_to_process), LENGTH_LONG).show()
            return
        }

        /* Create new scaled image to run analysis on. */
        faceImage1 = bitmap
        faceImgView1.setImageBitmap(bitmap)

    }

    private fun showProgressDialog() {

        if (!dialog.isShowing)
            dialog.show()
    }

    private fun dismissProgressDialog() {

        if (dialog.isShowing)
            dialog.dismiss()
    }

}