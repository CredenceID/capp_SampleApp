package com.credenceid.sdkapp

import android.os.Bundle
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.credenceid.biometrics.Biometrics.BarcodeErrorCode
import com.credenceid.biometrics.Biometrics.BarcodeReadEvent
import com.credenceid.biometrics.Biometrics.BarcodeScannerStatus.BUSY
import com.credenceid.biometrics.Biometrics.BarcodeScannerStatus.ERROR
import com.credenceid.biometrics.Biometrics.BarcodeScannerStatus.OPENED
import com.credenceid.biometrics.Biometrics.OnBarcodeReadListener
import com.credenceid.sdkapp.databinding.ActBarcodeBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Demonstrates the TAB4 barcode scanner (Honeywell N6603 engine), one button
 * per API:
 *
 *   Open  -> openBarcodeScanner()   claim + power the engine, arm the
 *                                   hardware trigger buttons
 *   Close -> closeBarcodeScanner()  power down, hardware buttons inert
 *   Scan  -> startBarcodeScan()     soft-trigger one decode session, same as
 *                                   a hardware button pull
 *   Stop  -> stopBarcodeScan()      cancel the decode session (surfaces as
 *                                   onBarcodeReadFailed(CANCELLED))
 *
 * Decoded barcodes arrive through registerOnBarcodeReadListener() — both from
 * Scan and from the hardware trigger buttons — and are prepended to the result
 * card. An empty pull ends with onBarcodeReadFailed(TIMEOUT) after the ~5-6 s
 * engine decode timeout. All callbacks arrive on the main thread.
 */
class BarcodeActivity : AppCompatActivity() {

    private lateinit var binding: ActBarcodeBinding
    private var readCount = 0

    private val readListener = object : OnBarcodeReadListener {
        override fun onBarcodeRead(event: BarcodeReadEvent) {
            showReadResult(event)
            setStatus(getString(R.string.barcode_status_open), R.color.barcode_state_open)
        }

        override fun onBarcodeReadFailed(error: BarcodeErrorCode) {
            setStatus(
                getString(R.string.barcode_read_failed, error.name),
                R.color.barcode_state_open
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActBarcodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this.configureLayoutComponents()
    }

    override fun onDestroy() {
        super.onDestroy()

        App.BioManager!!.unregisterOnBarcodeReadListener()
        App.BioManager!!.closeBarcodeScanner()
    }

    /**
     * Configure all objects in layout file, set up listeners, views, etc.
     */
    private fun configureLayoutComponents() {
        binding.openBtn.setOnClickListener { openScanner() }
        binding.closeBtn.setOnClickListener { closeScanner() }
        binding.scanBtn.setOnClickListener { startScan() }
        binding.stopBtn.setOnClickListener { stopScan() }
        binding.clearBtn.setOnClickListener {
            readCount = 0
            binding.resultTextView.text = getString(R.string.barcode_results_hint)
        }
    }

    private fun openScanner() {
        App.BioManager!!.registerOnBarcodeReadListener(readListener)
        App.BioManager!!.openBarcodeScanner { status ->
            when (status) {
                OPENED -> {
                    setStatus(
                        getString(R.string.barcode_status_open),
                        R.color.barcode_state_open
                    )
                    setScanControlsEnabled(true)
                }
                BUSY -> setStatus(
                    getString(R.string.barcode_status_busy),
                    R.color.barcode_state_scanning
                )
                ERROR -> setStatus(getString(R.string.barcode_status_error), R.color.red)
                else -> {
                }
            }
        }
    }

    private fun closeScanner() {
        App.BioManager!!.closeBarcodeScanner()
        setStatus(getString(R.string.barcode_status_closed), R.color.barcode_state_closed)
        setScanControlsEnabled(false)
    }

    private fun startScan() {
        App.BioManager!!.startBarcodeScan()
        setStatus(getString(R.string.barcode_status_scanning), R.color.barcode_state_scanning)
    }

    private fun stopScan() {
        App.BioManager!!.stopBarcodeScan()
    }

    private fun showReadResult(event: BarcodeReadEvent) {
        readCount++
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(event.timestamp))
        val entry = getString(
            R.string.barcode_read_entry,
            readCount,
            time,
            event.symbology,
            event.barcodeData,
            event.charset,
            event.barcodeDataBytes.size
        )

        /* Newest read on top; first read replaces the placeholder hint. */
        val previous = binding.resultTextView.text.toString()
            .takeUnless { it == getString(R.string.barcode_results_hint) } ?: ""
        binding.resultTextView.text = (entry + previous)
    }

    /**
     * Scan, Stop, and Clear only make sense while the scanner is open; they are
     * greyed out until openBarcodeScanner() reports OPENED.
     */
    private fun setScanControlsEnabled(enabled: Boolean) {
        binding.scanBtn.isEnabled = enabled
        binding.stopBtn.isEnabled = enabled
        binding.clearBtn.isEnabled = enabled
    }

    private fun setStatus(text: String, @ColorRes dotColor: Int) {
        binding.statusTextView.text = text
        binding.statusDotView.background.setTint(ContextCompat.getColor(this, dotColor))
    }
}
