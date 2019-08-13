package com.credenceid.sdkapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.DeviceFamily;
import com.credenceid.face.AnalyzeFaceSyncResponse;
import com.credenceid.face.CompareFaceSyncResponse;
import com.credenceid.face.CreateFaceTemplateSyncResponse;
import com.credenceid.face.DetectFaceSyncResponse;
import com.credenceid.face.FaceEngine;
import com.credenceid.face.FaceEngine.Emotion;
import com.credenceid.face.FaceEngine.Gender;
import com.credenceid.face.FaceEngine.HeadPoseDirection;
import com.credenceid.sdkapp.android.camera.DrawingView;
import com.credenceid.sdkapp.android.camera.PreviewFrameLayout;
import com.credenceid.sdkapp.android.camera.Utils;
import com.credenceid.sdkapp.util.Beeper;
import com.credenceid.sdkapp.util.BitmapUtils;
import com.credenceid.sdkapp.util.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_SHORT;
import static com.credenceid.biometrics.Biometrics.ResultCode.FAIL;
import static com.credenceid.biometrics.Biometrics.ResultCode.INTERMEDIATE;
import static com.credenceid.biometrics.Biometrics.ResultCode.OK;
import static com.credenceid.biometrics.DeviceFamily.CredenceOne;
import static com.credenceid.biometrics.DeviceFamily.CredenceTAB;
import static com.credenceid.biometrics.DeviceFamily.CredenceTwo;
import static com.credenceid.biometrics.DeviceFamily.TridentOne;
import static com.credenceid.biometrics.DeviceFamily.TridentTwo;

