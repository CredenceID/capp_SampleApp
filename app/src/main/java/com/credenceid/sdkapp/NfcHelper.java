package com.credenceid.sdkapp;


import android.util.Log;

public class NfcHelper { 
	private boolean isInitialized = false;
   	private static NfcHelper instance = null;
   	protected NfcHelper() {
      		// Exists only to defeat instantiation.
		isInitialized = false;
   	}	
	public static NfcHelper getInstance() {
		if(instance == null) {
         		instance = new NfcHelper();
      		}
      		return instance;	
	}
	public boolean isInitialized() {
		Log.d("NfcHelper", "Is Initialized returning " + isInitialized);
		return isInitialized;
	}
}

