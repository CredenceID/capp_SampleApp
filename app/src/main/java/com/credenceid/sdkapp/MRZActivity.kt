@file:Suppress("unused")

package com.credenceid.sdkapp

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.credenceid.biometrics.Biometrics.*
import com.credenceid.biometrics.Biometrics.ResultCode.*
import com.credenceid.icao.ICAODocumentData
import com.credenceid.icao.ICAOReadIntermediateCode
import com.credenceid.sdkapp.databinding.ActMrzBinding

/**
 * Used for Android Logcat.
 */
private val TAG = MRZActivity::class.java.simpleName

/**
 * MRZ reader returns one giant string of data back. Once user splits this string by space
 * delimiter they are supposed to have ten elements. This constant can be used to confirm
 * that appropriate data was read.
 */
private const val MRZ_DATA_COUNT = 10

/**
 * If a document is present on either MRZ/EPassport sensor then C-Service returns this code in
 * sensors respective callback.
 */
private const val DOCUMENT_PRESENT_CODE = 2

/**
 * Once MRZ data is received and split, there are ten different sections. Each sections
 * corresponds with an index in split array.
 */
private const val DATE_OF_BIRTH = 0
private const val EXPIRATION = 1
private const val ISSUER = 2
private const val DOCUMENT_TYPE = 3
private const val LAST_NAME = 4
private const val FIRST_NAME = 5
private const val NATIONALITY = 6
private const val DISCRETIONARY = 7
private const val DISCRETIONARY_TWO = 8
private const val DOCUMENT_NUMBER = 9
private const val GENDER = 10

private var docNumber = ""
private var dateOfBirth = ""
private var dateOfExp = ""

/**
 * These keep track of MRZ/EPassport sensor states. These are used to regulate button enables
 * and handle branches in functionality.
 */
private var isMRZOpen = false
private var isEPassportOpen = false
private var hasMRZData = false
private var isDocPresentOnEPassport = false

class MRZActivity : Activity() {

    private lateinit var binding: ActMrzBinding

    /**
     * "readICAOBtn" should only be enabled if three conditions are all met.
     * 1. EPassport is open.
     * 2. MRZ has been read and document number, D.O.B., and D.O.E. have been captured
     * 3. A document is present on EPassport sensor.
     */

    private val mrzReadListener = OnMRZReaderListener { resultCode, _, _, _, parsedData ->
        when (resultCode!!) {
            OK -> {
                /* Once data is read, it is auto parsed and returned as one big string of data. */
                if (null == parsedData || parsedData.isEmpty()) {
                    binding.statusTextView.text = getString(R.string.mrz_failed_reswipe)
                    return@OnMRZReaderListener
                }

                /* Each section of data is separated by a "\r\n" character. If we split this data
                 * up, we should have TEN elements of data. Please see the constants defined at top
                 * of this class to see the different pieces of information MRZ contains and their
                 * respective indexes.
                 */
                val splitData = parsedData.split("\r\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (splitData.size < MRZ_DATA_COUNT) {
                    binding.statusTextView.text = getString(R.string.mrz_failed_reswipe)
                    return@OnMRZReaderListener
                }

                dateOfBirth = splitData[DATE_OF_BIRTH]
                    .substring(splitData[DATE_OF_BIRTH].indexOf(":") + 1)
                dateOfExp = splitData[EXPIRATION]
                    .substring(splitData[EXPIRATION].indexOf(":") + 1)
                docNumber = splitData[DOCUMENT_NUMBER]
                    .substring(splitData[DOCUMENT_NUMBER].indexOf(":") + 1)

                val issuer = splitData[ISSUER].substring(splitData[ISSUER].indexOf(":") + 1)
                val docType = splitData[DOCUMENT_TYPE]
                    .substring(splitData[DOCUMENT_TYPE].indexOf(":") + 1).replace("\\s+".toRegex(), "")
                val discretionary = splitData[DISCRETIONARY]
                    .substring(splitData[DISCRETIONARY].indexOf(":") + 1)

                /* Only for Senegal Identity cards is document number split into discretionary. */
                if (issuer == "SEN" && docType == "I" && discretionary.matches(".*\\d+.*".toRegex())) {
                    var tmp = discretionary.replace("<".toRegex(), "")
                    if (tmp.length >= 8)
                        tmp = tmp.substring(0, 8)
                    docNumber += tmp
                }

                binding.statusTextView.text = getString(R.string.mrz_read_success)
                binding.icaoTextView.text = parsedData
                hasMRZData = true

            }

            INTERMEDIATE -> binding.statusTextView.text = getString(R.string.mrz_reading_wait)
            FAIL -> {
                binding.statusTextView.text = getString(R.string.mrz_failed_reswipe)
                hasMRZData = false
            }

            else -> {
            }
        }
    }