@SuppressWarnings({"StatementWithEmptyBody", "unused"})
public class CameraActivity
        extends Activity
        implements SurfaceHolder.Callback {

    private final static String TAG = CameraActivity.class.getSimpleName();
    private static final String IMG_ABS_PATH = App.SDCARD_PATH + "oreofaceimage.bmp";

    /* To obtain high face detection rate we use lowest possible camera resolution for preview.
     * For the actual picture size, we will use the largest available resolution so there is no
     * loss in face image quality.
     */
    private final static int P_WIDTH = 320;
    private final static int P_HEIGHT = 240;
    private static final int IMAGE_WIDTH_4_BY_3_8MP = 3264;
    private static final int IMAGE_HEIGHT_4_BY_3_8MP = 2448;
    private static final int COMPRESSION_QUALITY = 100;
    private static final int SYNC_API_TIMEOUT_MS = 3000;

    /* It is always good to have a global context in case non-activity classes require it. In this
     * case "Beeper" class requires it so it may grab audio file from assets.
     */
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    /* Absolute paths of where face images are stores on disk. */
    private final File mFiveMPFile
            = new File(Environment.getExternalStorageDirectory() + "/c-sampleapp_5mp.jpg");
    private final File mEightMPFile
            = new File(Environment.getExternalStorageDirectory() + "/c-sampleapp_8mp.jpg");

    /* --------------------------------------------------------------------------------------------
     *
     * Components in layout file.
     *
     * --------------------------------------------------------------------------------------------
     */
    private PreviewFrameLayout mPreviewFrameLayout;
    private DrawingView mDrawingView;
    private SurfaceView mScannedImageView;
    private SurfaceHolder mSurfaceHolder;
    private TextView mStatusTextView;
    private Button mFlashOnButton;
    private Button mFlashOffButton;
    private Button mCaptureButton;
    private CheckBox mEightMPCheckbox;

    private Camera mCamera = null;

    /* If true then camera is in preview, if false it is not. */
    private boolean mInPreview = false;
    /* Has camera preview settings been initialized. If true yes, false otherwise. This is required
     * so camera preview does not start without it first being configured.
     */
    private boolean mIsCameraConfigured = false;

    /* --------------------------------------------------------------------------------------------
     *
     * Callbacks.
     *
     * --------------------------------------------------------------------------------------------
     */

    /* This callback is invoked after camera finishes taking a picture. */
    private Camera.PictureCallback mOnPictureTakenCallback = new Camera.PictureCallback() {
        public void
        onPictureTaken(byte[] data,
                       Camera cam) {

            /* Produce "camera shutter" sound so user knows that picture was captured. */
            Beeper.click(mContext);
            /* Now that picture has been taken, turn off flash. */
            setTorchEnable(false);
            /* Camera is no longer in preview. */
            mInPreview = false;

            try {
                Intent intent = new Intent(getApplicationContext(), FaceActivity.class);

                /* There is an Android framework's bug in Oreo where sending image data through
                 * Intent's causes an "FAILED BINDER TRANSACTION" crash to occur. This same block of
                 * code works as expected on Marshmallow, Nougat, Pie, yet fails on ALL Android Oreo
                 * devices. To get around it at app level, we simply save image then pass its path.
                 */
                if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    FileUtils.saveBitmap(BitmapUtils.decode(data), IMG_ABS_PATH);
                    intent.putExtra("image", IMG_ABS_PATH);
                } else {
                    /* Pass image bytes to face.Activity so it may perform face analysis. */
                    intent.putExtra("image", data);
                }

                /* Destroy this activity and launch new one. */
                finish();
                startActivity(intent);

            } catch (Exception e) {
                Toast.makeText(getApplicationContext(),
                    "Unable to run face analysis, retry.",
                    Toast.LENGTH_LONG).show();
            }
        }
    };

    /* This callback is invoked on each camera preview frame. In this callback will run call face
     * detection API and pass it preview frame.
     */
    private Camera.PreviewCallback mCameraPreviewCallback
            = (byte[] data, Camera camera) -> detectFace(data);

    /* This callback is invoked each time camera finishes auto-focusing. */
    private Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        public void
        onAutoFocus(boolean autoFocusSuccess,
                    Camera arg1) {

            /* Remove previous status since auto-focus is now done. */
            mStatusTextView.setText("");

            /* Tell DrawingView to stop displaying auto-focus circle by giving it a region of 0. */
            mDrawingView.setHasTouch(false, new Rect(0, 0, 0, 0));
            mDrawingView.invalidate();

            /* Re-enable capture button. */
            setCaptureButtonVisibility(true);
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
        setContentView(R.layout.act_camera);

        mContext = this;
        mCamera = null;

        this.initializeLayoutComponents();
        this.configureLayoutComponents();

        this.reset();
        this.doPreview();
    }

    @Override
    protected void
    onResume() {

        super.onResume();

        new Thread(() -> {
            try {
                /* Add a slight delay to avoid "Application passed NULL surface" error. */
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                this.reset();
                this.doPreview();
            });
        }).start();
    }

    /* This is required to stop camera every time back button is pressed.  */
    @Override
    public void
    onBackPressed() {

        super.onBackPressed();
        this.stopReleaseCamera();
        this.finish();
    }

    /* This is required to stop camera preview every time activity loses focus. */
    @Override
    protected void
    onPause() {

        super.onPause();
        this.stopReleaseCamera();
    }

    /* This is required to stop camera every time application is killed.  */
    @Override
    protected void
    onStop() {

        super.onStop();
        this.stopReleaseCamera();
    }

    /* This is required to stop camera every time application is killed.  */
    @Override
    protected void
    onDestroy() {

        super.onDestroy();
        this.setTorchEnable(false);

        if (mCamera != null) {
            if (mInPreview)
                mCamera.stopPreview();

            mCamera.release();
            mCamera = null;
            mInPreview = false;
        }

        /* Can only remove surface's and callbacks AFTER camera has been told to stop preview and it
         * has been released. If we did this first, then camera would try to write to an invalid
         * surface.
         */
        mSurfaceHolder.removeCallback(this);

        this.surfaceDestroyed(mSurfaceHolder);
    }

    /* --------------------------------------------------------------------------------------------
     *
     * SurfaceHolder.Callback methods.
     *
     * --------------------------------------------------------------------------------------------
     */

    @Override
    public void
    surfaceChanged(SurfaceHolder holder,
                   int format,
                   int width,
                   int height) {

        if (null == mCamera) {
            Log.w(TAG, "Camera object is null, will not set up preview.");
            return;
        }

        this.initPreview();
        this.startPreview();
    }

    @Override
    public void
    surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void
    surfaceDestroyed(SurfaceHolder holder) {

        if (null == mCamera)
            return;

        if (mInPreview)
            mCamera.stopPreview();

        mCamera.release();
        mCamera = null;
        mInPreview = false;
    }

    /* --------------------------------------------------------------------------------------------
     *
     * Layout initialization and configure.
     *
     * --------------------------------------------------------------------------------------------
     */

    /* Initializes all layout file component objects. */
    private void
    initializeLayoutComponents() {

        mPreviewFrameLayout = findViewById(R.id.preview_frame_layout);
        mDrawingView = findViewById(R.id.drawing_view);
        mScannedImageView = findViewById(R.id.scanned_imageview);

        mStatusTextView = findViewById(R.id.status_textview);
        mFlashOnButton = findViewById(R.id.flash_on_button);
        mFlashOffButton = findViewById(R.id.flash_off_button);
        mCaptureButton = findViewById(R.id.capture_button);
        mEightMPCheckbox = findViewById(R.id.eight_mp_checkbox);
    }

    /* Configured all layout file component objects. Assigns listeners, configurations, etc. */
    @SuppressWarnings("deprecation")
    private void
    configureLayoutComponents() {
        this.setFlashButtonVisibility(true);

        /* Only CredenceTAB family of device's support 8MP back camera resolution.  */
        if (CredenceTAB != App.DevFamily)
            mEightMPCheckbox.setVisibility(View.GONE);

        mPreviewFrameLayout.setVisibility(VISIBLE);
        mDrawingView.setVisibility(VISIBLE);
        mScannedImageView.setVisibility(VISIBLE);

        mSurfaceHolder = mScannedImageView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mCaptureButton.setOnClickListener((View v) -> {
            if (!mInPreview) {
                this.reset();
                this.doPreview();

                mCaptureButton.setText(getString(R.string.capture_label));
            } else if (mCamera != null)
                doCapture();
        });

        mFlashOnButton.setOnClickListener((View v) -> this.setTorchEnable(true));
        mFlashOffButton.setOnClickListener((View v) -> this.setTorchEnable(false));
    }

    /* --------------------------------------------------------------------------------------------
     *
     * Camera initialization, reset, close, etc.
     *
     * --------------------------------------------------------------------------------------------
     */

    private void
    initPreview() {

        if (null == mCamera || null == mSurfaceHolder.getSurface()) {
            Log.d(TAG, "Either camera or SurfaceHolder was null, skip initPreview().");
            return;
        }
        if (mIsCameraConfigured) {
            Log.d(TAG, "camera is already configured, no need to iniPreview().");
            return;
        }

        try {
            /* Tell camera object where to display preview frames. */
            mCamera.setPreviewDisplay(mSurfaceHolder);
            /* Initialize camera preview in proper orientation. */
            this.setCameraPreviewDisplayOrientation();

            /* Get camera parameters. We will edit these, then write them back to camera. */
            Camera.Parameters parameters = mCamera.getParameters();

            /* Enable auto-focus if available. */
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

            /* For FaceEngine we show a preview with 320x240, but the actual image is
             * captured with largest available picture size, this way we get a high
             * resolution in final image.
             */
            Camera.Size picSize = Utils.getLargestPictureSize(parameters);
            parameters.setPictureSize(picSize.width, picSize.height);

            /* Regardless of what size is returned we always use a 320x240 preview size for face
             * detection since it is extremely fast.
             *
             * This previewSize is used to set up dimensions of all camera views.
             */
            Camera.Size previewSize = parameters.getPreviewSize();
            previewSize.width = P_WIDTH;
            previewSize.height = P_HEIGHT;

            if (CredenceTwo == App.DevFamily) {
                mPreviewFrameLayout.getLayoutParams().width = (int) (previewSize.height * 2.5);
                mPreviewFrameLayout.getLayoutParams().height = (int) (previewSize.width * 2.5);
            }
            mPreviewFrameLayout.setAspectRatio((previewSize.width) / (double) (previewSize.height));

            ViewGroup.LayoutParams drawingViewLayoutParams = mDrawingView.getLayoutParams();

            if (CredenceTAB == App.DevFamily) {
                drawingViewLayoutParams.width = (int) (previewSize.width * 2.75);
                drawingViewLayoutParams.height = (int) (previewSize.height * 2.75);
            } else {
                ViewGroup.LayoutParams prevParams = mPreviewFrameLayout.getLayoutParams();
                drawingViewLayoutParams.width = prevParams.width;
                drawingViewLayoutParams.height = prevParams.height;
            }
            mDrawingView.setLayoutParams(drawingViewLayoutParams);

            /* Need to set FaceEngine specific bitmap size so DrawingView knows
             * where and how to draw face detection points. Otherwise it would
             * assume the bitmap size is 0.
             */
            mDrawingView.setBitmapDimensions(P_WIDTH, P_HEIGHT);

            mCamera.setParameters(parameters);
            mIsCameraConfigured = true;
        } catch (Throwable t) {
            Log.e("PreviewDemo-Callback", "Exception in setPreviewDisplay()", t);
        }
    }

    private void
    startPreview() {

        if (mIsCameraConfigured && null != mCamera) {
            mStatusTextView.setText("");
            mPreviewFrameLayout.setVisibility(VISIBLE);
            mDrawingView.setVisibility(VISIBLE);
            mScannedImageView.setVisibility(VISIBLE);

            mCamera.startPreview();

            mInPreview = true;
            mCaptureButton.setText(getString(R.string.capture_label));
            this.setCaptureButtonVisibility(true);
        } else Log.w(TAG, "Camera not configured, aborting start preview.");
    }

    private void
    doPreview() {

        try {
            /* If camera was not already opened, open it. */
            if (null == mCamera) {
                mCamera = Camera.open();

                /* Tells camera to give us preview frames in these dimensions. */
                this.setPreviewSize(P_WIDTH, P_HEIGHT, (double) P_WIDTH / P_HEIGHT);
            }

            if (null != mCamera) {
                /* Tell camera where to draw frames to. */
                mCamera.setPreviewDisplay(mSurfaceHolder);
                /* Tell camera to invoke this callback on each frame. */
                mCamera.setPreviewCallback(mCameraPreviewCallback);
                /* Rotate preview frames to proper orientation based on DeviceType. */
                this.setCameraPreviewDisplayOrientation();
                /* Now we can tell camera to start preview frames. */
                this.startPreview();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start camera preview: " + e.getLocalizedMessage());
            if (null != mCamera)
                mCamera.release();

            mCamera = null;
            mInPreview = false;
        }
    }

    /* Tells camera to return preview frames in a certain width/height and aspect ratio.
     *
     * @param width Width of preview frames to send back.
     * @param height Height of preview frames to send back.
     * @param ratio Aspect ration of preview frames to send back.
     */
    @SuppressWarnings("SameParameterValue")
    private void
    setPreviewSize(int width,
                   int height,
                   double ratio) {

        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(width, height);
        mPreviewFrameLayout.setAspectRatio(ratio);
        mCamera.setParameters(parameters);
    }

    /* Tells camera to rotate captured pictured by a certain angle. This is required since on some
     * devices the physical camera hardware is 90 degrees, etc.
     */
    private void
    setCameraPictureOrientation() {

        Camera.Parameters parameters = mCamera.getParameters();

        if (App.DevFamily == DeviceFamily.TridentOne)
            parameters.setRotation(270);
        else if (App.DevFamily == DeviceFamily.TridentTwo)
            parameters.setRotation(180);
        else if (App.DevFamily == CredenceOne || App.DevFamily == CredenceTAB)
            parameters.setRotation(0);

        mCamera.setParameters(parameters);
    }

    /* Tells camera to rotate preview frames by a certain angle. This is required since on some
     * devices the physical camera hardware is 90 degrees, etc.
     */
    private void
    setCameraPreviewDisplayOrientation() {

        int orientation = 90;

        /* For C-TAB, the BACK camera requires 0, but FRONT camera is 180. In this example FRONT
         * camera is not used, so that case was not programed in.
         */
        if (App.DevFamily == TridentOne || App.DevFamily == TridentTwo
                || App.DevFamily == CredenceTAB) {

            orientation = 0;
        }
        mCamera.setDisplayOrientation(orientation);
    }

    /* Captures image, before capturing image it will set proper picture orientation. */
    private void
    doCapture() {

        this.setCameraPictureOrientation();

        if (mCamera != null) {
            this.setCaptureButtonVisibility(false);
            mStatusTextView.setText(getString(R.string.start_capture_hold_still));

            /* We are no longer going to be in preview. Set variable BEFORE telling camera to take
             * picture. Camera takes time to take a picture so we do not want any preview event to
             * take place while a picture is being captured.
             */
            mInPreview = false;
            mCamera.takePicture(null, null, null, mOnPictureTakenCallback);
        }
    }

    /* Sets camera flash.
     *
     * @param useFlash If true turns on flash, if false disables flash.
     */
    private void
    setTorchEnable(boolean useFlash) {

        /* If camera object was destroyed, there is nothing to do. */
        if (null == mCamera)
            return;

        /* Camera flash parameters do not work on TAB/TRIDENT devices. In order to use flash on
         * these devices you must use the Credence APIs.
         */
        if (App.DevFamily == CredenceTAB || App.DevFamily == TridentOne || App.DevFamily == TridentTwo) {
            App.BioManager.cameraTorchEnable(useFlash);
        } else {
            try {
                Camera.Parameters p = mCamera.getParameters();
                p.setFlashMode(useFlash ? FLASH_MODE_TORCH : FLASH_MODE_OFF);
                mCamera.setParameters(p);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* Resets camera flash and UI back to camera preview state. */
    private void
    reset() {

        /* This method is called before we start a camera preview, so we update global variable. */
        mInPreview = true;

        /* Change capture button image to "Capture". */
        mCaptureButton.setText(getString(R.string.capture_label));

        /* Turn off flash since new preview. */
        this.setTorchEnable(false);

        /* Display all buttons in their proper states. */
        this.setCaptureButtonVisibility(true);
        this.setFlashButtonVisibility(true);
    }

    /* Stops camera preview, turns off torch, releases camera object, and sets it to null. */
    private void
    stopReleaseCamera() {

        if (null != mCamera) {
            /* Tell camera to no longer invoke callback on each preview frame. */
            mCamera.setPreviewCallback(null);
            /* Turn off flash. */
            this.setTorchEnable(false);

            /* Stop camera preview. */
            if (mInPreview)
                mCamera.stopPreview();

            /* Release camera and nullify object. */
            mCamera.release();
            mCamera = null;
            /* We are no longer in preview mode. */
            mInPreview = false;
        }

        /* Remove camera surfaces. */
        mSurfaceHolder.removeCallback(this);
        this.surfaceDestroyed(mSurfaceHolder);
    }

    /* --------------------------------------------------------------------------------------------
     *
     * Private helpers.
     *
     * --------------------------------------------------------------------------------------------
     */

    /* This method either hides or shows capture button allowing user to capture an image. This is
     * required because while camera is focusing user should not be allowed to press capture. Once
     * focusing finishes and a clear preview is available, only then should an image be allowed to
     * be taken.
     *
     * @param visibility If true button is shown, if false button is hidden.
     */
    private void
    setCaptureButtonVisibility(boolean visibility) {

        mCaptureButton.setVisibility(visibility ? VISIBLE : INVISIBLE);
    }

    /* This method either hides or shows flash buttons allowing user to control flash. This is
     * required because after an image is captured a user should not be allowed to control flash
     * since camera is no longer in preview. Instead of disabling the buttons we hide them from
     * the user.
     *
     * @param visibility If true buttons are show, if false they are hidden.
     */
    @SuppressWarnings("SameParameterValue")
    private void
    setFlashButtonVisibility(boolean visibility) {

        mFlashOnButton.setVisibility(visibility ? VISIBLE : INVISIBLE);
        mFlashOffButton.setVisibility(visibility ? VISIBLE : INVISIBLE);
    }

    /* Attempts to perform tap-to-focus on camera with given focus region.
     *
     * @param touchRect Region to focus on.
     */
    @SuppressLint("SetTextI18n")
    public void
    performTapToFocus(final Rect touchRect) {

        if (!mInPreview)
            return;

        this.setCaptureButtonVisibility(false);
        mStatusTextView.setText(getString(R.string.autofocus_wait));

        final int one = 2000, two = 1000;

        /* Here we properly bound our Rect for a better tap to focus region */
        final Rect targetFocusRect = new Rect(
                touchRect.left * one / mDrawingView.getWidth() - two,
                touchRect.top * one / mDrawingView.getHeight() - two,
                touchRect.right * one / mDrawingView.getWidth() - two,
                touchRect.bottom * one / mDrawingView.getHeight() - two);

        /* Since Camera parameters only accept a List of  areas to focus, create a list. */
        final List<Camera.Area> focusList = new ArrayList<>();
        /* Convert Graphics.Rect to Camera.Rect for camera parameters to understand.
         * Add custom focus Rect. region to focus list.
         */
        focusList.add(new Camera.Area(targetFocusRect, 1000));

        /* For certain device auto-focus parameters need to be explicitly setup. */
        if (App.DevFamily == CredenceOne) {
            Camera.Parameters para = mCamera.getParameters();
            para.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            para.setFocusAreas(focusList);
            para.setMeteringAreas(focusList);
            mCamera.setParameters(para);
        }

        /* Call mCamera AutoFocus and pass callback to be called when auto focus finishes */
        mCamera.autoFocus(mAutoFocusCallback);
        /* Tell our drawing view we have a touch in the given Rect */
        mDrawingView.setHasTouch(true, touchRect);
        /* Tell our drawing view to Update */
        mDrawingView.invalidate();
    }

    /* Attempts to detect face Rect. region in given image. If face image is found it updates
     * DrawingView on where to draw Rectangle and then tells it to perform an "onDraw()".
     *
     * @param bitmapBytes Bitmap image in byte format to run detection on.
     */
    private void
    detectFace(byte[] bitmapBytes) {

        /* If camera was closed, immediately after a preview callback exit out, this is to prevent
         * NULL pointer exceptions when using the camera object later on.
         */
        if (null == mCamera || null == bitmapBytes)
            return;

        /* We need to stop camera preview callbacks from continuously being invoked while processing
         * is going on. Otherwise we would have a backlog of frames needing to be processed. To fix
         * this we remove preview callback, then re-enable it post-processing.
         *
         * - Preview callback invoked.
         * -- Tell camera to sto preview callbacks.
         * **** Meanwhile camera is still receiving frames, but continues to draw them. ****
         * -- Process camera preview frame.
         * -- Draw detected face Rect.
         * -- Tell camera to invoke preview callback with next frame.
         *
         * Using this technique does not drop camera frame-rate, so camera does not look "laggy".
         * Instead now we use every 5-th frame for face detection.
         */
        mCamera.setPreviewCallback(null);

        /* Need to fix color format of raw camera preview frames. */
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        Rect rect = new Rect(0, 0, 320, 240);
        YuvImage yuvimage = new YuvImage(bitmapBytes, ImageFormat.NV21, 320, 240, null);
        yuvimage.compressToJpeg(rect, 100, outStream);

        /* Save fixed color image as final good Bitmap. */
        Bitmap bm = BitmapFactory.decodeByteArray(outStream.toByteArray(), 0, outStream.size());

        /* On CredenceTWO device's captured image is rotated by 270 degrees. To fix this rotate
         * image by another 90 degrees to have it right-side-up.
         */
        if (CredenceTwo == App.DevFamily)
            bm = Utils.rotateBitmap(bm, 90);

        /* Detect face on finalized Bitmap image. */
        App.BioManager.detectFace(bm, (Biometrics.ResultCode resultCode,
                                       RectF rectF) -> {
            /* If camera was closed or preview stopped, immediately exit out. This is done so that
             * we do not continue to process invalid frames, or draw to NULL surfaces.
             */
            if (null == mCamera || !mInPreview)
                return;

            /* Tell camera to start preview callbacks again. */
            mCamera.setPreviewCallback(mCameraPreviewCallback);

            if (resultCode == OK) {
                /* Tell view that it will need to draw a detected face's Rect. region. */
                mDrawingView.setHasFace(true);

                /* If a CredenceTWO device then bounding Rect needs to be scaled to properly fit. */
                if (CredenceTwo == App.DevFamily) {
                    mDrawingView.setFaceRect(rectF.left + 40,
                            rectF.top - 25,
                            rectF.right + 40,
                            rectF.bottom - 50);
                } else {
                    mDrawingView.setFaceRect(rectF.left,
                            rectF.top,
                            rectF.right,
                            rectF.bottom);
                }
            } else {
                /* Tell view to not draw face Rect. region on next "onDraw()" call. */
                mDrawingView.setHasFace(false);
            }

            /* Tell view to invoke an "onDraw()". */
            mDrawingView.invalidate();
        });
    }

    /* --------------------------------------------------------------------------------------------
     *
     * Un-used Async APIs.
     *
     * --------------------------------------------------------------------------------------------
     */

    private void
    createFaceTemplateAsync(Bitmap bitmap) {

        App.BioManager.createFaceTemplate(bitmap, (resultCode, bytes) -> {

            if (OK == resultCode) {
                Log.d(TAG, "createFaceTemplateAsync(Bitmap): Template created.");
            } else if (INTERMEDIATE == resultCode) {
                /* This code is never returned here. */
            } else if (FAIL == resultCode) {
                Log.d(TAG, "createFaceTemplateAsync(Bitmap): Failed to create template.");
            }
        });
    }

    private void
    createFaceTemplateAsync(byte[] bitmapBytes,
                            int bitmapWidth,
                            int bitmapHeight) {

        App.BioManager.createFaceTemplate(bitmapBytes, bitmapWidth, bitmapHeight, (resultCode,
                                                                                   bytes) -> {

            if (OK == resultCode) {
                Log.d(TAG, "createFaceTemplateAsync(byte[], int, int): Template created.");
            } else if (INTERMEDIATE == resultCode) {
                /* This code is never returned here. */
            } else if (FAIL == resultCode) {
                Log.d(TAG, "createFaceTemplateAsync(byte[], int, int): Failed to create template.");
            }
        });
    }

    private void
    matchFacesAsync(byte[] templateOne,
                    byte[] templateTwo) {

        App.BioManager.compareFace(templateOne, templateTwo, (resultCode, i) -> {
            if (OK == resultCode) {
                Log.d(TAG, "matchFacesAsync(byte[], byte[]): Score = " + i);
            } else if (INTERMEDIATE == resultCode) {
                /* This code is never returned here. */
            } else if (FAIL == resultCode) {
                Log.d(TAG, "matchFacesAsync(byte[], byte[]): Failed to compare templates.");
            }
        });
    }

    /* --------------------------------------------------------------------------------------------
     *
     * Synchronous APIs.
     *
     * --------------------------------------------------------------------------------------------
     */

    private RectF
    detectFaceSync(Bitmap bitmap) {

        DetectFaceSyncResponse res = App.BioManager.detectFaceSync(bitmap, SYNC_API_TIMEOUT_MS);

        if (null != res && OK == res.resultCode)
            return res.faceRect;
        return new RectF(0, 0, 0, 0);
    }

    private RectF
    detectFaceSync(byte[] bitmapBytes,
                   int bitmapWidth,
                   int bitmapHeight) {

        DetectFaceSyncResponse res = App.BioManager.detectFaceSync(bitmapBytes,
                bitmapWidth,
                bitmapHeight,
                SYNC_API_TIMEOUT_MS);

        if (null != res && OK == res.resultCode)
            return res.faceRect;
        return new RectF(0, 0, 0, 0);
    }

    private FaceData
    analyzeFaceSync(Bitmap bitmap) {

        AnalyzeFaceSyncResponse res = App.BioManager.analyzeFaceSync(bitmap, SYNC_API_TIMEOUT_MS);

        if (null != res && OK == res.resultCode) {
            return new FaceData(res.faceRect,
                    res.landmark5,
                    res.landmark68,
                    res.headPoseEstimations,
                    res.headPoseDirections,
                    res.gender,
                    res.dominantEmotion,
                    res.age,
                    res.hasGlasses,
                    res.imageQuality);

        }
        return new FaceData();
    }

    private FaceData
    analyzeFaceSync(byte[] bitmapBytes,
                    int bitmapWidth,
                    int bitmapHeight) {

        AnalyzeFaceSyncResponse res = App.BioManager.analyzeFaceSync(bitmapBytes,
                bitmapWidth,
                bitmapHeight,
                SYNC_API_TIMEOUT_MS);

        if (null != res && OK == res.resultCode) {
            return new FaceData(res.faceRect,
                    res.landmark5,
                    res.landmark68,
                    res.headPoseEstimations,
                    res.headPoseDirections,
                    res.gender,
                    res.dominantEmotion,
                    res.age,
                    res.hasGlasses,
                    res.imageQuality);

        }
        return new FaceData();
    }

    private byte[]
    createFaceTemplateSync(Bitmap bitmap) {

        CreateFaceTemplateSyncResponse res
                = App.BioManager.createFaceTemplateSync(bitmap, SYNC_API_TIMEOUT_MS);

        if (null != res && OK == res.resultCode)
            return res.template;
        return new byte[]{};
    }

    private byte[]
    createFaceTemplateSync(byte[] bitmapBytes,
                           int bitmapWidth,
                           int bitmapHeight) {

        CreateFaceTemplateSyncResponse res
                = App.BioManager.createFaceTemplateSync(bitmapBytes,
                bitmapWidth,
                bitmapHeight,
                SYNC_API_TIMEOUT_MS);

        if (null != res && OK == res.resultCode)
            return res.template;
        return new byte[]{};
    }

    private int
    matchFacesSync(byte[] templateOne,
                   byte[] templateTwo) {

        CompareFaceSyncResponse res
                = App.BioManager.compareFaceSync(templateOne, templateTwo, SYNC_API_TIMEOUT_MS);

        if (null != res && OK == res.resultCode)
            return res.score;
        return 0;
    }

    @SuppressWarnings({"WeakerAccess", "unchecked"})
    private class FaceData {

        public RectF FaceRect = new RectF();
        public ArrayList<PointF> Landmark5 = new ArrayList();
        public ArrayList<PointF> Landmark68 = new ArrayList();
        public float[] PoseEstimations = new float[3];
        public HeadPoseDirection[] PoseDirections = new HeadPoseDirection[3];
        public Gender Gender = FaceEngine.Gender.UNKNOWN;
        public int Age = -1;
        public Emotion Emotion = FaceEngine.Emotion.UNKNOWN;
        public boolean Glasses = false;
        public int Quality = -1;

        public FaceData() {
        }

        public FaceData(RectF rect,
                        ArrayList<PointF> landmark5,
                        ArrayList<PointF> landmark68,
                        float[] pose,
                        HeadPoseDirection[] poseDirections,
                        Gender gender,
                        Emotion emotion,
                        int age,
                        boolean glasses,
                        int quality) {

            this.FaceRect = rect;
            this.Landmark5 = landmark5;
            this.Landmark68 = landmark68;
            this.PoseEstimations = pose;
            this.PoseDirections = poseDirections;
            this.Gender = gender;
            this.Age = age;
            this.Emotion = emotion;
            this.Glasses = glasses;
            this.Quality = quality;
        }
    }
}
