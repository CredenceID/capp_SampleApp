package com.credenceid.sdkapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import com.credenceid.biometrics.ApduCommand
import com.credenceid.biometrics.Biometrics
import com.credenceid.biometrics.Biometrics.CloseReasonCode
import com.credenceid.biometrics.Biometrics.ResultCode
import com.credenceid.biometrics.Biometrics.ResultCode.*
import com.util.HexUtils
import kotlinx.android.synthetic.main.act_cardreader.*
import kotlinx.android.synthetic.main.act_cardreader.openCloseBtn
import kotlinx.android.synthetic.main.act_fp.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random.Default.nextInt

@Suppress("unused")
class CardReaderActivity : Activity() {
    /**
     * Keeps track of card reader sensor. If true then sensor is open, if false sensor is closed.
     */
    private var isSensorOpen = false
    /**
     * Keeps track of if card is present on sensor. If true card is present, if false no card is
     * present.
     */
    private var isCardPresent = false

    /**
     * Get challenge eID document
     */
    private val getChallenge = ("00"         // MiFare Card
            + "84"                            // MiFare Card READ Command
            + "00"                            // P1
            + "00"                            // P2: Block Number
            + "08")                       // Number of bytes to read

    /**
     * Reads Mifare card UID.
     */
    private val readUID= ("FF"         // MiFare Card
            + "CA"                            // MiFare Card READ Command
            + "00"                            // P1
            + "00"                          // P2: Block Number
            + "00")                       // Number of bytes to read

    /**
     * Reads 4096 (4K) number of bytes from card.
     */
    private val read4KAPDU = ("00"         // MiFare Card
            + "FF"                            // MiFare Card READ Command
            + "00"                            // P1
            + "00"                            // P2: Block Number
            + "001000")                     // Number of bytes to read

    /**
     * Reads 2048 (2K) number of bytes from card.
     */
    private val read2KAPDU = ("00"         // MiFare Card
            + "FF"                            // MiFare Card READ Command
            + "00"                            // P1
            + "00"                            // P2: Block Number
            + "000800")                       // Number of bytes to read


    /**
     * Reads 1024 (1K) number of bytes from card.
     */
    private val read1KAPDU = ("00"         // MiFare Card
            + "FF"                            // MiFare Card READ Command
            + "00"                            // P1
            + "00"                            // P2: Block Number
            + "000400")                       // Number of bytes to read

    /**
     * This APDU is used to read "specialData" written to the card.
     */
    private var readSpecialDataAPDU = ("FF"  // MiFare Card
            + "B0"                              // MiFare Card READ Command
            + "00"                              // P1
            + "01"                              // P2: Block Number
            + "00")                             // Number of bytes to read

    /**
     * Writes 4096 (4K) number of bytes to card.
     */
    private val write4KAPDU = ("FF"        // MiFare Card
            + "D6"                            // MiFare Card READ Command
            + "00"                            // P1
            + "00"                            // P2: Block Number
            + "001000")                       // Number of bytes to read
    /**
     * Writes 2048 (2K) number of bytes to card.
     */
    private val write2KAPDU = ("FF"        // MiFare Card
            + "D6"                            // MiFare Card READ Command
            + "00"                            // P1
            + "00"                            // P2: Block Number
            + "000800")                       // Number of bytes to read
    /**
     * Writes 1024 (1K) number of bytes to card.
     */
    private val write1KAPDU = ("FF"        // MiFare Card
            + "D6"                            // MiFare Card READ Command
            + "00"                            // P1
            + "00"                            // P2: Block Number
            + "000400")                       // Number of bytes to read
    /**
     * Data to be written to card will be stored here.
     */
    private var specialData: ByteArray = ByteArray(0)

    /**
     * APDU executed when "readDataBtn" is clicked. This APDU will change each time a
     * different type of read is selected via "readAPDUSelector".
     */
    private var currentReadAPDU = read1KAPDU

    /**
     * Callback invoked each time sensor detects a card change.
     */
    private val onCardStatusListener = { _: String, _: Int, currentState: Int ->
        /* If currentState is 1, then no card is present. */
        if (CARD_ABSENT == currentState) {
            isCardPresent = false
            cardStatusTextView.text = getString(R.string.card_absent)
        }
        /* currentStates [2, 6] represent a card present. If a card is present code will reach.
         * Here you may perform any operations you want ran automatically when a card is detected.
         */
        else {
            isCardPresent = true
            cardStatusTextView.text = getString(R.string.card_present)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_cardreader)
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
        App.BioManager!!.cardCloseCommand()
    }

