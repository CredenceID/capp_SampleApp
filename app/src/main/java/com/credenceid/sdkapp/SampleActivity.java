package com.credenceid.sdkapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.credenceid.biometrics.BiometricsActivity;

import java.io.File;

public class SampleActivity extends BiometricsActivity {
    private static final String TAG = SampleActivity.class.getName();

    private static final int MENU_EXIT = Menu.FIRST;

    private ViewFlipper mViewFlipper;

    private AboutPage mAboutPage;
    private FingerprintPage mFingerprintPage;
    private IrisPage mIrisPage;
    private CardReaderPage mCardReaderPage;
    private EncryptPage mEncryptPage;
    private UsbAccessPage mUsbAccessPage;
    private NfcPage mNfcPage;
    private MrzReaderPage mMrzPage;

    private ImageButton mAboutBtn;
    private ImageButton mFingerprintBtn;
    private ImageButton mIrisBtn;
    private ImageButton mCardReaderBtn;
    private ImageButton mEncryptionBtn;
    private ImageButton mNfcBtn;
    private ImageButton mUsbAccessBtn;
    private ImageButton mMrzBtn;

    private ImageButton mCurrentBtn;
    private PageView mCurrentPage;
    private ViewGroup mButtons;
    private WebView mWebView;

    // flag to determine when biometrics initialized 1st time
    private boolean first_time = true;

    private long pause_time = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setting up screen based on device screen size and resolution
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int orientation = (size.x > size.y) ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        setRequestedOrientation(orientation);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        Log.d(TAG, "DisplayMetrics - density: " + metrics.density + ", dpi: " + metrics.densityDpi);
        if (metrics.densityDpi < DisplayMetrics.DENSITY_MEDIUM) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // Initializing all of the views
        setContentView(R.layout.activity_sample);
        mViewFlipper = (ViewFlipper) findViewById(R.id.view_flipper);
        mAboutPage = (AboutPage) findViewById(R.id.about_view);
        mFingerprintPage = (FingerprintPage) findViewById(R.id.fingerprint_view);
        mFingerprintPage.setActivity(this);
        mIrisPage = (IrisPage) findViewById(R.id.iris_view);
        mIrisPage.setActivity(this);
        mCardReaderPage = (CardReaderPage) findViewById(R.id.card_reader_view);
        mEncryptPage = (EncryptPage) findViewById(R.id.encrypt_view);
        mUsbAccessPage = (UsbAccessPage) findViewById(R.id.usb_access_view);
        mNfcPage = (NfcPage) findViewById(R.id.nfc_view);
        mMrzPage = (MrzReaderPage) findViewById(R.id.mrz_reader_view);
        mMrzPage.setActivity(this);
        mButtons = (ViewGroup) findViewById(R.id.buttons);
        mAboutBtn = (ImageButton) findViewById(R.id.about_btn);
        mFingerprintBtn = (ImageButton) findViewById(R.id.fingerprint_btn);
        mIrisBtn = (ImageButton) findViewById(R.id.iris_btn);
        mCardReaderBtn = (ImageButton) findViewById(R.id.card_reader_btn);
        mEncryptionBtn = (ImageButton) findViewById(R.id.encryption_btn);
        mUsbAccessBtn = (ImageButton) findViewById(R.id.usb_access_btn);
        mNfcBtn = (ImageButton) findViewById(R.id.nfc_btn);
        mMrzBtn = (ImageButton) findViewById(R.id.mrz_reader_btn);

        mWebView = (WebView) findViewById(R.id.web_view);

        // all buttons not enabled until biometrics initialized
        enableButtons(false);

        // All pages are shown until biometics has initialized then this method is called again
        showValidPages();

