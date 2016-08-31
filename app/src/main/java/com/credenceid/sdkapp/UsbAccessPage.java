package com.credenceid.sdkapp;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.credenceid.biometrics.Biometrics;

public class UsbAccessPage extends LinearLayout implements PageView {
	private static final String TAG = UsbAccessPage.class.getName();

	private Biometrics mBiometrics;
	private Button mEnableBtn;
	private Button mDisableBtn;
	private TextView mStatusTextView;

	public UsbAccessPage(Context context) {
		super(context);
		initialize();
	}

	public UsbAccessPage(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}

	public UsbAccessPage(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}

	private void initialize() {
		Log.d(TAG, "initialize");
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.page_usb_access, this, true);

		mEnableBtn = (Button) findViewById(R.id.enable_btn);
		mEnableBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onEnable(v);
			}
		});
		mDisableBtn = (Button) findViewById(R.id.disable_btn);
		mDisableBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onDisable(v);
			}
		});
		mStatusTextView = (TextView) findViewById(R.id.status);

	}

	@Override
	public String getTitle() {
		return getContext().getResources().getString(R.string.usb_access_header);
	}

	@Override
	public void activate(Biometrics biometrics) {
		mBiometrics = biometrics;
		doResume();
	}

	public void doResume() {
		mStatusTextView.setText("");
	}

	@Override
	public void deactivate() {
	}

	private void onEnable(View v) {
		mBiometrics.enableFileAccessOverUsb(true);
		mStatusTextView.setText(R.string.usb_access_enabled);
	}

	private void onDisable(View v) {
		mBiometrics.enableFileAccessOverUsb(false);
		mStatusTextView.setText(R.string.usb_access_disabled);
	}
}
