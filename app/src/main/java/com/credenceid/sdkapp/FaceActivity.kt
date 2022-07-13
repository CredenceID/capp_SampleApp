@file:Suppress("DEPRECATION")

package com.credenceid.sdkapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import com.credenceid.biometrics.Biometrics.ResultCode.*
import kotlinx.android.synthetic.main.act_cid_face.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

@Suppress("DEPRECATION")
class FaceActivity : Activity() {

    val TAG = "CID-TEST"
    val syncAPITimeoutMS = 3000

    lateinit var templatet1:ByteArray
    lateinit var templatet2:ByteArray
    lateinit var templatet3:ByteArray


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_cid_face)
        this.configureLayoutComponents()
    }

    /**
     * Invoked when user pressed back menu button.
     */
    override fun onBackPressed() {

        super.onBackPressed()
        this.finish()
    }

    private fun configureLayoutComponents() {

        var face1Bitmap = getBitmapFromAsset(this,"face1.jpg")
        var face2Bitmap = getBitmapFromAsset(this,"face2.jpg")
        var face3Bitmap = getBitmapFromAsset(this,"face.jpg")
        faceImageView1.setImageBitmap(face1Bitmap)
        faceImageView2.setImageBitmap(face2Bitmap)
        faceImageView3.setImageBitmap(face3Bitmap)

        detectButton.setOnClickListener {
            detectTest()
        }

        analyse1Button.setOnClickListener {
            if (face1Bitmap != null) {
                analyseTest(face1Bitmap)
            }
        }

        analyse2Button.setOnClickListener {
            if (null != face2Bitmap) {
                analyseTest( face2Bitmap)
                var faceBitmap = getBitmapFromAsset(this,"FaceMenBlack.jpg")
                if (null != faceBitmap) {
                    analyseTest(faceBitmap)
                }
                faceBitmap = getBitmapFromAsset(this,"FaceMenAsiaticGlasses.jpg")
                if (null != faceBitmap) {
                    analyseTest(faceBitmap)
                }

                faceBitmap = getBitmapFromAsset(this,"FaceMenWhiteOld.jpg")
                if (null != faceBitmap) {
                    analyseTest(faceBitmap)
                }

                faceBitmap = getBitmapFromAsset(this,"FaceWomenAsiatic.jpg")
                if (null != faceBitmap) {
                    analyseTest(faceBitmap)
                }

                faceBitmap = getBitmapFromAsset(this,"FaceWomenIndian.jpg")
                if (null != faceBitmap) {
                    analyseTest(faceBitmap)
                }
            }
        }

        templateButton.setOnClickListener {
            if ((face1Bitmap != null)&&(null != face2Bitmap)&&(null != face3Bitmap)) {
                templateTest(face1Bitmap, face2Bitmap, face3Bitmap)
            }
        }

        compareButton.setOnClickListener {
            matchTest(templatet1, templatet2, templatet3)
        }

    }

    fun detectTest(){
        startActivity(Intent(this, CameraActivity::class.java))
    }

    fun analyseTest(faceBitmap:Bitmap){
        App.BioManager!!.analyzeFace(faceBitmap) { rc,
                                                   rectF,
                                                   _,
                                                   _,
                                                   _,
                                                   _,
                                                   gender,
                                                   age,
                                                   emotion,
                                                   hasGlasses,
                                                   imageQuality ->

            when (rc) {
                OK -> {
                    Log.d(TAG, "analyzeFaceAsync: RectF: $rectF")
                    Log.d(TAG, "analyzeFaceAsync: Gender: " + gender.name)
                    Log.d(TAG, "analyzeFaceAsync: Age: $age")
                    Log.d(TAG, "analyzeFaceAsync: Emotion: " + emotion.name)
                    Log.d(TAG, "analyzeFaceAsync: Glasses: $hasGlasses")
                    Log.d(TAG, "analyzeFaceAsync: ImageQuality: $imageQuality")
                }
                INTERMEDIATE -> {
                    /* This code is never returned for this API. */
                }
                FAIL -> Log.d(TAG, "analyzeFaceAsync: Failed to find face.")
            }
        }

    }

    fun templateTest (face1Bitmap:Bitmap, face2Bitmap:Bitmap, face3Bitmap:Bitmap){

        val res = App.BioManager!!.createFaceTemplateSync(face1Bitmap, syncAPITimeoutMS)
        if (null != res && OK == res.resultCode){
            templatet1 = res.template
            faceImageView1.setImageBitmap(decodeBitmap(templatet1))
            Log.d(TAG, "Template 1 available")
        } else {
            Log.d(TAG, "Template 1 Failed - result = " + res.resultCode.name)
        }

        val res2 = App.BioManager!!.createFaceTemplateSync(face2Bitmap, syncAPITimeoutMS)
        if (null != res2 && OK == res2.resultCode){
            templatet2 = res2.template
            faceImageView2.setImageBitmap(decodeBitmap(templatet2))
            Log.d(TAG, "Template 2 available")
        } else {
            Log.d(TAG, "Template 2 Failed - result = " + res2.resultCode.name)
        }

        App.BioManager!!.createFaceTemplate(toBytes(face3Bitmap), face3Bitmap.width, face3Bitmap.height) { rc, template ->
            when (rc) {
                OK -> {
                    templatet3 = template
                    faceImageView3.setImageBitmap(decodeBitmap(templatet3))
                    Log.d(TAG, "Template 3 available")
                }
                INTERMEDIATE -> {
                    /* This code is never returned for this API. */
                }
                FAIL -> Log.d(TAG, "createFaceTemplateAsync - Template 3 : FAIL")
            }
        }

    }

    fun matchTest(template1:ByteArray, template2:ByteArray, template3:ByteArray){
        val res = App.BioManager!!.compareFaceSync(template1, template2, syncAPITimeoutMS)
        if (null != res && OK == res.resultCode){
            Log.d(TAG, "compareFaceSync - template1 vs template2 - RES = " +  res.score)
        } else {
            Log.d(TAG, "compareFaceSync - template1 vs template2 - ERROR:" + res.resultCode.name)
        }

        val res2 = App.BioManager!!.compareFaceSync(template1, template3, syncAPITimeoutMS)
        if (null != res2 && OK == res.resultCode){
            Log.d(TAG, "compareFaceSync - template1 vs template3 - RES = " +  res2.score)
        } else {
            Log.d(TAG, "compareFaceSync - template1 vs template3 - ERROR:" + res2.resultCode.name)
        }

        App.BioManager!!.compareFace(template2, template3) { rc, score ->
            when (rc) {
                OK -> {
                    Log.d(TAG, "compareFaceSync - template2 vs template3 - RES Score = $score")
                }
                INTERMEDIATE -> {
                    /* This code is never returned for this API. */
                }
                FAIL -> Log.d(TAG, "compareFaceSync - template2 vs template3 - ERROR: ${rc.name}")
            }
        }

    }

    fun getBitmapFromAsset(context: Context, filePath: String?): Bitmap? {
        val assetManager: AssetManager = context.getAssets()
        val istr: InputStream
        var bitmap: Bitmap? = null
        try {
            istr = assetManager.open(filePath.toString())
            bitmap = BitmapFactory.decodeStream(istr)
        } catch (e: IOException) {
            // handle exception
        }
        return bitmap
    }

    fun toBytes(bitmap: Bitmap?): ByteArray? {
        if (null == bitmap) return null
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    fun decodeBitmap(data: ByteArray): Bitmap? {
        return try {
            BitmapFactory.decodeByteArray(data, 0, data.size)
        } catch (ignore: Exception) {
            null
        }
    }


}