    /**
     * Configure all objects in layout file, set up listeners, views, etc.
     */
    private fun configureLayoutComponents() {

        /* Disable views which allow user to read/write to/from card until card reader is open. */
        this.setReadWriteComponentEnable(false)

        /* This will remove focus from view, meaning keyboard will hide. */
        writeEditText.clearFocus()

        openCloseBtn.setOnClickListener {
            openCloseBtn.isEnabled = false
            cardStatusTextView.text = ""

            /* Based on status of card reader take appropriate action. */
            if (isSensorOpen)
                App.BioManager!!.cardCloseCommand()
            else
                this.openCardReader()
        }

        /* Each time an item is selected we need up update "currentReadAPDU". This is so when user
         * presses "readDataBtn", APDU which matches  selected option has already been set.
         */
        readAPDUSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>,
                                        view: View,
                                        position: Int,
                                        id: Long) {

                when (position) {
                    0 -> currentReadAPDU = readSpecialDataAPDU
                    1 -> currentReadAPDU = readUID
                    2 -> currentReadAPDU = read1KAPDU
                    3 -> currentReadAPDU = read2KAPDU
                    4 -> currentReadAPDU = read4KAPDU
                    5 -> currentReadAPDU = getChallenge
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        writeDataBtn.setOnClickListener {
            /* Do not do anything if card is not present. */
            if (!isCardPresent) {
                cardReaderStatusTextView.text = getString(R.string.no_card_present_to_write_to)
                return@setOnClickListener
            }

            /* Check to make sure user has entered some valid data to write to card. If nothing
             * exists then do not do anything.
             */
            val data = writeEditText.text.toString()
            if (mEMPTY_STRING_LEN == data.length) {
                cardReaderStatusTextView.text = getString(R.string.no_data_to_write_to_card)
                return@setOnClickListener
            }

            /* Disable UI components so they do not interfere with ongoing operation. */
            this.setReadWriteComponentEnable(false)

            /* Save data to write inside our global variable. */
            specialData = data.toByteArray()

            /* Based on if user has selected sync/async APIs call appropriate method. */
            if (syncCheckBox.isChecked)
                writeCardSync(specialData)
            else
                writeCardAsync(specialData)
        }

        readDataBtn.setOnClickListener {
            /* Do not do anything if card is not present. */
            if (!isCardPresent) {
                cardReaderStatusTextView.text = getString(R.string.no_card_present_read_from)
                return@setOnClickListener
            }

            /* Disable UI components so they do not interfere with ongoing operation. */
            this.setReadWriteComponentEnable(false)

            /* Based on if user has selected sync/async APIs call appropriate method. */
            if (syncCheckBox.isChecked)
                readCardSync(currentReadAPDU)
            else
                readCardAsync(currentReadAPDU)
        }

        writeEditText.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus)
                hideKeyboard(v)
        }

    }

    /**
     * Hides keyboard for a give view. This is usually used with EditText components.
     */
    private fun hideKeyboard(view: View) {

        try {
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(view.windowToken, 0)
        } catch (ignore: NullPointerException) {
        }
    }

    /**
     * Calls Credence APIs to open card reader.
     */
    private fun openCardReader() {

        /* Let user know card reader will now try to be opened. */
        cardReaderStatusTextView.text = getString(R.string.opening_card_reader)

        App.BioManager!!.cardOpenCommand(object : Biometrics.CardReaderStatusListener {
            override fun onCardReaderOpen(resultCode: ResultCode) {
                when {
                    OK == resultCode -> {
                        openCloseBtn.isEnabled = true
                        isSensorOpen = true

                        cardReaderStatusTextView.text = getString(R.string.card_reader_opened)
                        openCloseBtn.text = getString(R.string.close_card_reader)

                        setReadWriteComponentEnable(true)

                        /* If card reader opened successfully, register a listener will be invoked
                         * each time card reader's status changes. Meaning that anytime a card is
                         * placed/removed invoke this callback.
                         */
                        App.BioManager!!.registerCardStatusListener(onCardStatusListener)
                    }
                    INTERMEDIATE == resultCode -> {
                        /* This code is never returned here. */
                    }
                    FAIL == resultCode -> {
                        openCloseBtn.isEnabled = true
                        cardReaderStatusTextView.text = getString(R.string.failed_open_card_reader)
                    }
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onCardReaderClosed(resultCode: ResultCode,
                                            closeReasonCode: CloseReasonCode) {

                openCloseBtn.isEnabled = true
                when {
                    OK == resultCode -> {
                        isSensorOpen = false
                        cardReaderStatusTextView.text =
                                getString(R.string.card_reader_closed) + closeReasonCode.name
                        cardStatusTextView.text = ""
                        openCloseBtn.text = getString(R.string.open_card)
                        setReadWriteComponentEnable(false)
                    }
                    INTERMEDIATE == resultCode -> {
                        /* This code is never returned here. */
                    }
                    FAIL == resultCode ->
                        cardReaderStatusTextView.text = getString(R.string.card_reader_fail_close)
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

        writeEditText.isEnabled = enabled
        writeDataBtn.isEnabled = enabled
        readAPDUSelector.isEnabled = enabled
        readDataBtn.isEnabled = enabled
    }

    /**
     * This method attempts to read 1K, 2K, and 4K blocks of data off of a card. You pass in either
     * one of three mAPDURead(4|2|1)k commands. It will execute this command then try the next size
     * block.
     *
     * ie. If you pass 1K command, it will next try the 2K then 4K.
     * ie. If you pass 2K command, it will next try 4K.
     * ie. If you pass 4K after command is done, it will do nothing.
     *
     * The purpose of this function is to demonstrate how you may execute multiple APDUs linearly
     * using async. APIs. The core of this is a "state machine".
     *
     * @param APDUCommand Initial APDU command to execute.
     */
    private fun readCardAsync(APDUCommand: String) {

        cardReaderStatusTextView.text = getString(R.string.reading_card_wait)

        App.BioManager!!.cardCommand(ApduCommand(APDUCommand), false) { rc: ResultCode,
                                                                        sw1: Byte,
                                                                        sw2: Byte,
                                                                        data: ByteArray? ->

            when {
                OK == rc -> {
                    var dataToDisplay: String
                    dataToDisplay = ""
                    /* If data read was equal to special data then convert each byte into human
                     * understandable text, ASCII chars.
                     */
                    if (mREAD_SPECIAL_APDU_LEN == currentReadAPDU.length) {
                        dataToDisplay = ""
                        /* Convert read data into human readable ASCII characters. */
                        if (data != null) {
                            for (aData in data)
                                dataToDisplay += aData.toChar()
                        }
                    } else {
                        /* If non special data was read then simply convert to String format. */
                        if (data != null)
                            dataToDisplay = HexUtils.toString(data)
                    }

                    val str = String.format(Locale.ENGLISH,
                            "SW1: %s, SW2: %s\nLength of data read: %d\n\n %s",
                            HexUtils.toString(sw1),
                            HexUtils.toString(sw2),
                            dataToDisplay.length,
                            dataToDisplay)

                    cardReaderStatusTextView.text = getString(R.string.done_reading_from_card)
                    dataTextView.text = str
                }
                INTERMEDIATE == rc -> {
                    /* This code is never returned here. */
                }
                FAIL == rc -> {
                    cardReaderStatusTextView.text = getString(R.string.done_reading_from_card)
                    dataTextView.text = getString(R.string.apdu_failed)
                }
            }

            this.setReadWriteComponentEnable(true)
        }
    }

    /**
     * This method attempts to read 1K, 2K, and 4K blocks of data off of a card. It will attempts to
     * read each block of data linearly. It will first try 1K, then 2K, then 4K card reads.
     *
     * The purpose of this function is to demonstrate how you may execute multiple APDUs linearly
     * using sync. APIs.
     */
    private fun readCardSync(APDUCommand: String) {

        cardReaderStatusTextView.text = getString(R.string.reading_card_wait)

        GlobalScope.launch(Dispatchers.Main) {
            val response = App.BioManager!!.cardCommandSync(ApduCommand(APDUCommand), false, 4000)

            /* If APDU failed then response will be NULL. */
            if (null == response) {
                cardReaderStatusTextView.text = getString(R.string.done_reading_from_card)
                dataTextView.text = getString(R.string.apdu_failed)
                setReadWriteComponentEnable(true)
                return@launch
            }

            var dataToDisplay: String

            /* If data read was special data then convert each read byte into human understandable
             * text, ASCII chars.
             */
            if (mREAD_SPECIAL_APDU_LEN == currentReadAPDU.length) {
                dataToDisplay = ""
                /* Convert read data into human readable ASCII characters. */
                for (i in response.data.indices)
                    dataToDisplay += response.data[i].toChar()
            } else {
                /* If non special data was read then simply convert to String format. */
                dataToDisplay = HexUtils.toString(response.data)
            }

            val str = String.format(Locale.ENGLISH,
                    "SW1: %s, SW2: %s\nLength of data read: %d\n\n %s",
                    HexUtils.toString(response.sw1),
                    HexUtils.toString(response.sw2),
                    response.data.size,
                    dataToDisplay)

            cardReaderStatusTextView.text = getString(R.string.done_reading_from_card)
            dataTextView.text = str
            setReadWriteComponentEnable(true)
        }
    }

    /**
     * This method attempts to write some data to MiFare card.
     *
     * @param data, Data to write to card in String format.
     */
    private fun writeCardSync(data: String) {

        this.writeCardSync(data.toByteArray())
    }

    /**
     * This method attempts to write some data to MiFare card.
     *
     * @param dataToWrite, Data to write to card in byte array format.
     */
    private fun writeCardSync(dataToWrite: ByteArray) {

        GlobalScope.launch(Dispatchers.Main) {
            val apdu = createWriteAPDUCommand(0x01.toByte(), dataToWrite)
            val response = App.BioManager!!.cardCommandSync(ApduCommand(apdu), false, 4000)

            /* If APDU failed then response will be NULL. */
            if (null == response) {
                cardReaderStatusTextView.text = getString(R.string.done_writing_to_card)
                dataTextView.text = getString(R.string.apdu_failed)
                setReadWriteComponentEnable(true)
                return@launch
            }

            val str = String.format(Locale.ENGLISH,
                    "SW1: %s, SW2: %s",
                    HexUtils.toString(response.sw1),
                    HexUtils.toString(response.sw2))

            cardReaderStatusTextView.text = getString(R.string.done_writing_to_card)
            dataTextView.text = str
            setReadWriteComponentEnable(true)

            /* If a write was successful we should then update "readSpecialDataAPDU" so that it
             * will same number of bytes that were written.
             */
            updateReadSpecialAPDU()
        }
    }

    /**
     * This method attempts to write some data to MiFare card. After writing data it will then try
     * to read that same data back.
     */
    private fun writeCardAsync(dataToWrite: ByteArray) {

        val apdu = createWriteAPDUCommand(0x01.toByte(), dataToWrite)

        App.BioManager!!.cardCommand(ApduCommand(apdu), false) { rc: ResultCode,
                                                                 sw1: Byte,
                                                                 sw2: Byte,
                                                                 _: ByteArray ->

            when {
                OK == rc -> {
                    val str = String.format(Locale.ENGLISH,
                            "SW1: %s, SW2: %s",
                            HexUtils.toString(sw1),
                            HexUtils.toString(sw2))

                    cardReaderStatusTextView.text = getString(R.string.done_writing_to_card)
                    dataTextView.text = str

                    this.setReadWriteComponentEnable(true)

                    /* If a write was successful we should then update "readSpecialDataAPDU" so that
                     * it will same number of bytes that were written.
                     */
                    this.updateReadSpecialAPDU()
                }
                INTERMEDIATE == rc -> {
                    /* This code is never returned here. */
                }
                FAIL == rc -> {
                    cardReaderStatusTextView.text = getString(R.string.done_writing_to_card)
                    dataTextView.text = getString(R.string.apdu_failed)
                    this.setReadWriteComponentEnable(true)
                }
            }
        }
    }

    /**
     * Creates an APDU command for writing data to a MiFare card.
     *
     * @param blockNumber, Block number to write data to.
     * @param data, Data to write to card.
     * @return APDU command in String format.
     */
    private fun createWriteAPDUCommand(@Suppress("SameParameterValue") blockNumber: Byte,
                                       data: ByteArray): String {

        val dataLen = data.size

        /* 7 MiFare bytes, 2 Data size bytes, CID header bytes+ data */
        val writeAPDU = ByteArray(7 + dataLen)

        writeAPDU[0] = 0xFF.toByte()                        // MiFare Card Header
        writeAPDU[1] = 0xD6.toByte()                        // MiFare Card WRITE Command
        writeAPDU[2] = 0x00.toByte()                        // P1
        writeAPDU[3] = blockNumber                        // P2: Block Number
        writeAPDU[4] = 0x00.toByte()                        // Escape Character
        writeAPDU[5] = (dataLen shr 8 and 0xFF).toByte()     // Number of bytes: MSB
        writeAPDU[6] = (dataLen and 0xFF).toByte()            // Number of bytes: LSB

        /* Append "data" to end of "writeAPDU" byte array. */
        System.arraycopy(data, 0, writeAPDU, 7, dataLen)

        /* Return "writeAPDU" as a String. */
        return HexUtils.toString(writeAPDU)
    }

    /**
     * After writing "special" data to a card we also want to be able to read it back. This method
     * will update special APDU read command to read data that was last written to card.
     */
    private fun updateReadSpecialAPDU() {

        readSpecialDataAPDU = ("FF"   // MiFare Card
                + "B0"                // MiFare Card READ Command
                + "00"                // P1
                + "01")               // P2: Block Number
        readSpecialDataAPDU += HexUtils.toString(specialData.size.toByte())

        /* If user has selected special read APDU, then we need to update it also. */
        if (mREAD_SPECIAL_APDU_LEN == currentReadAPDU.length)
            currentReadAPDU = readSpecialDataAPDU

    }

    companion object {
        private val TAG = CardReaderActivity::class.java.simpleName
        private const val mEMPTY_STRING_LEN = 0
        private const val mREAD_SPECIAL_APDU_LEN = 10
        private const val CARD_ABSENT = 1
        private const val CARD_PRESENT = 2
    }

}