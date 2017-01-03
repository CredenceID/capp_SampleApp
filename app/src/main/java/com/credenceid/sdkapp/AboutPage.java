package com.credenceid.sdkapp;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.Biometrics.OnCheckAlgorithmLicensesListener;
import com.credenceid.biometrics.Biometrics.PreferencesListener;
import com.credenceid.biometrics.Biometrics.ResultCode;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class AboutPage extends LinearLayout implements PageView {
	private static final String TAG = AboutPage.class.getName();

	private Biometrics mBiometrics;
	private Context mContext;

	private TextView mProductNameTextView;
	private TextView mDeviceIdTextView;
	private TextView mSdkVersionTextView;
	private TextView mDeviceLibraryTextView;
	private TextView mLicenseStatus;

	private TextView mProductNameManTextView;
	private TextView mSdkVersionManTextView;
	private TextView mDeviceLibraryManTextView;

	private Spinner mGetPrefName;
	private Spinner mSetPrefName;
	private TextView mGetPrefValue;
	private Spinner mSetPrefValue;

	private OnCheckAlgorithmLicensesListener check_algorithm_licenses_listener = new OnCheckAlgorithmLicensesListener() {
		@Override
		public void onCheckAlgorithmLicenses(ResultCode result) {
			int res_id = R.string.not_obtained;
			if (result == ResultCode.INTERMEDIATE)
				res_id = R.string.obtaining;
			else if (result == ResultCode.OK)
				res_id = R.string.obtained;
			mLicenseStatus.setText(res_id);
		}
	};

	public AboutPage(Context context) {
		super(context);
		initialize(context);
	}

	public AboutPage(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(context);
	}

	public AboutPage(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize(context);
	}

	private void initialize(Context context) {
		Log.d(TAG, "initialize");
		mContext = context;
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.page_about, this, true);

		TextView app_version_tv = (TextView) findViewById(R.id.app_version);
		app_version_tv.setText(getPackageVersion());

		mProductNameTextView = (TextView) findViewById(R.id.product_name);
		mDeviceIdTextView = (TextView) findViewById(R.id.device_id);
		mSdkVersionTextView = (TextView) findViewById(R.id.sdk_version);
		mDeviceLibraryTextView = (TextView) findViewById(R.id.device_library_version);
		mLicenseStatus = (TextView) findViewById(R.id.license_status);

		mProductNameManTextView = (TextView) findViewById(R.id.product_name_man);
		mSdkVersionManTextView = (TextView) findViewById(R.id.sdk_version_man);
		mDeviceLibraryManTextView = (TextView) findViewById(R.id.device_library_version_man);

		mGetPrefValue = (TextView) findViewById(R.id.get_pref_value);
		mGetPrefName = (Spinner) findViewById(R.id.get_pref_name);
		mGetPrefName.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				getPreferences((String)parent.getItemAtPosition(position));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub

			}
		});

		mSetPrefName = (Spinner) findViewById(R.id.set_pref_name);
		mSetPrefName.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				//Do nothing...
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
		});

		mSetPrefValue = (Spinner) findViewById(R.id.set_pref_value);
		mSetPrefValue.setSelection(1); //default to 1 as position 0 is a label
		mSetPrefValue.setSelection(0, false);
		mSetPrefValue.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				String key = (String) mSetPrefName.getSelectedItem();
				setPreferences(key, (String)parent.getItemAtPosition(position));
				
				if(key.equalsIgnoreCase((String)mGetPrefName.getSelectedItem())) {
					getPreferences(key);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
		});

		// Singleton instance of BiometricsManager
		// this will initalize Service
		BiometricsManagerInstance.getInstance();
	}

	// Gets the Version name in manifest
	private String getPackageVersion() {
		String version = "???";
		try {
			PackageInfo pInfo = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
			version = pInfo.versionName;
		} catch (NameNotFoundException e) {
			Log.w(TAG, "getVersion - " + e.getMessage());
		} catch (Exception e1) {
			Log.w(TAG, "getVersion - " + e1.getMessage());
		}
		return version;
	}

	@Override
	public void activate(Biometrics biometrics) {
		this.mBiometrics = biometrics;
		doResume();
	}

	@Override
	public void deactivate() {
	}

	@Override
	public String getTitle() {
		return null;
	}

	@Override
	public void doResume() {
		Log.w(TAG, "doResume - DeviceId: " + mBiometrics.getDeviceId());

		mProductNameTextView.setText(mBiometrics.getProductName());
		mDeviceIdTextView.setText(mBiometrics.getDeviceId());
		mSdkVersionTextView.setText(mBiometrics.getSDKVersion());
		mDeviceLibraryTextView.setText(mBiometrics.getDeviceLibraryVersion());
		mBiometrics.checkAlgorithmLicenses(check_algorithm_licenses_listener);
		
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
	            Log.w(TAG, "doInBackground - getPreferences");
				getPreferences(mGetPrefName.getSelectedItem().toString());
				return null;
			}
		};
		task.execute();

		mProductNameManTextView.setText(mBiometrics.getProductName());
		mSdkVersionManTextView.setText(mBiometrics.getSDKVersion());
		mDeviceLibraryManTextView.setText(mBiometrics.getDeviceLibraryVersion());
	}

	// For AsyncTask in doResume so it can call getPreferences()
	private Runnable run_get_preferences = new Runnable() {
		@Override
		public void run() {
			getPreferences(mGetPrefName.getSelectedItem().toString());
		}
	};

	// Calls getPreferences API in CredenceService and updates TextView
	private void getPreferences(String name) {
        Log.w(TAG, "getPreferences: " + name);

		if(mBiometrics == null) {
			return;
		}

		mBiometrics.getPreferences(name, new PreferencesListener() {
			@Override
			public void onPreferences(ResultCode result, String key, String value) {
		        Log.w(TAG, "onPreferences: " + key + ", " + value);
		        if(result == ResultCode.OK) {
		        	mGetPrefValue.setText(value);
		        } else {
		        	mGetPrefValue.setTag("---Error---");
		        	Toast.makeText(mContext, "Error Getting Preferences",Toast.LENGTH_LONG).show();	
		        }
			}
		});
	}

	// Calls setPreferences API in CredenceService and shows Toast if succesful or not
	private void setPreferences(String name, String value) {
        Log.w(TAG, "setPreferences: " + name + ", " + value);

		if(mBiometrics == null) {
			return;
		}

		mBiometrics.setPreferences(name, value, new PreferencesListener() {
			@Override
			public void onPreferences(ResultCode result, String key, String value) {
		        Log.w(TAG, "onPreferences: " + key + ", " + value);
		        if(result == ResultCode.OK) {
		        	Toast.makeText(mContext, "Setting preferences succeeded",Toast.LENGTH_LONG).show();	
		        } else {
		        	Toast.makeText(mContext, "Error Setting Preferences",Toast.LENGTH_LONG).show();	
		        }
			}
		});
	}
}


