package com.credenceid.sdkapp.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.Log;

import com.credenceid.sdkapp.TheApp;

import java.io.IOException;

/* This class is used to play the "click" sound you hear when doing a Fingerprint capture. This
 * class a custom Audio file we have placed inside the <assets/> directory.
 */
public class Beeper {
    private final static String CLICK_ASSET = "camera_click.ogg";
    private static Beeper instance;
    private final String TAG = Beeper.class.getSimpleName();
    private final int beepDurationInSeconds = 1;
    private final int sampleRate = 8000;
    private final int sampleCount = beepDurationInSeconds * sampleRate;
    private int audioStreamType = AudioManager.STREAM_ALARM;
    private double beepSamples[];
    private byte generatedBeep[];
    private MediaPlayer soundPlayer = null;

    private Beeper() {
    }

    public static Beeper getInstance() {
        if (instance == null) instance = new Beeper();
        return instance;
    }

    @SuppressWarnings("unused")
    public void beep() {
        if (this.beepSamples == null)
            createBeepSamples();

        final AudioTrack audioTrack = new AudioTrack(this.audioStreamType,
                this.sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                this.sampleCount,
                AudioTrack.MODE_STATIC);

        audioTrack.write(this.generatedBeep, 0, this.generatedBeep.length);
        audioTrack.play();
    }

    @SuppressWarnings("unused")
    public void releaseBeeper() {
        if (this.soundPlayer != null) {
            this.soundPlayer.release();
            this.soundPlayer = null;
        }
    }

    private void createBeepSamples() {
        final double TONE_FREQUENCY_HZ = 880;

        this.beepSamples = new double[this.sampleCount];
        this.generatedBeep = new byte[2 * this.sampleCount];

        for (int i = 0; i < this.sampleCount; ++i)
            this.beepSamples[i] =
                    Math.sin(2 * Math.PI * i / (this.sampleRate / TONE_FREQUENCY_HZ));

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;

        for (final double dVal : this.beepSamples) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            this.generatedBeep[idx++] = (byte) (val & 0x00ff);
            this.generatedBeep[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }

    public void click() {
        Log.d(TAG, "Beeper::click");
        try {
            if (this.soundPlayer == null) {
                final AudioManager mgr = (AudioManager) TheApp.getAppContext()
                        .getSystemService(Context.AUDIO_SERVICE);

                if (mgr != null) mgr.setSpeakerphoneOn(true);

                this.soundPlayer = new MediaPlayer();
                this.soundPlayer.setAudioStreamType(this.audioStreamType);
                this.soundPlayer.setOnCompletionListener(new OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Log.d(TAG, "Shutter finished release it now....");
                        mp.release();
                        if (mgr != null) {
                            mgr.setSpeakerphoneOn(false);
                        }
                        soundPlayer = null;
                    }
                });
                this.soundPlayer.setOnPreparedListener(new OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        Log.d(TAG, "Shutter Onprepared, playing click now");
                        mp.seekTo(0);
                        mp.start();
                    }
                });
                this.soundPlayer.setOnErrorListener(new OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int arg1, int arg2) {
                        Log.d(TAG, "Beeper onError");
                        mp.release();
                        soundPlayer = null;
                        return true;
                    }
                });

                AssetFileDescriptor click_fd =
                        TheApp.getAppContext().getAssets().openFd(CLICK_ASSET);

                this.soundPlayer.setDataSource(click_fd.getFileDescriptor(),
                        click_fd.getStartOffset(),
                        click_fd.getLength());

                Log.d(TAG, "Beeper prepare");
                this.soundPlayer.prepare();
            } else {
                Log.d(TAG, "this.soundPlayer is not null, stop the play and release it");
                this.soundPlayer.stop();
                this.soundPlayer.release();
                this.soundPlayer = null;
            }

        } catch (IllegalStateException e) {
            Log.w(TAG, "click - IllegalStateException - " + e.getLocalizedMessage());
        } catch (IOException e) {
            Log.w(TAG, CLICK_ASSET + " not found");
        }
    }
}
