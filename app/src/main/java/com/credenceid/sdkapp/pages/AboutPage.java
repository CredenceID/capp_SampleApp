package com.credenceid.sdkapp.pages;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.Biometrics.OnCheckAlgorithmLicensesListener;
import com.credenceid.biometrics.Biometrics.PreferencesListener;
import com.credenceid.biometrics.Biometrics.ResultCode;
import com.credenceid.sdkapp.R;
import com.credenceid.sdkapp.models.PageView;
import com.credenceid.sdkapp.utils.BiometricsManagerInstance;

public class AboutPage extends LinearLayout implements PageView {
    private static final String TAG = AboutPage.class.getName();

    private Biometrics biometrics;
    private Context context;

    private TextView textViewProductName;
    private TextView textViewDeviceID;
    private TextView textViewSdkVersion;
    private TextView textViewDeviceLibJarVersion;
    private TextView textViewLicenseStatus;

    private TextView textViewBiometricsManagerProductName;
    private TextView textViewBiometricsManagerSdkVersion;
    private TextView textViewBiometricsManagerDeviceLibJarVersion;

    private Spinner spinnerGetPreferencesKey;
    private Spinner spinnerSetPreferencesKey;

    private TextView textViewGetPreferencesValue;

    private OnCheckAlgorithmLicensesListener onCheckAlgorithmLicensesListener = new OnCheckAlgorithmLicensesListener() {
        @Override
        public void onCheckAlgorithmLicenses(ResultCode result) {
            int res_id = R.string.not_obtained;
            if (result == ResultCode.INTERMEDIATE)
                res_id = R.string.obtaining;
            else if (result == ResultCode.OK)
                res_id = R.string.obtained;
            textViewLicenseStatus.setText(res_id);
        }
    };

    public AboutPage(Context context) {
        super(context);
        this.initialize(context);
    }

    public AboutPage(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.initialize(context);
    }

    public AboutPage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.initialize(context);
    }

    private void initialize(Context context) {
        Log.d(TAG, "initialize");

        this.context = context;

        LayoutInflater li = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        li.inflate(R.layout.page_about, this, true);

        this.initializeLayoutComponents();

        this.textViewGetPreferencesValue = (TextView) findViewById(R.id.get_pref_value);

        this.spinnerGetPreferencesKey = (Spinner) findViewById(R.id.get_pref_name);
        this.spinnerGetPreferencesKey.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                getPreferences((String) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub

            }
        });

        this.spinnerSetPreferencesKey = (Spinner) findViewById(R.id.set_pref_name);
        this.spinnerSetPreferencesKey.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        Spinner spinnerSetPreferencesValue = (Spinner) findViewById(R.id.set_pref_value);
        spinnerSetPreferencesValue.setSelection(1); //default to 1 as position 0 is a label
        spinnerSetPreferencesValue.setSelection(0, false);
        spinnerSetPreferencesValue.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String key = (String) spinnerSetPreferencesKey.getSelectedItem();
                setPreferences(key, (String) parent.getItemAtPosition(position));

                if (key.equalsIgnoreCase((String) spinnerSetPreferencesKey.getSelectedItem()))
                    getPreferences(key);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        /* Call our singleton for BiometricsManager. This will cause BiometricsManager to
         * initialize.
         */
        BiometricsManagerInstance.getInstance();
    }

    private void initializeLayoutComponents() {
        TextView textViewAppVersion = (TextView) findViewById(R.id.app_version);
        textViewAppVersion.setText(getPackageVersion());

        this.textViewProductName = (TextView) findViewById(R.id.product_name);
        this.textViewDeviceID = (TextView) findViewById(R.id.device_id);
        this.textViewSdkVersion = (TextView) findViewById(R.id.sdk_version);
        this.textViewDeviceLibJarVersion = (TextView) findViewById(R.id.device_library_version);
        this.textViewLicenseStatus = (TextView) findViewById(R.id.license_status);

        this.textViewBiometricsManagerProductName = (TextView) findViewById(R.id.product_name_man);
        this.textViewBiometricsManagerSdkVersion = (TextView) findViewById(R.id.sdk_version_man);
        this.textViewBiometricsManagerDeviceLibJarVersion = (TextView) findViewById(R.id.device_library_version_man);
    }

    /* Get this application's version number through manifest file. */
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
        this.biometrics = biometrics;
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
        Log.w(TAG, "doResume - DeviceId: " + this.biometrics.getDeviceId());

        this.biometrics.checkAlgorithmLicenses(onCheckAlgorithmLicensesListener);

        this.textViewProductName.setText(this.biometrics.getProductName());
        this.textViewDeviceID.setText(this.biometrics.getDeviceId());
        this.textViewSdkVersion.setText(this.biometrics.getSDKVersion());
        this.textViewDeviceLibJarVersion.setText(this.biometrics.getDeviceLibraryVersion());

        this.textViewBiometricsManagerProductName.setText(this.biometrics.getProductName());
        this.textViewBiometricsManagerSdkVersion.setText(this.biometrics.getSDKVersion());
        this.textViewBiometricsManagerDeviceLibJarVersion.setText(this.biometrics.getDeviceLibraryVersion());

        Log.w(TAG, "doInBackground - getPreferences");
        getPreferences(spinnerGetPreferencesKey.getSelectedItem().toString());
    }

    /* Calls getPreferences() API and updates TextView's with updated information. */
    private void getPreferences(String name) {
        Log.w(TAG, "getPreferences: " + name);

        if (this.biometrics == null) return;

        this.biometrics.getPreferences(name, new PreferencesListener() {
            @Override
            public void onPreferences(ResultCode result, String key, String value) {
                Log.w(TAG, "onPreferences: " + key + ", " + value);
                if (result == ResultCode.OK)
                    textViewGetPreferencesValue.setText(value);
                else textViewGetPreferencesValue.setTag("Error getting preferences.");
            }
        });
    }

    /* Calls setPreferecnes() API to set given key/value pair of preferences. Updates respective
     * TextViews with results.
     */
    private void setPreferences(String name, String value) {
        Log.w(TAG, "setPreferences: " + name + ", " + value);

        if (this.biometrics == null) return;

        this.biometrics.setPreferences(name, value, new PreferencesListener() {
            @Override
            public void onPreferences(ResultCode result, String key, String value) {
                Log.w(TAG, "onPreferences: " + key + ", " + value);
                if (result == ResultCode.OK)
                    Toast.makeText(context, "Setting preferences succeeded", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(context, "Error Setting Preferences", Toast.LENGTH_LONG).show();
            }
        });
    }
}


