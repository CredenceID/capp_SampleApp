package com.credenceid.sdkapp.pages;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.Biometrics.CloseReasonCode;
import com.credenceid.biometrics.Biometrics.FingerprintScannerType;
import com.credenceid.biometrics.Biometrics.OnCompareFmdListener;
import com.credenceid.biometrics.Biometrics.OnConvertToFmdListener;
import com.credenceid.biometrics.Biometrics.OnConvertToWsqListener;
import com.credenceid.biometrics.Biometrics.OnFingerprintGrabbedFullListener;
import com.credenceid.biometrics.Biometrics.OnFingerprintGrabbedListener;
import com.credenceid.biometrics.Biometrics.ResultCode;
import com.credenceid.biometrics.Biometrics.ScanType;
import com.credenceid.sdkapp.R;
import com.credenceid.sdkapp.SampleActivity;
import com.credenceid.sdkapp.TheApp;
import com.credenceid.sdkapp.models.PageView;
import com.credenceid.sdkapp.utils.Beeper;

import java.io.File;

import static com.credenceid.sdkapp.R.id.status;

public class FingerprintPage extends LinearLayout implements PageView {
    private static final String TAG = FingerprintPage.class.getName();

    private SampleActivity mActivity;
    private Biometrics mBiometrics;

    private Button mCaptureBtn;
    private Button mCloseBtn;
    private Button mMatchBtn;
    private View mMatchSpacer;
    private Spinner mScanTypeSpinner;
    private View mSpinnerSpacer;
    private Spinner mBitrateSpinner;
    private ImageView mCaptureImage;
    private ImageView mCaptureImageFinger1;
    private ImageView mCaptureImageFinger2;
    private TextView mStatusTextView;
    private TextView mInfoTextView;
    private ScanType mScanType = ScanType.SINGLE_FINGER;
    private Biometrics.ScanType scan_types[] = {
            ScanType.SINGLE_FINGER,
            ScanType.TWO_FINGERS, ScanType.ROLL_SINGLE_FINGER,
            ScanType.TWO_FINGERS_SPLIT
    };
    private String mPathname;
    private String mPathnameFinger1;
    private String mPathnameFinger2;
    private float mBitrate = 0;
    private boolean mHasMatcher;

    private Bitmap mCurrentBitmap;
    private byte[] mFmd1 = null;
    private byte[] mFmd2 = null;

    private long uncompressed_size;
    private long compressed_size;
    private long start_time;

    private boolean mIsCapturing = false;

    // Set this to true to use the new Fingerprint callback API
    private boolean useOnFingerprintGrabbedFullListener = true;

    // Set WSQ Fingerprint callback to false to determine later if device supports wsq
    private boolean isOnFingerprintGrabbedWSQListener = false;

    public FingerprintPage(Context context) {
        super(context);
        initialize();
    }

