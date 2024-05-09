package com.credenceid.sdkapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.credenceid.sdkapp.databinding.ActDeviceInfoBinding
import java.util.Locale


class DeviceInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActDeviceInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActDeviceInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this.configureLayoutComponents()
    }

    /**
     * Configure all objects in layout file, set up listeners, views, etc.
     */
    private fun configureLayoutComponents() {

        binding.getHardwareIdentifierBtn.setOnClickListener {
            App.BioManager!!.getDeviceHardwareIdentifiers() { serialNumber: String,
                                                              imei: String,
                                                              androidId: String,
                                                              wifiMac: String,
                                                              btMac: String,
                                                              simIccId: String ->

                val strResult = String.format(
                    Locale.ENGLISH,
                    "Serial Numner: %s\n" +
                        "Imei: %s\n" +
                        "Android ID: %s\n" +
                        "Wifi Mac add.: %s\n" +
                        "Bt Mac add: %s\n" +
                        "Sim IccID: %s\n",
                    serialNumber,
                    imei,
                    androidId,
                    wifiMac,
                    btMac,
                    simIccId
                )

                binding.statusTextView.text = strResult
            }
        }
    }
}
