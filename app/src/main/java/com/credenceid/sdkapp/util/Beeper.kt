package com.credenceid.sdkapp.util

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import java.io.IOException

/**
 * This class is used to play the "click" sound you hear when doing a capture.
 */
object Beeper {

    private val TAG = Beeper::class.java.name
    private const val CLICK_ASSET = "camera_click.ogg"

    fun click(context: Context) {

        val mp = MediaPlayer()
        mp.setAudioStreamType(AudioManager.STREAM_ALARM)

        val fd: AssetFileDescriptor
        try {
            fd = context.assets.openFd(CLICK_ASSET)
            mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            mp.prepare()
        } catch (e: IOException) {
            Log.w(TAG, "click(): Unable to play media file.")
        }
        mp.start()
    }
}