package com.credenceid.sdkapp.pages;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
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
import com.credenceid.biometrics.Biometrics.ResultCode;
import com.credenceid.biometrics.Biometrics.ScanType;
import com.credenceid.biometrics.FingerQuality;
import com.credenceid.biometrics.FingerprintSyncResponse;
import com.credenceid.sdkapp.R;
import com.credenceid.sdkapp.SampleActivity;
import com.credenceid.sdkapp.TheApp;
import com.credenceid.sdkapp.models.PageView;
import com.credenceid.sdkapp.utils.Beeper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.credenceid.biometrics.Biometrics.ResultCode.OK;
import static com.credenceid.sdkapp.R.id.status;

public class FingerprintPage extends LinearLayout implements PageView {
    private static final String TAG = FingerprintPage.class.getName();
    private final Biometrics.ScanType scanTypes[] = {
            ScanType.SINGLE_FINGER,
            ScanType.TWO_FINGERS, ScanType.ROLL_SINGLE_FINGER,
            ScanType.TWO_FINGERS_SPLIT
    };
    private SampleActivity sampleActivity;
    private Biometrics biometrics;
    private Button buttonCapture;
    private Button buttonOpenClose;
    private Button buttonMatch;

    private View viewMatchButtonSpacer;
    private Spinner spinnerScanType;
    private View viewGenericSpinnerSpacer;
    private Spinner spinnerBitrate;
    private Spinner spinnerSaveToDisk;
    private Spinner spinnerSync;

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

    /* global path variables for grabbed fingerprints.*/
    private Bitmap currentBitmap;
    private Bitmap currentFingerprint1Bitmap;
    private Bitmap currentFingerprint2Bitmap;

    private byte[] fmdFingerTemplate1 = null;
    private byte[] fmdFingerTemplate2 = null;

    private long originalImageSize;
    private long compressedImageSize;

