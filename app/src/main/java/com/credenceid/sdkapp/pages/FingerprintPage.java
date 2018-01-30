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
import android.widget.Toast;

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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;

import static com.credenceid.biometrics.Biometrics.ResultCode.OK;
import static com.credenceid.sdkapp.R.id.status;

public class FingerprintPage extends LinearLayout implements PageView {
    private static final String TAG = FingerprintPage.class.getName();
    private final Biometrics.ScanType scan_types[] = {
            ScanType.SINGLE_FINGER,
            ScanType.TWO_FINGERS, ScanType.ROLL_SINGLE_FINGER,
            ScanType.TWO_FINGERS_SPLIT
    };
    private SampleActivity sampleActivity;
    private Biometrics biometrics;
    private Button buttonCapture;
    private Button buttonClose;
    private Button buttonMatch;

    private View viewMatchButtonSpacer;
    private Spinner spinnerScanType;
    private View viewGenericSpinnerSpacer;
    private Spinner spinnerBitrate;

    private ImageView imageViewCapturedImage;
    private ImageView imageViewCapturedImageFinger1;
    private ImageView imageViewCapturedImageFinger2;
    private TextView textViewStatus;
    private TextView textViewInfo;
    private ScanType scanType = ScanType.SINGLE_FINGER;
    private String pathName;
    private String pathNameFingerprint1;
    private String pathNameFingerprint2;
    private float bitrate = 0;
    private boolean hasFmdMatcher;

    private Bitmap currentBitmap;
    private byte[] fmdFingerTemplate1 = null;
    private byte[] fmdFingerTemplate2 = null;

    private long originalImageSize;
    private long compressedImageSize;

    private boolean isCapturing = false;
    // indicates if the grabbed fingper print is saved to disk
    private boolean saveToDisk = false;

    /* The newer API call of grabFingerprint() takes a "onFingerprintGrabbedFullListener" as its
     * listener. This "full" listener can be used on all device types, but to demonstrate the
     * grabFingerprint() API will all three types of listeners, we only this full listener
     * with certain devices.
     */
    private boolean useFingerprintFullListener = true;

    /* The newer API call of grabFingerprint() takes a "onFingerprintGrabbedWSQListener" as its
     * listener. This "wsq" listener can only be used on CredenceTAB/Trident devices. So we only
     * set this flag to true for those device types.
     */
    private boolean useFingerprintWsqListener = false;

    public FingerprintPage(Context context) {
        super(context);
        this.initialize();
    }

    public FingerprintPage(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.initialize();
    }