    /**
     * Callback invoked whenever a document status change is detected on MRZ sensor.
     */
    private val onMRZDocumentStatusListener = OnMRZDocumentStatusListener { _, currState ->

        Log.d("MTEST", "MRZ - Current state = " + currState);
        /* If currentState is two, then document is present. */
        if (DOCUMENT_PRESENT_CODE == currState) {
            binding.statusTextView.text = getString(R.string.mrz_reading_wait)

            /* If current state is 2, then a document is present on MRZ reader. If a document
             * is present we must read it to obtain MRZ field data. Call "readMRZ" to read document.
             *
             * When MRZ is read this callback is invoked "mOnMRZReadListener".
             */
            App.BioManager!!.readMRZ(mrzReadListener)
        }
    }

    /**
     * Callback invoked each time sensor detects a document change from EPassport reader.
     */
    private val ePassportCardStatusListener = OnEPassportStatusListener { _, currState ->


        Log.d("MTEST", "RFID - Current state = " + currState);

        /* If currentState is not 2, then no document is present. */
        if (DOCUMENT_PRESENT_CODE != currState)
            isDocPresentOnEPassport = false
        else {
            isDocPresentOnEPassport = true

            /* Only if remaining other conditions (1 & 2) are met should button be enabled. */
            binding.readICAOBtn.isEnabled = hasMRZData && isEPassportOpen
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActMrzBinding.inflate(layoutInflater)
        setContentView(binding.root)
        this.configureLayoutComponents()
    }

    override fun onDestroy() {

        super.onDestroy()

        /* Make sure to close all peripherals on application exit. */
        App.BioManager!!.ePassportCloseCommand()
        App.BioManager!!.closeMRZ()
    }

    /**
     * Configure all objects in layout file, set up listeners, views, etc.
     */
    private fun configureLayoutComponents() {

        binding.openMRZBtn.isEnabled = true
        binding.openMRZBtn.text = getString(R.string.open_mrz)
        binding.openMRZBtn.setOnClickListener {
            /* Based on current state of MRZ reader take appropriate action. */
            if (!isMRZOpen)
                openMRZReader()
            else {
                App.BioManager!!.closeMRZ()
                App.BioManager!!.ePassportCloseCommand()
            }
        }

        binding.openEPassBtn.isEnabled = false
        binding.openEPassBtn.text = getString(R.string.open_epassport)
        binding.openEPassBtn.setOnClickListener {
            /* Based on current state of EPassport reader take appropriate action. */
            if (!isEPassportOpen)
                openEPassportReader()
            else App.BioManager!!.ePassportCloseCommand()
        }

        binding.readICAOBtn.isEnabled = false
        binding.readICAOBtn.setOnClickListener {
            binding.icaoDG2ImageView.setImageBitmap(null)
            this.readICAODocument(dateOfBirth, docNumber, dateOfExp)
        }
    }

    /**
     * Calls Credence APIs to open MRZ reader.
     */
    private fun openMRZReader() {

        binding.icaoDG2ImageView.setImageBitmap(null)
        binding.statusTextView.text = getString(R.string.mrz_opening)

        /* Register a listener that will be invoked each time MRZ reader's status changes. Meaning
         * that anytime a document is placed/removed invoke this callback.
         */
        App.BioManager!!.registerMRZDocumentStatusListener(onMRZDocumentStatusListener)

        /* Once our callback is registered we may now open the reader. */
        App.BioManager!!.openMRZ(object : MRZStatusListener {
            override fun onMRZOpen(resultCode: ResultCode) {
                /* This code is returned once sensor has fully finished opening. */
                when (resultCode) {
                    OK -> {
                        /* Now that sensor is open, if user presses "openMRZBtn" sensor should
                         * close. To achieve this we change flag which controls what action button
                         * will take.
                         */
                        isMRZOpen = true

                        binding.statusTextView.text = getString(R.string.mrz_opened)
                        binding.openMRZBtn.text = getString(R.string.close_mrz)
                        binding.openEPassBtn.isEnabled = true
                    }
                    /* This code is returned while sensor is in the middle of opening. */
                    INTERMEDIATE -> {
                    }
                    /* This code is returned if sensor fails to open. */
                    FAIL -> binding.statusTextView.text = getString(R.string.mrz_open_failed)
                    else -> {
                    }
                }
            }

            override fun onMRZClose(
                resultCode: ResultCode,
                closeReasonCode: CloseReasonCode
            ) {


                Log.d(TAG, "onMRZClose - result = " + resultCode.name)

                when (resultCode) {
                    OK -> {
                        /* Now that sensor is closed, if user presses "openMRZBtn" sensor should
                         * open. To achieve this we change flag which controls what action button
                         * will take.
                         */
                        isMRZOpen = false

                        binding.statusTextView.text = getString(R.string.mrz_closed)
                        binding.openMRZBtn.text = getString(R.string.open_mrz)

                        binding.openEPassBtn.isEnabled = false
                        binding.openEPassBtn.text = getString(R.string.open_epassport)

                    }
                    /* This code is never returned for this API. */
                    INTERMEDIATE -> {
                    }

                    FAIL -> binding.statusTextView.text = getString(R.string.mrz_failed_close)
                    else -> {
                    }
                }
            }
        })
    }

    /**
     * Calls Credence APIs to open EPassport reader.
     */
    private fun openEPassportReader() {

        binding.statusTextView.text = getString(R.string.epassport_opening)

        /* Register a listener will be invoked each time EPassport reader's status changes. Meaning
         * that anytime a document is placed/removed invoke this callback.
         */
        App.BioManager!!.registerEPassportStatusListener(ePassportCardStatusListener)

        /* Once our callback is registered we may now open the reader. */
        App.BioManager!!.ePassportOpenCommand(object : EPassportReaderStatusListener {
            override fun onEPassportReaderOpen(resultCode: ResultCode) {

                when (resultCode) {
                    /* This code is returned once sensor has fully finished opening. */
                    OK -> {
                        /* Now that sensor is open, if user presses "openEPassBtn" sensor should
                         * close. To achieve this we change flag which controls what action button
                         * will take.
                         */
                        isEPassportOpen = true

                        binding.openEPassBtn.text = getString(R.string.close_epassport)
                        binding.statusTextView.text = getString(R.string.epassport_opened)
                    }
                    /* This code is returned while sensor is in the middle of opening. */
                    INTERMEDIATE -> {
                        /* Do nothing while operation is still on-going. */
                    }
                    /* This code is returned if sensor fails to open. */
                    FAIL ->
                        binding.statusTextView.text = getString(R.string.epassport_open_failed)

                    else -> {
                    }
                }
            }

            override fun onEPassportReaderClosed(
                resultCode: ResultCode,
                closeReasonCode: CloseReasonCode
            ) {

                when (resultCode) {
                    OK -> {
                        /* Now that sensor is closed, if user presses "openEPassBtn" sensor should
                          * open. To achieve this we change flag which controls what action button
                          * will take.
                          */
                        isEPassportOpen = false

                        binding.readICAOBtn.isEnabled = false
                        binding.openEPassBtn.isEnabled = true
                        binding.openEPassBtn.text = getString(R.string.open_epassport)
                        binding.statusTextView.text = getString(R.string.epassport_closed)
                    }
                    /* This code is never returned for this API. */
                    INTERMEDIATE -> {
                    }

                    FAIL -> binding.statusTextView.text = getString(R.string.mrz_failed_close)
                    else -> {
                    }
                }
            }
        })
    }

    /**
     * Calls Credence APIs to read an ICAO document.
     *
     * @param dateOfBirth Date of birth on ICAO document (YYMMDD format).
     * @param documentNumber Document number of ICAO document.
     * @param dateOfExpiry Date of expiry on ICAO document (YYMMDD format).
     */
    @SuppressLint("SetTextI18n")
    private fun readICAODocument(
        dateOfBirth: String?,
        documentNumber: String?,
        dateOfExpiry: String?
    ) {

        /* If any one of three parameters is bad then do not proceed with document reading. */
        if (null == dateOfBirth || dateOfBirth.isEmpty()) {
            Log.w(TAG, "DateOfBirth parameter INVALID, will not read ICAO document.")
            return
        }
        if (null == documentNumber || documentNumber.isEmpty()) {
            Log.w(TAG, "DocumentNumber parameter INVALID, will not read ICAO document.")
            return
        }
        if (null == dateOfExpiry || dateOfExpiry.isEmpty()) {
            Log.w(TAG, "DateOfExpiry parameter INVALID, will not read ICAO document.")
            return
        }

        Log.d(TAG, "Reading ICAO document: $dateOfBirth, $documentNumber, $dateOfExpiry")

        /* Disable button so user does not initialize another readICAO document API call. */
        binding.readICAOBtn.isEnabled = false
        binding.statusTextView.text = getString(R.string.reading)

        App.BioManager!!.readICAODocument(dateOfBirth, documentNumber, dateOfExpiry)
        { rc: ResultCode, stage: ICAOReadIntermediateCode, hint: String?, data: ICAODocumentData ->

            Log.d(TAG, "STAGE: " + stage.name + ", Status: " + rc.name + "Hint: $hint")
            Log.d(TAG, "ICAODocumentData: $data")

            binding.statusTextView.text = "Finished reading stage: " + stage.name
            if (ICAOReadIntermediateCode.BAC == stage) {
                /* If on BAC stage and it FAILS, then reading is done
                 * Re-enable button if:
                 *
                 * 1. Sensor is open.
                 * 2. MRZ data is valid.
                 * 3. Document is still present.
                 */
                if (FAIL == rc) {
                    binding.statusTextView.text = getString(R.string.bac_failed)
                    binding.readICAOBtn.isEnabled = (isEPassportOpen && hasMRZData && isDocPresentOnEPassport)
                }

            } else if (ICAOReadIntermediateCode.DG1 == stage) {
                if (OK == rc)
                    binding.icaoTextView.text = data.DG1.toString()

            } else if (ICAOReadIntermediateCode.DG2 == stage) {
                if (OK == rc) {
                    binding.icaoTextView.text = data.DG2.toString()
                    binding.icaoDG2ImageView.setImageBitmap(data.DG2.faceImage)
                }

            } else if (ICAOReadIntermediateCode.DG3 == stage) {
                if (OK == rc)
                    binding.icaoTextView.text = data.DG3.toString()

            } else if (ICAOReadIntermediateCode.DG7 == stage) {
                if (OK == rc)
                    binding.icaoTextView.text = data.DG7.toString()

            } else if (ICAOReadIntermediateCode.DG11 == stage) {
                if (OK == rc)
                    binding.icaoTextView.text = data.DG1.toString()

            } else if (ICAOReadIntermediateCode.DG12 == stage) {
                if (OK == rc)
                    binding.icaoTextView.text = data.DG12.toString()

                binding.statusTextView.text = getString(R.string.icao_done)

                /* Once this code is returned that means reading is finished.
                 * Re-enable button if:
                 *
                 * 1. Sensor is open.
                 * 2. MRZ data is valid.
                 * 3. Document is still present.
                 */
                binding.readICAOBtn.isEnabled = (isEPassportOpen
                    && hasMRZData
                    && isDocPresentOnEPassport)
            }
        }
    }
}
