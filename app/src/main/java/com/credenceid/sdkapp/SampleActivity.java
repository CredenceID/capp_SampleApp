package com.credenceid.sdkapp;

import java.io.File;

import android.content.Context;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.credenceid.biometrics.BiometricsActivity;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int orientation = (size.x > size.y) ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE :
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        setRequestedOrientation(orientation);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        Log.d(TAG, "DisplayMetrics - density: " + metrics.density + ", dpi: " + metrics.densityDpi);
        if (metrics.densityDpi < DisplayMetrics.DENSITY_MEDIUM) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

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

        enableButtons(false);

        showValidPages();

        getDisplaySize();
    }

    private boolean first_time = true;

    @Override
    public void onBiometricsInitialized(ResultCode result, String sdk_version,
                                        String required_version) {
        Log.d(TAG, "Sdkapp product name is " + getDeviceTypeFromDisk());
        if (result != ResultCode.OK) {
            String str = String.format("Biometric initialization failed\nSDK version: %s\nRequired_version: %s", sdk_version, required_version);
            TheApp.getInstance().showToast(str);
        }

        enableButtons(true);

        if (first_time) {
            saveValidPages();
            showValidPages();
            // redisplay AboutPage with correct device id and SDK version
            setCurrentPage(mAboutBtn, mAboutPage);
            // set MRZ page again so it can show ePassport option for starlight.
            mMrzPage.setActivity(this);
            first_time = false;
        }

        Toast.makeText(this, "Biometrics Initialized", Toast.LENGTH_LONG).show();
    }

    private long pause_time = 0;
    private static long SHORT_PAUSE = 500;

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
    protected void onResume() {
        super.onResume();
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

    public void onAbout(View v) {
        setCurrentPage(v, mAboutPage);
        mAboutBtn.setImageResource(R.drawable.ic_abouton);

    }

    public void onFingerprint(View v) {
        Log.d(TAG, "onFingerprint button clicked!!!!!!");
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

    private void setCurrentPage(View btn, View page) {
        if (mCurrentPage != null) {
            mCurrentPage.deactivate();
            cancelCapture();
            mCurrentPage = null;
        }
        if (mCurrentBtn != null)
            mCurrentBtn.setActivated(false);
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

    public void setButtonsVisibility(int visibility) {
        mButtons.setVisibility(visibility);
    }

    private void getDisplaySize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        Log.d(TAG, "getDisplaySize - width: " + size.x + ", height: " + size.y);
    }

    private void showValidPages() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        boolean has_fingerprint_scanner = prefs.getBoolean(
                "has_fingerprint_scanner", true);
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK
                && mWebView.getVisibility() == View.VISIBLE) {
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

    public void showFullScreen(String path_name) {
        if (path_name == null || path_name.isEmpty())
            return;
        File file = new File(path_name);
        mWebView.setVisibility(View.VISIBLE);
        mWebView.getSettings().setBuiltInZoomControls(true);
        String url = "file:///" + file.getAbsolutePath();
        mWebView.loadUrl(url);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_EXIT, Menu.NONE, R.string.menu_exit);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_EXIT:
                onExit();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onExit() {
        finalizeBiometrics(true);
        finish();
    }

    @Override
    public boolean hasNfcCard() {
        if (getDeviceTypeFromDisk().equals("twizzler"))
            return true;
        else
            return super.hasNfcCard();
    }

    @Override
    public boolean hasIrisScanner() {
        if (getDeviceTypeFromDisk().equals("twizzler"))
            return false;
        else
            return super.hasIrisScanner();
    }

    @Override
    public boolean hasMrzReader() {

        return super.hasMrzReader();

    }


    @Override
    public boolean hasFingerprintScanner() {
        return true;
    }

    @Override
    public boolean hasFmdMatcher() {
        Log.d(TAG, "has fmdMatcher called");
        if (getDeviceTypeFromDisk().equals("twizzler")) {
            Log.d(TAG, "has fmdMatcher return true for twizzler");
            return true;
        } else {
            Log.d(TAG, "has fmdMatcher return super value");
            return super.hasFmdMatcher();
        }
    }

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

    private void enableButton(ImageButton button, boolean enabled) {
        if (button != null)
            button.setEnabled(enabled);
    }
}
