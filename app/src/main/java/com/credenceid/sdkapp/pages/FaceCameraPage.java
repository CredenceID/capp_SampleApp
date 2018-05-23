package com.credenceid.sdkapp.pages;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.camera.DrawingView;
import com.android.camera.PreviewFrameLayout;
import com.credenceid.biometrics.Biometrics;
import com.credenceid.sdkapp.R;
import com.credenceid.sdkapp.SampleActivity;
import com.credenceid.sdkapp.models.PageView;
import com.credenceid.sdkapp.utils.Beeper;
import com.credenceid.sdkapp.utils.ImageTools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FaceCameraPage extends LinearLayout implements PageView,
    SurfaceHolder.Callback{
    private static final String TAG = FaceCameraPage.class.getName();

    private Biometrics biometrics;
    private SampleActivity sampleActivity;

    // File Location
    private String fileLocation = Environment.getExternalStorageDirectory() + File.separator + "face.jpg";
    private Bitmap finalBitmap = null;

    public Camera camera;
    private PreviewFrameLayout previewFrameLayout;
    private SurfaceView scannedImageView;
    private DrawingView drawingView;
    private SurfaceHolder surfaceHolder;

    private TextView statusTextView;
    private ImageButton scanButton;
    private ImageButton rightSideButton;
    private ImageButton flashOnButton;
    private ImageButton flashOffButton;

    private boolean isFrontCameraEnabled = false;
    private boolean useFlash = false;
    private boolean inPreview = false;
    private boolean isLive = true;
    private boolean cameraConfigured = false;

    private boolean isRightSizeButtonVisible = false;

    /*
    * Callback for camera capture
    */
    Camera.PictureCallback pictureCB = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera cam) {
            Log.d(TAG, "Camera.PictureCallback");

            isLive = false;
            Beeper.getInstance().click();
            scanButton.setImageResource(R.drawable.button_recapture);

            runFaceOperation(data);
        }
    };

    /*
    * Callback for camera auto focus
    */
    private Camera.AutoFocusCallback myAutoFocusCallback = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean autoFocusSuccess, Camera arg1) {
            Log.i("App", "Auto focus called success...........");

            statusTextView.setText("");
            setControlButtonVisibility(true);

            drawingView.setHaveTouch(false, new Rect(0, 0, 0, 0));
            drawingView.invalidate();
        }
    };

    public FaceCameraPage(Context context) {
        super(context);
        this.initialize();
    }

    public FaceCameraPage(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.initialize();
    }

    public FaceCameraPage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.initialize();
    }

    /*
    * Initialize
    */
    private void initialize() {
        Log.d(TAG, "initialize");

        this.initializeLayout();
        this.initializeCameraComponents();
        this.initializeActionComponents();
        this.activateCameraComponents();
        this.resetInternal();
    }

    /*
    * Initialize: initializeLayout
    */
    private void initializeLayout() {
        Log.d(TAG, "initializeLayout()");

        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.page_face_camera, this, true);
    }

    /*
    * Initialize: initializeCameraComponents
    */
    private void initializeCameraComponents() {
        Log.d(TAG, "initializeCameraComponens()");

        previewFrameLayout = (PreviewFrameLayout) findViewById(R.id.preview_frame_layout);
        scannedImageView = (SurfaceView) findViewById(R.id.scanned_image);
        drawingView = (DrawingView) findViewById(R.id.drawing_view);

        surfaceHolder = scannedImageView.getHolder();
        surfaceHolder.addCallback(this);
        Log.d(TAG, "Setting SurfaceCallback ");
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /*
     * Initialize: initializeActionComponents
     */
    private void initializeActionComponents() {
        Log.d(TAG, "initializeActionComponents()");

        statusTextView = (TextView) findViewById(R.id.status_view);
        scanButton = (ImageButton) findViewById(R.id.scan_button);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLive) {
                    resetInternal();
                    doPreview();
                    isLive = true;
                    scanButton.setImageResource(R.drawable.button_capture);
                } else {
                    if (camera != null) {
                        doCapture();
                    }
                }
            }
        });

        flashOnButton = (ImageButton) findViewById(R.id.flash_on_button);
        flashOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFlashMode(true);
            }
        });
        flashOffButton = (ImageButton) findViewById(R.id.flash_off_button);
        flashOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFlashMode(false);
            }
        });

        statusTextView = (TextView) findViewById(R.id.status_view);
    }

    /*
    * Initialize: activateCameraComponents
    */
    private void activateCameraComponents() {
        setFlashButtonVisiblity(true);

        previewFrameLayout.setVisibility(VISIBLE);
        drawingView.setVisibility(VISIBLE);
        scannedImageView.setVisibility(VISIBLE);

        initializeRightSideButton();

        isFrontCameraEnabled = false;

        resetInternal();
    //    doPreview();
        updateFlashButtons();
        setStatusText("");
    }

    /*
    * Set activity
    */
    public void setActivity(SampleActivity activity) {
        this.sampleActivity = activity;
    }

    /*
    * Activate
    */
    @Override
    public void activate(Biometrics biometrics) {
        Log.d(TAG, "activate(Biometrics)");

        this.biometrics = biometrics;
        this.setFlashButtonVisiblity(true);

        Log.d(TAG, "displaying all camera surfaces");
        previewFrameLayout.setVisibility(VISIBLE);
        drawingView.setVisibility(VISIBLE);
        scannedImageView.setVisibility(VISIBLE);

        Log.d(TAG, "re-initializing right side buttons");
        initializeRightSideButton();

        isFrontCameraEnabled = false;

        resetInternal();
        doPreview();
        updateFlashButtons();
        setStatusText("");

        if (biometrics.getProductName().contains("TAB")) {
            isRightSizeButtonVisible = true;
            rightSideButton.setVisibility(VISIBLE);
        } else {
            isRightSizeButtonVisible = false;
            rightSideButton.setVisibility(INVISIBLE);
        }
    }

    /*
    * Deactivate
    */
    @Override
    public void deactivate() {
        if (camera != null) {
            setFlashMode(false);

            if (inPreview)
                camera.stopPreview();
            camera.release();
            camera = null;
            inPreview = false;
            Log.d(TAG, "deactivate: ->>>Camera - null + inPreview = false");
        }
        Log.d(TAG, "deactivate: ->>>Camera - Deactivate is Called");
        resetMisc();
        surfaceHolder.removeCallback(this);
        surfaceDestroyed(surfaceHolder);
    }

    @Override
    public void doResume() {
        Log.i(TAG, "Camera onResume");
        new Thread(new Runnable(){
            public void run(){
                try {
                    // Add a slight delay to avoid "app passed NULL surface" error
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sampleActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resetInternal();
                        doPreview();
                    }
                });
            }
        }).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged ");
        if (camera != null) {
            Log.i(TAG, "camera != null, calling initPreview()");
            initPreview(width, height);
            startPreview();

            Log.i(TAG, "Zoom " + camera.getParameters().getZoom()
                    + " Focus " + camera.getParameters().getFocusMode()
                    + " Scene mode " + camera.getParameters().getSceneMode()
                    + " Exposure " + camera.getParameters().getExposureCompensation()
                    + " Preview format " + camera.getParameters().getPreviewFormat()
                    + " Preview width " + camera.getParameters().getPreviewSize().width
                    + " Preview height " + camera.getParameters().getPreviewSize().height
                    + " Zoom Ratio " + camera.getParameters().getZoomRatios().get(0)
                    + " Focus Mode " + camera.getParameters().getFocusMode());
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated.");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            Log.d(TAG, "surfaceDestroyed camera is not null");
            if (inPreview)
                camera.stopPreview();
            camera.release();
            camera = null;
            inPreview = false;
        } else
            Log.d(TAG, "surfaceDestroyed camera is null.");
    }

    /*
    * surfaceChanged: initPreview
    */
    private void initPreview(int width, int height) {
        Log.d(TAG, "initPreview(int, int)");

        if (camera != null && surfaceHolder.getSurface() != null) {
            Log.d(TAG, "valid params for configuring camera");
            try {
                if (!cameraConfigured) {
                    Camera.Parameters parameters = camera.getParameters();
                    List<String> focusModes = parameters.getSupportedFocusModes();
                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

                    camera.setPreviewDisplay(surfaceHolder);

                    setCameraDisplayOrientation();

                    Camera.Size size;

                    if (this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v1_product_name))
                            || this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v2_product_name))
                            || this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v3_product_name))
                            || this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v4_product_name))) {
                        size = getOptimalPreviewSize(width, height, parameters);
                    } else if (this.biometrics.getProductName().equals(R.string.trident2_product_name))
                        size = getBestPreviewSize(width, height, parameters);

                    else size = getBestPreviewSize(width, height, parameters);

                    if (size != null) {
                        parameters.setPreviewSize(size.width, size.height);
                        //set picture size for camera
                        parameters.setPictureSize(size.width, size.height);
                        Log.i(TAG, "Preview Size on TAB " + size.width + " " + size.height);
                        if (this.biometrics.getProductName().equals(getResources().getString(R.string.trident1_product_name))) {
                            previewFrameLayout.setAspectRatio((size.height) / (double) (size.width));
                            parameters.setZoom(0);
                        } else if (this.biometrics.getProductName().equals(getResources().getString(R.string.trident2_product_name))) {
                            previewFrameLayout.setAspectRatio((size.height) / (double) (size.width));
                        } else if (this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v1_product_name))
                                || this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v2_product_name))
                                || this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v3_product_name))
                                || this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v4_product_name))) {
                            Log.i(TAG, "Surface Preview is configured");
                            previewFrameLayout.setAspectRatio((size.width) / (double) (size.height));
                        } else if (this.biometrics.getProductName().equals(getResources().getString(R.string.credence_two_v1_product_name))
                                || this.biometrics.getProductName().equals(getResources().getString(R.string.credence_two_v2_product_name))) {
                            previewFrameLayout.setAspectRatio((size.width) / (double) (size.height));
                            previewFrameLayout.getLayoutParams().width = (int) (size.width * 2.3);
                            previewFrameLayout.getLayoutParams().height = (int) (size.height * 2.3);
                        } else if (this.biometrics.getProductName().equals(getResources().getString(R.string.credence_one_v1_product_name))
                                || this.biometrics.getProductName().equals(getResources().getString(R.string.credence_one_v2_product_name))
                                || this.biometrics.getProductName().equals(getResources().getString(R.string.credence_one_v3_product_name))
                                || this.biometrics.getProductName().equals(R.string.credence_one_ektp_product_name))
                            previewFrameLayout.setAspectRatio(size.width / (double) size.height);

                        double aspect = (size.width / (double) size.height);
                        Log.i(TAG, "aspect ratio " + aspect);

                        camera.setParameters(parameters);
                        cameraConfigured = true;

                        Log.i(TAG, "Preview0 " + previewFrameLayout.getCameraDistance()
                                + " width0 " + previewFrameLayout.getLayoutParams().width
                                + " height0 " + previewFrameLayout.getLayoutParams().height
                                + " FOCUS MODE: " + camera.getParameters().getFocusMode());
                    }
                } else Log.d(TAG, "camera was already configured, no need now");
            } catch (Throwable t) {
                Log.e("PreviewDemo-Callback", "Exception in setPreviewDisplay()", t);
            }
        }

    }

    /*
     * surfaceChanged: initPreview: getBestPreviewSize
     */
    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) result = size;
                else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea)
                        result = size;
                }
            }
        }
        return (result);
    }

    /*
     * surfaceChanged: initPreview: getOptimalPreviewSize
     */
    private Camera.Size getOptimalPreviewSize(int w, int h, Camera.Parameters params) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        List<Camera.Size> sizes = params.getSupportedPictureSizes();
        if (sizes == null) return null;

        Camera.Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /*
    * Get camera and do preview
    */
    private void doPreview() {
        Log.d(TAG, "doPreview()");

        try {
            if (camera == null) {
                Log.d(TAG, "page_type was face");

                if ((this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v1_product_name))
                        || this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v2_product_name))
                        || this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v3_product_name))
                        || this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v4_product_name)))
                        && isFrontCameraEnabled) {
                    Log.d(TAG, "opening front camera");
                    camera = openFrontFacingCamera();
                    setPreviewSize(640, 480, 1.25555555);
                } else if (this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v1_product_name))
                        || this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v2_product_name))
                        || this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v3_product_name))
                        || this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v4_product_name))){
                    Log.d(TAG, "opening back camera on tablet");
                    camera = Camera.open();
                    setPreviewSize(1024, 768, 1.3333333333);
                } else {
                    camera = Camera.open();
                }
            } else
                Log.d(TAG, "mCamera object is not null");

            if (camera != null) {
                Log.d(TAG, "Started camera preview");
                camera.setPreviewDisplay(surfaceHolder);
                this.setCameraDisplayOrientation();
                startPreview();
                inPreview = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Demo App failed to start preview", e);
            if (camera != null)
                camera.release();
            camera = null;
            inPreview = false;
        }
    }

    private void setPreviewSize(int width, int height, double ratio) {
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(width, height);
        //set picture size for camera
        parameters.setPictureSize(width, height);
        previewFrameLayout.setAspectRatio(ratio);
        camera.setParameters(parameters);
    }

    private void startPreview() {
        if (cameraConfigured && camera != null) {
            Log.d(TAG, "startPreview called");

            statusTextView.setText("");
            previewFrameLayout.setVisibility(VISIBLE);
            drawingView.setVisibility(VISIBLE);
            scannedImageView.setVisibility(VISIBLE);

            camera.startPreview();
            inPreview = true;
            scanButton.setImageResource(R.drawable.button_capture);
            scanButton.setVisibility(VISIBLE);

            if (isRightSizeButtonVisible) {
                rightSideButton.setVisibility(VISIBLE);
            } else {
                rightSideButton.setVisibility(INVISIBLE);
            }
        }
    }

    private void setCameraDisplayOrientation() {
        if (this.biometrics.getProductName().equals(getResources().getString(R.string.trident1_product_name)))
            camera.setDisplayOrientation(0);
        else if (this.biometrics.getProductName().equals(getResources().getString(R.string.trident2_product_name)))
            camera.setDisplayOrientation(0);
        else if (this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v1_product_name))
                || this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v2_product_name))
                || this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v3_product_name))
                || this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v4_product_name))) {
            if (isFrontCameraEnabled) {
                camera.setDisplayOrientation(180);
            } else camera.setDisplayOrientation(0);
        } else if (this.biometrics.getProductName().equals(getResources().getString(R.string.credence_two_v1_product_name))
                || this.biometrics.getProductName().equals(getResources().getString(R.string.credence_two_v2_product_name)))
            camera.setDisplayOrientation(90);
        else camera.setDisplayOrientation(90);
    }

    @Override
    public String getTitle() {
        return getContext().getResources().getString(R.string.title_face_camera);
    }

    private void initializeRightSideButton() {
        Log.d(TAG, "initializeRightSideButton");

        rightSideButton = (ImageButton) findViewById(R.id.right_side_button);
        rightSideButton.setVisibility(VISIBLE);
        rightSideButton.setImageResource(R.drawable.icon_camera_flip);

        rightSideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickReverseCamera();
                isLive = true;
            }
        });
    }

    public void resetInternal() {
        inPreview = true;

        if (finalBitmap != null)
            finalBitmap.recycle();
        finalBitmap = null;

        previewFrameLayout.setVisibility(VISIBLE);
        drawingView.setVisibility(VISIBLE);
        scannedImageView.setVisibility(VISIBLE);

        setControlButtonVisibility(true);
        scanButton.setImageResource(R.drawable.button_capture);
    }

    /*
    * Auto focus by touching
    */
    public void touchFocus(final Rect tfocusRect) {
        if (!inPreview) return;

        setControlButtonVisibility(false);
        setStatusText("Autofocusing, please wait...");

        final int one = 2000, two = 1000;
        // Here we properly bound our Rect for a better tap to focus region
        final Rect targetFocusRect = new Rect(
                tfocusRect.left * one / drawingView.getWidth() - two,
                tfocusRect.top * one / drawingView.getHeight() - two,
                tfocusRect.right * one / drawingView.getWidth() - two,
                tfocusRect.bottom * one / drawingView.getHeight() - two);

        // Since the camera parameters only accept a List of camera areas to focus, we create a list
        final List<Camera.Area> focusList = new ArrayList<Camera.Area>();
        // Here we take the Graphics.Rect and convert it to a Camera.Rect for the camera to understand
        Camera.Area focusArea = new Camera.Area(targetFocusRect, 1000);
        // Add our custom Rect to focus into our list
        focusList.add(focusArea);

        if (!this.biometrics.getProductName().equals(getResources().getString(R.string.trident2_product_name))
                && !this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v1_product_name))
                && !this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v2_product_name))
                && !this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v3_product_name))
                && !this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v4_product_name))
                && !this.biometrics.getProductName().equals(getResources().getString(R.string.credence_two_v1_product_name))
                && !this.biometrics.getProductName().equals(getResources().getString(R.string.credence_two_v2_product_name))) {
            Camera.Parameters para = camera.getParameters();
            para.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            para.setFocusAreas(focusList);
            para.setMeteringAreas(focusList);
            camera.setParameters(para);
        }

        // Call camera AutoFocus & pass callback to be called when auto focus finishes
        camera.autoFocus(myAutoFocusCallback);
        // Tell our drawing view we have a touch in the given Rect
        drawingView.setHaveTouch(true, tfocusRect);
        // Tell our drawing view to Update
        drawingView.invalidate();
    }

    private void setControlButtonVisibility(boolean visibility) {
        scanButton.setVisibility(visibility ? VISIBLE : INVISIBLE);
    }

    /*
    * Do capture after clicking capture button
    */
    private void doCapture() {
        resetInternal();

        Camera.Parameters parameters = camera.getParameters();

        if (this.biometrics.getProductName().equals(getResources().getString(R.string.trident1_product_name)))
            parameters.setRotation(270);
        else if (this.biometrics.getProductName().equals(getResources().getString(R.string.trident2_product_name)))
            parameters.setRotation(180);
        else if (this.biometrics.getProductName().equals(getResources().getString(R.string.credence_one_v1_product_name))
                || this.biometrics.getProductName().equals(getResources().getString(R.string.credence_one_v2_product_name))
                || this.biometrics.getProductName().equals(getResources().getString(R.string.credence_one_v3_product_name))
                || this.biometrics.getProductName().equals(R.string.credence_one_ektp_product_name))
            parameters.setRotation(0);
        else if (this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v1_product_name))
                || this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v2_product_name))
                || this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v3_product_name))
                || this.biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v4_product_name))) {
            if (isFrontCameraEnabled) {
                parameters.setRotation(180);
                for (int i = 0; i <= 5; i++) {
                    Log.i(TAG, " Supported Front Camera params: " + parameters.getSupportedPreviewSizes().get(i).width
                            + " " + parameters.getSupportedPreviewSizes().get(i).height);
                }

            } else parameters.setRotation(0);
        }
        camera.setParameters(parameters);

        try {
            if (camera != null) {
                setStatusText("Starting capture, hold still...");
                inPreview = false;
                this.setControlButtonVisibility(false);
                camera.takePicture(null, null, null, pictureCB);
                Log.i(TAG, "FrontCamera " + parameters.getPreviewSize().width + " " + parameters.getPreviewSize().height);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while taking picture " + e);
        }
    }

    private void setStatusText(String text) {
        statusTextView.setText(text);
    }

    private void flashControl(boolean useFlash) {
        if (camera != null) {
            this.biometrics.cameraFlashControl(useFlash);
        }
    }

    private void setFlashMode(boolean useFlash) {
        this.useFlash = useFlash;
        flashControl(useFlash);
        updateFlashButtons();
    }

    private void updateFlashButtons() {
        flashOnButton.setActivated(useFlash);
        flashOffButton.setActivated(!useFlash);
    }

    private void setFlashButtonVisiblity(boolean visiblity) {
        this.flashOnButton.setVisibility(visiblity ? VISIBLE : INVISIBLE);
        this.flashOffButton.setVisibility(visiblity ? VISIBLE : INVISIBLE);
    }


    public String getFaceFileLocation() {
        return fileLocation;
    }

    /*
    * Reset when onStop
    */
    private void resetMisc() {
        inPreview = true;

        if (finalBitmap != null)
            finalBitmap.recycle();
        finalBitmap = null;

        previewFrameLayout.setVisibility(VISIBLE);
        drawingView.setVisibility(VISIBLE);
        scannedImageView.setVisibility(VISIBLE);

        setControlButtonVisibility(true);
        rightSideButton.setVisibility(INVISIBLE);
        scanButton.setImageResource(R.drawable.button_capture);
    }

    /*
    * Toggle between front camera and back camera
    */
    public void onClickReverseCamera() {
        isFrontCameraEnabled = !isFrontCameraEnabled;
        setFlashButtonVisiblity(!isFrontCameraEnabled);

        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }

        doPreview();
    }

    /*
    * When do preview, get camera if front facing camera is open
    */
    private Camera openFrontFacingCamera() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }

        if (cam != null) {
            Log.i(TAG, "front camera1: " + cam.getParameters().getPreviewSize().width
                    + " " + cam.getParameters().getPreviewSize().height);
        }

        return cam;
    }


    /*
    * Save face image
    */
    private void runFaceOperation(byte[] imageData) {
        setStatusText("Image Captured");
        new AsyncTask<byte[], String, String>() {
            File photo;

            @Override
            protected String doInBackground(byte[]... jpeg) {
                sampleActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setStatusText("Saving image, please wait...");
                    }
                });

                photo = getOutputMediaFile();
                if (photo.exists()) {
                    photo.delete();
                }

                Bitmap realImage;

                if (Build.MODEL.equals("CID")) {
                    realImage = BitmapFactory.decodeByteArray(jpeg[0], 0, jpeg[0].length);
                    Log.i(TAG, "Camera: --> MODEL --> CID");
                } else {
                    realImage = getProperOrientation(photo.getPath(), jpeg[0]);
                }

                try {
                    FileOutputStream fos = new FileOutputStream(photo.getPath());
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    realImage.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                    realImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                } catch (IOException e) {
                    Log.e("PictureDemo", "Exception in photoCallback", e);
                    return null;
                }

                return (photo.getPath());
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);

                sampleActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setStatusText("");
                        setControlButtonVisibility(true);
                    }
                });
            }

            private File getOutputMediaFile() {
                File f;
                f = new File(getFaceFileLocation());
                Log.d(TAG, "Image file saved to " + f.getName());
                return f;
            }

            Bitmap getProperOrientation(String filename, byte[] image) {
                Bitmap realImage = BitmapFactory.decodeByteArray(image, 0, image.length);

                ExifInterface exif = null;
                try {
                    exif = new ExifInterface(filename);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d("EXIF value", exif.getAttribute(ExifInterface.TAG_ORIENTATION));
                int rotation = 0;

                if ((biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v1_product_name))
                        || biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v2_product_name))
                        || biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v3_product_name))
                        || biometrics.getProductName().equals(getResources().getString(R.string.credencetab_v4_product_name))) && isFrontCameraEnabled) {
                    rotation = 180;
                } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("6"))
                    rotation = 90;
                else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("8"))
                    rotation = 270;
                else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("3"))
                    rotation = 180;
                else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("0"))
                    rotation = 0;

                Log.v(TAG, "rotation value " + rotation);
                return ImageTools.Editor.Rotate(realImage, rotation);
            }
        }.execute(imageData);
    }
}
