package com.credenceid.sdkapp;

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

import java.io.IOException;


public class Beeper {
	private final String TAG = Beeper.class.getName();

	private int audio_stream_type = AudioManager.STREAM_ALARM;
	private final int beep_duration = 1; // seconds
	private final int sample_rate = 8000;
	private final double freq_of_tone = 880; // hz
	private final int num_samples = beep_duration * sample_rate;
	private double beep_samples[];
	private byte generated_beep[];
	
	private static Beeper instance;
	private AudioManager mgr = null;

	public static Beeper getInstance() {
		if (instance == null)
			instance = new Beeper();
		return instance;
	}

	private Beeper() {
	}

	// originally from
	// http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
	// and modified by Steve Pomeroy <steve@staticfree.info>
	// by way of IBScan SDK

	public void beep() {
		if (beep_samples == null)
			createBeepSamples();

		final AudioTrack audioTrack = new AudioTrack(audio_stream_type, sample_rate,
				AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, num_samples,
				AudioTrack.MODE_STATIC);
		audioTrack.write(generated_beep, 0, generated_beep.length);
		audioTrack.play();
	}
	public void releaseBeeper() {
		if ( sound_player != null )  {
			sound_player.release();
			sound_player = null;
		}
	}

	private void createBeepSamples() {
		beep_samples = new double[num_samples];
		generated_beep = new byte[2 * num_samples];
		for (int i = 0; i < num_samples; ++i) {
			beep_samples[i] = Math.sin(2 * Math.PI * i / (sample_rate / freq_of_tone));
		}

		// convert to 16 bit pcm sound array
		// assumes the sample buffer is normalised.
		int idx = 0;
		for (final double dVal : beep_samples) {
			// scale to maximum amplitude
			final short val = (short) ((dVal * 32767));
			// in 16 bit wav PCM, first byte is the low order byte
			generated_beep[idx++] = (byte) (val & 0x00ff);
			generated_beep[idx++] = (byte) ((val & 0xff00) >>> 8);

		}
	}

	private MediaPlayer sound_player = null;
	private static String CLICK_ASSET = "camera_click.ogg";

	public void click() {
		Log.d(TAG, "Beeper::click");
		try {
			if (sound_player == null) {
				mgr = (AudioManager) TheApp.getAppContext().getSystemService(Context.AUDIO_SERVICE);
				if ( mgr != null ) {
					mgr.setSpeakerphoneOn(true);
				}
				sound_player = new MediaPlayer();
				sound_player.setAudioStreamType(audio_stream_type);
				sound_player.setOnCompletionListener(new OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						Log.d(TAG, "Shutter finished release it now....");
						mp.release();
						if ( mgr != null ) {
							mgr.setSpeakerphoneOn(false);
						}
						sound_player = null;
					}
				});
				sound_player.setOnPreparedListener(new OnPreparedListener() {
    					@Override
    					public void onPrepared(MediaPlayer mp) {
							Log.d(TAG, "Shutter Onprepared, playing click now");
							mp.seekTo(0);
							mp.start();
    					}
				});	
				sound_player.setOnErrorListener(new OnErrorListener() {
    					@Override
    					public boolean onError(MediaPlayer mp, int arg1, int arg2) {
							Log.d(TAG, "Beeper onError");
                            mp.release();
							sound_player = null;
							return true;
    					}
				});	
				AssetFileDescriptor click_fd = TheApp.getAppContext().getAssets().openFd(CLICK_ASSET);
				sound_player.setDataSource(click_fd.getFileDescriptor(), click_fd.getStartOffset(),
						click_fd.getLength());

				Log.d(TAG, "Beeper prepare");
				sound_player.prepare();

			}
			else {
				Log.d(TAG, "sound_player is not null, stop the play and release it");
				sound_player.stop();
				sound_player.release();
				sound_player = null;
			}
			
		} catch (IllegalStateException e) {
			Log.w(TAG, "click - IllegalStateException - " + e.getLocalizedMessage());
		} catch (IOException e) {
			Log.w(TAG, CLICK_ASSET + " not found");
		}

	}


}
