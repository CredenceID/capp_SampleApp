package com.credenceid.sdkapp

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import com.credenceid.biometrics.Biometrics.*
import com.credenceid.biometrics.Biometrics.FMDFormat.*
import com.credenceid.biometrics.Biometrics.ResultCode.*
import com.util.HexUtils
import kotlinx.android.synthetic.main.act_fp.*
import kotlinx.android.synthetic.main.act_mrz.*

private const val SYNC_API_TIMEOUT_MS = 3000

@SuppressLint("StaticFieldLeak")
class FingerprintActivity : Activity() {

    /**
     * List of different fingerprint scan types supported across all Credence devices.
     */
    private val mScanTypes = arrayOf(
            ScanType.SINGLE_FINGER,
            ScanType.TWO_FINGERS,
            ScanType.ROLL_SINGLE_FINGER,
            ScanType.TWO_FINGERS_SPLIT)
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
    /**
     * Stores FMD templates (used for fingerprint matching) for each fingerprint.
     */
    private var mFingerprintOneFMDTemplate: ByteArray? = null
    private var mFingerprintTwoFMDTemplate: ByteArray? = null

    /**
     * Callback invoked every time fingerprint sensor on device opens or closes.
     */
    private val mFingerprintOpenCloseListener = object : FingerprintReaderStatusListener {
        override fun onOpenFingerprintReader(resultCode: ResultCode,
                                             hint: String?) {

            /* If hint is valid, display it. Regardless of ResultCode we should
             * message indicating what is going on with sensor.
             */
            if (hint != null && hint.isNotEmpty())
                fpStatusTextView.text = hint

            /* This code is returned once sensor has fully finished opening. */
            when (resultCode) {
                OK -> {
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
                    /* Do nothing while operation is still on-going. */
                }
                /* This code is returned if sensor fails to open. */
                FAIL -> {
                    /* Operation is complete, re-enable button. */
                    openCloseBtn.isEnabled = true
                }
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onCloseFingerprintReader(resultCode: ResultCode,
                                              closeReasonCode: CloseReasonCode) {

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

            if (mOpenFingerprint)
                App.BioManager!!.openFingerprintReader(mFingerprintOpenCloseListener)
            else
                App.BioManager!!.closeFingerprintReader()
        }

        captureBtn.setOnClickListener {
            this.setAllComponentEnable(false)
            infoTextView.text = ""

            /* Based on which ImageView was selected, capture appropriate fingerprint. */
            if (mCaptureFingerprintOne)
                this.captureFingerprintOne()
            else
                this.captureFingerprintTwo()
        }

        matchBtn.setOnClickListener {
            if(mFingerprintOneFMDTemplate!=null && mFingerprintTwoFMDTemplate!=null) {
                this.setAllComponentEnable(false)
                this.matchFMDTemplates(mFingerprintOneFMDTemplate, mFingerprintTwoFMDTemplate)
            }else{
                Toast.makeText(this,"Please capture both fingerprints to match.",Toast.LENGTH_LONG).show()
            }
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
        App.BioManager!!.grabFingerprint(mScanTypes[0], object : OnFingerprintGrabbedWSQListener {
            @SuppressLint("SetTextI18n")
            override fun onFingerprintGrabbed(resultCode: ResultCode,
                                              bitmap: Bitmap?,
                                              bytes: ByteArray?,
                                              filepath: String?,
                                              wsqFilepath: String?,
                                              hint: String?,
                                              nfiqScore: Int) {

                /* If a valid hint was given then display it for user to see. */
                if (hint != null && hint.isNotEmpty())
                    fpStatusTextView.text = hint

                when (resultCode) {
                    /* This code is returned once sensor captures fingerprint image. */
                    OK -> {
                        if (null != bitmap)
                            fingerOneImageView.setImageBitmap(bitmap)

                        fpStatusTextView.text = "WSQ File: $wsqFilepath"
                        infoTextView.text = "Quality: $nfiqScore"

                        /* Create template from fingerprint image. */
                        createFMDTemplate(bitmap)
                    }
                    /* This code is returned on every new frame/image from sensor. */
                    INTERMEDIATE -> {
                        if (null != bitmap)
                            fingerOneImageView.setImageBitmap(bitmap)

                        /* This hint is returned if cancelCapture()" or "closeFingerprint()" are
                         * called while in middle of capture.
                         */
                        if (hint != null && hint == "Capture Stopped")
                            setAllComponentEnable(true)
                    }
                    /* This code is returned if sensor fails to capture image. */
                    FAIL -> {
                        setAllComponentEnable(true)
                    }
                }
            }

            override fun onCloseFingerprintReader(resultCode: ResultCode,
                                                  closeReasonCode: CloseReasonCode) {

                /* This case is already handled by "mFingerprintOpenCloseListener". */
            }
        })
    }

    /**
     * Make CredenceSDK API calls to capture "second" fingerprint. This is fingerprint image on
     * right side of layout file.
     */
    private fun captureFingerprintTwo() {

        mFingerprintTwoFMDTemplate = null

        App.BioManager!!.grabFingerprint(mScanTypes[0], object : OnFingerprintGrabbedListener {
            override fun onFingerprintGrabbed(resultCode: ResultCode,
                                              bitmap: Bitmap?,
                                              bytes: ByteArray?,
                                              s: String?,
                                              hint: String?) {

                /* If a valid hint was given then display it for user to see. */
                if (hint != null && hint.isNotEmpty())
                    fpStatusTextView.text = hint

                when (resultCode) {
                    OK -> {
                        if (null != bitmap)
                            fingerTwoImageView.setImageBitmap(bitmap)

                        /* Create template from fingerprint image. */
                        createFMDTemplate(bitmap)
                    }
                    /* This code is returned on every new frame/image from sensor. */
                    INTERMEDIATE -> {
                        if (null != bitmap)
                            fingerTwoImageView.setImageBitmap(bitmap)

                        /* This hint is returned if cancelCapture()" or "closeFingerprint()" are
                         * called while in middle of capture.
                         */
                        if (hint != null && hint == "Capture Stopped")
                            setAllComponentEnable(true)
                    }
                    /* This code is returned if sensor fails to capture image. */
                    FAIL -> {
                        setAllComponentEnable(true)
                    }
                }
            }

            override fun onCloseFingerprintReader(resultCode: ResultCode,
                                                  closeReasonCode: CloseReasonCode) {

                /* This case is already handled by "mFingerprintOpenCloseListener". */
            }
        })
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
                                                                  bytes: ByteArray ->

            when (resultCode) {
                OK -> {
                    /* Display how long it took for FMD template to be created. */
                    val durationInSeconds = (SystemClock.elapsedRealtime() - startTime) / 1000.0
                    infoTextView.text = "Created FMD template in: $durationInSeconds seconds."

                    if (mCaptureFingerprintOne)
                        mFingerprintOneFMDTemplate = bytes.copyOf(bytes.size)
                    else
                        mFingerprintTwoFMDTemplate = bytes.copyOf(bytes.size)

                    /* If both templates have been created then enable Match button. */
                    if (mFingerprintOneFMDTemplate != null && mFingerprintTwoFMDTemplate != null)
                        matchBtn.isEnabled = true
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
    private fun matchFMDTemplates(templateOne: ByteArray?,
                                  templateTwo: ByteArray?) {

        /* Normally one would handle parameter checking, but this API handles it for us. Meaning
         * that if any FMD is invalid it will return the proper score of 0, etc.
         */
        App.BioManager!!.compareFMD(templateOne, templateTwo, ISO_19794_2_2005) { rc: ResultCode,
                                                                                  score: Float ->

            when (rc) {
                OK -> {
                    var matchDecision = "No Match"
                    /* This is how to properly determine a match or not. */
                    if (score !=0f)
                        matchDecision = "Match"

                    fpStatusTextView.text = "Matching complete."
                    infoTextView.text = "Match outcome: $matchDecision"

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

        val res = App.BioManager!!.convertToFMDSync(WSQ,
                ISO_19794_2_2005,
                SYNC_API_TIMEOUT_MS)

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
        val res = App.BioManager!!.convertCCFToFMDSync(CCF,
                400.toShort(),
                500.toShort(),
                100.toShort(),
                100.toShort(),
                SYNC_API_TIMEOUT_MS)

        return if (null != res && OK == res.resultCode) res.FMD else byteArrayOf()
    }

    @Suppress("unused")
    private fun compareFMDSync(FMDOne: ByteArray,
                               FMDTwo: ByteArray): Int {

        val res = App.BioManager!!.compareFMDSync(FMDOne,
                FMDTwo,
                ISO_19794_2_2005,
                SYNC_API_TIMEOUT_MS)
        /* Score may be multiplied by 100 to obtain a percentage in range [0, 100]. */
        return if (null == res) 0 else (res.dissimilarity * 100).toInt()
    }

    @Suppress("unused")
    private fun convertToFMDSync(fingerprintImage: Bitmap): ByteArray {

        val res = App.BioManager!!.convertToFMDSync(fingerprintImage,
                ISO_19794_2_2005,
                SYNC_API_TIMEOUT_MS)

        return if (null != res && OK == res.resultCode) res.FMD else byteArrayOf()
    }

    @Suppress("unused")
    private fun grabFingerprint() {

        App.BioManager!!.grabFingerprint(ScanType.SINGLE_FINGER,
                false,
                object : OnMultiFingerprintGrabbedListener {
                    @SuppressLint("SetTextI18n")
                    override fun onFingerprintGrabbed(resultCode: ResultCode,
                                                      bitmap: Bitmap?,
                                                      bitmaps: Array<Bitmap>,
                                                      filePath: String,
                                                      strings: Array<String>,
                                                      hint: String?) {

                        /* If a valid hint was given then display it for user to see. */
                        if (hint != null && hint.isNotEmpty())
                            fpStatusTextView.text = hint

                        when (resultCode) {
                            /* This code is returned once sensor captures fingerprint image. */
                            OK -> {
                                if (null != bitmap)
                                    fingerOneImageView.setImageBitmap(bitmap)

                                fpStatusTextView.text = "File: $filePath"

                                /* Create template from fingerprint image. */
                                createFMDTemplate(bitmap)
                            }
                            /* This code is returned on every new frame/image from sensor. */
                            INTERMEDIATE -> {
                                /* On every frame, if image preview is available, show it to user. */
                                if (null != bitmap)
                                    fingerOneImageView.setImageBitmap(bitmap)

                                /* This hint is returned if cancelCapture()" or
                                 * "closeFingerprint()" are called while in middle of capture.
                                 */
                                if (hint != null && hint == "Capture Stopped")
                                    setAllComponentEnable(true)
                            }
                            /* This code is returned if sensor fails to capture image. */
                            FAIL -> {
                                setAllComponentEnable(true)
                            }
                        }
                    }

                    override fun onCloseFingerprintReader(resultCode: ResultCode,
                                                          closeReasonCode: CloseReasonCode) {

                        /* This case is already handled by "mFingerprintOpenCloseListener". */
                    }
                })
    }

    @SuppressLint("SetTextI18n")
    @Suppress("unused")
    private fun decompressWSQ(WSQ: ByteArray) {

        App.BioManager!!.decompressWSQ(WSQ) { resultCode, bytes ->
            when {
                OK == resultCode -> {                    fpStatusTextView.text = "WSQ Decompression: SUCCESS"
                    infoTextView.text = "Data: " + HexUtils.toString(bytes)
                }
                INTERMEDIATE == resultCode -> {
                    /* This code is never returned for this API. */
                }
                FAIL == resultCode -> fpStatusTextView.text = "WSQ Decompression: FAIL"
            }
        }
    }
}