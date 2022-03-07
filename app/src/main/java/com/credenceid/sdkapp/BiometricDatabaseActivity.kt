package com.credenceid.sdkapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Toast
import com.util.HexUtils
import kotlinx.android.synthetic.main.act_fp.*
import java.lang.Exception
import android.graphics.BitmapFactory
import com.credenceid.database.BiometricData
import com.credenceid.database.FingerprintRecord
import com.credenceid.database.MatchItem
import kotlinx.android.synthetic.main.act_bio_db.*
import android.content.res.AssetManager
import com.credenceid.database.FaceRecord
import java.io.*


private const val SYNC_API_TIMEOUT_MS = 3000

@SuppressLint("StaticFieldLeak")
class BiometricDatabaseActivity : Activity() {



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_bio_db)
        this.configureLayoutComponents()
    }

    /**
     * Invoked when user pressed back menu button.
     */
    override fun onBackPressed() {

        super.onBackPressed()
        this.finish()
    }

    /**
     * Configure all objects in layout file, set up listeners, views, etc.
     */
    @Suppress("DEPRECATION")
    private fun configureLayoutComponents() {

        val fpBitmap = getBitmapFromAsset(this,"fingerprint.jpg")
        val faceBitmap = getBitmapFromAsset(this,"face.jpg")
        fingerImageView.setImageBitmap(fpBitmap)
        faceImageView.setImageBitmap(faceBitmap)

        enrollBtn.setOnClickListener {
            if ((fpBitmap != null)&&(null != faceBitmap)) {
                enrollTest(fpBitmap, faceBitmap)
            }
        }

        verifyBtn.setOnClickListener {
            if ((fpBitmap != null)&&(null != faceBitmap)) {
                verifyTest(fpBitmap, faceBitmap)
            }
        }

        matchDbBtn.setOnClickListener {
            if ((fpBitmap != null)&&(null != faceBitmap)) {
                matchTest(fpBitmap, faceBitmap)
            }
        }

        deleteBtn.setOnClickListener {
            if ((fpBitmap != null)&&(null != faceBitmap)) {
                deleteTest(1)
            }
        }

    }

    fun enrollTest(fpImage:Bitmap, faceBitmap:Bitmap){

        val fpRecord = FingerprintRecord(FingerprintRecord.Position.RIGHT_THUMB, fpImage, 500)
        val faceRecord = FaceRecord(faceBitmap)

        App.BioManager!!.enroll(1, fpRecord, faceRecord, null){status, i ->
            Log.d("TEST-credence", "Enroll Result = " + status )
            Log.d("TEST-credence", "Enroll ID = " + i )
            info1DbViewTextView.text = "Enroll Result = " + status + " - Enroll ID = " + i
            App.BioManager!!.enroll(2, fpRecord, faceRecord, null){status, i ->
                Log.d("TEST-credence", "Enroll Result = " + status )
                Log.d("TEST-credence", "Enroll ID = " + i )
                info2DbViewTextView.text = "Enroll Result = " + status + " - Enroll ID = " + i
            }
        }
    }

    fun matchTest(fpImage:Bitmap, faceBitmap:Bitmap){

        var fpRecord = FingerprintRecord(FingerprintRecord.Position.RIGHT_THUMB, fpImage, 500)
        val faceRecord = FaceRecord(faceBitmap)

        App.BioManager!!.match( fpRecord, faceRecord, null){status, arrayList ->
            Log.d("TEST-credence", "Match Result = " + status )
            info1DbViewTextView.text = "Match Result = " + status
            if(null != arrayList) {
                Log.d("TEST-credence", "Match Result Arraylist size = " + arrayList?.size)
                info2DbViewTextView.text = "Match Result Arraylist size = " + arrayList?.size
                for (match: MatchItem in arrayList){
                    Log.d("TEST-credence", "Match Result candidate = " + match.id)
                    Log.d("TEST-credence", "Match Result Face Score = " + match.faceScore)
                    Log.d("TEST-credence", "Match Result Fingerprint Score = " + match.fingerprintScore)
                }
            } else {
                Log.d("TEST-credence", "ArrayList is null")
                info1DbViewTextView.text = "ArrayList is null"
            }
        }
    }

    fun verifyTest(fpImage:Bitmap, faceBitmap:Bitmap){

        var fpRecord = FingerprintRecord(FingerprintRecord.Position.RIGHT_THUMB, fpImage, 500)
        val faceRecord = FaceRecord(faceBitmap)

        App.BioManager!!.verify(1,  fpRecord, faceRecord, null){status, matchResults->
            Log.d("TEST-credence", "Verify status = " + status )
            info1DbViewTextView.text = "Verify status = " + status
            if(null != matchResults) {
                Log.d("TEST-credence", "Verify candidate was " + matchResults.id)
                Log.d("TEST-credence", "Verify Result Face Score = " + matchResults.faceScore)
                Log.d("TEST-credence", "Verify Result Fingerprint Score = " + matchResults.fingerprintScore)
            } else {
                Log.d("TEST-credence", "Verify matchResults is null")
                info1DbViewTextView.text = "Verify matchResults is null"
            }
        }
    }

    fun deleteTest(id:Int){
        App.BioManager!!.delete(id){status->
            Log.d("TEST-credence", "delete Result = " + status )
            info1DbViewTextView.text = "delete id = " + id
            info2DbViewTextView.text = "delete Result = " + status
        }
    }

    fun getBitmapFromAsset(context: Context, filePath: String?): Bitmap? {
        val assetManager: AssetManager = context.getAssets()
        val istr: InputStream
        var bitmap: Bitmap? = null
        try {
            istr = assetManager.open(filePath)
            bitmap = BitmapFactory.decodeStream(istr)
        } catch (e: IOException) {
            // handle exception
        }
        return bitmap
    }

}