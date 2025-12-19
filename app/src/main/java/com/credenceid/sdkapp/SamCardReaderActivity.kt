package com.credenceid.sdkapp

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.credenceid.biometrics.ApduCommand
import com.credenceid.biometrics.Biometrics
import com.credenceid.biometrics.Biometrics.CloseReasonCode
import com.credenceid.biometrics.Biometrics.ResultCode
import com.credenceid.biometrics.Biometrics.ResultCode.*
import com.credenceid.sdkapp.databinding.ActSamcardreaderBinding
import com.util.HexUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

@Suppress("unused")
class SamCardReaderActivity : Activity() {
    private lateinit var binding: ActSamcardreaderBinding

    /**
     * Keeps track of card reader sensor. If true then sensor is open, if false sensor is closed.
     */
    private var isSensorOpen = false

    /**
     * Keeps track of if connection to SAM card is established.
     */
    private var isSamCardConnected = false

    /**
     * Get challenge eID document
     */
    private val selectMfApdu = (
        "00" + //Instruction Class (Standard ISO)
            "A4" + //Instruction Code (SELECT FILE)
            "00" + //Select by File ID
            "00" + //Return First Record / No specific data expected
            "02" + //Length of the data field (2 bytes)
            "3F" + //The File ID for the Master File (Root)
            "00"
        )

