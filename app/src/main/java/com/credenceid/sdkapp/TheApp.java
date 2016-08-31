package com.credenceid.sdkapp;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

public class TheApp extends Application {
	private static final String TAG = TheApp.class.getName();

	public static boolean DEBUG = true;
	private static TheApp mInstance;

	public TheApp() {
		mInstance = this;
	}

	public static TheApp getInstance() {
		return mInstance;
	}

	private Toast mToast;

	public void showToast(CharSequence cs) {
		if (DEBUG)
			Log.d(TAG, "showToast: " + cs);
		if (mToast == null) {
			mToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
		}
		mToast.setText(cs);
		mToast.show();
	}

	public static String abbreviateNumber(long value) {
		if (value < 1024)
			return String.valueOf(value);
		value /= 1024;
		if (value < 1024)
			return String.valueOf(value) + "K";
		value /= 1024;
		return String.valueOf(value) + "M";
	}
	
	public static String nullTerminatedByteArrayToString(byte[] buffer) {
		if (buffer == null)
			return null;
		int length = buffer.length;
		for (int i = 0; i < buffer.length; i++) {
			if (buffer[i] == 0) {
				length = i;
				break;
			}
		}
		return new String(buffer, 0, length);
	}

}
