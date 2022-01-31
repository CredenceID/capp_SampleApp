package com.credenceid.sdkapp

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.credenceid.biometrics.Biometrics.ResultCode
import com.credenceid.biometrics.Biometrics.ResultCode.*
import com.credenceid.biometrics.BiometricsManager
import com.util.HexUtils
import kotlinx.android.synthetic.main.act_device_info.*
import kotlinx.android.synthetic.main.act_main.*
import java.util.*


class DeviceInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_device_info)

        this.configureLayoutComponents()
    }

    /**
     * Configure all objects in layout file, set up listeners, views, etc.
     */
    private fun configureLayoutComponents() {

        getHardwareIdentifierBtn.setOnClickListener {

            Log.d("CIDTEST", "Current time = " + System.currentTimeMillis()/1000/60);

            App.BioManager!!.getDeviceHardwareIdentifiers(){
                serialNumber: String,
                imei: String,
                androidId: String,
                wifiMac: String,
                btMac: String,
                simIccId: String ->

                val strResult = String.format(Locale.ENGLISH,
                        "Serial Number: %s\n" +
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
                        simIccId)

                statusTextView.text = strResult

            }
        }

    }


}