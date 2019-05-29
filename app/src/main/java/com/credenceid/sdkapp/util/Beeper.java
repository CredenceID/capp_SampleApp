package com.credenceid.sdkapp.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

/* This class is used to play the "click" sound you hear when doing a capture. */
@SuppressWarnings({"unused"})
public class Beeper {

    private static final String TAG = Beeper.class.getName();
    private static final String CLICK_ASSET = "camera_click.ogg";

    public static void
    click(Context context) {

        new Thread(() -> {
            MediaPlayer mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);

            AssetFileDescriptor click_fd;
            try {
                click_fd = context.getAssets().openFd(CLICK_ASSET);

                mMediaPlayer.setDataSource(click_fd.getFileDescriptor(),
                        click_fd.getStartOffset(),
                        click_fd.getLength());

                mMediaPlayer.prepare();
            } catch (IOException e) {
                Log.w(TAG, "click(): Unable to play media file.");
            }

            mMediaPlayer.start();
        }).start();
    }
}