    public FingerprintPage(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public FingerprintPage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    public void setActivity(SampleActivity activity) {
        mActivity = activity;
    }

    private void initialize() {
        Log.d(TAG, "initialize");
        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.page_fingerprint, this, true);

        mCaptureImage = (ImageView) findViewById(R.id.capture_1_image);
        mCaptureImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.showFullScreen(mPathname);
            }
        });

        mCaptureImageFinger1 = (ImageView) findViewById(R.id.capture_finger1_image);
        mCaptureImageFinger1.setVisibility(INVISIBLE);
        mCaptureImageFinger1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.showFullScreen(mPathnameFinger1);
            }
        });

        mCaptureImageFinger2 = (ImageView) findViewById(R.id.capture_finger2_image);
        mCaptureImageFinger2.setVisibility(INVISIBLE);
        mCaptureImageFinger2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.showFullScreen(mPathnameFinger2);
            }
        });

        mCaptureBtn = (Button) findViewById(R.id.capture_1_btn);
        mCaptureBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onCapture();
            }
        });

        mCloseBtn = (Button) findViewById(R.id.close_btn);
        mCloseBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onClose();
            }
        });

        mMatchBtn = (Button) findViewById(R.id.match_btn);
        mMatchBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onMatch();
            }
        });
        mMatchSpacer = findViewById(R.id.match_spacer);

        mStatusTextView = (TextView) findViewById(status);
        mInfoTextView = (TextView) findViewById(R.id.info);

        mScanTypeSpinner = (Spinner) findViewById(R.id.scan_type_spinner);
        if (mScanTypeSpinner != null) {
            ArrayAdapter<CharSequence> scan_type_adapter = ArrayAdapter.createFromResource(getContext(), R.array.scan_type, android.R.layout.simple_spinner_item);
            scan_type_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mScanTypeSpinner.setAdapter(scan_type_adapter);
            mScanTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mScanType = scan_types[position];
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Nothing To Handle
                }
            });
        }
        mSpinnerSpacer = findViewById(R.id.spinner_spacer);

        mBitrateSpinner = (Spinner) findViewById(R.id.bitrate_spinner);
        if (mBitrateSpinner != null) {
            ArrayAdapter<CharSequence> bitrate_adapter = ArrayAdapter.createFromResource(getContext(), R.array.bitrate_array, android.R.layout.simple_spinner_item);
            bitrate_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mBitrateSpinner.setAdapter(bitrate_adapter);
            mBitrateSpinner.setSelection(1);
            mBitrateSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String item = mBitrateSpinner.getSelectedItem().toString();
                    String value = item.replaceAll(".*(\\d+\\.\\d+).*", "$1");
                    mBitrate = Float.parseFloat(value);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
    }

    @Override
    public String getTitle() {
        // Return title of current activity
        return getContext().getResources().getString(R.string.title_fingerprint);
    }

    @Override
    public void activate(Biometrics biometrics) {
        // Initialize new biometrics and set up page for usage
        this.mBiometrics = biometrics;

        boolean show_spinner = biometrics.getFingerprintScannerType() == FingerprintScannerType.FAP45;
        if (mScanTypeSpinner != null) {
            mScanTypeSpinner.setVisibility(show_spinner ? VISIBLE : GONE);
            mSpinnerSpacer.setVisibility(show_spinner ? VISIBLE : GONE);
        }

        // Sets which type of listener for grab fingerprint api call
        useOnFingerprintGrabbedFullListener = show_spinner;

        // determines if device has FMD match
        mHasMatcher = mActivity.hasFmdMatcher();
        mMatchSpacer.setVisibility(mHasMatcher ? VISIBLE : GONE);
        mMatchBtn.setVisibility(mHasMatcher ? VISIBLE : GONE);

        String name = mBiometrics.getProductName();

        if (name.equals("Trident-1") || name.equals("Trident-2") || name.equals("Credence TAB") || name.equals("Credence TAB V1")
                || name.equals("Credence TAB V2") || name.equals("Credence TAB V3") || name.equals("Credence TAB V4")) {
            isOnFingerprintGrabbedWSQListener = true;
        }


        doResume();
    }

    @Override
    public void doResume() {
        // Reset capture since user left
        if (mFmd1 == null && !mIsCapturing) {
            resetCapture();
        }
    }

    @Override
    public void deactivate() {
        // Nothing to do
    }

    // Called from capture button will call Grab Fingerprint API and use 1 of 2 listeners
    // This is set in the activate method based on type of scanner
    private void onCapture() {
        // Start by resetting page for new capture
        resetCapture();

        //disable capture button to avoid double clicks
        mCaptureBtn.setEnabled(false);

        // turn on close button so user can close at anytime
        mCloseBtn.setEnabled(true);

        setStatusText("initalizing...");

        mIsCapturing = true;

        // disable scanner
        mScanTypeSpinner.setEnabled(false);


        if (isOnFingerprintGrabbedWSQListener) {

            mBiometrics.grabFingerprint(mScanType, new Biometrics.OnFingerprintGrabbedWSQListener() {
                @Override
                public void onFingerprintGrabbed(Biometrics.ResultCode result, Bitmap bm, byte[] iso, String filepath,
                                                 String wsq, String hint, int nfiqScore) {

                    start_time = SystemClock.elapsedRealtime();

                    if (bm != null) {
                        // Set the image to image view for user to see
                        mCaptureImage.setImageBitmap(bm);
                    }
                    // If hint was given back set text to passed hint
                    if (hint != null && !hint.isEmpty()) {
                        setStatusText(hint);
                    }
                    // If result was OK meaning proper image captured, then handle case
                    if (result == ResultCode.OK) {
                        // MEE 12/28/2016 - moved shutter from CredenceService to here in the client
                        Beeper.getInstance().click();
                        captureState();


                        // Calculate total time taken for image to return back as good
                        long duration = SystemClock.elapsedRealtime() - start_time;
                        // Log output for debugging
                        Log.i(TAG, "Capture Complete in " + duration + "msec");
                        // Set text for user to see how long capturing process took
                        setStatusText("Capture Complete in " + duration + "msec");

                        // Set global path variable of image
                        mPathname = filepath;

                        mCurrentBitmap = bm;

                        showImageSize(filepath, wsq, duration);

                        // Get fingerprint quality
                        setStatusText("Fingerprint Quality: " + nfiqScore);
                        Log.d(TAG, "NFIQ Score - Fingerprint Quality: " + nfiqScore);

                        if (mHasMatcher) {
                            // Set current bitmap image to captured image
                            mCurrentBitmap = bm;
                            // Convert image
                            convertToFmd(mCurrentBitmap);

                        } else {
                            // If there is no associated path name with image
                            if (mPathname == null) {
                                // Log there is no associated path name
                                Log.w(TAG, "onFingerprintGrabbed - OK but filepath null");
                                // Exit outof function, nothing else to do since null path
                                return;
                            }
                        }

                    }
                    // If fingerprint read failed
                    if (result == ResultCode.FAIL) {
                        // Log output error
                        Log.e(TAG, "onFingerprintGrabbed - FAILED");
                        // Let user know captured failed
                        setStatusText("Fingerprint Open-FAILED");
                        closeState();
                    }
                }

                @Override
                public void onCloseFingerprintReader(ResultCode resultCode, CloseReasonCode closeReasonCode) {

                    if (resultCode == ResultCode.OK) {
                        // Log output for debugging
                        Log.d(TAG, "FingerPrint reader closed:" + closeReasonCode.toString());
                        // Let uesr know why finger print reader closed
                        setStatusText("FingerPrint reader closed:" + closeReasonCode.toString());
                        resetCapture();
                    } else if (resultCode == ResultCode.FAIL) {
                        mCloseBtn.setEnabled(true);
                        Log.d(TAG, "FingerPrint reader closed: FAILED");
                        // Let uesr know why finger print reader closed
                        setStatusText("FingerPrint reader closed: FAILED");
                    }
                }
            });
        }

        //  If using older version API
        else if (!useOnFingerprintGrabbedFullListener) {
            // Keep a track of time. Used to check if peripheral has not responded in given time
            start_time = SystemClock.elapsedRealtime();
            // Call grabFingerprint functions

            mBiometrics.grabFingerprint(mScanType, new OnFingerprintGrabbedListener() {
                @Override
                public void onFingerprintGrabbed(ResultCode result, Bitmap bm, byte[] iso, String filepath, String hint) {

                    if (bm != null) {
                        // Set the image to image view for user to see
                        mCaptureImage.setImageBitmap(bm);
                    }

                    //  If hint was given back set text to passed hint
                    if (hint != null && !hint.isEmpty()) {
                        setStatusText(hint);
                    }

                    // If result was OK meaning proper image captured, then handle case
                    if (result == ResultCode.OK) {
                        // MEE 12/28/2016 - moved shutter from CredenceService to here in the client
                        Beeper.getInstance().click();
                        captureState();

                        // Calculate total time taken for image to return back as good
                        long duration = SystemClock.elapsedRealtime() - start_time;
                        // Log output for debugging
                        Log.i(TAG, "Capture Complete in " + duration + "msec");
                        // Set text for user to see how long capturing process took
                        //    setStatusText("Capture Complete in " + duration + "msec");

                        // Set global path variable of image
                        mPathname = filepath;

                        // Get fingerprint quality
                        getNfiqScore(mCurrentBitmap);

                        // Create wsq image
                        createWsqImage(mPathname);

                        if (mHasMatcher) {
                            // Set current bitmap image to captured image
                            mCurrentBitmap = bm;
                            // Convert image
                            convertToFmd(mCurrentBitmap);

                            // Get fingerprint quality
                            getNfiqScore(mCurrentBitmap);

                        } else {
                            // If there is no associated path name with image
                            if (mPathname == null) {
                                // Log there is no associated path name
                                Log.w(TAG, "onFingerprintGrabbed - OK but filepath null");
                                // Exit outof function, nothing else to do since null path
                                return;
                            }

                        }

                    }
                    // If fingerprint read failed
                    if (result == ResultCode.FAIL) {
                        // Log output error
                        Log.e(TAG, "onFingerprintGrabbed - FAILED");
                        // Let user know captured failed
                        setStatusText("Fingerprint Open-FAILED");
                        closeState();

                    }

                }

                @Override
                public void onCloseFingerprintReader(ResultCode resultCode, CloseReasonCode reasonCode) {

                    if (resultCode == ResultCode.OK) {
                        // Log output for debugging
                        Log.d(TAG, "FingerPrint reader closed:" + reasonCode.toString());
                        // Let uesr know why finger print reader closed
                        setStatusText("FingerPrint reader closed:" + reasonCode.toString());
                        resetCapture();
                    } else if (resultCode == ResultCode.FAIL) {
                        mCloseBtn.setEnabled(true);
                        Log.d(TAG, "FingerPrint reader closed: FAILED");
                        // Let uesr know why finger print reader closed
                        setStatusText("FingerPrint reader closed: FAILED");
                    }
                }
            });

        } else { // If user newest API version
            mBiometrics.grabFingerprint(mScanType, new OnFingerprintGrabbedFullListener() {
                @Override
                public void onFingerprintGrabbed(Biometrics.ResultCode result, Bitmap bm, Bitmap bitmap_finger1, Bitmap bitmap_finger2,
                                                 byte[] iso, byte[] iso_finger1, byte[] iso_finger2, String filepath, String filepath_finger1, String filepath_finger2, String hint) {
                    // If image received back is not null
                    if (bm != null) {
                        // Set image view to returned image bitmap for user
                        mCaptureImage.setImageBitmap(bm);
                    }
                    // If hint returned back was not null
                    if (hint != null && !hint.isEmpty()) {
                        // Set hint for user to see on text view
                        setStatusText(hint);
                    }
                    // If image captured was good fingerprint
                    if (result == ResultCode.OK) {
                        // MEE 12/28/2016 - moved shutter from CredenceService to here in the client
                        Beeper.getInstance().click();
                        // Log output for debugging purposes
                        Log.d(TAG, "OnFingerprintGrabbedFullListener: mScanType        = [" + mScanType + "]");
                        Log.d(TAG, "OnFingerprintGrabbedFullListener: filepath         = [" + filepath + "]");
                        Log.d(TAG, "OnFingerprintGrabbedFullListener: filepath_finger1 = [" + filepath_finger1 + "]");
                        Log.d(TAG, "OnFingerprintGrabbedFullListener: filepath_finger2 = [" + filepath_finger2 + "]");
                        // If user did two fingers split image capture
                        if (mScanType.equals(ScanType.TWO_FINGERS_SPLIT)) {
                            // Log output saying what kind of image scan type was used
                            Log.d(TAG, "OnFingerprintGrabbedFullListener: Showing Split Images");
                            // Turn on main single finger image view
                            mCaptureImage.setVisibility(INVISIBLE);
                            // If finger one was valid set fingerOne image view to finger one image
                            if (bitmap_finger1 != null) {
                                mCaptureImageFinger1.setImageBitmap(bitmap_finger1);
                            }
                            // If finger two was valid set fingerTwo image view to finger two image
                            if (bitmap_finger2 != null) {
                                mCaptureImageFinger2.setImageBitmap(bitmap_finger2);
                            }
                            // Turn on individual finger capture image views for user to see
                            mCaptureImageFinger1.setVisibility(VISIBLE);
                            mCaptureImageFinger2.setVisibility(VISIBLE);
                        } else { // else for any other device of capture type (single finger)
                            // If image returned was valid set image view
                            if (bm != null) {
                                // Set image to image view for user to see
                                mCaptureImage.setImageBitmap(bm);
                            }
                            // Set image view visible so user may actually see it
                            mCaptureImage.setVisibility(VISIBLE);
                            // Based on which image view is valid turn each one off since single finger capture
                            if (mCaptureImageFinger1 != null) {
                                mCaptureImageFinger1.setVisibility(INVISIBLE);
                            }
                            if (mCaptureImageFinger2 != null) {
                                mCaptureImageFinger2.setVisibility(INVISIBLE);
                            }
                        }

                        captureState();

                        // Set global path variables for image locations
                        mPathname = filepath;
                        mPathnameFinger1 = filepath_finger1;
                        mPathnameFinger2 = filepath_finger2;

                        if (mHasMatcher) {
                            // Set current bitmap image to captured image
                            if (mScanType.equals(ScanType.TWO_FINGERS_SPLIT)) {
                                mCurrentBitmap = bitmap_finger1;
                            } else {
                                mCurrentBitmap = bm;
                            }

                            // Convert image
                            convertToFmd(mCurrentBitmap);

                            getNfiqScore(mCurrentBitmap);

                        } else {
                            // If there is no associated path name with image
                            if (mPathname == null) {
                                // Log there is no associated path name
                                Log.w(TAG, "onFingerprintGrabbed - OK but filepath null");
                                // Exit outof function, nothing else to do since null path
                                return;
                            }
                            // Convert to different file type
                            // createWsqImage(mPathname);

                        }
                    }

                    // If fingerprint read failed
                    if (result == ResultCode.FAIL) {
                        // Log output error
                        Log.e(TAG, "onFingerprintGrabbed - FAILED");
                        // Let user know captured failed
                        setStatusText("Fingerprint Open-FAILED");
                        closeState();
                    }

                }

                @Override
                public void onCloseFingerprintReader(ResultCode resultCode, CloseReasonCode reasonCode) {
                    if (resultCode == ResultCode.OK) {
                        // Log output for debugging
                        Log.d(TAG, "FingerPrint reader closed:" + reasonCode.toString());
                        // Let uesr know why finger print reader closed
                        setStatusText("FingerPrint reader closed:" + reasonCode.toString());
                        // Turn off appropriate buttons
                        resetCapture();
                    } else if (resultCode == ResultCode.FAIL) {
                        mCloseBtn.setEnabled(true);
                        Log.d(TAG, "FingerPrint reader closed: FAILED");
                        // Let uesr know why finger print reader closed
                        setStatusText("FingerPrint reader closed: FAILED");
                    }
                }
            });
        }
    }

    // Called from close button
    private void onClose() {
        // Start by resetting capture system
        resetCapture();

        // Disable capture button to avoid double clicks
        mCaptureBtn.setEnabled(false);

        mCloseBtn.setEnabled(false);

        setStatusText("Closing scanner, Please wait...");

        // Close fingerprint sensor
        mBiometrics.closeFingerprintReader();
    }

    // if FMD match is for device then this api is called to create template for matching
    private void convertToFmd(Bitmap capturedImage) {
        // Keep track of initial time to see how long conversion takes
        start_time = SystemClock.elapsedRealtime();
        // Call biometrics API for image conversion
        mBiometrics.convertToFmd(capturedImage, Biometrics.FmdFormat.ISO_19794_2_2005, new OnConvertToFmdListener() {
            @Override
            public void onConvertToFmd(ResultCode result, byte[] fmd) {
                // If result failed, log output
                if (result != ResultCode.OK || fmd == null) {
                    Log.w(TAG, "convertToFmd failed so mFmd1 is null");
                    setStatusText("Failed to convert to FMD");
                } else {   // If conversion succeeded set global Fmd image variable and set appropriate buttons
                    mFmd1 = fmd;
                    mMatchBtn.setEnabled(true);
                }
                // Calculate total time for callback & log output for debugging purposes
                long duration = SystemClock.elapsedRealtime() - start_time;
                Log.d(TAG, "convertToFmd " + String.valueOf(duration) + "ms");
            }
        });
    }

    // Called from Match button will call grab fingerprint api and capture another fingerprint to be matched
    // with the previous one that was scanned
    private void onMatch() {
        // turn off so no duplicate clicks
        mMatchBtn.setEnabled(false);
        // If a fingerprint image was not taken or failed to return
        if (mCurrentBitmap == null) {
            // Let user know, no image exists
            setStatusText("NO Fingerprint image ");
            return;
        }
        // If Fmd image type does not exist
        if (mFmd1 == null) {
            // Let user know
            setStatusText("NO Fingerprint template");
            return;
        }

        resetCaptureImages();

        mCaptureBtn.setEnabled(false);
        setStatusText("");
        setInfoText("");


        // If using older API version
        if (!useOnFingerprintGrabbedFullListener) {
            // Make API call
            mBiometrics.grabFingerprint(mScanType, new OnFingerprintGrabbedListener() {
                @Override
                public void onFingerprintGrabbed(Biometrics.ResultCode result, Bitmap bm, byte[] iso, String filepath, String hint) {
                    // Image returned
                    if (bm != null) {
                        // Set image
                        mCaptureImage.setImageBitmap(bm);
                    }
                    if (hint != null && !hint.isEmpty()) {
                        setStatusText(hint);
                    }
                    // OK result and image
                    if (result == ResultCode.OK && bm != null) {
                        // MEE 12/28/2016 - moved shutter from CredenceService to here in the client
                        Beeper.getInstance().click();

                        // Set global path variable for image location
                        mPathname = filepath;
                        convertMatchImage(bm);
                    }
                    // If result failed turn on button to match
                    if (result == ResultCode.FAIL) {
                        setStatusText("Matching Fingerprint Grab-FAILED");
                        captureState();
                    }
                }

                @Override
                public void onCloseFingerprintReader(ResultCode resultCode, CloseReasonCode reasonCode) {
                    if (resultCode == ResultCode.OK) {
                        // Log output for why reader closed
                        Log.d(TAG, "FingerPrint reader closed:" + reasonCode.toString());
                        // Let user know why closed
                        setStatusText("FingerPrint reader closed:" + reasonCode.toString());
                        closeState();
                    } else if (resultCode == ResultCode.FAIL) {
                        Log.d(TAG, "FingerPrint reader closed: FAILED");
                        // Let uesr know why finger print reader closed
                        setStatusText("FingerPrint reader closed: FAILED");
                    }
                }
            });

        } else {
            // If using latest API version, make fingerprint call
            mBiometrics.grabFingerprint(mScanType, new OnFingerprintGrabbedFullListener() {
                @Override
                public void onFingerprintGrabbed(Biometrics.ResultCode result, Bitmap bm, Bitmap bitmap_finger1, Bitmap bitmap_finger2, byte[] iso,
                                                 byte[] iso_finger1, byte[] iso_finger2, String filepath, String filepath_finger1,
                                                 String filepath_finger2, String hint) {
                    // If result image exists then set image view to taken image
                    if (bm != null) {
                        mCaptureImage.setImageBitmap(bm);
                    }
                    // If hint exists set it for user to see
                    if (hint != null && !hint.isEmpty()) {
                        setStatusText(hint);
                    }
                    // If returned result was good, set match button on for user to match fingerprint and convert image
                    if (result == ResultCode.OK && bm != null) {
                        // MEE 12/28/2016 - moved shutter from CredenceService to here in the client
                        Beeper.getInstance().click();
                        // Log output for debugging purposes
                        Log.d(TAG, "onMatch()::OnFingerprintGrabbedFullListener: mScanType        = [" + mScanType + "]");
                        Log.d(TAG, "onMatch()::OnFingerprintGrabbedFullListener: filepath         = [" + filepath + "]");
                        Log.d(TAG, "onMatch()::OnFingerprintGrabbedFullListener: filepath_finger1 = [" + filepath_finger1 + "]");
                        Log.d(TAG, "onMatch()::OnFingerprintGrabbedFullListener: filepath_finger2 = [" + filepath_finger2 + "]");
                        // If user did two fingers split image capture
                        if (mScanType.equals(ScanType.TWO_FINGERS_SPLIT)) {
                            // Log output saying what kind of image scan type was used
                            Log.d(TAG, "OnFingerprintGrabbedFullListener: Showing Split Images");
                            // Turn on main single finger image view
                            mCaptureImage.setVisibility(INVISIBLE);
                            // If finger one was valid set fingerOne image view to finger one image
                            if (bitmap_finger1 != null) {
                                mCaptureImageFinger1.setImageBitmap(bitmap_finger1);
                            }
                            // If finger two was valid set fingerTwo image view to finger two image
                            if (bitmap_finger2 != null) {
                                mCaptureImageFinger2.setImageBitmap(bitmap_finger2);
                            }
                            // Turn on individual finger capture image views for user to see
                            mCaptureImageFinger1.setVisibility(VISIBLE);
                            mCaptureImageFinger2.setVisibility(VISIBLE);
                        } else { // else for any other device of capture type (single finger)
                            // If image returned was valid set image view
                            // Set image to image view for user to see
                            mCaptureImage.setImageBitmap(bm);
                            // Set image view visible so user may actually see it
                            mCaptureImage.setVisibility(VISIBLE);
                            // Based on which image view is valid turn each one off since single finger capture
                            if (mCaptureImageFinger1 != null) {
                                mCaptureImageFinger1.setVisibility(INVISIBLE);
                            }
                            if (mCaptureImageFinger2 != null) {
                                mCaptureImageFinger2.setVisibility(INVISIBLE);
                            }
                        }

                        // Set global path variables for image locations
                        mPathname = filepath;
                        mPathnameFinger1 = filepath_finger1;
                        mPathnameFinger2 = filepath_finger2;
                        convertMatchImage(bm);
                    }
                    // If result failed turn on button to match
                    if (result == ResultCode.FAIL) {
                        setStatusText("Matching Fingerprint Grab-FAILED");
                        captureState();
                    }
                }

                @Override
                public void onCloseFingerprintReader(ResultCode resultCode, CloseReasonCode reasonCode) {
                    if (resultCode == ResultCode.OK) {
                        // Log output for why reader closed
                        Log.d(TAG, "FingerPrint reader closed:" + reasonCode.toString());
                        // Let user know why closed
                        setStatusText("FingerPrint reader closed:" + reasonCode.toString());
                        closeState();

                    } else if (resultCode == ResultCode.FAIL) {
                        Log.d(TAG, "FingerPrint reader closed: FAILED");
                        // Let uesr know why finger print reader closed
                        setStatusText("FingerPrint reader closed: FAILED");
                    }
                }
            });
        }
    }

    // method to called after match fingerprint was captured to convert to template
    private void convertMatchImage(Bitmap inputImage) {
        // Make API call to convert image to Fmd, pass callback to handle results
        mBiometrics.convertToFmd(inputImage, Biometrics.FmdFormat.ISO_19794_2_2005, new OnConvertToFmdListener() {
            @Override
            public void onConvertToFmd(ResultCode result, byte[] fmd) {
                // If conversion failed
                if (result != ResultCode.OK || fmd == null) {
                    Log.w(TAG, "convertMatchImage failed");
                    setStatusText("convertToFmd on matching fingerprint failed so NO Match");
                    captureState();
                } else {
                    // Log output, size of converted image
                    Log.d(TAG, "Received FMD of size: " + fmd.length);
                    mFmd2 = fmd;
                    // If second converted image was good
                    if (mFmd1 != null) {
                        // Compare two images
                        compareFmd(mFmd1, mFmd2, "convertMatchImage:");
                    } else {
                        // No image to compare to
                        Log.e(TAG, "NO FMD-1 to match against");
                    }
                }
            }
        });
    }

    // After match fingerpint has been converted to template this method is called to compare both
    private void compareFmd(byte[] fmd1, byte[] fmd2, final String info) {
        // Get a start time to keep track of when result comes back
        start_time = SystemClock.elapsedRealtime();
        // Log output for debugging purposes
        Log.d(TAG, "FMD1 = " + fmd1.length + " data " + fmd1);
        Log.d(TAG, "FMD2 = " + fmd2.length + " data " + fmd2);
        // Call API function to compare Fmd type images, pass callback to handle result
        mBiometrics.compareFmd(fmd1, fmd2, Biometrics.FmdFormat.ISO_19794_2_2005, new OnCompareFmdListener() {
            @Override
            public void onCompareFmd(ResultCode result, float dissimilarity) {
                String matchDecision;
                // Calculate time taken for result from API function call
                long duration = SystemClock.elapsedRealtime() - start_time;
                // If returned result was not good
                if (result != ResultCode.OK) {
                    // Let user know failure
                    setStatusText("compareFmd failed");
                } else {
                    // If result is good
                    // dissimilarity score ranges from 0 to Integer.MAX_VALUE (2147483647)
                    // a score of less than 2147 is a match with false positive probability of 1 in million.
                    if (dissimilarity < (Integer.MAX_VALUE / 1000000)) {
                        matchDecision = "HIT. ";
                    } else {
                        matchDecision = "NO-HIT. ";
                    }
                    String str = String.format(matchDecision + " Dur: %dms, Dissimilarity Score %d", duration, (int) dissimilarity);
                    Log.d(TAG, str);
                    setStatusText(matchDecision);
                }
                captureState();
            }
        });
    }

    // Will call if device is not FMD
    private void createWsqImage(String pathname) {
        // Initialize variables for bitmap image conversion
        uncompressed_size = 0;
        File uncompressed = new File(pathname);
        // If image exists then get length of image
        if (uncompressed.exists()) {
            uncompressed_size = uncompressed.length();
        }
        // Keep track of initialize time to see how long conversions takes
        start_time = SystemClock.elapsedRealtime();
        // Log output for debugging purposes
        Log.d(TAG, "convertToWsq - bitrate: " + mBitrate);
        // Call biometrics API for converting images
        mBiometrics.convertToWsq(pathname, mBitrate, new OnConvertToWsqListener() {
            @Override
            public void onConvertToWsq(ResultCode result, String pathname) {
                // If result is in between FAIL and OK, it is still going through algorithms for result
                // Log output letting user know it is still calculating
                if (result == ResultCode.INTERMEDIATE) {
                    setInfoText("Service initializing...");
                    // If result failed, let user know
                } else if (result == ResultCode.FAIL) {
                    setInfoText("Convert to WSQ failed");
                } else {  // If result was OK and properly converted image
                    // Reset variables, check if file exists, and get length
                    compressed_size = 0;
                    File compressed = new File(pathname);
                    if (compressed.exists()) {
                        compressed_size = compressed.length();
                    }
                    // Calculate total time taken for image conversion
                    long duration = SystemClock.elapsedRealtime() - start_time;
                    // Set textview letting user know image conversion specifications & duration
                    String str = String.format(
                            "PNG: %s, WSQ: %s, Dur: %dms",
                            TheApp.abbreviateNumber(uncompressed_size),
                            TheApp.abbreviateNumber(compressed_size),
                            duration);
                    setInfoText(str);
                }

                captureState();
            }
        });
    }

    // Display uncompressed and compressed images
    private void showImageSize(String png, String wsq, long duration) {
        uncompressed_size = 0;
        File uncompressed = new File(png);
        if (uncompressed.exists()) {
            uncompressed_size = uncompressed.length();
        }

        File compressed = new File(wsq);
        if (compressed.exists()) {
            compressed_size = compressed.length();
        }

        String str = String.format(
                "PNG: %s, WSQ: %s, Dur: %dms",
                TheApp.abbreviateNumber(uncompressed_size),
                TheApp.abbreviateNumber(compressed_size),
                duration);
        setInfoText(str);

    }

    // Re-sets UI state
    private void resetCapture() {
        // Reset all capture images
        resetCaptureImages();

        mFmd1 = null;

        mIsCapturing = false;

        // Clear text view
        setInfoText("");

        // set buttons to close state
        closeState();
    }

    private void resetCaptureImages() {
        // Make image view visible
        mCaptureImage.setVisibility(VISIBLE);
        // Based on what image is available make image turned off for user
        if (mCaptureImageFinger1 != null) {
            mCaptureImageFinger1.setVisibility(INVISIBLE);
            mCaptureImageFinger1.setImageDrawable(null);
        }
        if (mCaptureImageFinger2 != null) {
            mCaptureImageFinger2.setVisibility(INVISIBLE);
            mCaptureImageFinger2.setImageDrawable(null);
        }

        // Set image view to null for fresh capture
        mCaptureImage.setImageDrawable(null);
    }

    // Sets buttons to fingerprint capture state
    private void captureState() {
        mCaptureBtn.setEnabled(true);
        // Check if match button should be enabled
        boolean enable_match = mHasMatcher && mFmd1 != null;
        mMatchBtn.setEnabled(enable_match);
        mCloseBtn.setEnabled(true);
        // set this to false to prevent onresume method bing called
        mIsCapturing = false;
    }

    // Sets buttons to close state
    private void closeState() {
        mCaptureBtn.setEnabled(true);
        mCloseBtn.setEnabled(false);
        mMatchBtn.setEnabled(false);

        mScanTypeSpinner.setEnabled(true);
    }

    // display status
    private void setStatusText(String text) {
        // If text passed contains a string
        if (!text.isEmpty()) {
            // Log output for debugging, passed text
            Log.d(TAG, "setStatusText: " + text);
        }
        mStatusTextView.setText(text);
    }

    // display info
    private void setInfoText(String text) {
        // If passed text contains a string
        if (!text.isEmpty()) {
            // Log output for debugging, text passed
            Log.d(TAG, "setInfoText: " + text);
        }
        // If text view responsible for showing user status info initialized
        if (mInfoTextView != null) {
            // Set text view to passed text
            mInfoTextView.setText(text);
        } else if (!text.isEmpty()) {
            // If it has not been initialized, set status text view to info
            setStatusText(text);
        }
    }

    // get fingerprint quality
    private void getNfiqScore(Bitmap bitmap) {

        mBiometrics.getFingerQuality(bitmap, new Biometrics.OnGetFingerQualityListener() {
            @Override
            public void onGetFingerQuality(ResultCode resultCode, int nfiqScore) {

                if (resultCode == ResultCode.OK) {
                    Log.d(TAG, "NFIQ Score - Fingerprint Quality: " + nfiqScore);
                    setStatusText("Fingerprint Quality: " + nfiqScore);

                } else {
                    Log.d(TAG, "NFIQ Score - Fingerprint Quality: " + resultCode + nfiqScore);
                    setStatusText("Fingerprint Quality: " + nfiqScore);
                }

            }
        });

    }

}
