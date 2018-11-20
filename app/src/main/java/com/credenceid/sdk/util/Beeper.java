package com.credenceid.sdk.util;


import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

import static com.credenceid.sdk.FaceActivity.getContext;

/* This class is used to play the "click" sound you hear when doing a Fingerprint capture. This
 * class a custom Audio file we have placed inside the <assets/> directory.
 */
@SuppressWarnings("WeakerAccess")
public class Beeper {
	private final static String CLICK_ASSET = "camera_click.ogg";
	private static Beeper instance;
	private final String TAG = Beeper.class.getSimpleName();
	private MediaPlayer soundPlayer = null;

	public static Beeper getInstance() {
		if (instance == null)
			instance = new Beeper();
		return instance;
	}

	public void click() {
		Log.d(TAG, "Beeper::click");
		try {
			if (this.soundPlayer == null) {
				final AudioManager mgr = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);

				if (mgr != null)
					mgr.setSpeakerphoneOn(true);

				this.soundPlayer = new MediaPlayer();
				this.soundPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);

				this.soundPlayer.setOnCompletionListener((MediaPlayer mp) -> {
					mp.release();
					if (mgr != null)
						mgr.setSpeakerphoneOn(false);
					soundPlayer = null;
				});

				this.soundPlayer.setOnPreparedListener((MediaPlayer mp) -> {
					mp.seekTo(0);
					mp.start();
				});

				this.soundPlayer.setOnErrorListener((MediaPlayer mp,
													 int arg1,
													 int arg2) -> {
					mp.release();
					soundPlayer = null;
					return true;
				});

				AssetFileDescriptor click_fd = getContext().getAssets().openFd(CLICK_ASSET);

				this.soundPlayer.setDataSource(click_fd.getFileDescriptor(),
						click_fd.getStartOffset(),
						click_fd.getLength());

				this.soundPlayer.prepare();
			} else {
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