        getDisplaySize();
    }

    @Override
    public void onBiometricsInitialized(ResultCode result, String sdk_version, String required_version) {
        Log.d(TAG, "Sdkapp product name is " + getProductName());
        if (result != ResultCode.OK) {
            String str = String.format("Biometric initialization failed\nSDK version: %s\nRequired_version: %s", sdk_version, required_version);
            TheApp.getInstance().showToast(str);
        }

        // called here to enable all buttons
        enableButtons(true);

        // only being called here once as CredenceService can unbind and bind again which onBiometricsInitialized is called again
        if (first_time) {
            saveValidPages();
            showValidPages();
            // redisplay AboutPage with correct device id and SDK version
            setCurrentPage(mAboutBtn, mAboutPage);
            // set MRZ page again so it can show ePassport option for starlight.
            mMrzPage.setActivity(this);
            first_time = false;

            setPreferences("PREF_TIMEOUT", "60", new PreferencesListener() {
                @Override
                public void onPreferences(ResultCode result, String key, String value) {
                    Log.w(TAG, "onPreferences: " + key + ", " + value);
                    if(result == ResultCode.OK) {

                    } else {
                        Toast.makeText(SampleActivity.this, "Error Setting Preferences",Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        Toast.makeText(this, "Biometrics Initialized", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.getVisibility() == View.VISIBLE) {
            mWebView.setVisibility(View.GONE);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK && mCurrentBtn == mAboutBtn) {
            Log.d(TAG, "force shutdown when exiting from About Page");
            onExit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        pause_time = SystemClock.elapsedRealtime();
        /*if (mCurrentPage != null) {
			mCurrentPage.deactivate();
		}*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_EXIT, Menu.NONE, R.string.menu_exit);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        long SHORT_PAUSE = 500;

        // if onResume happened right after onPause, do not pass it on to the
        // pages
        // as the onPause / onResume sequence was likely triggered by USB device
        // activation
        long time_since_pause = SystemClock.elapsedRealtime() - pause_time;
        if (time_since_pause < SHORT_PAUSE) {
            Log.d(TAG, "onResume - ignoring short pause");
            return;
        }
        Log.d(TAG, "onResume");
        if (mCurrentPage != null) {
            mCurrentPage.doResume();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_EXIT:
                onExit();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // give BiometricsManager a chance to process onActivityResult
        TheApp.getInstance().getBiometricsManager().onActivityResult(requestCode, resultCode, data);
    }

    // enables all page buttons fased on parameter
    private void enableButtons(boolean enable) {
        enableButton(mAboutBtn, enable);
        enableButton(mFingerprintBtn, enable);
        enableButton(mIrisBtn, enable);
        enableButton(mCardReaderBtn, enable);
        enableButton(mEncryptionBtn, enable);
        enableButton(mNfcBtn, enable);
        enableButton(mUsbAccessBtn, enable);
        enableButton(mMrzBtn, enable);
    }

    // enables specific button based on parameter
    private void enableButton(ImageButton button, boolean enabled) {
        if (button != null)
            button.setEnabled(enabled);
    }

    // Will set visibility of biometric pages based on device type from CredenceService
    private void showValidPages() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        boolean has_fingerprint_scanner = prefs.getBoolean("has_fingerprint_scanner", true);
        mFingerprintBtn.setVisibility(has_fingerprint_scanner ? View.VISIBLE : View.GONE);

        boolean has_iris_scanner = prefs.getBoolean("has_iris_scanner", true);
        mIrisBtn.setVisibility(has_iris_scanner ? View.VISIBLE : View.GONE);

        boolean has_card_reader = prefs.getBoolean("has_card_reader", true);
        mCardReaderBtn.setVisibility(has_card_reader ? View.VISIBLE : View.GONE);

        boolean has_encryption = prefs.getBoolean("has_encryption", true);
        mEncryptionBtn.setVisibility(has_encryption ? View.VISIBLE : View.GONE);

        boolean has_usb_access = prefs.getBoolean("has_usb_access", true);
        mUsbAccessBtn.setVisibility(has_usb_access ? View.VISIBLE : View.GONE);

        boolean has_mrz_reader = prefs.getBoolean("has_mrz_reader", true);
        mMrzBtn.setVisibility(has_mrz_reader ? View.VISIBLE : View.GONE);

        boolean has_nfc_card = prefs.getBoolean("has_nfc_card", true);
        mNfcBtn.setVisibility(has_nfc_card ? View.VISIBLE : View.GONE);
    }

    // this shows in logcat width and height of screen
    private void getDisplaySize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        Log.d(TAG, "getDisplaySize - width: " + size.x + ", height: " + size.y);
    }

    // based on what device CredenceService was initialized will save what peripherals in shared preference
    private void saveValidPages() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean("has_fingerprint_scanner", hasFingerprintScanner());
        editor.putBoolean("has_iris_scanner", hasIrisScanner());
        editor.putBoolean("has_card_reader", hasCardReader());
        editor.putBoolean("has_encryption", true);
        editor.putBoolean("has_usb_access", hasUsbFileAccessEnabling());
        editor.putBoolean("has_nfc_card", hasNfcCard());
        editor.putBoolean("has_mrz_reader", hasMrzReader());
        editor.commit();
    }

    // called from menu or device backbutton on about page to call finalize API and end app
    private void onExit() {
        finalizeBiometrics(true);
        finish();
    }

    /****************************************************************/
    /* Button methods for onClick event setup in layout             */
    /****************************************************************/
    public void onAbout(View v) {
        setCurrentPage(v, mAboutPage);
        mAboutBtn.setImageResource(R.drawable.ic_abouton);
    }

    public void onFingerprint(View v) {
        setCurrentPage(v, mFingerprintPage);
        mAboutBtn.setImageResource(R.drawable.ic_aboutoff);
    }

    public void onIris(View v) {
        setCurrentPage(v, mIrisPage);
        mAboutBtn.setImageResource(R.drawable.ic_aboutoff);
    }

    public void onCardReader(View v) {
        setCurrentPage(v, mCardReaderPage);
        mAboutBtn.setImageResource(R.drawable.ic_aboutoff);
    }

    public void onMRZ(View v) {
        setCurrentPage(v, mMrzPage);
        mAboutBtn.setImageResource(R.drawable.ic_aboutoff);
    }

    public void onEncrypt(View v) {
        setCurrentPage(v, mEncryptPage);
        mAboutBtn.setImageResource(R.drawable.ic_aboutoff);
    }

    public void onUsbAccess(View v) {
        setCurrentPage(v, mUsbAccessPage);
        mAboutBtn.setImageResource(R.drawable.ic_aboutoff);
    }

    public void onNfcView(View v) {
        setCurrentPage(v, mNfcPage);
        mAboutBtn.setImageResource(R.drawable.ic_aboutoff);
    }
    /****************************************************************/

    // sets the page that the corresponding button to the current page and activates it
    private void setCurrentPage(View btn, View page) {
        // call the old current page deactivate and cancelCapture API just in case it is fingerprint or Iris
        if (mCurrentPage != null) {
            mCurrentPage.deactivate();
            cancelCapture();
            mCurrentPage = null;
        }
        // deactivates old current button
        if (mCurrentBtn != null) {
            mCurrentBtn.setActivated(false);
        }
        if (btn == null || !(btn instanceof ImageButton)) {
            mCurrentBtn = null;
            return;
        }

        mCurrentBtn = (ImageButton) btn;
        mCurrentBtn.setActivated(true);

        int index = mViewFlipper.indexOfChild(page);
        if (index < 0) {
            Log.w(TAG, "invalid view");
        } else {
            mViewFlipper.setDisplayedChild(index);
            // setting new page to current and calling its activate method
            if (page instanceof PageView) {
                mCurrentPage = (PageView) page;
                mCurrentPage.activate(this);
                String title = getResources().getString(R.string.app_name);
                String page_title = mCurrentPage.getTitle();
                if (page_title != null) {
                    title += " - " + page_title;
                }
                setTitle(title);
            }
        }
    }

    // Shows fingerprint or iris scanned image when user taps it and allows zooming
    public void showFullScreen(String path_name) {
        if (path_name == null || path_name.isEmpty()) {
            return;
        }
        File file = new File(path_name);
        mWebView.setVisibility(View.VISIBLE);
        mWebView.getSettings().setBuiltInZoomControls(true);
        String url = "file:///" + file.getAbsolutePath();
        mWebView.loadUrl(url);
    }


    public void setButtonsVisibility(int visibility) {
        mButtons.setVisibility(visibility);
    }
}
