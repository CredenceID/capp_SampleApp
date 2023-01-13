package com.credenceid.sdkapp

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Toast
import com.credenceid.biometrics.Biometrics.*
import com.credenceid.biometrics.Biometrics.FMDFormat.*
import com.credenceid.biometrics.Biometrics.ResultCode.*
import com.credenceid.biometrics.ConvertToFMDSyncResponse
import com.credenceid.biometrics.FMDToCCFSyncResponse
import com.credenceid.database.FingerprintRecord
import com.credenceid.database.MatchItem
import com.credenceid.sdkapp.App.Companion.TAG
import com.util.HexUtils
import kotlinx.android.synthetic.main.act_fp.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

private const val SYNC_API_TIMEOUT_MS = 3000

@SuppressLint("StaticFieldLeak")
class FingerprintActivity : Activity() {

    var start = 0L

    /**
     * List of different fingerprint scan types supported across all Credence devices.
     */
    private val mScanTypes = arrayOf(
        ScanType.SINGLE_FINGER,
        ScanType.TWO_FINGERS,
        ScanType.ROLL_SINGLE_FINGER,
        ScanType.TWO_FINGERS_SPLIT
    )

    /**
     * If true, then "mOpenClose" button text is "Open" meaning we need to open fingerprint.
     * If false, then "mOpenClose" button text is "Close" meaning we need to close fingerprint.
     */
    private var mOpenFingerprint = true

    /**
     * We are capturing two fingerprints. If true then saves data as first fingerprint; if false
     * saves data as second fingerprint.
     */
    private var mCaptureFingerprintOne = true

    private var mIsCapturingFp = false

    /**
     * Stores FMD templates (used for fingerprint matching) for each fingerprint.
     */
    private var mFingerprintOneFMDTemplate: ByteArray? = null
    private var mFingerprintTwoFMDTemplate: ByteArray? = null

