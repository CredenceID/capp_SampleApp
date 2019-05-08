package com.credenceid.sample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.Biometrics.CloseReasonCode;
import com.credenceid.biometrics.Biometrics.OnFingerprintGrabbedWSQListener;
import com.credenceid.biometrics.Biometrics.ResultCode;

import java.util.Arrays;

import static com.credenceid.biometrics.Biometrics.FMDFormat.ISO_19794_2_2005;
import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;

@SuppressWarnings("StatementWithEmptyBody")
@SuppressLint("StaticFieldLeak")
public class FingerprintActivity
        extends Activity {

    /* List of different fingerprint scan types supported across all Credence devices. */
    private final Biometrics.ScanType[] mScanTypes = {
            Biometrics.ScanType.SINGLE_FINGER,
            /* These three scan types are only supported on Trident family of devices. */
            Biometrics.ScanType.TWO_FINGERS,
            Biometrics.ScanType.ROLL_SINGLE_FINGER,
            Biometrics.ScanType.TWO_FINGERS_SPLIT
    };

    /* --------------------------------------------------------------------------------------------
     *
     * Components in layout file.
     *
     * --------------------------------------------------------------------------------------------
     */
    private ImageView mFingerprintOneImageView;
    private ImageView mFingerprintTwoImageView;
    private Button mOpenCloseButton;
    private Button mCaptureButton;
    private Button mMatchButton;
    private TextView mStatusTextView;
    private TextView mInfoTextView;

    /* If true, then "mOpenClose" button text is "Open" meaning we need to open fingerprint.
     * If false, then "mOpenClose" button text is "Close" meaning we need to close fingerprint.
     */
    private boolean mOpenFingerprint = true;
    /* We are capturing two fingerprints. If true then saves data as first fingerprint; if false
     * saves data as second fingerprint.
     */
    private boolean mCaptureFingerprintOne = true;
    /* Stores FMD templates (used for fingerprint matching) for each fingerprint. */
    private byte[] mFingerprintOneFMDTemplate = null;
    private byte[] mFingerprintTwoFMDTemplate = null;

    /* --------------------------------------------------------------------------------------------
     *
     * Callbacks.
     *
     * --------------------------------------------------------------------------------------------
     */

    /* Callback invoked every time fingerprint sensor on device opens or closes. */
    private Biometrics.FingerprintReaderStatusListener mFingerprintOpenCloseListener =
            new Biometrics.FingerprintReaderStatusListener() {
                @Override
                public void
                onOpenFingerprintReader(ResultCode resultCode,
                                        String hint) {

                    /* If hint is valid, display it. Regardless of ResultCode we should
                     * message indicating what is going on with sensor.
                     */
                    if (hint != null && !hint.isEmpty())
                        mStatusTextView.setText(hint);

                    /* This code is returned once sensor has fully finished opening. */
                    if (OK == resultCode) {
                        /* Now that sensor is open, if user presses "mOpenCloseButton" fingerprint
                         * sensor should now close. To achieve this we change flag which controls
                         * what action mOpenCloseButton takes.
                         */
                        mOpenFingerprint = false;

                        /* Operation is complete, re-enable button. */
                        mOpenCloseButton.setEnabled(true);
                        /* Only if fingerprint opened do we allow user to capture fingerprints. */
                        mCaptureButton.setEnabled(true);
                        /* If fingerprint opened then we change button to say "Close". */
                        mOpenCloseButton.setText(getString(R.string.close));
                    }
                    /* This code is returned while sensor is in the middle of opening. */
                    else if (INTERMEDIATE == resultCode) {
                        /* Do nothing while operation is still on-going. */
                    }
                    /* This code is returned if sensor fails to open. */
                    else if (FAIL == resultCode) {
                        /* Operation is complete, re-enable button. */
                        mOpenCloseButton.setEnabled(true);
                    }
                }

                @SuppressLint("SetTextI18n")
                @Override
                public void
                onCloseFingerprintReader(ResultCode resultCode,
                                         CloseReasonCode closeReasonCode) {

                    if (OK == resultCode) {
                        mStatusTextView.setText("Fingerprint Closed: " + closeReasonCode.name());

                        /* Now that sensor is closed, if user presses "mOpenCloseButton" fingerprint
                         * sensor should now open. To achieve this we change flag which controls
                         * what action mOpenCloseButton takes.
                         */
                        mOpenFingerprint = true;

                        /* Change text back to "Open" and allow button to be clickable. */
                        mOpenCloseButton.setText(getString(R.string.open));
                        mOpenCloseButton.setEnabled(true);
                        /* Sensor is closed, user should NOT be able to press capture or match. */
                        mCaptureButton.setEnabled(false);
                        mMatchButton.setEnabled(false);

                    } else if (INTERMEDIATE == resultCode) {
                        /* This code is never returned here. */

                    } else if (FAIL == resultCode) {
                        mStatusTextView.setText("Fingerprint FAILED to close.");
                    }
                }
            };

    /* --------------------------------------------------------------------------------------------
     *
     * Android activity lifecycle event methods.
     *
     * --------------------------------------------------------------------------------------------
     */

    @Override
    protected void
    onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint);

        this.initializeLayoutComponents();
        this.configureLayoutComponents();
    }

    @Override
    public void
    onBackPressed() {

        super.onBackPressed();
        App.BioManager.cancelCapture();
        App.BioManager.closeFingerprintReader();
    }

    /* Invoked when application is killed, either by user or system. */
    @Override
    protected void
    onDestroy() {

        super.onDestroy();

        /* Tell biometrics to cancel current on-going capture. */
        App.BioManager.cancelCapture();
        /* Close all open peripherals. */
        App.BioManager.closeFingerprintReader();
    }

    /* --------------------------------------------------------------------------------------------
     *
     * Initialize and configure components.
     *
     * --------------------------------------------------------------------------------------------
     */

    /* Initializes all objects inside layout file. */
    private void
    initializeLayoutComponents() {

        mFingerprintOneImageView = findViewById(R.id.finger_one_imageview);
        mFingerprintTwoImageView = findViewById(R.id.finger_two_imageview);

        mOpenCloseButton = findViewById(R.id.open_close_button);
        mCaptureButton = findViewById(R.id.capture_button);
        mMatchButton = findViewById(R.id.match_button);

        mStatusTextView = findViewById(R.id.status_textview);
        mInfoTextView = findViewById(R.id.fingeprint_info_textview);
    }

    /* Configure all objects in layout file, set up listeners, views, etc. */
    private void
    configureLayoutComponents() {

        /* Only allow capture once fingerprint is open. */
        /* Only allow match once both fingerprints have been captured. */
        this.setCaptureMatchButtonEnable(false);

        mFingerprintOneImageView.setOnClickListener((View v) -> {
            /* This ImageView should turn green since it was selected. */
            mFingerprintOneImageView.setBackground(getResources().getDrawable(R.drawable.image_border_on));
            /* Other ImageView should turn black or off. */
            mFingerprintTwoImageView.setBackground(getResources().getDrawable(R.drawable.image_border_off));

            mCaptureFingerprintOne = true;
        });

        mFingerprintTwoImageView.setOnClickListener((View v) -> {
            /* This ImageView should turn green since it was selected. */
            mFingerprintTwoImageView.setBackground(getResources().getDrawable(R.drawable.image_border_on));
            /* Other ImageView should turn black or off. */
            mFingerprintOneImageView.setBackground(getResources().getDrawable(R.drawable.image_border_off));

            mCaptureFingerprintOne = false;
        });

        /* Inside onClickListeners for each button, we disable all buttons until their respective
         * operation is complete. Once it is done, the appropriate buttons are re-enabled.
         */
        mOpenCloseButton.setOnClickListener((View v) -> {
            /* Disable button so user does not try a second open while fingerprint it opening.
             * Hide capture/math buttons since sensor is opening/closing.
             */
            this.setAllComponentEnable(false);

            if (mOpenFingerprint)
                App.BioManager.openFingerprintReader(mFingerprintOpenCloseListener);
            else App.BioManager.closeFingerprintReader();
        });

        mCaptureButton.setOnClickListener((View v) -> {
            this.setAllComponentEnable(false);
            mInfoTextView.setText("");

            /* Based on which ImageView was selected, capture appropriate fingerprint. */
            if (mCaptureFingerprintOne)
                this.captureFingerprintOne();
            else this.captureFingerprintTwo();
        });

        mMatchButton.setOnClickListener((View v) -> {
            this.setAllComponentEnable(false);
            this.matchFMDTemplates(mFingerprintOneFMDTemplate, mFingerprintTwoFMDTemplate);
        });
    }

    /* --------------------------------------------------------------------------------------------
     *
     * Private Helpers.
     *
     * --------------------------------------------------------------------------------------------
     */

    /* Sets enable for "mCaptureButton" and "mMatchButton" components.
     *
     * @param enable If true components are enabled, if false they are disabled.
     */
    @SuppressWarnings("SameParameterValue")
    private void
    setCaptureMatchButtonEnable(boolean enable) {

        mCaptureButton.setEnabled(enable);
        mMatchButton.setEnabled(enable);
    }

    /* Sets enable for all UI components in layout.
     *
     * @param enable If true components are enabled, if false they are disabled.
     */
    @SuppressWarnings("SameParameterValue")
    private void
    setAllComponentEnable(boolean enable) {

        this.setCaptureMatchButtonEnable(enable);
        mOpenCloseButton.setEnabled(enable);
        mFingerprintOneImageView.setEnabled(enable);
        mFingerprintTwoImageView.setEnabled(enable);
    }

    /* Make CredenceSDK API calls to capture "first" fingerprint. This is fingerprint image on left
     * side of layout file.
     */
    private void
    captureFingerprintOne() {

        mFingerprintOneFMDTemplate = null;

        /* OnFingerprintGrabbedWSQListener: This listener is to be used if you wish to obtain a WSQ
         * template, fingerprint quality score, along with captured fingerprint image. This saves
         * from having to make separate API calls.
         */
        App.BioManager.grabFingerprint(mScanTypes[0], new OnFingerprintGrabbedWSQListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void
            onFingerprintGrabbed(ResultCode resultCode,
                                 Bitmap bitmap,
                                 byte[] bytes,
                                 String filepath,
                                 String wsqFilepath,
                                 String hint,
                                 int nfiqScore) {

                /* If a valid hint was given then display it for user to see. */
                if (hint != null && !hint.isEmpty())
                    mStatusTextView.setText(hint);

                /* This code is returned once sensor captures fingerprint image. */
                if (OK == resultCode) {
                    if (null != bitmap)
                        mFingerprintOneImageView.setImageBitmap(bitmap);

                    mStatusTextView.setText("WSQ File: " + wsqFilepath);
                    mInfoTextView.setText("Quality: " + nfiqScore);

                    /* Create template from fingerprint image. */
                    createFMDTemplate(bitmap);
                }
                /* This code is returned on every new frame/image from sensor. */
                else if (INTERMEDIATE == resultCode) {
                    /* On every frame, if image preview is available, show it to user. */
                    if (null != bitmap)
                        mFingerprintOneImageView.setImageBitmap(bitmap);

                    /* This hint is returned if cancelCapture()" or "closeFingerprint()" are called
                     * while in middle of capture.
                     */
                    if (hint != null && hint.equals("Capture Stopped"))
                        setAllComponentEnable(true);
                }
                /* This code is returned if sensor fails to capture fingerprint image. */
                else if (FAIL == resultCode) {
                    setAllComponentEnable(true);
                }
            }

            @Override
            public void
            onCloseFingerprintReader(ResultCode resultCode,
                                     CloseReasonCode closeReasonCode) {

                /* This case is already handled by "mFingerprintOpenCloseListener". */
            }
        });
    }

    /* Make CredenceSDK API calls to capture "second" fingerprint. This is fingerprint image on
     * right side of layout file.
     */
    private void
    captureFingerprintTwo() {

        mFingerprintTwoFMDTemplate = null;

        App.BioManager.grabFingerprint(mScanTypes[0], new Biometrics.OnFingerprintGrabbedListener() {
            @Override
            public void
            onFingerprintGrabbed(ResultCode resultCode,
                                 Bitmap bitmap,
                                 byte[] bytes,
                                 String s,
                                 String hint) {

                /* If a valid hint was given then display it for user to see. */
                if (hint != null && !hint.isEmpty())
                    mStatusTextView.setText(hint);

                /* This code is returned once sensor captures fingerprint image. */
                if (OK == resultCode) {
                    if (null != bitmap)
                        mFingerprintTwoImageView.setImageBitmap(bitmap);

                    /* Create template from fingerprint image. */
                    createFMDTemplate(bitmap);
                }
                /* This code is returned on every new frame/image from sensor. */
                else if (INTERMEDIATE == resultCode) {
                    /* On every frame, if image preview is available, show it to user. */
                    if (null != bitmap)
                        mFingerprintTwoImageView.setImageBitmap(bitmap);

                    /* This hint is returned if cancelCapture()" or "closeFingerprint()" are called
                     * while in middle of capture.
                     */
                    if (hint != null && hint.equals("Capture Stopped"))
                        setAllComponentEnable(true);
                }
                /* This code is returned if sensor fails to capture fingerprint image. */
                else if (FAIL == resultCode) {
                    setAllComponentEnable(true);
                }
            }

            @Override
            public void
            onCloseFingerprintReader(ResultCode resultCode,
                                     CloseReasonCode closeReasonCode) {

                /* This case is already handled by "mFingerprintOpenCloseListener". */
            }
        });
    }

    /* Attempts to create a FMD template from given Bitmap image. If successful it saves FMD to
     * respective fingerprint template array.
     *
     * @param bitmap
     */
    @SuppressLint("SetTextI18n")
    private void
    createFMDTemplate(Bitmap bitmap) {

        /* Keep a track of how long it takes for FMD creation. */
        final long startTime = SystemClock.elapsedRealtime();

        App.BioManager.convertToFMD(bitmap, ISO_19794_2_2005, (ResultCode resultCode,
                                                               byte[] bytes) -> {

            if (OK == resultCode) {
                /* Display how long it took for FMD template to be created. */
                final double durationInSeconds = (SystemClock.elapsedRealtime() - startTime) / 1000.0;
                mInfoTextView.setText("Created FMD template in: " + durationInSeconds + " seconds.");

                if (mCaptureFingerprintOne)
                    mFingerprintOneFMDTemplate = Arrays.copyOf(bytes, bytes.length);
                else mFingerprintTwoFMDTemplate = Arrays.copyOf(bytes, bytes.length);

                /* If both templates have been created then enable Match button. */
                if (mFingerprintOneFMDTemplate != null && mFingerprintTwoFMDTemplate != null)
                    mMatchButton.setEnabled(true);

            } else if (INTERMEDIATE == resultCode) {
                /* This code is never returned here. */

            } else if (FAIL == resultCode) {
                mStatusTextView.setText("Failed to create FMD template.");
            }

            setAllComponentEnable(true);
        });
    }

    /* Matches two FMD templates and displays score.
     *
     * @param templateOne FMD template.
     * @param templateTwo FMD template to match against.
     */
    @SuppressLint("SetTextI18n")
    private void
    matchFMDTemplates(byte[] templateOne,
                      byte[] templateTwo) {

        /* Normally one would handle parameter checking, but this API handles it for us. Meaning
         * that if any FMD is invalid it will return the proper score of 0, etc.
         */
        App.BioManager.compareFMD(templateOne, templateTwo, ISO_19794_2_2005,
                (ResultCode resultCode, float dissimilarity) -> {

                    if (OK == resultCode) {
                        String matchDecision = "No Match";
                        /* This is how to properly determine a match or not. */
                        if (dissimilarity < (Integer.MAX_VALUE / 1000000))
                            matchDecision = "Match";

                        mStatusTextView.setText("Matching complete.");
                        mInfoTextView.setText("Match outcome: " + matchDecision);

                    } else if (INTERMEDIATE == resultCode) {
                        /* This API will never return ResultCode.INTERMEDIATE. */

                    } else if (FAIL == resultCode) {
                        mStatusTextView.setText("Failed to compare templates.");
                        mInfoTextView.setText("");
                    }

                    /* Re-enable all components since operation is now complete. */
                    this.setAllComponentEnable(true);
                });
    }
}