    private boolean isCapturing = false;
    // if true, save the grabbed fingerprint to disk
    private boolean saveToDisk = false;
    private boolean saveToDiskDefault = false;
    // If true, grab fingerprint synchronously
    private boolean grabFingerprintSync = false;
    // If true, return the raw byte array of fingerprint which grabbed asynchronously
    private boolean grabFingerprintAsyncRaw = false;
    // Handler for display bitmap which grabbed synchronously
    Handler syncHandler;
    // Selected value for saveToDisk spinner
    String spinnerSaveToDiskItem;
    // Array for sync spinner
    String[] spinnerSyncArray;
    List<String> spinnerSyncArrayList;
    String spinnerSyncItem;
    // Array for scanType spinner
    String[] spinnerScanTypeArray;
    List<String> spinnerScanTypeArrayList;
    String spinnerScanTypeItem;

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
                // if saveToDisk is true, show the full screen image with the PNG format;
                // if saveToDisk is false, show the full screen image with the Bitmap format
                if (saveToDisk) {
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
                // if saveToDisk is true, show the full screen image with the PNG format;
                // if saveToDisk is false, show the full screen image with the Bitmap format
                if (saveToDisk) {
                    sampleActivity.showFullScreenScannedImage(pathNameFingerprint1);
                } else {
                    sampleActivity.showFullScreenScannedImage(currentFingerprint1Bitmap);
                }
            }
        });

        imageViewCapturedImageFinger2 = (ImageView) findViewById(R.id.capture_finger2_image);
        imageViewCapturedImageFinger2.setVisibility(INVISIBLE);
        imageViewCapturedImageFinger2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // if saveToDisk is true, show the full screen image with the PNG format;
                // if saveToDisk is false, show the full screen image with the Bitmap format
                if (saveToDisk) {
                    sampleActivity.showFullScreenScannedImage(pathNameFingerprint2);
                } else {
                    sampleActivity.showFullScreenScannedImage(currentFingerprint2Bitmap);
                }
            }
        });

        viewMatchButtonSpacer = findViewById(R.id.match_spacer);
        textViewStatus = (TextView) findViewById(status);
        textViewStatus.setSingleLine(false);
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

        buttonOpenClose = (Button) findViewById(R.id.open_close_btn);
        buttonOpenClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonOpenClose.getText().toString().equalsIgnoreCase(getResources().getString(R.string.open))) {
                    openFingerprint();
                } else {
                    closeFingerprint();
                }
                buttonOpenClose.setEnabled(false);
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
        spinnerSaveToDiskItem = "";
        // Initialize array for spinner
        // Sync Spinner
        spinnerSyncArray = new String[]{
                getResources().getString(R.string.async),
                getResources().getString(R.string.async_raw),
                getResources().getString(R.string.sync)
        };
        spinnerSyncArrayList = new ArrayList<String>(Arrays.asList(spinnerSyncArray));
        spinnerSyncItem = "";
        // ScanType Spinner
        spinnerScanTypeArray = new String[] {
                getResources().getString(R.string.single_finger),
                getResources().getString(R.string.two_fingers),
                getResources().getString(R.string.roll_finger),
                getResources().getString(R.string.two_finger_split),
        };
        spinnerScanTypeArrayList = new ArrayList<String>(Arrays.asList(spinnerScanTypeArray));
        spinnerScanTypeItem = "";

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

        initializeSaveToDiskSpinner();
        initializeSyncSpinners();
        initializeScanTypeSpinner();
    }

    private void initializeSaveToDiskSpinner() {
        /*
         *  Spinner with the options of "Default(Save as PNG)", "No PNG", "Save as PNG"
         * "Default(Save as PNG)" will use api with parameter saveToDisk;
         * "No PNG", "Save as PNG" will use API without parameter saveToDisk;
         */
        spinnerSaveToDisk = (Spinner) findViewById(R.id.save_to_disk_spinner);
        if (spinnerSaveToDisk != null) {
            ArrayAdapter<CharSequence> saveToDiskAdapter =
                    ArrayAdapter.createFromResource(getContext(),
                            R.array.save_to_disk_array,
                            android.R.layout.simple_spinner_item);
            saveToDiskAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spinnerSaveToDisk.setAdapter(saveToDiskAdapter);
            spinnerSaveToDisk.setSelection(0);
            spinnerSaveToDisk.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    spinnerSaveToDiskItem = spinnerSaveToDisk.getItemAtPosition(spinnerSaveToDisk.getSelectedItemPosition()).toString();
                    Log.v(TAG, "saveToDisk onItemSelected: " + spinnerSaveToDiskItem);
                    if (spinnerSaveToDiskItem.equalsIgnoreCase(getResources().getString(R.string.no_png))) {
                        saveToDisk = false;
                        saveToDiskDefault = false;
                        spinnerSyncArrayList.clear();
                        spinnerSyncArrayList.add(getResources().getString(R.string.async));
                        spinnerSyncArrayList.add(getResources().getString(R.string.async_raw));
                        spinnerSyncArrayList.add(getResources().getString(R.string.sync));
                        spinnerSync.setEnabled(true);
                    } else if (spinnerSaveToDiskItem.equalsIgnoreCase(getResources().getString(R.string.save_as_png))){
                        saveToDisk = true;
                        saveToDiskDefault = false;
                        spinnerSyncArrayList.clear();
                        spinnerSyncArrayList.add(getResources().getString(R.string.async));
                        spinnerSyncArrayList.add(getResources().getString(R.string.async_raw));
                        spinnerSync.setEnabled(true);
                    } else if (spinnerSaveToDiskItem.equalsIgnoreCase(getResources().getString(R.string.default_save_png))){
                        saveToDisk = true;
                        saveToDiskDefault = true;
                        spinnerSyncArrayList.clear();
                        spinnerSyncArrayList.add(getResources().getString(R.string.async));
                        spinnerSync.setSelection(
                                getSpinnerSelectedIndex(spinnerSync, spinnerSyncArrayList,
                                        getResources().getString(R.string.async))
                        );
                        spinnerSync.setEnabled(false);
                    }
                    int spinnerSyncSelectedIndex = getSpinnerSelectedIndex(spinnerSync,
                            spinnerSyncArrayList, spinnerSyncItem);
                    if (spinnerSyncSelectedIndex == -1) {
                        spinnerSync.setSelection(0);
                    } else {
                        spinnerSync.setSelection(spinnerSyncSelectedIndex);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
    }

    private void initializeSyncSpinners() {
        spinnerSync = (Spinner) findViewById(R.id.sync_spinner);

        if (spinnerSync != null) {
            ArrayAdapter<String> syncAdapter =
                    new ArrayAdapter<String>(getContext(),
                            android.R.layout.simple_spinner_item,
                            spinnerSyncArrayList);
            syncAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSync.setAdapter(syncAdapter);
            spinnerSync.setSelection(0);
            spinnerSync.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    spinnerSyncItem = spinnerSync.getItemAtPosition(spinnerSync.getSelectedItemPosition()).toString();
                    Log.v(TAG, "sync onItemSelected: " + spinnerSyncItem);
                    if (spinnerSyncItem.equalsIgnoreCase(getResources().getString(R.string.async))) {
                        grabFingerprintAsyncRaw = false;
                        grabFingerprintSync = false;
                        if (spinnerScanType != null) {
                            spinnerScanType.setEnabled(true);
                            spinnerScanTypeArrayList.clear();
                            spinnerScanTypeArrayList.add(getResources().getString(R.string.single_finger));
                            spinnerScanTypeArrayList.add(getResources().getString(R.string.two_fingers));
                            spinnerScanTypeArrayList.add(getResources().getString(R.string.roll_finger));
                            spinnerScanTypeArrayList.add(getResources().getString(R.string.two_finger_split));
                        }
                        spinnerSaveToDisk.setEnabled(true);
                    } else if (spinnerSyncItem.equalsIgnoreCase(getResources().getString(R.string.async_raw))) {
                        grabFingerprintAsyncRaw = true;
                        grabFingerprintSync = false;
                        if (spinnerScanType != null) {
                            spinnerScanType.setEnabled(true);
                            spinnerScanTypeArrayList.clear();
                            spinnerScanTypeArrayList.add(getResources().getString(R.string.single_finger));
                            spinnerScanTypeArrayList.add(getResources().getString(R.string.two_fingers));
                            spinnerScanTypeArrayList.add(getResources().getString(R.string.roll_finger));
                        }
                        spinnerSaveToDisk.setEnabled(true);
                    } else if (spinnerSyncItem.equalsIgnoreCase(getResources().getString(R.string.sync))){
                        grabFingerprintAsyncRaw = false;
                        grabFingerprintSync = true;
                        if (spinnerScanType != null) {
                            spinnerScanTypeArrayList.clear();
                            spinnerScanTypeArrayList.add(getResources().getString(R.string.single_finger));
                            spinnerScanType.setSelection(
                                    getSpinnerSelectedIndex(spinnerScanType, spinnerScanTypeArrayList,
                                            getResources().getString(R.string.single_finger))
                            );
                            spinnerScanType.setEnabled(false);
                        }
                        spinnerSaveToDisk.setEnabled(false);
                    }
                    int spinnerScanTypeSelectedIndex = getSpinnerSelectedIndex(spinnerScanType,
                            spinnerScanTypeArrayList, spinnerScanTypeItem);
                    if (spinnerScanTypeSelectedIndex == -1) {
                        spinnerScanType.setSelection(0);
                    } else {
                        spinnerScanType.setSelection(spinnerScanTypeSelectedIndex);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
    }

    private void initializeScanTypeSpinner() {
        // Initialize ScanType Spinner
        spinnerScanType = (Spinner) findViewById(R.id.scan_type_spinner);
        if (spinnerScanType != null) {
            ArrayAdapter<String> scanTypeAdapter =
                    new ArrayAdapter<String>(getContext(),
                            android.R.layout.simple_spinner_item,
                            spinnerScanTypeArrayList);
            scanTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spinnerScanType.setAdapter(scanTypeAdapter);
            spinnerScanType.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    spinnerScanTypeItem = spinnerScanType.getItemAtPosition(spinnerScanType.getSelectedItemPosition()).toString();
                    Log.v(TAG, "scantype onItemSelected: " + spinnerScanTypeItem);
                    if (spinnerScanTypeItem.equalsIgnoreCase(getResources().getString(R.string.two_finger_split))) {
                        scanType = ScanType.TWO_FINGERS_SPLIT;
                    } else if (spinnerScanTypeItem.equalsIgnoreCase(getResources().getString(R.string.single_finger))) {
                        scanType = ScanType.SINGLE_FINGER;
                    } else if (spinnerScanTypeItem.equalsIgnoreCase(getResources().getString(R.string.roll_finger))) {
                        scanType = ScanType.ROLL_SINGLE_FINGER;
                    } else if (spinnerScanTypeItem.equalsIgnoreCase(getResources().getString(R.string.two_fingers))) {
                        scanType = ScanType.TWO_FINGERS;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
    }


    /*
    * Get selected id in spinner by value
    */
    private int getSpinnerSelectedIndex(Spinner spinner, List<String> stringList, String curString){
        Log.v(TAG, "spinner list size: " + stringList.size());
        for (int i = 0;i < stringList.size();i++){
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(curString)){
                return i;
            }
        }

        return -1;
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
        /* Start by resetting page for new capture. */
        this.resetCapture();
        /* Turn on close button so user can close at anytime. */
        updateToCloseButton();

        setStatusText("initializing...");
        this.isCapturing = true;
        this.syncHandler = new Handler();
        /* Turn off scanner to allow capture option selection. */
        this.spinnerSync.setEnabled(false);
        this.spinnerSaveToDisk.setEnabled(false);
        /* Turn off scanner to allow scan type selection. */
        if (this.spinnerScanType != null) {
            this.spinnerScanType.setEnabled(false);
        }

        Log.v(TAG, "scantype: " + scanType);

        final long startCaptureTime = SystemClock.elapsedRealtime();

        if (grabFingerprintSync) {
            Log.v(TAG, "grabFingerprintSync is true");
            setStatusText("Place finger on sensor");
            Runnable mRunnable = new Runnable() {
                @Override
                public void run() {
                    final FingerprintSyncResponse fingerprintSyncResponse = biometrics.grabFingerprintSync(8000);
                    final Bitmap bitmap = fingerprintSyncResponse.bitmap;
                    //Beeper.getInstance().click();
                    syncHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (fingerprintSyncResponse.resultCode == ResultCode.OK) {
                                Beeper.getInstance().click();
                                imageViewCapturedImage.setImageBitmap(bitmap);
                                currentBitmap = bitmap;
                                /* Calculate total time taken for image to return back as good. */
                                long duration = SystemClock.elapsedRealtime() - startCaptureTime;
                                setStatusText("Capture Complete in " + duration + " msec");
                                Log.v(TAG, "Capture succeed: status: " + fingerprintSyncResponse.resultCode
                                        + "," + fingerprintSyncResponse.status);
                                if (fingerprintSyncResponse.rawImage != null) {
                                    Toast toast = Toast.makeText(getContext(), "Raw image length is " + fingerprintSyncResponse.rawImage.length,
                                            Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.BOTTOM, getResources().getInteger(R.integer.toast_offset_x),
                                            getResources().getInteger(R.integer.toast_offset_y));
                                    toast.show();
                                    Log.v(TAG, "Capture succeed: rawImage length is: "
                                            + fingerprintSyncResponse.rawImage.length);
                                }
                                getFingerQuality(bitmap);
                                resetToOneFingerCaptureState();
                                if (hasFmdMatcher) {
                                    convertToFmd(bitmap);
                                }
                            } else {
                                Log.v(TAG, "Capture fails: " + fingerprintSyncResponse.resultCode
                                        + "," + fingerprintSyncResponse.status);
                            }
                        }
                    });
                }
            };
            new Thread(mRunnable).start();
        } else if (this.grabFingerprintAsyncRaw) {
            Log.v(TAG, "grabFingerprintAsyncRaw is true. saveToDisk is " + saveToDisk);
            this.biometrics.grabFingerprint(this.scanType, this.saveToDisk,
                    new Biometrics.OnFingerprintGrabbedRawListener() {
                        @Override
                        public void onFingerprintGrabbed(ResultCode result,
                                                         Bitmap bm,
                                                         byte[] iso,
                                                         String filepath,
                                                         byte[] rawImage,
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
                                /* Calculate total time taken for image to return back as good. */
                                long duration = SystemClock.elapsedRealtime() - startCaptureTime;
                                setStatusText("Capture Complete in " + duration + " msec");
                                if (rawImage != null) {
                                    Toast toast = Toast.makeText(getContext(), "Raw image length is " + rawImage.length,
                                            Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.BOTTOM, getResources().getInteger(R.integer.toast_offset_x),
                                            getResources().getInteger(R.integer.toast_offset_y));
                                    toast.show();
                                }
                                /* Set global image variables. */
                                pathName = filepath;
                                currentBitmap = bm;


                                /* With the resulting Bitmap we may either calcualte its NFIQ score,
                                 * compress image down to a WSQ format, or do both.
                                 */
                                if (filepath != null) {
                                    //if have filepath, then convert to wsq
                                    createWsqImage(filepath, bm);
                                    getFingerQuality(filepath);
                                } else {
                                    getFingerQuality(currentBitmap);
                                }

                                if (hasFmdMatcher) convertToFmd(currentBitmap);
                                else if (pathName == null)
                                    Log.w(TAG, "onFingerprintGrabbed - OK but filepath null");
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
                                updateToCloseButton();
                                setStatusText("FingerPrint reader closed: FAILED");
                            }
                        }
                    });
        } else {
            if (this.useFingerprintWsqListener) {
                Log.v(TAG, "useFingerprintWsqListener is true.");
                if (this.saveToDiskDefault) {
                    Log.v(TAG, "saveToDiskDefault is true.");
                    this.biometrics.grabFingerprint(this.scanType,
                            new Biometrics.OnFingerprintGrabbedWSQListener() {
                                @Override
                                public void onFingerprintGrabbed(Biometrics.ResultCode result,
                                                                 Bitmap bm, byte[] iso,
                                                                 String filepath,
                                                                 String wsqFilepath,
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
                                        setStatusText("Capture Complete in " + duration + " msec");
                                        /* Set global path variables for Bitmap image. */
                                        pathName = filepath;
                                        currentBitmap = bm;

                                        Log.v(TAG, "wsq file path is: " + wsqFilepath);
                                        decompressWsq(wsqFilepath, bm);
                                        /* Display captured finger quality. */
                                        setInfoText("Fingerprint Quality: " + nfiqScore);
                                        Log.d(TAG, "NFIQ Score - Fingerprint Quality: " + nfiqScore);
                                        /* If device supports creation of FMD templates then create first FMD
                                         * template from Bitmap.
                                         */
                                        if (hasFmdMatcher) {
                                            //Convert to wsq
                                            if (wsqFilepath != null) {
                                                convertToFmd(wsqFilepath);
                                            }
                                            if (filepath != null && wsqFilepath != null) {
                                                showImageSize(filepath, wsqFilepath, duration);
                                            }
                                        }
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
                                        updateToCloseButton();
                                        setStatusText("FingerPrint reader closed: FAILED");
                                    }
                                }
                            });
                } else {
                    Log.v(TAG, "saveToDisk is " + saveToDisk);
                    this.biometrics.grabFingerprint(this.scanType, this.saveToDisk,
                            new Biometrics.OnFingerprintGrabbedWSQListener() {
                                @Override
                                public void onFingerprintGrabbed(Biometrics.ResultCode result,
                                                                 Bitmap bm, byte[] iso,
                                                                 String filepath,
                                                                 String wsqFilepath,
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
                                        setStatusText("Capture Complete in " + duration + " msec");
                                        /* Set global path variables for Bitmap image. */
                                        pathName = filepath;
                                        currentBitmap = bm;

                                        if (wsqFilepath != null) {
                                            decompressWsq(wsqFilepath, bm);
                                        }
                                        /* Display captured finger quality. */
                                        setInfoText("Fingerprint Quality: " + nfiqScore);
                                        Log.d(TAG, "NFIQ Score - Fingerprint Quality: " + nfiqScore);
                                        /* If device supports creation of FMD templates then create first FMD
                                         * template from Bitmap.
                                         */
                                        if (hasFmdMatcher) {
                                            //if have filepath, then convert to wsq
                                            if (filepath != null && wsqFilepath != null) {
                                                showImageSize(filepath, wsqFilepath, duration);
                                            }
                                            if (wsqFilepath != null) {
                                                convertToFmd(wsqFilepath);
                                            } else {
                                                convertToFmd(currentBitmap);
                                            }
                                        }
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
                                        updateToCloseButton();
                                        setStatusText("FingerPrint reader closed: FAILED");
                                    }
                                }
                            });
                }
            }
            else if (!useFingerprintFullListener) {
                Log.v(TAG, "useFingerprintFullListener is false.");
                if (this.saveToDiskDefault) {
                    Log.v(TAG, "saveToDiskDefault is true.");
                    this.biometrics.grabFingerprint(this.scanType,
                            new Biometrics.OnFingerprintGrabbedListener() {
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
                                        /* Calculate total time taken for image to return back as good. */
                                        long duration = SystemClock.elapsedRealtime() - startCaptureTime;
                                        setStatusText("Capture Complete in " + duration + " msec");
                                        /* Set global image variables. */
                                        pathName = filepath;
                                        currentBitmap = bm;

                                        /* With the resulting Bitmap we may either calcualte its NFIQ score,
                                         * compress image down to a WSQ format, or do both.
                                         */
                                        //if have filepath, then convert to wsq
                                        if (filepath != null) {
                                            createWsqImage(filepath, bm);
                                            getFingerQuality(filepath);
                                        } else {
                                            getFingerQuality(currentBitmap);
                                        }

                                        if (hasFmdMatcher) convertToFmd(currentBitmap);
                                        else if (pathName == null)
                                            Log.w(TAG, "onFingerprintGrabbed - OK but filepath null");
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
                                        updateToCloseButton();
                                        setStatusText("FingerPrint reader closed: FAILED");
                                    }
                                }
                            });
                } else {
                    Log.v(TAG, "saveToDisk is " + saveToDisk);
                    this.biometrics.grabFingerprint(this.scanType, this.saveToDisk,
                            new Biometrics.OnFingerprintGrabbedListener() {
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
                                        /* Calculate total time taken for image to return back as good. */
                                        long duration = SystemClock.elapsedRealtime() - startCaptureTime;
                                        setStatusText("Capture Complete in " + duration + " msec");
                                        /* Set global image variables. */
                                        pathName = filepath;
                                        currentBitmap = bm;

                                        /* With the resulting Bitmap we may either calcualte its NFIQ score,
                                         * compress image down to a WSQ format, or do both.
                                         */
                                        //if have filepath, then convert to wsq
                                        if (filepath != null) {
                                            createWsqImage(filepath, bm);
                                            getFingerQuality(filepath);
                                        } else {
                                            getFingerQuality(currentBitmap);
                                        }

                                        if (hasFmdMatcher) convertToFmd(currentBitmap);
                                        else if (pathName == null)
                                            Log.w(TAG, "onFingerprintGrabbed - OK but filepath null");
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
                                        updateToCloseButton();
                                        setStatusText("FingerPrint reader closed: FAILED");
                                    }
                                }
                            });
                }
            }
            else {
                Log.v(TAG, "useFingerprintFullListener is true.");
                if (this.saveToDiskDefault) {
                    Log.v(TAG, "saveToDiskDefault is true.");
                    this.biometrics.grabFingerprint(this.scanType, new OnFingerprintGrabbedFullListener() {
                        @Override
                        public void onFingerprintGrabbed(Biometrics.ResultCode result,
                                                         Bitmap bm,
                                                         Bitmap bitmapFinger1, Bitmap bitmapFinger2,
                                                         byte[] iso,
                                                         byte[] isoFinger1, byte[] isoFinger2,
                                                         String filepath,
                                                         String filepathFinger1, String filepathFinger2,
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
                                resetToOneFingerCaptureState();
                                /* Calculate total time taken for image to return back as good. */
                                long duration = SystemClock.elapsedRealtime() - startCaptureTime;
                                setStatusText("Capture Complete in " + duration + " msec");

                                /* If scan type initiated was TWO_FINGERS_SPLIT then we need to display
                                 * each of the two split fingerprints in their own ImageViews.
                                 */
                                if (scanType.equals(ScanType.TWO_FINGERS_SPLIT)) {
                                    Log.d(TAG, "OnFingerprintGrabbedFullListener: Showing Split Images");
                                    /* Turn off single fingerprint ImageView and enable split ones. */
                                    imageViewCapturedImage.setVisibility(INVISIBLE);
                                    imageViewCapturedImageFinger1.setVisibility(VISIBLE);
                                    imageViewCapturedImageFinger2.setVisibility(VISIBLE);

                                    if (bitmapFinger1 != null) {
                                        imageViewCapturedImageFinger1.setImageBitmap(bitmapFinger1);
                                        //save to the global variable currentFingerprint1Bitmap
                                        currentFingerprint1Bitmap = bitmapFinger1;
                                    }
                                    if (bitmapFinger2 != null) {
                                        imageViewCapturedImageFinger2.setImageBitmap(bitmapFinger2);
                                        //save to the global variable currentFingerprint2Bitmap
                                        currentFingerprint2Bitmap = bitmapFinger2;
                                    }
                                    //if have filepath, then convert to wsq
                                    if (filepathFinger1 != null) {
                                        createWsqImage(filepathFinger1, bitmapFinger1);
                                        getFingerQuality(filepathFinger1);
                                    } else {
                                        getFingerQuality(bitmapFinger1);
                                    }
                                } else {
                                    imageViewCapturedImage.setImageBitmap(bm);
                                    imageViewCapturedImage.setVisibility(VISIBLE);
                                    //if have filepath, then convert to wsq
                                    if (filepath != null) {
                                        createWsqImage(filepath, bm);
                                        getFingerQuality(filepath);
                                    } else {
                                        getFingerQuality(bm);
                                    }
                                }

                                /* set global path variables for image locations. There are used later on
                                 * for actions such as getFingerQuality() or convertToWsq().
                                 */
                                pathName = filepath;
                                pathNameFingerprint1 = filepathFinger1;
                                pathNameFingerprint2 = filepathFinger2;

                                if (hasFmdMatcher) {
                                    if (scanType.equals(ScanType.TWO_FINGERS_SPLIT))
                                        currentBitmap = bitmapFinger1;
                                    else currentBitmap = bm;
                                    convertToFmd(currentBitmap);
                                } else if (pathName == null)
                                    Log.w(TAG, "onFingerprintGrabbed - OK but filepath null");
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
                                updateToCloseButton();
                                setStatusText("FingerPrint reader closed: FAILED");
                            }
                        }
                    });
                } else {
                    Log.v(TAG, "saveToDisk is " + saveToDisk);
                    this.biometrics.grabFingerprint(this.scanType, this.saveToDisk, new OnFingerprintGrabbedFullListener() {
                        @Override
                        public void onFingerprintGrabbed(Biometrics.ResultCode result,
                                                         Bitmap bm,
                                                         Bitmap bitmapFinger1, Bitmap bitmapFinger2,
                                                         byte[] iso,
                                                         byte[] isoFinger1, byte[] isoFinger2,
                                                         String filepath,
                                                         String filepathFinger1, String filepathFinger2,
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
                                resetToOneFingerCaptureState();
                                /* Calculate total time taken for image to return back as good. */
                                long duration = SystemClock.elapsedRealtime() - startCaptureTime;
                                setStatusText("Capture Complete in " + duration + " msec");

                                /* If scan type initiated was TWO_FINGERS_SPLIT then we need to display
                                 * each of the two split fingerprints in their own ImageViews.
                                 */
                                if (scanType.equals(ScanType.TWO_FINGERS_SPLIT)) {
                                    Log.d(TAG, "OnFingerprintGrabbedFullListener: Showing Split Images");
                                    /* Turn off single fingerprint ImageView and enable split ones. */
                                    imageViewCapturedImage.setVisibility(INVISIBLE);
                                    imageViewCapturedImageFinger1.setVisibility(VISIBLE);
                                    imageViewCapturedImageFinger2.setVisibility(VISIBLE);

                                    if (bitmapFinger1 != null) {
                                        imageViewCapturedImageFinger1.setImageBitmap(bitmapFinger1);
                                        //save to the global variable currentFingerprint1Bitmap
                                        currentFingerprint1Bitmap = bitmapFinger1;
                                    }
                                    if (bitmapFinger2 != null) {
                                        imageViewCapturedImageFinger2.setImageBitmap(bitmapFinger2);
                                        //save to the global variable currentFingerprint2Bitmap
                                        currentFingerprint2Bitmap = bitmapFinger2;
                                    }
                                    //if have filepath, then convert to wsq
                                    if (filepathFinger1 != null) {
                                        createWsqImage(filepathFinger1, bitmapFinger1);
                                        getFingerQuality(filepathFinger1);
                                    } else {
                                        getFingerQuality(bitmapFinger1);
                                    }
                                } else {
                                    imageViewCapturedImage.setImageBitmap(bm);
                                    imageViewCapturedImage.setVisibility(VISIBLE);
                                    //if have filepath, then convert to wsq
                                    if (filepath != null) {
                                        createWsqImage(filepath, bm);
                                        getFingerQuality(filepath);
                                    } else {
                                        getFingerQuality(bm);
                                    }
                                }

                                /* set global path variables for image locations. There are used later on
                                 * for actions such as getFingerQuality() or convertToWsq().
                                 */
                                pathName = filepath;
                                pathNameFingerprint1 = filepathFinger1;
                                pathNameFingerprint2 = filepathFinger2;

                                if (hasFmdMatcher) {
                                    if (scanType.equals(ScanType.TWO_FINGERS_SPLIT))
                                        currentBitmap = bitmapFinger1;
                                    else currentBitmap = bm;
                                    convertToFmd(currentBitmap);
                                } else if (pathName == null)
                                    Log.w(TAG, "onFingerprintGrabbed - OK but filepath null");
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
                                updateToCloseButton();
                                setStatusText("FingerPrint reader closed: FAILED");
                            }
                        }
                    });
                }
            }
        }
    }

    private void openFingerprint() {
        /* Start by resetting page for new capture. */
        this.resetCapture();

        // Set text view letting user know we are opening the fingerprint reader
        setStatusText("Opening scanner");
        this.biometrics.openFingerprintReader(new Biometrics.FingerprintReaderStatusListener() {
            @Override
            public void onOpenFingerprintReader(ResultCode resultCode, String hint) {
                Log.d(TAG, "Fingerprint reader opened- " + resultCode.name());
                if (hint != null && !hint.isEmpty()) {
                    setStatusText(hint);
                }
                /* Set certain widgets on/off based on if result was good. */
                if (resultCode == ResultCode.OK) {
                    setStatusText("Fingerprint reader opened: " + resultCode.toString());
                    updateToCloseButton();
                    resetToOneFingerCaptureState();
                } else if (resultCode == ResultCode.FAIL) {
                    updateToOpenButton();
                    setStatusText("Fingerprint reader opened: FAILED");
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
                    updateToCloseButton();
                    setStatusText("FingerPrint reader closed: FAILED");
                }
            }
        });
    }

    private void closeFingerprint() {
        setStatusText("Closing scanner, Please wait...");

        /* Disable capture button to avoid double clicks. */
        this.buttonCapture.setEnabled(false);
         /* Start by resetting capture system. */
        this.resetCapture();
        /* Now close fingerprint sensor. */
        this.biometrics.closeFingerprintReader();
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
        this.syncHandler = new Handler();
        /* Turn off scanner to allow capture option selection. */
        this.spinnerSync.setEnabled(false);
        this.spinnerSaveToDisk.setEnabled(false);

        if (grabFingerprintSync) {
            Log.v(TAG, "grabFingerprintSync is true");
            setStatusText("Place finger on sensor");
            Runnable mRunnable = new Runnable() {
                @Override
                public void run() {
                    FingerprintSyncResponse fingerprintSyncResponse = biometrics.grabFingerprintSync(8000);
                    final Bitmap bitmap = fingerprintSyncResponse.bitmap;
                    //Beeper.getInstance().click();
                    syncHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (bitmap != null) {
                                currentBitmap = bitmap;
                                Beeper.getInstance().click();
                                imageViewCapturedImage.setImageBitmap(currentBitmap);
                                resetToOneFingerCaptureState();
                                setStatusText("Capture Complete");
                                if (hasFmdMatcher) {
                                    convertToFmdAndMatch(currentBitmap);
                                }
                            } else {
                                Log.v(TAG, "currentBitmap is null");
                            }
                        }
                    });
                }
            };
            new Thread(mRunnable).start();
        } else if (this.grabFingerprintAsyncRaw) {
            Log.v(TAG, "grabFingerprintAsyncRaw is true");
            this.biometrics.grabFingerprint(this.scanType, this.saveToDisk, new Biometrics.OnFingerprintGrabbedRawListener() {
                @Override
                public void onFingerprintGrabbed(ResultCode result,
                                                 Bitmap bm,
                                                 byte[] iso,
                                                 String filepath,
                                                 byte[] rawImage,
                                                 String hint) {
                    /* If we got a valid Bitmap result back then ImageView to display Bitmap. */
                    if (bm != null) imageViewCapturedImage.setImageBitmap(bm);
                    /* If we got back a valid hint then set it to our status for user to see. */
                    if (hint != null && !hint.isEmpty()) setStatusText(hint);

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
                        if (filepath != null) {
                            pathName = filepath;
                        }
                        currentBitmap = bm;

                        /* Since this is our second fingerprint captured, we may convert and
                         * compare it against the first captured one.
                         */
                        convertToFmdAndMatch(currentBitmap);
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
                        updateToCloseButton();
                        setStatusText("FingerPrint reader closed: FAILED");
                    }
                }
            });
        } else {
            if (this.useFingerprintFullListener) {
                Log.v(TAG, "useFingerprintFullListener is true");
                if (this.saveToDiskDefault) {
                    Log.v(TAG, "saveToDiskDefault is true");
                    this.biometrics.grabFingerprint(this.scanType, new OnFingerprintGrabbedFullListener() {
                        @Override
                        public void onFingerprintGrabbed(Biometrics.ResultCode result,
                                                         Bitmap bm,
                                                         Bitmap bitmapFinger1, Bitmap bitmapFinger2, byte[] iso,
                                                         byte[] isoFinger1, byte[] isoFinger2,
                                                         String filepath,
                                                         String filepathFinger1, String filepathFinger2,
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
                                //save to the global variable currentBitmap
                                currentBitmap = bm;
                                /* If scan type initiated was TWO_FINGERS_SPLIT then we need to display
                                 * each of the two split fingerprints in their own ImageViews.
                                 */
                                if (scanType.equals(ScanType.TWO_FINGERS_SPLIT)) {
                                    Log.d(TAG, "OnFingerprintGrabbedFullListener: Showing Split Images");
                                    /* Turn off single fingerprint ImageView and enable split ones. */
                                    imageViewCapturedImage.setVisibility(INVISIBLE);
                                    imageViewCapturedImageFinger1.setVisibility(VISIBLE);
                                    imageViewCapturedImageFinger2.setVisibility(VISIBLE);

                                    if (bitmapFinger1 != null) {
                                        imageViewCapturedImageFinger1.setImageBitmap(bitmapFinger1);
                                        //save to the global variable currentFingerprint1Bitmap
                                        currentFingerprint1Bitmap = bitmapFinger1;
                                    }
                                    if (bitmapFinger2 != null) {
                                        imageViewCapturedImageFinger2.setImageBitmap(bitmapFinger2);
                                        //save to the global variable currentFingerprint2Bitmap
                                        currentFingerprint2Bitmap = bitmapFinger2;
                                    }
                                } else {
                                    imageViewCapturedImage.setImageBitmap(bm);
                                    imageViewCapturedImage.setVisibility(VISIBLE);
                                }

                                /* set global path variables for image locations. There are used later on
                                 * for actions such as getFingerQuality() or convertToWsq().
                                 */
                                pathName = filepath;
                                pathNameFingerprint1 = filepathFinger1;
                                pathNameFingerprint2 = filepathFinger2;

                                /* Call method to convert Bitmap to FMD template and match it against first
                                 * FMD template created.
                                 */
                                if (hasFmdMatcher) {
                                    //If the scan is for split fingers, then consider first fingerprint for matching
                                    if (scanType.equals(ScanType.TWO_FINGERS_SPLIT))
                                        convertToFmdAndMatch(bitmapFinger1);
                                    else
                                        convertToFmdAndMatch(bm);
                                } else
                                    Log.w(TAG, "Fmd matcher is not present");
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
                                updateToCloseButton();
                                setStatusText("FingerPrint reader closed: FAILED");
                            }
                        }
                    });
                } else {
                    Log.v(TAG, "saveToDisk is " + saveToDisk);
                    this.biometrics.grabFingerprint(this.scanType, this.saveToDisk, new OnFingerprintGrabbedFullListener() {
                        @Override
                        public void onFingerprintGrabbed(Biometrics.ResultCode result,
                                                         Bitmap bm,
                                                         Bitmap bitmapFinger1, Bitmap bitmapFinger2, byte[] iso,
                                                         byte[] isoFinger1, byte[] isoFinger2,
                                                         String filepath,
                                                         String filepathFinger1, String filepathFinger2,
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
                                //save to the global variable currentBitmap
                                currentBitmap = bm;
                                /* If scan type initiated was TWO_FINGERS_SPLIT then we need to display
                                 * each of the two split fingerprints in their own ImageViews.
                                 */
                                if (scanType.equals(ScanType.TWO_FINGERS_SPLIT)) {
                                    Log.d(TAG, "OnFingerprintGrabbedFullListener: Showing Split Images");
                                    /* Turn off single fingerprint ImageView and enable split ones. */
                                    imageViewCapturedImage.setVisibility(INVISIBLE);
                                    imageViewCapturedImageFinger1.setVisibility(VISIBLE);
                                    imageViewCapturedImageFinger2.setVisibility(VISIBLE);

                                    if (bitmapFinger1 != null) {
                                        imageViewCapturedImageFinger1.setImageBitmap(bitmapFinger1);
                                        //save to the global variable currentFingerprint1Bitmap
                                        currentFingerprint1Bitmap = bitmapFinger1;
                                    }
                                    if (bitmapFinger2 != null) {
                                        imageViewCapturedImageFinger2.setImageBitmap(bitmapFinger2);
                                        //save to the global variable currentFingerprint2Bitmap
                                        currentFingerprint2Bitmap = bitmapFinger2;
                                    }
                                } else {
                                    imageViewCapturedImage.setImageBitmap(bm);
                                    imageViewCapturedImage.setVisibility(VISIBLE);
                                }

                                /* set global path variables for image locations. There are used later on
                                 * for actions such as getFingerQuality() or convertToWsq().
                                 */
                                pathName = filepath;
                                pathNameFingerprint1 = filepathFinger1;
                                pathNameFingerprint2 = filepathFinger2;

                                /* Call method to convert Bitmap to FMD template and match it against first
                                 * FMD template created.
                                 */
                                if (hasFmdMatcher) {
                                    //If the scan is for split fingers, then consider first fingerprint for matching
                                    if (scanType.equals(ScanType.TWO_FINGERS_SPLIT))
                                        convertToFmdAndMatch(bitmapFinger1);
                                    else
                                        convertToFmdAndMatch(bm);
                                } else
                                    Log.w(TAG, "Fmd matcher is not present");
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
                                updateToCloseButton();
                                setStatusText("FingerPrint reader closed: FAILED");
                            }
                        }
                    });
                }
            } else
                Log.v(TAG, "useFingerprintFullListener is false");
                if (this.saveToDiskDefault) {
                    Log.v(TAG, "saveToDiskDefault is true");
                    this.biometrics.grabFingerprint(this.scanType,
                    new Biometrics.OnFingerprintGrabbedListener() {
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
                                public void onCloseFingerprintReader(ResultCode resultCode, CloseReasonCode closeReasonCode) {
                                    if (resultCode == OK) {
                                        setStatusText("FingerPrint reader closed:" + closeReasonCode.toString());
                                        resetCapture();
                                    } else if (resultCode == ResultCode.FAIL) {
                                        /* If sensor failed to close, then close button should still be clickable
                                         * since it did not actually close.
                                         */
                                        updateToCloseButton();
                                        setStatusText("FingerPrint reader closed: FAILED");
                                    }
                                }
                            });
                } else {
                    Log.v(TAG, "saveToDisk is " + saveToDisk);
                    this.biometrics.grabFingerprint(this.scanType, this.saveToDisk,
                            new Biometrics.OnFingerprintGrabbedListener() {
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
                                public void onCloseFingerprintReader(ResultCode resultCode, CloseReasonCode closeReasonCode) {
                                    if (resultCode == OK) {
                                        setStatusText("FingerPrint reader closed:" + closeReasonCode.toString());
                                        resetCapture();
                                    } else if (resultCode == ResultCode.FAIL) {
                                        /* If sensor failed to close, then close button should still be clickable
                                         * since it did not actually close.
                                         */
                                        updateToCloseButton();
                                        setStatusText("FingerPrint reader closed: FAILED");
                                    }
                                }
                            });
                }
        }
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
                    convertFmdToCCf(fmd);
                }
                // Calculate total time for callback & log output for debugging purposes
                long duration = SystemClock.elapsedRealtime() - startTime;
                Log.d(TAG, "convertToFmd " + String.valueOf(duration) + "ms");
            }
        });
    }

    /* Extract Fingerprint Minutiae Data (FMD) from the supplied WSQ image. */
    private void convertToFmd(String imagePath) {
        Log.i(TAG, "convertToFmd(String image_path[" + imagePath + "])");

        /* Keep track of initial time to see how long conversion takes. */
        final long startTime = SystemClock.elapsedRealtime();

        File file = new File(imagePath);
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
            public void onConvertToFmd(ResultCode resultCode, byte[] fmd) {
                Log.i(TAG, "onConvertToFmd(ResultCode, byte[]): CALLBACK INVOKED");
                if (resultCode != OK || fmd == null) {
                    Log.w(TAG, "convertToFmd failed so mFmd1 is null");
                    setStatusText("convertToFmd failed so NO Fingerprint template");
                } else {
                    fmdFingerTemplate1 = fmd;
                    buttonMatch.setEnabled(true);
                    convertFmdToCCf(fmd);
                }
                // Calculate total time for callback & log output for debugging purposes
                long duration = SystemClock.elapsedRealtime() - startTime;
                Log.d(TAG, "convertToFmd " + String.valueOf(duration) + "ms");
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
        if (fmd1 == null || fmd1.length == 0 || fmd2 == null || fmd2.length == 0) {
            Toast.makeText(getContext(), "FMD template for comparison is empty", Toast.LENGTH_SHORT).show();
            return;
        }
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
        Log.v(TAG, "get finger quality with bitmap");
        if (bitmap == null) {
            Toast.makeText(getContext(), "Bitmap is null. Can't get FingerQuality", Toast.LENGTH_SHORT).show();
            return;
        }
        this.biometrics.getFingerQuality(bitmap, new Biometrics.OnGetFingerQualityListener() {
            @Override
            public void onGetFingerQuality(ResultCode resultCode, int nfiqScore) {
                if (resultCode == OK)
                    setStatusText("Fingerprint Quality: " + nfiqScore);
                else setStatusText("Fingerprint Quality: " + nfiqScore);
            }
        });
    }

    /* Make API call to get NIST NFIQ fingerprint score. Pass absolute file path as the parameter. */
    private void getFingerQuality(String filePath) {
        Log.v(TAG, "get finger quality for: " + filePath);
        if (filePath == null || filePath.length() == 0) {
            Toast.makeText(getContext(), "filePath is empty. Can't get FingerQuality", Toast.LENGTH_SHORT).show();
            return;
        }
        this.biometrics.getFingerQuality(filePath, new Biometrics.OnFingerQualityListener() {
            @Override
            public void onGetFingerQuality(ResultCode resultCode, FingerQuality fingerQuality) {
                if (resultCode == OK)
                    setStatusText("Fingerprint Quality: " + fingerQuality.templateQuality);
                else setStatusText("Fingerprint Quality: " + fingerQuality.templateQuality);
            }
        });
    }

    /* Make API call to convert Bitmap image to compressed WSQ format. */
    private void createWsqImage(final String originalFilePath, final Bitmap bitmap) {
        Log.i(TAG, "createWsqImage " + originalFilePath);

        if (originalFilePath == null || originalFilePath.length() == 0) {
            Toast.makeText(getContext(), "filePath is empty. Can't create wsq image", Toast.LENGTH_SHORT).show();
            return;
        }
        if (bitmap == null) {
            Toast.makeText(getContext(), "bitmap is null. Can't create wsq image", Toast.LENGTH_SHORT).show();
            return;
        }

        /* Get original image size. */
        Log.i(TAG, "getting original image size");
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
                    Toast toast = Toast.makeText(getContext(), str, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.BOTTOM, getResources().getInteger(R.integer.toast_offset_x),
                            getResources().getInteger(R.integer.toast_offset_y));
                    toast.show();

                    /* Now that we have compressed fingerprint image we will also decompress it in
                     * order to demonstrate another CredenceSDK API call.
                     */
                    decompressWsq(pathname, bitmap);
                }
                resetToOneFingerCaptureState();
            }
        });
    }

    private void decompressWsq(String filePath, final Bitmap bitmap) {
        Log.i(TAG, "Going to call decompress API call " + filePath);

        if (filePath == null || filePath.length() == 0) {
            Toast.makeText(getContext(), "filePath is empty. Can't decompress wsq", Toast.LENGTH_SHORT).show();
            return;
        }
        if (bitmap == null) {
            Toast.makeText(getContext(), "bitmap is null. Can't decompress wsq", Toast.LENGTH_SHORT).show();
            return;
        }

        biometrics.decompressWsq(filePath, new Biometrics.OnDecompressWsqListener() {
            @Override
            public void onDecompressWsq(ResultCode resultCode, byte[] bytes) {
                String message = "De-CompressWsq was " + resultCode.toString();

                if (bytes != null) {
                    Toast toast = Toast.makeText(getContext(),
                            message + ", Length: " + bytes.length,
                            Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.BOTTOM, getResources().getInteger(R.integer.toast_offset_x),
                            getResources().getInteger(R.integer.toast_offset_y));
                    toast.show();
                    Log.v(TAG, "De-CompressWsq length is " + bytes.length);
                    byteArrayToBitmap(bytes, bitmap.getWidth(), bitmap.getHeight());
                } else {
                    Toast toast = Toast.makeText(getContext(),
                            message + ", Data was NULL.",
                            Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.BOTTOM, getResources().getInteger(R.integer.toast_offset_x),
                            getResources().getInteger(R.integer.toast_offset_y));
                    toast.show();
                }
            }
        });
    }

    private void convertFmdToCCf(final byte[] fmdTemplate) {
        Log.d(TAG, "convertFmdToCCf(byte[])");

        if (fmdTemplate == null || fmdTemplate.length == 0) {
            Toast.makeText(getContext(), "fmd template is empty. Can't convert to ccf", Toast.LENGTH_SHORT).show();
            return;
        }

        this.biometrics.convertFmdToCcf(fmdTemplate, new Biometrics.OnFmdToCcfConversionListener() {
            @Override
            public void onFmdToCcfConversion(final Biometrics.ResultCode resultCode, final byte[] ccfBytes) {
                if (ccfBytes == null || ccfBytes.length == 0) {
                    Toast.makeText(getContext(), "fmd to ccf conversion is not supported", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.v(TAG,  "Fmd -> Ccf: " + resultCode.toString() + " " + ccfBytes.length);

                if (resultCode == Biometrics.ResultCode.OK) {
                    biometrics.convertCcfToFmd(ccfBytes, (short) 400, (short) 500, (short) 100, (short) 100, new Biometrics.OnCcfToFmdConverionListener() {
                        @Override
                        public void onCcfToFmdConversion(final Biometrics.ResultCode resultCode, final byte[] fmdBytes) {
                            if (resultCode == Biometrics.ResultCode.OK) {
                                biometrics.compareFmd(fmdTemplate, fmdBytes, Biometrics.FmdFormat.ISO_19794_2_2005, new Biometrics.OnCompareFmdListener() {
                                    @Override
                                    public void onCompareFmd(Biometrics.ResultCode resultCode, float v) {
                                        Log.v(TAG, "compare Fmd and Fmd -> Ccf -> Fmd: " + fmdTemplate.length + " " + fmdBytes.length);
                                        if (resultCode != Biometrics.ResultCode.OK) {
                                            Toast.makeText(getContext(), "can't validate Fmd/Ccf conversion", Toast.LENGTH_SHORT).show();
                                        } else {
                                            if (v < (Integer.MAX_VALUE / 1000000)) {
                                                Toast.makeText(getContext(), "Fmd/Ccf conversion is validated", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(getContext(), "Fmd/Ccf conversion is not validated", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
                                });
                            } else {
                                Toast.makeText(getContext(), "convert Ccf to Fmd failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "convert Fmd to ccf failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void byteArrayToBitmap(byte[] bytes, int width, int height) {
        Log.d(TAG, "byteArrayToBitmap");

        if (bytes == null || bytes.length == 0) {
            Toast.makeText(getContext(), "byte array is empty. Can't convert to bitmap", Toast.LENGTH_SHORT).show();
            return;
        }
        if (width <= 0 || height <= 0) {
            Toast.makeText(getContext(), "bitmap size is negative", Toast.LENGTH_SHORT).show();
            return;
        }

        String productName = this.biometrics.getProductName();
        Log.d(TAG, "productName: " + productName);

        Log.v(TAG, "byteArrayToBitmap width: " + width + " height: " + height);
        byte[] testBits = new byte[bytes.length * 4];
        int i = 0;
        for (i = 0; i < bytes.length; i++) {
            testBits[i * 4] = testBits[i * 4 + 1] = testBits[i * 4 + 2] = (byte) bytes[i];
            testBits[i * 4 + 3] = -1;
        }
        Bitmap recreatedBitmap = Bitmap.createBitmap(width , height,
                Bitmap.Config.ARGB_8888);
        recreatedBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(testBits));
        imageViewCapturedImage.setImageBitmap(recreatedBitmap);

        String recreatedfilepath = new File(
                Environment.getExternalStorageDirectory(),
                "fingerprint_recreated.png")
                .getPath();
        try {
            FileOutputStream fos = new FileOutputStream(recreatedfilepath);
            recreatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Log.e(TAG, "save to file " + recreatedfilepath);
        } catch (Exception e) {
            Log.e(TAG, "Unable to save PNG image to file " + recreatedfilepath);
            e.printStackTrace();
        }
    }

    private void showImageSize(String png, String wsq, long duration) {
        if (png == null || png.length() == 0) {
            Toast.makeText(getContext(), "PNG file path is empty. Can't show image size", Toast.LENGTH_SHORT).show();
            return;
        }
        if (wsq == null || wsq.length() == 0) {
            Toast.makeText(getContext(), "WSQ file path is empty. Can't show image size", Toast.LENGTH_SHORT).show();
            return;
        }
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
        Toast toast = Toast.makeText(getContext(), str, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, getResources().getInteger(R.integer.toast_offset_x),
                getResources().getInteger(R.integer.toast_offset_y));
        toast.show();
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
        updateToCloseButton();
        /* set this to false to prevent onResume() method bing called. */
        this.isCapturing = false;
        /* alllow capture option selection */
        this.spinnerSaveToDisk.setEnabled(true);
        this.spinnerSync.setEnabled(true);
        if (this.spinnerScanType != null) {
            this.spinnerScanType.setEnabled(true);
        }
        if (spinnerSyncItem.equalsIgnoreCase(getResources().getString(R.string.sync))) {
            spinnerSaveToDisk.setEnabled(false);
            if (this.spinnerScanType != null) {
                spinnerScanType.setEnabled(false);
            }
        }
        if (spinnerSaveToDiskItem.equalsIgnoreCase(getResources().getString(R.string.default_save_png))){
            spinnerSync.setEnabled(false);
        }
    }

    private void resetToClosedState() {
        this.buttonCapture.setEnabled(false);
        updateToOpenButton();
        this.buttonMatch.setEnabled(false);
        /* alllow capture option selection */
        this.spinnerSync.setEnabled(true);
        this.spinnerSaveToDisk.setEnabled(true);
        if (this.spinnerScanType != null) {
            this.spinnerScanType.setEnabled(true);
        }
        if (spinnerSyncItem.equalsIgnoreCase(getResources().getString(R.string.sync))) {
            spinnerSaveToDisk.setEnabled(false);
            if (this.spinnerScanType != null) {
                spinnerScanType.setEnabled(false);
            }
        }
        if (spinnerSaveToDiskItem.equalsIgnoreCase(getResources().getString(R.string.default_save_png))){
            spinnerSync.setEnabled(false);
        }
    }

    /* Update buttons to close. */
    private void updateToCloseButton() {
        buttonOpenClose.setEnabled(true);
        buttonOpenClose.setText(R.string.close);
    }

    /* Update buttons to open. */
    private void updateToOpenButton() {
        buttonOpenClose.setEnabled(true);
        buttonOpenClose.setText(R.string.open);
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