    public FingerprintPage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.initialize();
    }

    public void setActivity(SampleActivity activity) {
        this.sampleActivity = activity;
    }

    private void initialize() {
        Log.d(TAG, "initialize");
        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.page_fingerprint, this, true);

        this.initializeLayoutViews();
        this.initializeLayoutButtons();
        this.initializeLayoutSpinners();
    }

    private void initializeLayoutViews() {
        imageViewCapturedImage = (ImageView) findViewById(R.id.capture_1_image);
        imageViewCapturedImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(saveToDisk) {
                    sampleActivity.showFullScreenScannedImage(pathName);
                } else {
                    sampleActivity.showFullScreenScannedImage(currentBitmap);
                }
            }
        });

        imageViewCapturedImageFinger1 = (ImageView) findViewById(R.id.capture_finger1_image);
        imageViewCapturedImageFinger1.setVisibility(INVISIBLE);
        imageViewCapturedImageFinger1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sampleActivity.showFullScreenScannedImage(pathNameFingerprint1);
            }
        });

        imageViewCapturedImageFinger2 = (ImageView) findViewById(R.id.capture_finger2_image);
        imageViewCapturedImageFinger2.setVisibility(INVISIBLE);
        imageViewCapturedImageFinger2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sampleActivity.showFullScreenScannedImage(pathNameFingerprint2);
            }
        });

        viewMatchButtonSpacer = findViewById(R.id.match_spacer);
        textViewStatus = (TextView) findViewById(status);
        textViewInfo = (TextView) findViewById(R.id.info);
    }

    private void initializeLayoutButtons() {
        buttonCapture = (Button) findViewById(R.id.capture_1_btn);
        buttonCapture.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickCaptureButton();
            }
        });

        buttonClose = (Button) findViewById(R.id.close_btn);
        buttonClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickCloseButton();
            }
        });

        buttonMatch = (Button) findViewById(R.id.match_btn);
        buttonMatch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickMatchButton();
            }
        });
    }

    private void initializeLayoutSpinners() {
        spinnerScanType = (Spinner) findViewById(R.id.scan_type_spinner);
        if (spinnerScanType != null) {
            ArrayAdapter<CharSequence> scan_type_adapter =
                    ArrayAdapter.createFromResource(getContext(),
                            R.array.scan_type,
                            android.R.layout.simple_spinner_item);
            scan_type_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spinnerScanType.setAdapter(scan_type_adapter);
            spinnerScanType.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    scanType = scan_types[position];
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
        viewGenericSpinnerSpacer = findViewById(R.id.spinner_spacer);

        spinnerBitrate = (Spinner) findViewById(R.id.bitrate_spinner);
        if (spinnerBitrate != null) {
            ArrayAdapter<CharSequence> bitrate_adapter =
                    ArrayAdapter.createFromResource(getContext(),
                            R.array.bitrate_array,
                            android.R.layout.simple_spinner_item);
            bitrate_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spinnerBitrate.setAdapter(bitrate_adapter);
            spinnerBitrate.setSelection(1);
            spinnerBitrate.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String item = spinnerBitrate.getSelectedItem().toString();
                    String value = item.replaceAll(".*(\\d+\\.\\d+).*", "$1");
                    bitrate = Float.parseFloat(value);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
    }

    @Override
    public String getTitle() {
        return getContext().getResources().getString(R.string.title_fingerprint);
    }

    @Override
    public void activate(Biometrics biometrics) {
        this.biometrics = biometrics;

        /* A FAP 45 sensor is used on the Trident devices, which allows them to do all four
         * different scan types. Only for Trident devices do we actually show a spinner which allows
         * user to select between one of four scan type.
         */
        boolean showSpinner =
                (biometrics.getFingerprintScannerType() == FingerprintScannerType.FAP45);

        if (spinnerScanType != null) {
            spinnerScanType.setVisibility(showSpinner ? VISIBLE : GONE);
            viewGenericSpinnerSpacer.setVisibility(showSpinner ? VISIBLE : GONE);
        }
        /* Sets which type of grabFingerprint listener to use. A full listener is only used when
         * doing a scan type that is any of the four available. So if we do have a FAP 45 sensor,
         * this means we are on a Trident which means use a full listener for all scan types.
         */
        useFingerprintFullListener = showSpinner;

        /* Determine if Credece device supports FMD template matching. */
        hasFmdMatcher = sampleActivity.hasFmdMatcher();
        viewMatchButtonSpacer.setVisibility(hasFmdMatcher ? VISIBLE : GONE);
        buttonMatch.setVisibility(hasFmdMatcher ? VISIBLE : GONE);

        /* Only Tridents/CredenceTABs support using the WSQ fingerprint listener, but we only
         * use listener for TABs so for Tridents we may instead demonstrate fullListener.
         */
        String name = this.biometrics.getProductName();
        useFingerprintWsqListener = name.contains("TAB");
        /* Always reset our captures when we activate this page. */
        this.doResume();
    }

    @Override
    public void doResume() {
        /* Reaching this method means activity/view was left, therefore reset our capture. 8*/
        if (this.fmdFingerTemplate1 == null && !this.isCapturing)
            resetCapture();
    }

    @Override
    public void deactivate() {
    }

    public void onClickCaptureButton() {
        /* Disable capture button to avoid double clicks. */
        this.buttonCapture.setEnabled(false);
        /* Turn off scanner to allow scan type selection. */
        this.spinnerScanType.setEnabled(false);
        /* Start by resetting page for new capture. */
        this.resetCapture();
        /* Turn on close button so user can close at anytime. */
        this.buttonClose.setEnabled(true);

        setStatusText("initializing...");
        this.isCapturing = true;

        final long startCaptureTime = SystemClock.elapsedRealtime();

        if (this.useFingerprintWsqListener)
            this.biometrics.grabFingerprint(this.scanType, this.saveToDisk, new Biometrics.OnFingerprintGrabbedWSQListener() {
                @Override
                public void onFingerprintGrabbed(Biometrics.ResultCode result,
                                                 Bitmap bm, byte[] iso,
                                                 String filepath,
                                                 String wsq,
                                                 String hint,
                                                 int nfiqScore) {
                   /* If we got a valid Bitmap result back then ImageView to display Bitmap. */
                    if (bm != null) imageViewCapturedImage.setImageBitmap(bm);
                    /* If we got back a valid hint then set it to our status for user to see. */
                    if (hint != null && !hint.isEmpty()) setStatusText(hint);

                    /* If result code was FAIL that means fingerprint sensor could not open. */
                    if (result == ResultCode.FAIL) {
                        setStatusText("Fingerprint Open-FAILED");
                        resetToOneFingerCaptureState();
                    } else if (result == OK) {
                        Beeper.getInstance().click();
                        resetToOneFingerCaptureState();
                        /* Calculate total time taken for image to return back as good. */
                        long duration = SystemClock.elapsedRealtime() - startCaptureTime;
                        setStatusText("Capture Complete in " + duration + "msec");
                        /* Set global path variables for Bitmap image. */
                        pathName = filepath;
                        currentBitmap = bm;

                        if(saveToDisk) {
                            convertToFmd(wsq);
                            showImageSize(filepath, wsq, duration);
                        }
                        /* Display captured finger quality. */
                        setStatusText("Fingerprint Quality: " + nfiqScore);
                        Log.d(TAG, "NFIQ Score - Fingerprint Quality: " + nfiqScore);
                        /* If device supports creation of FMD templates then create first FMD
                         * template from Bitmap.
                         */
                        if (hasFmdMatcher) convertToFmd(currentBitmap);
                    }
                }

                @Override
                public void onCloseFingerprintReader(ResultCode resultCode, CloseReasonCode closeReasonCode) {
                    if (resultCode == OK) {
                        setStatusText("FingerPrint reader closed:" + closeReasonCode.toString());
                        resetCapture();
                    } else if (resultCode == ResultCode.FAIL) {
                        /* If sensor failed to close, then close button should still be clickable
                         * since it did not actually close.
                         */
                        buttonClose.setEnabled(true);
                        setStatusText("FingerPrint reader closed: FAILED");
                    }
                }
            });
        else if (!useFingerprintFullListener)
            this.biometrics.grabFingerprint(this.scanType, this.saveToDisk, new OnFingerprintGrabbedListener() {
                @Override
                public void onFingerprintGrabbed(ResultCode result,
                                                 Bitmap bm,
                                                 byte[] iso,
                                                 String filepath,
                                                 String hint) {
                   /* If we got a valid Bitmap result back then ImageView to display Bitmap. */
                    if (bm != null) imageViewCapturedImage.setImageBitmap(bm);
                    /* If we got back a valid hint then set it to our status for user to see. */
                    if (hint != null && !hint.isEmpty()) setStatusText(hint);

                    if (result == ResultCode.FAIL) {
                        setStatusText("Fingerprint Open-FAILED");
                        resetToOneFingerCaptureState();
                    } else if (result == OK) {
                        Beeper.getInstance().click();
                        resetToOneFingerCaptureState();

                        /* Set global image variables. */
                        pathName = filepath;
                        currentBitmap = bm;

                        /* With the resulting Bitmap we may either calcualte its NFIQ score,
                         * compress image down to a WSQ format, or do both.
                         */
                        getFingerQuality(currentBitmap);
                        if(saveToDisk) {
                            createWsqImage(pathName);
                            File temp = new File(filepath);
                            Toast.makeText(getContext(), "Length: " + temp.length(), Toast.LENGTH_SHORT);
                        }

                        if (hasFmdMatcher) convertToFmd(currentBitmap);
                        else if (pathName == null)
                            Log.w(TAG, "onFingerprintGrabbed - OK but filepath null");
                    }
                }

                @Override
                public void onCloseFingerprintReader(ResultCode resultCode, CloseReasonCode reasonCode) {
                    if (resultCode == OK) {
                        setStatusText("FingerPrint reader closed:" + reasonCode.toString());
                        resetCapture();
                    } else if (resultCode == ResultCode.FAIL) {
                        buttonClose.setEnabled(true);
                        setStatusText("FingerPrint reader closed: FAILED");
                    }
                }
            });
        else
            this.biometrics.grabFingerprint(this.scanType, this.saveToDisk, new OnFingerprintGrabbedFullListener() {
                @Override
                public void onFingerprintGrabbed(Biometrics.ResultCode result,
                                                 Bitmap bm,
                                                 Bitmap bitmap_finger1, Bitmap bitmap_finger2,
                                                 byte[] iso,
                                                 byte[] iso_finger1, byte[] iso_finger2,
                                                 String filepath,
                                                 String filepath_finger1, String filepath_finger2,
                                                 String hint) {
                  /* If we got a valid Bitmap result back then ImageView to display Bitmap. */
                    if (bm != null) imageViewCapturedImage.setImageBitmap(bm);
                    /* If we got back a valid hint then set it to our status for user to see. */
                    if (hint != null && !hint.isEmpty()) setStatusText(hint);

                    /* If our result failed then turn OFF button to allow matching. Otherwise turn
                     * ON button to match and also appropriately handle ScanType.
                     */
                    if (result == ResultCode.FAIL) {
                        setStatusText("Matching Fingerprint Grab-FAILED");
                        resetToOneFingerCaptureState();
                    } else if (result == OK) {
                        Beeper.getInstance().click();

                        /* If scan type initiated was TWO_FINGERS_SPLIT then we need to display
                         * each of the two split fingerprints in their own ImageViews.
                         */
                        if (scanType.equals(ScanType.TWO_FINGERS_SPLIT)) {
                            Log.d(TAG, "OnFingerprintGrabbedFullListener: Showing Split Images");
                            /* Turn off single fingerprint ImageView and enable split ones. */
                            imageViewCapturedImage.setVisibility(INVISIBLE);
                            imageViewCapturedImageFinger1.setVisibility(VISIBLE);
                            imageViewCapturedImageFinger2.setVisibility(VISIBLE);

                            if (bitmap_finger1 != null)
                                imageViewCapturedImageFinger1.setImageBitmap(bitmap_finger1);
                            if (bitmap_finger2 != null)
                                imageViewCapturedImageFinger2.setImageBitmap(bitmap_finger2);

                            if(saveToDisk) {
                                createWsqImage(filepath_finger1);
                            }
                            getNfiqScore(bitmap_finger1);
                        } else {
                            imageViewCapturedImage.setImageBitmap(bm);
                            imageViewCapturedImage.setVisibility(VISIBLE);
                            if(saveToDisk) {
                                createWsqImage(filepath);
                            }
                            getNfiqScore(bm);
                        }

                        /* set global path variables for image locations. There are used later on
                         * for actions such as getFingerQuality() or convertToWsq().
                         */
                        pathName = filepath;
                        pathNameFingerprint1 = filepath_finger1;
                        pathNameFingerprint2 = filepath_finger2;

                        if (hasFmdMatcher) {
                            if (scanType.equals(ScanType.TWO_FINGERS_SPLIT))
                                currentBitmap = bitmap_finger1;
                            else currentBitmap = bm;
                            convertToFmd(currentBitmap);
                        } else if (pathName == null)
                            Log.w(TAG, "onFingerprintGrabbed - OK but filepath null");
                    }
                }

                @Override
                public void onCloseFingerprintReader(ResultCode resultCode, CloseReasonCode reasonCode) {
                    if (resultCode == OK) {
                        setStatusText("FingerPrint reader closed:" + reasonCode.toString());
                        resetToClosedState();
                    } else if (resultCode == ResultCode.FAIL) {
                        buttonClose.setEnabled(true);
                        setStatusText("FingerPrint reader closed: FAILED");
                    }
                }
            });
    }

    public void onClickCloseButton() {
        setStatusText("Closing scanner, Please wait...");

        /* Disable capture button to avoid double clicks. */
        this.buttonCapture.setEnabled(false);
        /* Update our status text for user. 8/
        this.setStatusText("Closing scanner, Please wait...");
        /* Start by resetting capture system. */
        this.resetCapture();
        /* Now close fingerprint sensor. */
        this.biometrics.closeFingerprintReader();
        /* Re-enable button after operations. */
        this.buttonClose.setEnabled(false);
    }

    /* The match section works by first looking to make sure user has already made on+e successful
     * fingerprint capture. If they have and a proper FMD template was created then it goes ahead
     * and captures another fingerprint image. It then converts this second image to a FMD template
     * then compares the two images.
     */
    public void onClickMatchButton() {
        /* Disable button to avoid double clicks. */
        this.buttonMatch.setEnabled(false);

        /* If user has not captured atleast one fingerprint so far then return out. */
        if (this.currentBitmap == null) {
            this.setStatusText("NO Fingerprint image ");
            return;
        }
        /* If there is not atleast one FMD template already made then return out. */
        if (this.fmdFingerTemplate1 == null) {
            this.setStatusText("NO Fingerprint template");
            return;
        }

        this.buttonCapture.setEnabled(false);
        this.resetCaptureImages();
        this.setStatusText("");
        this.setInfoText("");

        if (this.useFingerprintFullListener)
            this.biometrics.grabFingerprint(this.scanType, this.saveToDisk, new OnFingerprintGrabbedFullListener() {
                @Override
                public void onFingerprintGrabbed(Biometrics.ResultCode result,
                                                 Bitmap bm,
                                                 Bitmap bitmap_finger1, Bitmap bitmap_finger2, byte[] iso,
                                                 byte[] iso_finger1, byte[] iso_finger2,
                                                 String filepath,
                                                 String filepath_finger1, String filepath_finger2,
                                                 String hint) {
                    /* If we got a valid Bitmap result back then ImageView to display Bitmap. */
                    if (bm != null) imageViewCapturedImage.setImageBitmap(bm);
                    /* If we got back a valid hint then set it to our status for user to see. */
                    if (hint != null && !hint.isEmpty()) setStatusText(hint);

                    /* If our result failed then turn OFF button to allow matching. Otherwise turn
                     * ON button to match and also appropriately handle ScanType.
                     */
                    if (result == ResultCode.FAIL) {
                        setStatusText("Matching Fingerprint Grab-FAILED");
                        resetToOneFingerCaptureState();
                    } else if (result == OK && bm != null) {
                        Beeper.getInstance().click();

                        /* If scan type initiated was TWO_FINGERS_SPLIT then we need to display
                         * each of the two split fingerprints in their own ImageViews.
                         */
                        if (scanType.equals(ScanType.TWO_FINGERS_SPLIT)) {
                            Log.d(TAG, "OnFingerprintGrabbedFullListener: Showing Split Images");
                            /* Turn off single fingerprint ImageView and enable split ones. */
                            imageViewCapturedImage.setVisibility(INVISIBLE);
                            imageViewCapturedImageFinger1.setVisibility(VISIBLE);
                            imageViewCapturedImageFinger2.setVisibility(VISIBLE);

                            if (bitmap_finger1 != null)
                                imageViewCapturedImageFinger1.setImageBitmap(bitmap_finger1);
                            if (bitmap_finger2 != null)
                                imageViewCapturedImageFinger2.setImageBitmap(bitmap_finger2);
                        } else {
                            imageViewCapturedImage.setImageBitmap(bm);
                            imageViewCapturedImage.setVisibility(VISIBLE);
                        }

                        /* set global path variables for image locations. There are used later on
                         * for actions such as getFingerQuality() or convertToWsq().
                         */
                        pathName = filepath;
                        pathNameFingerprint1 = filepath_finger1;
                        pathNameFingerprint2 = filepath_finger2;

                        /* Call method to convert Bitmap to FMD template and match it against first
                         * FMD template created.
                         */
                        if (hasFmdMatcher) {
                            //If the scan is for split fingers, then consider first fingerprint for matching
                            if (scanType.equals(ScanType.TWO_FINGERS_SPLIT))
                                convertToFmdAndMatch(bitmap_finger1);
                            else
                                convertToFmdAndMatch(bm);
                        } else
                            Log.w(TAG, "Fmd matcher is not present");
                    }
                }

                @Override
                public void onCloseFingerprintReader(ResultCode resultCode, CloseReasonCode reasonCode) {
                    if (resultCode == OK) {
                        setStatusText("FingerPrint reader closed:" + reasonCode.toString());
                        resetToClosedState();
                    } else if (resultCode == ResultCode.FAIL)
                        setStatusText("FingerPrint reader closed: FAILED");
                }
            });
        else
            this.biometrics.grabFingerprint(this.scanType, this.saveToDisk, new OnFingerprintGrabbedListener() {
                @Override
                public void onFingerprintGrabbed(Biometrics.ResultCode result,
                                                 Bitmap bm,
                                                 byte[] iso,
                                                 String filepath,
                                                 String hint) {
                    /* If we got a valid Bitmap result back then ImageView to display Bitmap. */
                    if (bm != null) imageViewCapturedImage.setImageBitmap(bm);
                    /* If we got back a valid hint then set it to our status for user to see. */
                    if (hint != null && !hint.isEmpty()) setStatusText(hint);

                    /* If our result failed then turn OFF button to allow matching. Otherwise turn
                     * ON button to match.
                     */
                    if (result == ResultCode.FAIL) {
                        setStatusText("Matching Fingerprint Grab-FAILED");
                        resetToOneFingerCaptureState();
                    } else if (result == OK && bm != null) {
                        Beeper.getInstance().click();

                        /* Set global image variables. */
                        pathName = filepath;
                        currentBitmap = bm;

                        /* Since this is our second fingerprint captured, we may convert and
                         * compare it against the first captured one.
                         */
                        convertToFmdAndMatch(currentBitmap);
                    }
                }

                @Override
                public void onCloseFingerprintReader(ResultCode resultCode, CloseReasonCode reasonCode) {
                    if (resultCode == OK) {
                        setStatusText("FingerPrint reader closed:" + reasonCode.toString());
                        resetToClosedState();
                    } else if (resultCode == ResultCode.FAIL)
                        setStatusText("FingerPrint reader closed: FAILED");
                }
            });
    }

    /* Method to convert a fingerprint Bitmap image to a FMD template. */
    private void convertToFmd(Bitmap capturedImage) {
        /* Keep track of initial time to see how long conversion takes. */
        final long startTime = SystemClock.elapsedRealtime();

        // Call biometrics API for image conversion
        this.biometrics.convertToFmd(capturedImage, Biometrics.FmdFormat.ISO_19794_2_2005, new OnConvertToFmdListener() {
            @Override
            public void onConvertToFmd(ResultCode result, byte[] fmd) {
                if (result != OK || fmd == null) {
                    Log.w(TAG, "convertToFmd failed so mFmd1 is null");
                    setStatusText("convertToFmd failed so NO Fingerprint template");
                } else {
                    fmdFingerTemplate1 = fmd;
                    buttonMatch.setEnabled(true);
                }
                // Calculate total time for callback & log output for debugging purposes
                long duration = SystemClock.elapsedRealtime() - startTime;
                Log.d(TAG, "convertToFmd " + String.valueOf(duration) + "ms");
            }
        });
    }

    private void convertToFmd(String image_path) {
        Log.i(TAG, "convertToFmd(String image_path[" + image_path + "])");

        File file = new File(image_path);
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            Log.i(TAG, "reading file into byte array");
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.i(TAG, "now going to make CredenceSDK API call");
        biometrics.convertToFmd(bytes, Biometrics.FmdFormat.ISO_19794_2_2005, new OnConvertToFmdListener() {
            @Override
            public void onConvertToFmd(ResultCode resultCode, byte[] bytes) {
                Log.i(TAG, "onConvertToFmd(ResultCode, byte[]): CALLBACK INVOKED");
                Toast.makeText(getContext(),
                        "convertToFmd(wsq): " + (resultCode == OK ? "OK" : "FAIL") + ", " + bytes,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /* After Match button was pressed, a fingerprint image was captured, was valid, then we call
     * this method to convert captured image to FMD template then compare against first FMD.
     */
    private void convertToFmdAndMatch(Bitmap inputImage) {
        /* Make API call to convert image to FMD template. */
        this.biometrics.convertToFmd(inputImage, Biometrics.FmdFormat.ISO_19794_2_2005, new OnConvertToFmdListener() {
            @Override
            public void onConvertToFmd(ResultCode result, byte[] fmd) {
                if (result != OK || fmd == null) {
                    Log.w(TAG, "convertMatchImage failed");
                    setStatusText("No Match: FMD convert fail");
                    resetToOneFingerCaptureState();
                } else {
                    Log.d(TAG, "Received FMD of size: " + fmd.length);
                    fmdFingerTemplate2 = fmd;

                    if (fmdFingerTemplate1 != null)
                        compareFmd(fmdFingerTemplate1, fmdFingerTemplate2);
                    else Log.e(TAG, "No Match: Missing first capture");
                }
            }
        });
    }

    /* After Match button was clicked, a proper fingerprint was captured, we call this method to
     * compare first FMD template with second.
     */
    private void compareFmd(byte[] fmd1, byte[] fmd2) {
        Log.d(TAG, "FMD1 = " + fmd1.length);
        Log.d(TAG, "FMD2 = " + fmd2.length);

        /* Get a start time to keep track of when result comes back. */
        final long startTime = SystemClock.elapsedRealtime();

        // Call API function to compare Fmd type images, pass callback to handle result
        this.biometrics.compareFmd(fmd1, fmd2, Biometrics.FmdFormat.ISO_19794_2_2005, new OnCompareFmdListener() {
            @Override
            public void onCompareFmd(ResultCode result, float dissimilarity) {
                String matchDecision;

                /* Calculate time taken for result from API function call. */
                final long duration = SystemClock.elapsedRealtime() - startTime;

                if (result != OK) setStatusText("Failed To Compare Templates.");
                else {
                    /* If result is good dissimilarity score ranges from 0 to
                     * Integer.MAX_VALUE(2147483647). A score of less than 2147 is a match with
                     * false positive probability of 1 in million.
                     */
                    if (dissimilarity < (Integer.MAX_VALUE / 1000000)) matchDecision = "Match.";
                    else matchDecision = "No Match.";

                    String str = String.format(matchDecision + " Dur: %dms, Dissimilarity Score %d",
                            duration,
                            (int) dissimilarity);

                    Log.d(TAG, str);
                    setStatusText(matchDecision);
                }
                resetToOneFingerCaptureState();
            }
        });
    }

    /* Make API call to get NIST NFIQ fingerprint score. */
    private void getFingerQuality(Bitmap bitmap) {
        this.biometrics.getFingerQuality(bitmap, new Biometrics.OnGetFingerQualityListener() {
            @Override
            public void onGetFingerQuality(ResultCode resultCode, int nfiqScore) {
                if (resultCode == OK)
                    setStatusText("Fingerprint Quality: " + nfiqScore);
                else setStatusText("Fingerprint Quality: " + nfiqScore);
            }
        });
    }

    /* Make API call to convert Bitmap image to compressed WSQ format. */
    private void createWsqImage(final String originalFilePath) {
        Log.i(TAG, "createWsqImage(final String filePath)");

        /* Get original image size. */
        Log.i(TAG, "getting original image size)");
        this.originalImageSize = 0;
        File uncompressed = new File(originalFilePath);
        if (uncompressed.exists()) {
            this.originalImageSize = uncompressed.length();
        }

        /* Keep track of initialize time to see how long conversions takes. */
        final long startTime = SystemClock.elapsedRealtime();

        // Call biometrics API for converting images
        Log.i(TAG, "Making convertToWsq API call");
        this.biometrics.convertToWsq(originalFilePath, bitrate, new OnConvertToWsqListener() {
            @Override
            public void onConvertToWsq(final ResultCode result, String pathname) {
                    /* If result is in between FAIL and OK, it is still being converted. */
                if (result == ResultCode.INTERMEDIATE) setInfoText("Converting to WSQ...");
                else if (result == ResultCode.FAIL) setInfoText("Convert to WSQ failed.");
                else {
                    Log.i(TAG, "onConvertToWsq(OK, " + pathname + ")");
                    /* Get converted image size. */
                    compressedImageSize = 0;
                    File compressed = new File(pathname);
                    if (compressed.exists())
                        compressedImageSize = compressed.length();

                    /* Similar to showImageSize() method, but this block of code would also display
                     * time taken for conversion.
                     */
                    long duration = SystemClock.elapsedRealtime() - startTime;
                    String str = String.format(Locale.getDefault(),
                            "PNG: %s, WSQ: %s, Dur: %dms",
                            TheApp.abbreviateNumber(originalImageSize),
                            TheApp.abbreviateNumber(compressedImageSize),
                            duration);
                    setInfoText(str);

                    /* Now that we have compressed fingerprint image we will also decompress it in
                     * order to demonstrate another CredenceSDK API call.
                     */
                    decompressWsq(pathname);
                }
                resetToOneFingerCaptureState();
            }
        });
    }

    private void decompressWsq(String filePath) {
        Log.i(TAG, "Going to call decompress API call");
        biometrics.decompressWsq(filePath, new Biometrics.OnDecompressWsqListener() {
            @Override
            public void onDecompressWsq(ResultCode resultCode, byte[] bytes) {
                String message = "De-CompressWsq was " + resultCode.toString();

                if (bytes != null)
                    Toast.makeText(getContext(),
                            message + ", Length: " + bytes.length,
                            Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(getContext(),
                            message + ", Data was NULL.",
                            Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showImageSize(String png, String wsq, long duration) {
        this.compressedImageSize = 0;
        File uncompressed = new File(png);
        if (uncompressed.exists()) this.originalImageSize = uncompressed.length();

        File compressed = new File(wsq);
        if (compressed.exists()) this.compressedImageSize = compressed.length();

        String str = String.format(Locale.getDefault(),
                "PNG: %s, WSQ: %s, Dur: %s ms",
                TheApp.abbreviateNumber(this.originalImageSize),
                TheApp.abbreviateNumber(this.compressedImageSize),
                duration);
        this.setInfoText(str);
    }

    // get fingerprint quality
    private void getNfiqScore(Bitmap bitmap) {
        this.biometrics.getFingerQuality(bitmap, new Biometrics.OnGetFingerQualityListener() {
            @Override
            public void onGetFingerQuality(ResultCode resultCode, int nfiqScore) {
                if (resultCode == OK) {
                    Log.d(TAG, "NFIQ Score - Fingerprint Quality: " + nfiqScore);
                    setStatusText("Fingerprint Quality: " + nfiqScore);
                } else {
                    Log.d(TAG, "NFIQ Score - Fingerprint Quality: " + resultCode + nfiqScore);
                    setStatusText("Fingerprint Quality: " + nfiqScore);
                }
            }
        });
    }

    /* Resets entire UI state. */
    private void resetCapture() {
        /* Start by re-setting all captured images. */
        this.resetCaptureImages();
        this.fmdFingerTemplate1 = null;
        this.isCapturing = false;
        this.setInfoText("");
        this.resetToClosedState();
    }

    /* Resets ImageViews which display our captured Bitmap images. */
    private void resetCaptureImages() {
        /* Turn on default single fingerprint ImageView. */
        this.imageViewCapturedImage.setVisibility(VISIBLE);
        /* Set ImageView image to NULL for fresh captures */
        this.imageViewCapturedImage.setImageDrawable(null);

        /* Turn off both split ImageViews if they are not NULL. */
        if (this.imageViewCapturedImageFinger1 != null) {
            this.imageViewCapturedImageFinger1.setVisibility(INVISIBLE);
            this.imageViewCapturedImageFinger1.setImageDrawable(null);
        }
        if (this.imageViewCapturedImageFinger2 != null) {
            this.imageViewCapturedImageFinger2.setVisibility(INVISIBLE);
            this.imageViewCapturedImageFinger2.setImageDrawable(null);
        }
    }

    private void resetToOneFingerCaptureState() {
        this.buttonCapture.setEnabled(true);
        /* Check if match button should be enabled. */
        boolean enable_match = this.hasFmdMatcher && this.fmdFingerTemplate1 != null;
        this.buttonMatch.setEnabled(enable_match);
        this.buttonClose.setEnabled(true);
        /* set this to false to prevent onResume() method bing called. */
        this.isCapturing = false;
    }

    private void resetToClosedState() {
        this.buttonCapture.setEnabled(true);
        this.buttonClose.setEnabled(false);
        this.buttonMatch.setEnabled(false);
        this.spinnerScanType.setEnabled(true);
    }

    private void setStatusText(String text) {
        if (!text.isEmpty()) Log.d(TAG, "setStatusText: " + text);
        this.textViewStatus.setText(text);
    }

    private void setInfoText(String text) {
        if (!text.isEmpty()) Log.d(TAG, "setInfoText: " + text);

        /* If our info TextView is initialized then set text to that view, otherwise set it to our
         * status TextView, only if text is not empty.
         */
        if (this.textViewInfo != null)
            this.textViewInfo.setText(text);
        else if (!text.isEmpty()) this.setStatusText(text);
    }
}