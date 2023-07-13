@file:Suppress("DEPRECATION")

package com.credenceid.sdkapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import com.credenceid.biometrics.Biometrics
import com.credenceid.biometrics.Biometrics.ResultCode.*
import com.credenceid.biometrics.DeviceFamily.CredenceTwo
import com.credenceid.face.FaceEngine
import com.credenceid.sdkapp.android.camera.Utils
import kotlinx.android.synthetic.main.act_face.*
import java.util.*

@Suppress("DEPRECATION")
class FaceActivity : Activity() {

    private lateinit var dialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_face)

        dialog = ProgressDialog(this)
        dialog.setMessage(getString(R.string.processing))
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        dialog.isIndeterminate = true
        dialog.setCancelable(false)

        this.configureLayoutComponents()

        /* If bytes were given to this activity, perform face analysis. */
        val imageBytes = intent.getByteArrayExtra(getString(R.string.camera_image))
        if (null != imageBytes) {
            var image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (CredenceTwo == App.DevFamily)
                image = Utils.rotateBitmap(image, 90f)

            this.detectFace(image)
        } else {
            this.onBackPressed()

            Handler().postDelayed({
                Toast.makeText(this, "Failed to detect face.", LENGTH_LONG).show()
            }, 1000)
        }
    }

    override fun onBackPressed() {

        super.onBackPressed()
        this.finish()
    }

    private fun configureLayoutComponents() {

        finishBtn.setOnClickListener { this.finish() }
    }

    @SuppressLint("SetTextI18n")
    fun detectFace(bitmap: Bitmap?) {

        /* If invalid Bitmap, display Toast and exit out.*/
        if (null == bitmap) {
            Toast.makeText(this, getString(R.string.no_image_found_to_process), LENGTH_LONG).show()
            return
        }

        /* Display dialog so user knows an operation is in progress. */
        this.showProgressDialog()

        /* Create new scaled image to run analysis on. */
        faceImageView.setImageBitmap(bitmap)

        App.BioManager!!.analyzeFace(bitmap) { rc: Biometrics.ResultCode,
                                               _: RectF,
                                               _: ArrayList<PointF>,
                                               _: ArrayList<PointF>,
                                               _: FloatArray,
                                               poseDir: Array<FaceEngine.HeadPoseDirection>,
                                               gender: FaceEngine.Gender,
                                               age: Int,
                                               emotion: FaceEngine.Emotion,
                                               glasses: Boolean,
                                               imageQuality: Int ->

            /* If we got back data, populate CropView and other widgets with face data. */
            when (rc) {
                OK -> {
                    genderTextView.text = getString(R.string.gender_colon_arg) + gender.name
                    ageTextView.text = getString(R.string.age_colon_arg) + age
                    glassesTextView.text = getString(R.string.glasses_colon_arg) + glasses
                    emotionTextView.text = getString(R.string.emotiona_colon_arg) + emotion.name
                    qualityTextView.text = getString(R.string.imagequal_colon_arg) + imageQuality + "%"

                    var text = getString(R.string.headposedir_colon_arg)

                    text += if (poseDir[1] == FaceEngine.HeadPoseDirection.STRAIGHT) {
                        if (poseDir[2] == FaceEngine.HeadPoseDirection.STRAIGHT)
                            "STRAIGHT"
                        else
                            poseDir[2].name

                    } else if (poseDir[2] == FaceEngine.HeadPoseDirection.STRAIGHT)
                        poseDir[1].name
                    else
                        poseDir[1].name + "\n  &\n" + poseDir[2].name

                    poseDirTextView.text = text

                }
                INTERMEDIATE -> {
                    /* This code is never returned for this API. */
                }
                FAIL -> {
                    this.onBackPressed()
                    Handler().postDelayed({
                        Toast.makeText(this, "Failed to detect face.", LENGTH_LONG).show()
                    }, 1000)
                }
                else -> {
                }
            }

            this.dismissProgressDialog()
        }
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