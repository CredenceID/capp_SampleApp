package com.credenceid.sdkapp.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.credenceid.biometrics.BiometricsActivity;
import com.credenceid.sdkapp.R;
import com.credenceid.sdkapp.TheApp;
import com.credenceid.sdkapp.models.PageView;
import com.credenceid.sdkapp.pages.AboutPage;
import com.credenceid.sdkapp.pages.CameraPage;
import com.credenceid.sdkapp.pages.CardReaderPage;
import com.credenceid.sdkapp.pages.EncryptPage;
import com.credenceid.sdkapp.pages.FingerprintPage;
import com.credenceid.sdkapp.pages.IrisPage;
import com.credenceid.sdkapp.pages.MrzReaderPage;
import com.credenceid.sdkapp.pages.NfcPage;
import com.credenceid.sdkapp.pages.UsbAccessPage;

import java.io.ByteArrayOutputStream;
import java.io.File;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.widget.Toast.LENGTH_LONG;

/* This class uses the second technique of using the CredenceID biometrics, via BiometricsActivity.
 */
public class SampleActivity
		extends BiometricsActivity
		implements SampleContract.View {
	private static final String TAG = SampleActivity.class.getName();

	private static final int MENU_EXIT = Menu.FIRST;

	// ViewFlipper is used to switch between different pages in our activity.
	private ViewFlipper mPageViewFlipper;

	// Each page or "Activity".
	private AboutPage mAboutPage;
	private CardReaderPage mCardReaderPage;
	private FingerprintPage mFingerprintPage;
	private CameraPage mCameraPage;
	private IrisPage mIrisPage;
	private MrzReaderPage mMRZReaderPage;
	private NfcPage mNFCPage;
	private EncryptPage mEncryptPage;
	private UsbAccessPage mUSBAccessPage;

	// Image buttons corresponding to each page.
	private ImageButton mImageButtonAboutPage;
	private ImageButton mImageButtonFingerprintPage;
	private ImageButton mImageButtonCameraPage;
	private ImageButton mImageButtonIrisPage;
	private ImageButton mImageButtonCardReaderPage;
	private ImageButton mImageButtonEncryptPage;
	private ImageButton mImageButtonNfcPage;
	private ImageButton mImageButtonUsbAccessPage;
	private ImageButton mImageButtonMrzPage;

	// This object stores which current ImageButton is activated.
	private ImageButton mImageButtonCurrentPage;
	// Stores which current PageView the user is on.
	private PageView mCurrentPageView;
	// WebView displaying device information.
	private WebView mWebView;

	private long mOnPauseTime = 0;

	@Override
	public void
	setPresenter(SampleContract.Presenter presenter) {

	}

	@Override
	protected void
	onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.initializeScreen();
		setContentView(R.layout.activity_sample);

		this.initializeLayoutComponents();
		this.configureLayoutComponents();

		// Disable all buttons until Biometrics becomes initialized. This prevents users from
		// making API calls with null objects.
		this.setGlobalButtonEnable(false);
	}

	/* Sets application orientation and window flags based on device's screen parameters. */
	private void
	initializeScreen() {
		// Setting up screen based on device screen size and resolution.
		Point size = this.getDisplaySize();

		final int orientation =
				(size.x > size.y) ? SCREEN_ORIENTATION_LANDSCAPE : SCREEN_ORIENTATION_PORTRAIT;
		setRequestedOrientation(orientation);

		DisplayMetrics metrics = getResources().getDisplayMetrics();
		if (metrics.densityDpi < DisplayMetrics.DENSITY_MEDIUM)
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	/* Method used to load different pages inside layout file. */
	private void
	initializeLayoutComponents() {
		// Initialize all PageViews.
		mPageViewFlipper = (ViewFlipper) findViewById(R.id.view_flipper);
		mWebView = (WebView) findViewById(R.id.web_view);
		mAboutPage = (AboutPage) findViewById(R.id.about_view);
		mFingerprintPage = (FingerprintPage) findViewById(R.id.fingerprint_view);
		mCameraPage = (CameraPage) findViewById(R.id.face_camera_view);
		mIrisPage = (IrisPage) findViewById(R.id.iris_view);
		mCardReaderPage = (CardReaderPage) findViewById(R.id.card_reader_view);
		mEncryptPage = (EncryptPage) findViewById(R.id.encrypt_view);
		mUSBAccessPage = (UsbAccessPage) findViewById(R.id.usb_access_view);
		mNFCPage = (NfcPage) findViewById(R.id.nfc_view);
		mMRZReaderPage = (MrzReaderPage) findViewById(R.id.mrz_reader_view);

		// Initialize buttons that allow user to switch between Pages.
		mImageButtonAboutPage = (ImageButton) findViewById(R.id.about_btn);
		mImageButtonFingerprintPage = (ImageButton) findViewById(R.id.fingerprint_btn);
		mImageButtonCameraPage = (ImageButton) findViewById(R.id.face_camera_btn);
		mImageButtonIrisPage = (ImageButton) findViewById(R.id.iris_btn);
		mImageButtonCardReaderPage = (ImageButton) findViewById(R.id.card_reader_btn);
		mImageButtonEncryptPage = (ImageButton) findViewById(R.id.encryption_btn);
		mImageButtonUsbAccessPage = (ImageButton) findViewById(R.id.usb_access_btn);
		mImageButtonNfcPage = (ImageButton) findViewById(R.id.nfc_btn);
		mImageButtonMrzPage = (ImageButton) findViewById(R.id.mrz_reader_btn);
	}

	/* Method used to load all buttons from layout file. */
	private void
	configureLayoutComponents() {
		mFingerprintPage.setActivity(this);
		mCameraPage.setActivity(this);
		mIrisPage.setActivity(this);
		mMRZReaderPage.setActivity(this);

		mImageButtonAboutPage.setOnClickListener((View v) -> {
			setCurrentPage(v, mAboutPage);
			mImageButtonAboutPage.setImageResource(R.drawable.ic_abouton);
		});

		mImageButtonFingerprintPage.setOnClickListener((View v) -> {
			setCurrentPage(v, mFingerprintPage);
			mImageButtonFingerprintPage.setImageResource(R.drawable.ic_aboutoff);
		});

		mImageButtonCardReaderPage.setOnClickListener((View v) -> {
			setCurrentPage(v, mCardReaderPage);
			mImageButtonAboutPage.setImageResource(R.drawable.ic_aboutoff);
		});

		mImageButtonIrisPage.setOnClickListener((View v) -> {
			setCurrentPage(v, mIrisPage);
			mImageButtonAboutPage.setImageResource(R.drawable.ic_aboutoff);
		});

		mImageButtonCameraPage.setOnClickListener((View v) -> {
			setCurrentPage(v, mCameraPage);
			mImageButtonAboutPage.setImageResource(R.drawable.ic_aboutoff);
		});

		mImageButtonNfcPage.setOnClickListener((View v) -> {
			setCurrentPage(v, mNFCPage);
			mImageButtonAboutPage.setImageResource(R.drawable.ic_aboutoff);
		});

		mImageButtonMrzPage.setOnClickListener((View v) -> {
			setCurrentPage(v, mMRZReaderPage);
			mImageButtonAboutPage.setImageResource(R.drawable.ic_aboutoff);
		});

		mImageButtonUsbAccessPage.setOnClickListener((View v) -> {
			setCurrentPage(v, mUSBAccessPage);
			mImageButtonAboutPage.setImageResource(R.drawable.ic_aboutoff);
		});

		mImageButtonEncryptPage.setOnClickListener((View v) -> {
			setCurrentPage(v, mEncryptPage);
			mImageButtonAboutPage.setImageResource(R.drawable.ic_aboutoff);
		});
	}

	@Override
	public void
	onBiometricsInitialized(ResultCode result,
							String sdk_version,
							String required_version) {
		// If biometric initialization was successful, enable all PageView buttons.
		this.setGlobalButtonEnable(result == ResultCode.OK);

		if (result == ResultCode.OK) {
			// Biometrics may initialize/un-initialize multiple times due to binding/un-binding. In
			// order to prevent "re-initialization" we have a check to see if it has already been
			// initialized.
			this.saveValidPages();
			this.showValidPages();

			// Re-display AboutPage since DeviceID and SDKVersion may have changed. This can happen
			// if CredenceService was previously binded to this application. The service was then
			// uninstalled and re-installed (different version maybe). Now when it re-binds it may
			// report a different version, etc.
			this.setCurrentPage(this.mImageButtonAboutPage, mAboutPage);

			// Re-initialize MRZ page since it varies based on DeviceType.
			mMRZReaderPage.setActivity(this);

			setPreferences("PREF_TIMEOUT", "60", (ResultCode prefResult,
												  String key,
												  String value) -> {
				if (prefResult != ResultCode.OK)
					Toast.makeText(this, "Error Setting Preferences", LENGTH_LONG).show();
			});

		}

		Toast.makeText(this, "Biometrics Initialized", LENGTH_LONG).show();
	}

	/* Handle key press for Back, Home, or OverView button on device. */
	@Override
	public boolean
	onKeyDown(int keyCode,
			  KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mWebView.getVisibility() == View.VISIBLE) {
				mWebView.setVisibility(View.GONE);
				return true;
			}

			if (mImageButtonCurrentPage == mImageButtonAboutPage) {
				Log.d(TAG, "Closing application.");
				onExit();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void
	onPause() {
		super.onPause();

		mOnPauseTime = SystemClock.elapsedRealtime();

		// Only CameraPage requires deactivate to also happen on "onPause()" because in Android the
		// camera resource needs to be "let go of" so other apps, etc. may use it.
		if (mCurrentPageView == mCameraPage)
			mCurrentPageView.deactivate();
	}

	@Override
	public boolean
	onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_EXIT, Menu.NONE, R.string.menu_exit);
		return true;
	}

	@Override
	protected void
	onResume() {
		super.onResume();

		// If onResume happened right after onPause, do not pass it on to the pages
		// as the onPause / onResume sequence was likely triggered by USB device activation.
		final long SHORT_PAUSE = 500;
		long timeSincePause = SystemClock.elapsedRealtime() - mOnPauseTime;

		if (timeSincePause < SHORT_PAUSE) {
			Log.d(TAG, "onResume(): Ignoring, due to pause time < 500ms.");
			return;
		}

		if (mCurrentPageView != null)
			mCurrentPageView.doResume();
	}

	@Override
	public boolean
	onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == MENU_EXIT) {
			this.onExit();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void
	onActivityResult(int requestCode, int resultCode, Intent data) {
		/* Give BiometricsManager a chance to process onActivityResult. */
		TheApp.getInstance().getBiometricsManager().onActivityResult(requestCode, resultCode, data);
	}

	/* Returns Point object containing screen dimensions. */
	private Point
	getDisplaySize() {
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		Log.d(TAG, "getDisplaySize - width: " + size.x + ", height: " + size.y);
		return size;
	}

	private void
	setGlobalButtonEnable(boolean enable) {
		setButtonEnable(mImageButtonAboutPage, enable);
		setButtonEnable(mImageButtonFingerprintPage, enable);
		setButtonEnable(mImageButtonIrisPage, enable);
		setButtonEnable(mImageButtonCardReaderPage, enable);
		setButtonEnable(mImageButtonEncryptPage, enable);
		setButtonEnable(mImageButtonNfcPage, enable);
		setButtonEnable(mImageButtonUsbAccessPage, enable);
		setButtonEnable(mImageButtonMrzPage, enable);
	}

	private void
	setButtonEnable(ImageButton button, boolean enabled) {
		if (button != null) button.setEnabled(enabled);
	}

	/* Set visibility of Biometric pages based on device type. */
	private void
	showValidPages() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		boolean hasFingerprintScanner = prefs.getBoolean("has_fingerprint_scanner", true);
		mImageButtonFingerprintPage.setVisibility(hasFingerprintScanner ? View.VISIBLE : View.GONE);

		boolean hasFaceCameraScanner = prefs.getBoolean("has_face_camera", true);
		mImageButtonFaceCameraPage.setVisibility(hasFaceCameraScanner ? View.VISIBLE : View.GONE);

		boolean hasIrisScanner = prefs.getBoolean("has_iris_scanner", true);
		mImageButtonIrisPage.setVisibility(hasIrisScanner ? View.VISIBLE : View.GONE);

		boolean hasCardReader = prefs.getBoolean("has_card_reader", true);
		mImageButtonCardReaderPage.setVisibility(hasCardReader ? View.VISIBLE : View.GONE);

		boolean hasEncryption = prefs.getBoolean("has_encryption", true);
		mImageButtonEncryptPage.setVisibility(hasEncryption ? View.VISIBLE : View.GONE);

		boolean hasUsbAccess = prefs.getBoolean("has_usb_access", true);
		mImageButtonUsbAccessPage.setVisibility(hasUsbAccess ? View.VISIBLE : View.GONE);

		boolean hasMrzReader = prefs.getBoolean("has_mrz_reader", true);
		mImageButtonMrzPage.setVisibility(hasMrzReader ? View.VISIBLE : View.GONE);

		boolean hasNfcCard = prefs.getBoolean("has_nfc_card", true);
		mImageButtonNfcPage.setVisibility(hasNfcCard ? View.VISIBLE : View.GONE);
	}

	/* Based on what device CredenceService was initialized on, this will save what peripherals
	 * pages are valid with SharesPreferences.
	 */
	private void
	saveValidPages() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = prefs.edit();

		editor.putBoolean("has_fingerprint_scanner", hasFingerprintScanner());
		editor.putBoolean("has_face_camera", getProductName().contains("TAB") || getProductName().contains("Trident"));
		editor.putBoolean("has_iris_scanner", hasIrisScanner());
		editor.putBoolean("has_card_reader", hasCardReader());
		editor.putBoolean("has_encryption", true);
		editor.putBoolean("has_usb_access", hasUsbFileAccessEnabling());
		editor.putBoolean("has_nfc_card", hasNfcCard());
		editor.putBoolean("has_mrz_reader", hasMrzReader());
		editor.apply();
	}

	/* Called from menu or device back button to call finalize API and end application. */
	private void
	onExit() {
		this.finalizeBiometrics(true);
		finish();
	}

	/* Sets current page based on which corresponding PageView button was clicked. */
	private void
	setCurrentPage(View pageViewButton, View pageView) {
		/* If application was on Fingerprint/Iris page, we need to cancel any on going captures. */
		if (this.currentPageView == this.fingerprintPage || this.currentPageView == this.irisPage)
			this.cancelCapture();
		/* Deactivate current page application was on. */
		if (this.currentPageView != null) {
			//            if(currentPageView.getTitle().compareToIgnoreCase("Fingerprint") != 0){
			this.currentPageView.deactivate();
			this.currentPageView = null;
			//            }
		}
		/* Deactivates old current page's ImageButton. */
		if (this.mImageButtonCurrentPage != null)
			this.mImageButtonCurrentPage.setActivated(false);

		/* If new button is IN-valid or button is not an ImageButton then we return, since that
		 * means a non-page view button was pressed.
		 */
		if (pageViewButton == null || !(pageViewButton instanceof ImageButton)) {
			this.mImageButtonCurrentPage = null;
			return;
		}
		/* Reaching here means all conditions were valid and met. Set current ImageButton to match
		 * passed argument and set button as activated.
		 */
		this.mImageButtonCurrentPage = (ImageButton) pageViewButton;
		this.mImageButtonCurrentPage.setActivated(true);

		/* Calculate index of page for which PageView we want to switch to. */
		int index = mPageViewFlipper.indexOfChild(pageView);
		/* This would only be the case if we pass a custom PageView not belonging to any listed
		 * inside layout file. Otherwise can set respective page.
		 */
		if (index < 0) Log.w(TAG, "invalid view");
		else {
			mPageViewFlipper.setDisplayedChild(index);
			if (pageView instanceof PageView) {
				/* Update our current page variables and activate new page. */
				currentPageView = (PageView) pageView;
				currentPageView.activate(this);
				/* Grab applications title, new Pagees title, and build fill title string. */
				String title = getResources().getString(R.string.app_name);
				String page_title = currentPageView.getTitle();
				title += (page_title != null) ? (" - " + page_title) : "";
				setTitle(title);
			}
		}
	}

	/* If user taps on scanned Fingerprint/Iris images, then application zooms in allowing user to
	 * interact with images in full screen "mode".
	 */
	public void
	showFullScreenScannedImage(String path_name) {
		if (path_name == null || path_name.isEmpty()) return;

		webView.setVisibility(View.VISIBLE);
		webView.getSettings().setBuiltInZoomControls(true);

		File file = new File(path_name);
		webView.loadUrl("file:///" + file.getAbsolutePath());
	}

	/*
	 * If user taps on scanned the fingerprint and it is not save to disk, then application zooms in
	 * allowing user to interact with images in full screen "mode" with its Bitmap format.
	 */
	public void
	showFullScreenScannedImage(Bitmap bitmap) {
		if (bitmap == null)
			return;

		mWebView.setVisibility(View.VISIBLE);
		mWebView.getSettings().setBuiltInZoomControls(true);

		String html = "<html><body><img src='{IMAGE_PLACEHOLDER}' /></body></html>";

		// Convert bitmap to Base64 encoded image for web
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
		byte[] byteArray = byteArrayOutputStream.toByteArray();
		String imgageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
		String image = "data:image/png;base64," + imgageBase64;

		// Use image for the img src parameter in html and load to webview
		html = html.replace("{IMAGE_PLACEHOLDER}", image);
		webView.loadData(html, "text/html", null);
	}

	public CameraPage getFaceCameraPage() {
		return this.faceCameraPage;
	}
}