    /**
     * Callback invoked every time fingerprint sensor on device opens or closes.
     */
    private val mFingerprintOpenCloseListener = object : FingerprintReaderStatusListener {
        override fun onOpenFingerprintReader(
            resultCode: ResultCode,
            hint: String?
        ) {
            Log.d("CID-TEST", "Fingerprint Reader opening time = " + (SystemClock.elapsedRealtime() - start))

            /* If hint is valid, display it. Regardless of ResultCode we should
             * message indicating what is going on with sensor.
             */
            if (hint != null && hint.isNotEmpty()) {
                fpStatusTextView.text = hint
            }

            /* This code is returned once sensor has fully finished opening. */
            when (resultCode) {
                OK -> {
                    Log.d("CID-TEST", "onOpenFingerprintReader - result OK")
                    /* Now that sensor is open, if user presses "openCloseBtn" fingerprint sensor
                     * should now close. To achieve this we change flag which controls what action
                     * openCloseBtn takes.
                     */
                    mOpenFingerprint = false

                    /* Operation is complete, re-enable button. */
                    openCloseBtn.isEnabled = true
                    /* Only if fingerprint opened do we allow user to capture fingerprints. */
                    captureBtn.isEnabled = true
                    /* If fingerprint opened then we change button to say "Close". */
                    openCloseBtn.text = getString(R.string.close)
                }
                /* This code is returned while sensor is in the middle of opening. */
                INTERMEDIATE -> {
                    Log.d("CID-TEST", "onOpenFingerprintReader - result INTERMEDIATE")
                    /* Do nothing while operation is still on-going. */
                }
                /* This code is returned if sensor fails to open. */
                FAIL -> {
                    Log.d("CID-TEST", "onOpenFingerprintReader - result FAIL")
                    /* Operation is complete, re-enable button. */
                    openCloseBtn.isEnabled = true
                }
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onCloseFingerprintReader(
            resultCode: ResultCode,
            closeReasonCode: CloseReasonCode
        ) {
            when {
                OK == resultCode -> {
                    fpStatusTextView.text = "Fingerprint Closed: " + closeReasonCode.name

                    /* Now that sensor is closed, if user presses "openCloseBtn" fingerprint
                     * sensor should now open. To achieve this we change flag which controls
                     * what action openCloseBtn takes.
                     */
                    mOpenFingerprint = true

                    /* Change text back to "Open" and allow button to be clickable. */
                    openCloseBtn.text = getString(R.string.open)
                    openCloseBtn.isEnabled = true
                    /* Sensor is closed, user should NOT be able to press capture or match. */
                    captureBtn.isEnabled = false
                    matchBtn.isEnabled = false
                }
                INTERMEDIATE == resultCode -> {
                    /* This code is never returned for this API. */
                }
                FAIL == resultCode -> fpStatusTextView.text = "Fingerprint FAILED to close."
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_fp)
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
     * Invoked when application is killed, either by user or system.
     */
    override fun onDestroy() {
        super.onDestroy()

        /* Tell biometrics to cancel current on-going capture. */
        App.BioManager!!.cancelCapture()
        /* Close all open peripherals. */
        App.BioManager!!.closeFingerprintReader()
    }

    /**
     * Configure all objects in layout file, set up listeners, views, etc.
     */
    @Suppress("DEPRECATION")
    private fun configureLayoutComponents() {
        /* Only allow capture once fingerprint is open. */
        /* Only allow match once both fingerprints have been captured. */
        this.setCaptureMatchButtonEnable(false)

        fingerOneImageView.setOnClickListener {
            /* This ImageView should turn green since it was selected. */
            fingerOneImageView.background = resources.getDrawable(R.drawable.image_border_on)
            /* Other ImageView should turn black or off. */
            fingerTwoImageView.background = resources.getDrawable(R.drawable.image_border_off)

            mCaptureFingerprintOne = true
        }

        fingerTwoImageView.setOnClickListener {
            /* This ImageView should turn green since it was selected. */
            fingerTwoImageView.background = resources.getDrawable(R.drawable.image_border_on)
            /* Other ImageView should turn black or off. */
            fingerOneImageView.background = resources.getDrawable(R.drawable.image_border_off)

            mCaptureFingerprintOne = false
        }

        /* Inside onClickListeners for each button, we disable all buttons until their respective
         * operation is complete. Once it is done, the appropriate buttons are re-enabled.
         */
        openCloseBtn.setOnClickListener {
            /* Disable button so user does not try a second open while fingerprint it opening.
             * Hide capture/math buttons since sensor is opening/closing.
             */
            this.setAllComponentEnable(false)

            if (mOpenFingerprint) {
                App.BioManager!!.openFingerprintReader(mFingerprintOpenCloseListener)
                Log.d("CID-TEST", "openFingerprintReader - start")
                start = SystemClock.elapsedRealtime()
            } else {
                App.BioManager!!.closeFingerprintReader()
            }
        }

        captureBtn.setOnClickListener {
            if (!mIsCapturingFp) {
                mIsCapturingFp = true
                // this.setAllComponentEnable(false)
                openCloseBtn.isEnabled = false
                captureBtn.text = "Cancel"
                infoTextView.text = ""

                /* Based on which ImageView was selected, capture appropriate fingerprint. */
                if (mCaptureFingerprintOne) {
                    // this.captureFingerprintOne()
                    this.captureFingerprintOneRawListener()
                } else {
                    this.captureFingerprintTwo()
                }
            } else {
                mIsCapturingFp = false
                App.BioManager!!.cancelCapture()
                this.setAllComponentEnable(true)
                captureBtn.text = "Capture"
            }
        }

        matchBtn.setOnClickListener {
            if (mFingerprintOneFMDTemplate != null && mFingerprintTwoFMDTemplate != null) {
                this.setAllComponentEnable(false)
                this.matchFMDTemplates(mFingerprintOneFMDTemplate, mFingerprintTwoFMDTemplate)
            } else {
                Toast.makeText(this, "Please capture both fingerprints to match.", Toast.LENGTH_LONG).show()
            }
        }

        if (App.BioManager!!.deviceType.name.contains("CredenceECO_FC")) {
            calibrateBtn.setOnClickListener {
                calibrateFingerprintSensor()
            }
        } else {
            calibrateBtn.visibility = View.GONE
        }
    }

    /**
     * Sets enable for "captureBtn" and "matchBtn" components.
     *
     * @param enable If true components are enabled, if false they are disabled.
     */
    private fun setCaptureMatchButtonEnable(enable: Boolean) {
        captureBtn.isEnabled = enable
        matchBtn.isEnabled = enable

        /* If both templates have been created then enable Match button. */
        matchBtn.isEnabled = mFingerprintOneFMDTemplate != null && mFingerprintTwoFMDTemplate != null
    }

    /**
     * Sets enable for all UI components in layout.
     *
     * @param enable If true components are enabled, if false they are disabled.
     */
    private fun setAllComponentEnable(enable: Boolean) {
        this.setCaptureMatchButtonEnable(enable)
        openCloseBtn.isEnabled = enable
        fingerOneImageView.isEnabled = enable
        fingerTwoImageView.isEnabled = enable
    }

    /**
     * Make CredenceSDK API calls to capture "first" fingerprint. This is fingerprint image on left
     * side of layout file.
     */
    private fun captureFingerprintOne() {
        mFingerprintOneFMDTemplate = null

        /* OnFingerprintGrabbedWSQListener: This listener is to be used if you wish to obtain a WSQ
         * template, fingerprint quality score, along with captured fingerprint image. This saves
         * from having to make separate API calls.
         */
        App.BioManager!!.grabFingerprint(
            mScanTypes[0],
            object : OnFingerprintGrabbedWSQListener {
                @SuppressLint("SetTextI18n")

                override fun onFingerprintGrabbed(
                    resultCode: ResultCode,
                    bitmap: Bitmap?,
                    bytes: ByteArray?,
                    imagePath: String?,
                    wsqData: ByteArray?,
                    wsqFilePath: String?,
                    hint: String?,
                    nfiqScore: Int
                ) {
                    /* If a valid hint was given then display it for user to see. */
                    if (hint != null && hint.isNotEmpty()) {
                        fpStatusTextView.text = hint
                    }

                    when (resultCode) {
                        /* This code is returned once sensor captures fingerprint image. */
                        OK -> {
                            mIsCapturingFp = false
                            captureBtn.text = "Capture"

                            if (null != bitmap) {
                                fingerOneImageView.setImageBitmap(bitmap)
                            }

                            // fpStatusTextView.text = "WSQ File: $wsqFilepath"
                            infoTextView.text = "Quality: $nfiqScore"

                            /* Create template from fingerprint image. */
                            createFMDTemplate(bitmap)

                            if (wsqData != null) {
                                Log.d(TAG, "wsqData not null => Saving file")
                                saveFile("fingerprint.wsq", wsqData)
                                Log.d(TAG, "wsqData not null => file saved")
                            } else {
                                Log.d(TAG, "wsqData is null => nothing to save")
                            }

                            val fmd: ConvertToFMDSyncResponse = App.BioManager!!.convertToFMDSync(bitmap, ISO_19794_2_2005, 9000)
                            if (fmd.resultCode == OK) {
                                Log.d(TAG, "ConvertToFMDSyncResponse ==> PASS")
                            } else if (fmd.resultCode == INTERMEDIATE) {
                            } else if (fmd.resultCode == FAIL) {
                                Log.d(TAG, "ConvertToFMDSyncResponse ==> FAIL")
                            }
                            val res: FMDToCCFSyncResponse = App.BioManager!!.convertFMDToCCFSync(fmd.FMD, 5000)
                            if (res.resultCode == OK && res.CCF != null) {
                                Log.d(TAG, "convertFMDToCCFSync ==> PASS")
                            } else if (res.resultCode == INTERMEDIATE) {
                            } else if (res.resultCode == FAIL) {
                                Log.d(TAG, "convertFMDToCCFSync ==> FAIL")
                            }

                            if (bitmap != null) {
                                saveBitmapAsPng("fingerprint.png", bitmap)
                            }

                            setAllComponentEnable(true)
                        }
                        /* This code is returned on every new frame/image from sensor. */
                        INTERMEDIATE -> {
                            if (null != bitmap) {
                                fingerOneImageView.setImageBitmap(bitmap)
                            }

                        /* This hint is returned if cancelCapture()" or "closeFingerprint()" are
                         * called while in middle of capture.
                         */
                            if (hint != null && hint == "Capture Stopped") {
                                setAllComponentEnable(true)
                            }
                        }
                        /* This code is returned if sensor fails to capture image. */
                        FAIL -> {
                            mIsCapturingFp = false
                            captureBtn.text = "Capture"
                            setAllComponentEnable(true)
                        }
                    }
                }

                override fun onCloseFingerprintReader(
                    resultCode: ResultCode,
                    closeReasonCode: CloseReasonCode
                ) {
                    /* This case is already handled by "mFingerprintOpenCloseListener". */
                }
            }
        )
    }

    private fun captureFingerprintOneRawListener() {
        mFingerprintOneFMDTemplate = null

        App.BioManager!!.grabFingerprint(
            mScanTypes[0],
            object : OnFingerprintGrabbedRawNewListener {
                @SuppressLint("SetTextI18n")

                override fun onFingerprintGrabbed(
                    resultCode: ResultCode,
                    bitmap: Bitmap?,
                    iso: ByteArray?,
                    RawDataBytes: ByteArray?,
                    hint: String?
                ) {
                    /* If a valid hint was given then display it for user to see. */
                    if (hint != null && hint.isNotEmpty()) {
                        fpStatusTextView.text = hint
                    }

                    when (resultCode) {
                        /* This code is returned once sensor captures fingerprint image. */
                        OK -> {
                            mIsCapturingFp = false
                            captureBtn.text = "Capture"

                            if (null != bitmap) {
                                fingerOneImageView.setImageBitmap(bitmap)
                            }

                            /* Create template from fingerprint image. */
                            createFMDTemplate(bitmap)

                            if (iso != null) {
                                Log.d(TAG, "iso not null => Saving file")
                                saveFile("fingerprint.wsq", iso)
                                Log.d(TAG, "iso not null => file saved")
                            } else {
                                Log.d(TAG, "iso is null => nothing to save")
                            }

                            if (RawDataBytes != null) {
                                Log.d(TAG, "RawDataBytes not null => RawDataBytes size = " + RawDataBytes.size)
                            } else {
                                Log.d(TAG, "RawDataBytes is null")
                            }

                            val fmd: ConvertToFMDSyncResponse = App.BioManager!!.convertToFMDSync(bitmap, ISO_19794_2_2005, 9000)
                            if (fmd.resultCode == OK) {
                                Log.d(TAG, "ConvertToFMDSyncResponse ==> PASS")
                            } else if (fmd.resultCode == INTERMEDIATE) {
                            } else if (fmd.resultCode == FAIL) {
                                Log.d(TAG, "ConvertToFMDSyncResponse ==> FAIL")
                            }
                            val res: FMDToCCFSyncResponse = App.BioManager!!.convertFMDToCCFSync(fmd.FMD, 5000)
                            if (res.resultCode == OK && res.CCF != null) {
                                Log.d(TAG, "convertFMDToCCFSync ==> PASS")
                            } else if (res.resultCode == INTERMEDIATE) {
                            } else if (res.resultCode == FAIL) {
                                Log.d(TAG, "convertFMDToCCFSync ==> FAIL")
                            }

                            if (bitmap != null) {
                                saveBitmapAsPng("fingerprint.png", bitmap)
                            }

                            setAllComponentEnable(true)
                        }
                        /* This code is returned on every new frame/image from sensor. */
                        INTERMEDIATE -> {
                            if (null != bitmap) {
                                fingerOneImageView.setImageBitmap(bitmap)
                            }

                        /* This hint is returned if cancelCapture()" or "closeFingerprint()" are
                         * called while in middle of capture.
                         */
                            if (hint != null && hint == "Capture Stopped") {
                                setAllComponentEnable(true)
                            }
                        }
                        /* This code is returned if sensor fails to capture image. */
                        FAIL -> {
                            mIsCapturingFp = false
                            captureBtn.text = "Capture"
                            setAllComponentEnable(true)
                        }
                    }
                }

                override fun onCloseFingerprintReader(
                    resultCode: ResultCode,
                    closeReasonCode: CloseReasonCode
                ) {
                    /* This case is already handled by "mFingerprintOpenCloseListener". */
                }
            }
        )
    }

    /**
     * Make CredenceSDK API calls to capture "second" fingerprint. This is fingerprint image on
     * right side of layout file.
     */
    private fun captureFingerprintTwo() {
        mFingerprintTwoFMDTemplate = null

        App.BioManager!!.grabFingerprint(
            mScanTypes[0],
            true,
            object : OnFingerprintGrabbedListener {
                override fun onFingerprintGrabbed(
                    resultCode: ResultCode?,
                    bitmap: Bitmap?,
                    bytes: ByteArray?,
                    path: String?,
                    hint: String?
                ) {
                    /* If a valid hint was given then display it for user to see. */
                    if (hint != null && hint.isNotEmpty()) {
                        fpStatusTextView.text = hint
                    }

                    when (resultCode) {
                        OK -> {
                            captureBtn.text = "Capture"
                            mIsCapturingFp = false
                            if (null != bitmap) {
                                fingerTwoImageView.setImageBitmap(bitmap)
                            }

                            if (null != path) {
                                Log.d("TEST", "File path = " + path)
                            }

                            /* Create template from fingerprint image. */
                            createFMDTemplate(bitmap)
                        }
                        /* This code is returned on every new frame/image from sensor. */
                        INTERMEDIATE -> {
                            if (null != bitmap) {
                                fingerTwoImageView.setImageBitmap(bitmap)
                            }

                        /* This hint is returned if cancelCapture()" or "closeFingerprint()" are
                         * called while in middle of capture.
                         */
                            if (hint != null && hint == "Capture Stopped") {
                                setAllComponentEnable(true)
                            }
                        }
                        /* This code is returned if sensor fails to capture image. */
                        FAIL -> {
                            mIsCapturingFp = false
                            captureBtn.text = "Capture"
                            setAllComponentEnable(true)
                        }
                    }
                }

                override fun onCloseFingerprintReader(
                    resultCode: ResultCode,
                    closeReasonCode: CloseReasonCode
                ) {
                    /* This case is already handled by "mFingerprintOpenCloseListener". */
                }
            }
        )
    }

    /**
     * Attempts to create a FMD template from given Bitmap image. If successful it saves FMD to
     * respective fingerprint template array.
     *
     * @param bitmap
     */
    @SuppressLint("SetTextI18n")
    private fun createFMDTemplate(bitmap: Bitmap?) {
        /* Keep a track of how long it takes for FMD creation. */
        val startTime = SystemClock.elapsedRealtime()

        App.BioManager!!.convertToFMD(bitmap, ISO_19794_2_2005) { resultCode: ResultCode,
            bytes: ByteArray? ->

            when (resultCode) {
                OK -> {
                    /* Display how long it took for FMD template to be created. */
                    val durationInSeconds = (SystemClock.elapsedRealtime() - startTime) / 1000.0
                    infoTextView.text = "Created FMD template in: $durationInSeconds seconds."

                    if (bytes != null) {
                        Log.d("CID-sample", "FMD template size = " + bytes.size)
                    } else {
                        Log.d("CID-sample", "FMD template is NULL ")
                    }

                    if (mCaptureFingerprintOne) {
                        mFingerprintOneFMDTemplate = bytes?.copyOf(bytes.size)
                    } else {
                        mFingerprintTwoFMDTemplate = bytes?.copyOf(bytes.size)
                    }

                    /* If both templates have been created then enable Match button. */
                    if (mFingerprintOneFMDTemplate != null && mFingerprintTwoFMDTemplate != null) {
                        matchBtn.isEnabled = true
                    }
                }
                INTERMEDIATE -> {
                    /* This code is never returned for this API. */
                }
                FAIL -> {
                    fpStatusTextView.text = "Failed to create FMD template."
                }
            }
            setAllComponentEnable(true)
        }
    }

    /**
     * Matches two FMD templates and displays score.
     *
     * @param templateOne FMD template.
     * @param templateTwo FMD template to match against.
     */
    @SuppressLint("SetTextI18n")
    private fun matchFMDTemplates(
        templateOne: ByteArray?,
        templateTwo: ByteArray?
    ) {
        /* Normally one would handle parameter checking, but this API handles it for us. Meaning
         * that if any FMD is invalid it will return the proper score of 0, etc.
         */
        App.BioManager!!.compareFMD(templateOne, templateTwo, ISO_19794_2_2005) { rc: ResultCode,
            score: Float ->

            when (rc) {
                OK -> {
                    var matchDecision = "No Match"
                    /* This is how to properly determine a match or not. */
                    if (score > 40) {
                        matchDecision = "Match"
                    }

                    fpStatusTextView.text = "Match outcome: $matchDecision"
                    infoTextView.text = "Match score: $score"
                }
                INTERMEDIATE -> {
                    /* This API will never return ResultCode.INTERMEDIATE. */
                }
                FAIL -> {
                    fpStatusTextView.text = "Failed to compare templates."
                    infoTextView.text = ""
                }
            }
            /* Re-enable all components since operation is now complete. */
            this.setAllComponentEnable(true)
        }
    }

    private fun calibrateFingerprintSensor() {
        App.BioManager!!.calibrateFingerprintReader() { resultCode: ResultCode,
            hint: String? ->

            fpStatusTextView.text = "FP sensor Calibration result = " + resultCode.name
        }
    }

    /**
     * --------------------------------------------------------------------------------------------
     *
     * Methods demonstrating how to use Credence ID other fingerprint APIs. This methods are not
     * used in this application, as they are only here for usage reference.
     *
     * --------------------------------------------------------------------------------------------
     */

    @Suppress("unused")
    private fun convertWSQToFMDSync(WSQ: ByteArray): ByteArray {
        val res = App.BioManager!!.convertToFMDSync(
            WSQ,
            ISO_19794_2_2005,
            SYNC_API_TIMEOUT_MS
        )

        return if (null != res && OK == res.resultCode) res.FMD else byteArrayOf()
    }

    @Suppress("unused")
    private fun convertFMDToCCFSync(FMD: ByteArray): ByteArray {
        val res = App.BioManager!!.convertFMDToCCFSync(FMD, SYNC_API_TIMEOUT_MS)
        return if (null != res && OK == res.resultCode) res.CCF else byteArrayOf()
    }

    @Suppress("unused")
    private fun convertCCFToFMDSync(CCF: ByteArray): ByteArray {
        /* Arguments to this API assume a fingerprint capture was made using Credence ID SDK and
         * device. Once a capture was made one of Credence's 'convertToFMD' methods was using to
         * obtain this CCF template. If so, then please use following values for CCF to FMD.
         */
        val res = App.BioManager!!.convertCCFToFMDSync(
            CCF,
            400.toShort(),
            500.toShort(),
            100.toShort(),
            100.toShort(),
            SYNC_API_TIMEOUT_MS
        )

        return if (null != res && OK == res.resultCode) res.FMD else byteArrayOf()
    }

    @Suppress("unused")
    private fun compareFMDSync(
        FMDOne: ByteArray,
        FMDTwo: ByteArray
    ): Int {
        val res = App.BioManager!!.compareFMDSync(
            FMDOne,
            FMDTwo,
            ISO_19794_2_2005,
            SYNC_API_TIMEOUT_MS
        )
        /* Score may be multiplied by 100 to obtain a percentage in range [0, 100]. */
        return if (null == res) 0 else (res.dissimilarity * 100).toInt()
    }

    @Suppress("unused")
    private fun convertToFMDSync(fingerprintImage: Bitmap): ByteArray {
        val res = App.BioManager!!.convertToFMDSync(
            fingerprintImage,
            ISO_19794_2_2005,
            SYNC_API_TIMEOUT_MS
        )

        return if (null != res && OK == res.resultCode) res.FMD else byteArrayOf()
    }

    @Suppress("unused")
    private fun grabFingerprint() {
        App.BioManager!!.grabFingerprint(
            ScanType.SINGLE_FINGER,
            false,
            object : OnMultiFingerprintGrabbedListener {
                @SuppressLint("SetTextI18n")
                override fun onFingerprintGrabbed(
                    resultCode: ResultCode,
                    bitmap: Bitmap?,
                    bitmaps: Array<Bitmap>,
                    filePath: String,
                    strings: Array<String>,
                    hint: String?
                ) {
                    /* If a valid hint was given then display it for user to see. */
                    if (hint != null && hint.isNotEmpty()) {
                        fpStatusTextView.text = hint
                    }

                    when (resultCode) {
                        /* This code is returned once sensor captures fingerprint image. */
                        OK -> {
                            if (null != bitmap) {
                                fingerOneImageView.setImageBitmap(bitmap)
                            }

                            fpStatusTextView.text = "File: $filePath"

                            /* Create template from fingerprint image. */
                            createFMDTemplate(bitmap)
                        }
                        /* This code is returned on every new frame/image from sensor. */
                        INTERMEDIATE -> {
                            /* On every frame, if image preview is available, show it to user. */
                            if (null != bitmap) {
                                fingerOneImageView.setImageBitmap(bitmap)
                            }

                                /* This hint is returned if cancelCapture()" or
                                 * "closeFingerprint()" are called while in middle of capture.
                                 */
                            if (hint != null && hint == "Capture Stopped") {
                                setAllComponentEnable(true)
                            }
                        }
                        /* This code is returned if sensor fails to capture image. */
                        FAIL -> {
                            setAllComponentEnable(true)
                        }
                    }
                }

                override fun onCloseFingerprintReader(
                    resultCode: ResultCode,
                    closeReasonCode: CloseReasonCode
                ) {
                    /* This case is already handled by "mFingerprintOpenCloseListener". */
                }
            }
        )
    }

    @SuppressLint("SetTextI18n")
    @Suppress("unused")
    private fun decompressWSQ(WSQ: ByteArray) {
        App.BioManager!!.decompressWSQ(WSQ) { resultCode, bytes ->
            when {
                OK == resultCode -> {
                    fpStatusTextView.text = "WSQ Decompression: SUCCESS"
                    infoTextView.text = "Data: " + HexUtils.toString(bytes)
                }
                INTERMEDIATE == resultCode -> {
                    /* This code is never returned for this API. */
                }
                FAIL == resultCode -> fpStatusTextView.text = "WSQ Decompression: FAIL"
            }
        }
    }

    @Throws(Exception::class)
    fun saveBitmapAsPng(file: String, bmp: Bitmap) {
        var outputPath = ""
        val externalStorageVolumes = applicationContext.getExternalFilesDirs("")
        if (null != externalStorageVolumes) {
            if (null != externalStorageVolumes[0]) {
                outputPath = externalStorageVolumes[0]!!.absolutePath + "/" + file
                val toWrite = File(outputPath)
                if (!toWrite.exists()) {
                    if (!toWrite.createNewFile()) throw Exception("Fail to create file")
                }
                val fOut: FileOutputStream = FileOutputStream(outputPath)
                val imageByteArray = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, imageByteArray)
                val imageData: ByteArray = imageByteArray.toByteArray()
                setDpi(imageData, 500)
                fOut.write(imageData)
                fOut.flush()
                fOut.close()
            }
        }
    }

    @Throws(Exception::class)
    fun saveFile(file: String, data: ByteArray) {
        var outputPath = ""
        val externalStorageVolumes = applicationContext.getExternalFilesDirs("")
        if (null != externalStorageVolumes) {
            if (null != externalStorageVolumes[0]) {
                outputPath = externalStorageVolumes[0]!!.absolutePath + "/" + file
                val toWrite = File(outputPath)
                Log.d(TAG, "Writing file : " + outputPath)
                if (!toWrite.exists()) {
                    if (!toWrite.createNewFile()) {
                        throw Exception("Fail to create file")
                    }
                }
                val fOut = FileOutputStream(outputPath)
                fOut.write(data)
                fOut.flush()
                fOut.close()
            }
        }
    }

    fun setDpi(imageData: ByteArray, dpi: Int) {
        imageData[13] = 1
        imageData[14] = (dpi shr 8).toByte()
        imageData[15] = (dpi and 0xff).toByte()
        imageData[16] = (dpi shr 8).toByte()
        imageData[17] = (dpi and 0xff).toByte()
    }

    fun enrollTest(fpImage: Bitmap) {
        var fpRecord = FingerprintRecord(FingerprintRecord.Position.RIGHT_THUMB, fpImage, 500)

        App.BioManager!!.enroll(0, fpRecord, null, null) { status, i ->
            Log.d("TEST", "Enroll Result = " + status)
            Log.d("TEST", "Enroll ID = " + i)
        }
    }

    fun matchTest(fpImage: Bitmap) {
        var fpRecord = FingerprintRecord(FingerprintRecord.Position.RIGHT_THUMB, fpImage, 500)

        App.BioManager!!.match(fpRecord, null, null) { status, arrayList ->
            Log.d("TEST", "Match Result = " + status)
            if (null != arrayList) {
                Log.d("TEST", "Match Result Arraylist size = " + arrayList?.size)
                for (match: MatchItem in arrayList) {
                    Log.d("TEST", "Match Result candidate = " + match.id)
                    Log.d("TEST", "Match Result Face Score = " + match.faceScore)
                    Log.d("TEST", "Match Result Fingerprint Score = " + match.fingerprintScore)
                }
            } else {
                Log.d("TEST", "ArrayList is null")
            }
        }
    }

    fun deleteTest(id: Int) {
        App.BioManager!!.delete(id) { status ->
            Log.d("TEST", "delete Result = " + status)
        }
    }
}