    /**
     * APDU executed when "readDataBtn" is clicked. This APDU will change each time a
     * different type of read is selected via "readAPDUSelector".
     */
    private var currentAPDU =  selectMfApdu


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActSamcardreaderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        this.configureLayoutComponents()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        /* If back button is pressed when we want to destroy activity. */
        this.onDestroy()
    }

    override fun onDestroy() {
        super.onDestroy()

        /* Close card reader since user is exiting activity. */
        App.BioManager!!.samCardCloseCommand()
    }

    /**
     * Configure all objects in layout file, set up listeners, views, etc.
     */
    private fun configureLayoutComponents() {
        /* Disable views which allow user to read/write to/from card until card reader is open. */
        this.setReadWriteComponentEnable(false)

        /* This will remove focus from view, meaning keyboard will hide. */
        binding.customApduEditText.clearFocus()

        binding.openCloseBtn.setOnClickListener {
            binding.openCloseBtn.isEnabled = false
            binding.cardStatusTextView.text = ""

            /* Based on status of card reader take appropriate action. */
            if (isSensorOpen) {
                App.BioManager!!.samCardCloseCommand()
            } else {
                this.openSamCardReader()
            }
        }


        binding.connectDisconnectBtn.setOnClickListener {
            binding.cardStatusTextView.text = ""

            /* Based on status of card reader take appropriate action. */
            if (isSamCardConnected) {
                App.BioManager!!.samCardDisconnectCommand()
            } else {
                this.connectToSamCard()
            }
        }

        binding.sendCustomApduBtn.setOnClickListener {
            /* Do not do anything if card is not present. */
            if (!isSamCardConnected) {
                binding.cardReaderStatusTextView.text = getString(R.string.no_card_present_to_write_to)
                return@setOnClickListener
            }

            /* Check to make sure user has entered some valid data to write to card. If nothing
             * exists then do not do anything.
             */
            val data = binding.customApduEditText.text.toString()
            if (mEMPTY_STRING_LEN == data.length) {
                binding.cardReaderStatusTextView.text = getString(R.string.invalid_apdu_to_write_to_card)
                return@setOnClickListener
            }

            /* Disable UI components so they do not interfere with ongoing operation. */
            this.setReadWriteComponentEnable(false)

            /* Based on if user has selected sync/async APIs call appropriate method. */
            if (binding.syncCheckBox.isChecked) {
                sendApduToSamCardSync(data)
            } else {
                sendApduToSamCardAsync(data)
            }
        }

        binding.sendApduBtn.setOnClickListener {
            /* Do not do anything if card is not present. */
            if (!isSamCardConnected) {
                binding.cardReaderStatusTextView.text = getString(R.string.no_card_present_read_from)
                return@setOnClickListener
            }

            /* Disable UI components so they do not interfere with ongoing operation. */
            this.setReadWriteComponentEnable(false)

            /* Based on if user has selected sync/async APIs call appropriate method. */
            if (binding.syncCheckBox.isChecked) {
                sendApduToSamCardSync(currentAPDU)
            } else {
                sendApduToSamCardAsync(currentAPDU)
            }
        }

        binding.customApduEditText.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                hideKeyboard(v)
            }
        }
    }

    /**
     * Hides keyboard for a give view. This is usually used with EditText components.
     */
    private fun hideKeyboard(view: View) {
        try {
            val im = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(view.windowToken, 0)
        } catch (ignore: NullPointerException) {
        }
    }

    /**
     * Calls Credence APIs to open card reader.
     */
    private fun openSamCardReader() {
        /* Let user know card reader will now try to be opened. */
        binding.cardReaderStatusTextView.text = getString(R.string.opening_card_reader)

        App.BioManager!!.samCardOpenCommand(object : Biometrics.CardReaderStatusListener {
            override fun onCardReaderOpen(resultCode: ResultCode) {
                when {
                    OK == resultCode -> {
                        binding.openCloseBtn.isEnabled = true
                        isSensorOpen = true

                        binding.cardReaderStatusTextView.text = getString(R.string.sam_card_reader_opened)
                        binding.openCloseBtn.text = getString(R.string.close_card_reader)

                        binding.connectDisconnectBtn.isEnabled = true

                        /* If card reader opened successfully, register a listener will be invoked
                         * each time card reader's status changes. Meaning that anytime a card is
                         * placed/removed invoke this callback.
                         */
                    }
                    INTERMEDIATE == resultCode -> {
                        /* This code is never returned here. */
                    }
                    FAIL == resultCode -> {
                        binding.openCloseBtn.isEnabled = true
                        binding.cardReaderStatusTextView.text = getString(R.string.failed_open_card_reader)
                        setReadWriteComponentEnable(false)
                        binding.connectDisconnectBtn.isEnabled = false
                    }
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onCardReaderClosed(
                resultCode: ResultCode,
                closeReasonCode: CloseReasonCode
            ) {
                Log.d(TAG, "onCardReaderClosed")
                binding.openCloseBtn.isEnabled = true
                when {
                    OK == resultCode -> {
                        isSensorOpen = false
                        binding.cardReaderStatusTextView.text =
                            getString(R.string.card_reader_closed) + closeReasonCode.name
                        binding.cardStatusTextView.text = ""
                        binding.openCloseBtn.text = getString(R.string.open_card)
                        setReadWriteComponentEnable(false)
                        binding.connectDisconnectBtn.isEnabled = false
                    }
                    INTERMEDIATE == resultCode -> {
                        /* This code is never returned here. */
                    }
                    FAIL == resultCode ->
                        binding.cardReaderStatusTextView.text = getString(R.string.card_reader_fail_close)
                }
            }
        })
    }

    /**
     * Calls Credence APIs to connect card reader.
     */
    @OptIn(ExperimentalStdlibApi::class)
    private fun connectToSamCard() {
        /* Let user know card reader will now try to be opened. */
        binding.cardReaderStatusTextView.text = getString(R.string.connect_sam_card)

        App.BioManager!!.samCardConnectCommand(object : Biometrics.SamCardStatusListener {
            override fun onSamCardConnected(resultCode: ResultCode?, atr: ByteArray?) {
                when {
                    OK == resultCode -> {
                        binding.connectDisconnectBtn.isEnabled = true
                        isSamCardConnected = true

                        binding.cardReaderStatusTextView.text =
                            getString(R.string.sam_present)
                        binding.cardStatusTextView.text = "ATR is ${atr?.toHexString()}"
                        binding.connectDisconnectBtn.text = getString(R.string.disconnect_sam_card_reader)

                        setReadWriteComponentEnable(true)

                        /* If card reader opened successfully, register a listener will be invoked
                     * each time card reader's status changes. Meaning that anytime a card is
                     * placed/removed invoke this callback.
                     */
                    }

                    INTERMEDIATE == resultCode -> {
                        /* This code is never returned here. */
                    }

                    FAIL == resultCode -> {
                        binding.openCloseBtn.isEnabled = true
                        binding.cardReaderStatusTextView.text =
                            getString(R.string.sam_absent)
                    }
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onSamCardDisconnected(
                resultCode: ResultCode,
                closeReasonCode: CloseReasonCode
            ) {
                binding.connectDisconnectBtn.isEnabled = true
                when {
                    OK == resultCode -> {
                        isSamCardConnected = false
                        binding.cardReaderStatusTextView.text =
                            getString(R.string.sam_card_disconnected) + closeReasonCode.name
                        binding.cardStatusTextView.text = ""
                        binding.connectDisconnectBtn.text = getString(R.string.connect_sam_card)
                        setReadWriteComponentEnable(false)
                    }
                    INTERMEDIATE == resultCode -> {
                        /* This code is never returned here. */
                    }
                    FAIL == resultCode ->
                        binding.cardReaderStatusTextView.text = getString(R.string.error)
                }
            }
        })
    }

    /**
     * Set enable for components which allow user to read/write from/to card and form APDUs.
     *
     * @param enabled If true enables components, if false dis-ables them.
     */
    private fun setReadWriteComponentEnable(enabled: Boolean) {
        binding.customApduEditText.isEnabled = enabled
        binding.sendCustomApduBtn.isEnabled = enabled
        binding.sendApduBtn.isEnabled = enabled
    }

    /**
     * This method attempts to send APDU to SAM card
     *
     * @param APDUCommand Initial APDU command to execute.
     */
    private fun sendApduToSamCardAsync(APDUCommand: String) {
        binding.cardReaderStatusTextView.text = getString(R.string.reading_card_wait)

        App.BioManager!!.samCardCommand(ApduCommand(APDUCommand), false) { rc: ResultCode,
            sw1: Byte,
            sw2: Byte,
            data: ByteArray? ->

            when {
                OK == rc -> {

                    val dataToDisplay: String =  if (null != data) HexUtils.toString(data) else ""

                    val str = String.format(
                        Locale.ENGLISH,
                        "SW1: %s, SW2: %s\nLength of data read: %d\n\n %s",
                        HexUtils.toString(sw1),
                        HexUtils.toString(sw2),
                        dataToDisplay.length,
                        dataToDisplay
                    )

                    binding.cardReaderStatusTextView.text = getString(R.string.done_reading_from_card)
                    binding.dataTextView.text = str
                }
                INTERMEDIATE == rc -> {
                    /* This code is never returned here. */
                }
                FAIL == rc -> {
                    binding.cardReaderStatusTextView.text = getString(R.string.done_reading_from_card)
                    binding.dataTextView.text = getString(R.string.apdu_failed)
                }
            }

            this.setReadWriteComponentEnable(true)
        }
    }

    /**
     * This method attempts to send apdu to SAM card.
     *
     * The purpose of this function is to demonstrate how you may execute APDU linearly using sync. APIs.
     */
    private fun sendApduToSamCardSync(APDUCommand: String) {
        binding.cardReaderStatusTextView.text = getString(R.string.reading_card_wait)

        GlobalScope.launch(Dispatchers.Main) {
            val response = App.BioManager!!.samCardCommandSync(ApduCommand(APDUCommand), false, 4000)

            /* If APDU failed then response will be NULL. */
            if (null == response) {
                binding.cardReaderStatusTextView.text = getString(R.string.done_reading_from_card)
                binding.dataTextView.text = getString(R.string.apdu_failed)
                setReadWriteComponentEnable(true)
                return@launch
            }

            val dataToDisplay: String =  if (null != response.data) HexUtils.toString(response.data) else ""

            val str = String.format(
                Locale.ENGLISH,
                "SW1: %s, SW2: %s\nLength of data read: %d\n\n %s",
                HexUtils.toString(response.sw1),
                HexUtils.toString(response.sw2),
                response.data.size,
                dataToDisplay
            )

            binding.cardReaderStatusTextView.text = getString(R.string.done_reading_from_card)
            binding.dataTextView.text = str
            setReadWriteComponentEnable(true)
        }
    }

    companion object {
        private val TAG = SamCardReaderActivity::class.java.simpleName
        private const val mEMPTY_STRING_LEN = 0
        private const val mREAD_SPECIAL_APDU_LEN = 10
        private const val CARD_ABSENT = 1
        private const val CARD_PRESENT = 2
    }
}
