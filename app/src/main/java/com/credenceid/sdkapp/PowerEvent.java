package com.credenceid.sdkapp;

import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.util.Log;

public class PowerEvent extends BroadcastReceiver {
	public String TAG = "com.credenceid.sdkapp.PowerEvent";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
			Log.d(TAG, "Credence SDK detected power connected event");
		} else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
			Log.d(TAG, "Credence SDK detected power disconnect event");
		}
	}
}