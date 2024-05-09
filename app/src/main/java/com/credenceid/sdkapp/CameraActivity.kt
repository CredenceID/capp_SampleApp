@file:Suppress("DEPRECATION")

package com.credenceid.sdkapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.Camera
import android.hardware.Camera.Parameters.FLASH_MODE_OFF
import android.hardware.Camera.Parameters.FLASH_MODE_TORCH
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast
import com.credenceid.biometrics.Biometrics
import com.credenceid.biometrics.Biometrics.ResultCode.*
import com.credenceid.biometrics.DeviceFamily.*
import com.credenceid.face.FaceEngine
import com.credenceid.face.FaceEngine.*
import com.credenceid.sdkapp.android.camera.Utils
import com.credenceid.sdkapp.databinding.ActCameraBinding
import com.credenceid.sdkapp.util.Beeper
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

private val TAG = CameraActivity::class.java.simpleName

/**
 * To obtain high face detection rate we use lowest possible camera resolution for preview.
 * For the actual picture size, we will use the largest available resolution so there is no
 * loss in face image quality.
 */
private const val P_WIDTH = 320
private const val P_HEIGHT = 240
private const val syncAPITimeoutMS = 3000

/**
 * It is always good to have a global context in case non-activity classes require it. In
 * this case "Beeper" class requires it so it may grab audio file from assets.
 */
@SuppressLint("StaticFieldLeak")
private var context: Context? = null

private var camera: Camera? = null

/**
 * If true then camera is in preview, if false it is not.
 */
private var inPreview = false
/**
 * Has camera preview settings been initialized. If true yes, false otherwise. This is required
 * so camera preview does not start without it first being configured.
 */
private var mIsCameraConfigured = false
private var surfaceHolder: SurfaceHolder? = null

@Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
class CameraActivity : Activity(), SurfaceHolder.Callback {

    private lateinit var binding: ActCameraBinding

    /**
     * This callback is invoked after camera finishes taking a picture.
     */
    private val mOnPictureTakenCallback = Camera.PictureCallback { data, _ ->
        /* Produce "camera shutter" sound so user knows that picture was captured. */
        Beeper.click(context!!)
        /* Now that picture has been taken, turn off flash. */
        setTorchEnable(false)
        /* Camera is no longer in preview. */
        inPreview = false

        val dataBytes = compressImageByteArray(data, 80)

        try {
            val intent = Intent(this, FaceActivity::class.java)
            intent.putExtra(getString(R.string.camera_image), dataBytes)
            stopReleaseCamera()
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()

            Toast.makeText(this, "Unable to run face analysis, retry.", Toast.LENGTH_LONG).show()
        }
    }

    fun compressImageByteArray(imageByteArray: ByteArray, quality: Int): ByteArray {
        // Convert the byte array to a Bitmap
        val bitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.size)

        // Compress the bitmap
        val compressedBitmap = compressBitmap(bitmap, quality)

        // Convert the compressed bitmap back to a byte array
        val stream = ByteArrayOutputStream()
        compressedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream) // Adjust format and quality as needed
        return stream.toByteArray()
    }
    private fun compressBitmap(bitmap: Bitmap, quality: Int): Bitmap {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val byteArray = stream.toByteArray()
        val options = BitmapFactory.Options()
        options.inSampleSize = calculateInSampleSize(byteArray, quality)
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
    }

    private fun calculateInSampleSize(byteArray: ByteArray, quality: Int): Int {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
        val originalWidth = options.outWidth
        val originalHeight = options.outHeight
        val targetWidth = originalWidth / 2
        val targetHeight = originalHeight / 2
        var inSampleSize = 1
        while ((originalWidth / inSampleSize) >= targetWidth && (originalHeight / inSampleSize) >= targetHeight) {
            inSampleSize *= 2
        }
        return inSampleSize
    }


    /**
     * This callback is invoked on each camera preview frame. In this callback will run call face
     * detection API and pass it preview frame.
     */
    private val mCameraPreviewCallback = { data: ByteArray, _: Camera -> detectFaceAsync(data) }

    /**
     * This callback is invoked each time camera finishes auto-focusing.
     */
    private val mAutoFocusCallback = Camera.AutoFocusCallback { _, _ ->
        /* Remove previous status since auto-focus is now done. */
        binding.statusTextView.text = ""

        /* Tell DrawingView to stop displaying auto-focus circle by giving it a region of 0. */
        binding.drawingView.setHasTouch(false, Rect(0, 0, 0, 0))
        binding.drawingView.invalidate()

        /* Re-enable capture button. */
        setCaptureButtonVisibility(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(App.TAG, "onCreate()")
        super.onCreate(savedInstanceState)
        binding = ActCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        context = this
    }

    /**
     * This is required to stop camera every time back button is pressed.
     */
    override fun onBackPressed() {

        Log.d(App.TAG, "onBackPressed()")
        super.onBackPressed()
        this.stopReleaseCamera()
        this.finish()
    }

    override fun onStart() {
        super.onStart()
        Log.d(App.TAG, "onStart()")

        context = this
        camera = null

        this.configureLayoutComponents()
        this.reset()
        this.doPreview()
    }

    /**
     * This is required to stop camera preview every time activity loses focus.
     */
    override fun onPause() {

        Log.d(App.TAG, "onPause()")
        super.onPause()
        this.stopReleaseCamera()
    }

    /**
     * This is required to stop camera every time application is killed.
     */
    override fun onDestroy() {

        Log.d(App.TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun surfaceChanged(holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int) {

        Log.d(App.TAG, "surfaceChanged()")

        if (null == camera) {
            Log.w(TAG, "Camera object is null, will not set up preview.")
            return
        }

        try {
            this.initPreview()
            this.startPreview()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {

        Log.d(App.TAG, "surfaceCreated()")
        surfaceHolder = holder
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

        if (null == camera)
            return

        stopReleaseCamera()
    }

    /**
     * Configure all layout file component objects. Assigns listeners, configurations, etc.
     */
    private fun configureLayoutComponents() {

        this.setFlashButtonVisibility(true)

        /* Only CredenceTAB family of device's support 8MP back camera resolution.  */
        if (CredenceTAB != App.DevFamily)
            binding.eightMPCheckBox.visibility = View.GONE

        binding.previewFrameLayout.visibility = VISIBLE
        binding.drawingView.visibility = VISIBLE
        binding.scanImageView.visibility = VISIBLE

        surfaceHolder = binding.scanImageView.holder
        surfaceHolder!!.addCallback(this)
        surfaceHolder!!.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        binding.captureBtn.setOnClickListener { v: View ->
            if (!inPreview) {
                this.reset()
                this.doPreview()

                binding.captureBtn.text = getString(R.string.capture_label)
            } else if (camera != null)
                doCapture()
        }

        binding.flashOnBtn.setOnClickListener { this.setTorchEnable(true) }
        binding.flashOffBtn.setOnClickListener { this.setTorchEnable(false) }
    }

    private fun initPreview() {

        Log.d(App.TAG, "initPreview()")

        if (null == camera || null == surfaceHolder!!.surface) {
            Log.d(TAG, "Either camera or SurfaceHolder was null, skip initPreview().")
            return
        }
        if (mIsCameraConfigured) {
            Log.d(TAG, "camera is already configured, no need to iniPreview().")
            return
        }
        try {
            Log.d(App.TAG, "Executing: camera.setPreviewDisplay(surfaceHolder)")
            camera!!.setPreviewDisplay(surfaceHolder)
        } catch (ignore: IOException) {
            Log.d(App.TAG, "Executing: camera.setPreviewDisplay(surfaceHolder) Error : " + ignore.toString())
            return
        }

        /* Initialize camera preview in proper orientation. */
        this.setCameraPreviewDisplayOrientation()

        /* Get camera parameters. We will edit these, then write them back to camera. */
        val parameters = camera!!.parameters

        Log.d(App.TAG, "Camera Parameters = " + parameters.flatten())

        // Enable auto-focus if available.
       val focusModes = parameters.supportedFocusModes
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            Log.d(App.TAG, "Set FOCUS_MODE_AUTO camera parameters")
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
        }

        /* For FaceEngine we show a preview with 320x240, but the actual image is
         * captured with largest available picture size, this way we get a high
         * resolution in final image.
         */
        val picSize = Utils.getLargestPictureSize(parameters)
        parameters.setPictureSize(picSize.width, picSize.height)

        /* Regardless of what size is returned we always use a 320x240 preview size for face
         * detection since it is extremely fast.
         *
         * This previewSize is used to set up dimensions of all camera views.
         */
        val previewSize = parameters.previewSize
        previewSize.width = P_WIDTH
        previewSize.height = P_HEIGHT

        if (CredenceTwo == App.DevFamily) {
            binding.previewFrameLayout.layoutParams.width = (previewSize.height * 2.5).toInt()
            binding.previewFrameLayout.layoutParams.height = (previewSize.width * 2.5).toInt()
        }
        binding.previewFrameLayout.setAspectRatio(previewSize.width / previewSize.height.toDouble())

        val drawingViewLayoutParams = binding.drawingView.layoutParams

        if (CredenceTAB == App.DevFamily) {
            drawingViewLayoutParams.width = (previewSize.width * 2.75).toInt()
            drawingViewLayoutParams.height = (previewSize.height * 2.75).toInt()
        } else {
            val prevParams = binding.previewFrameLayout.layoutParams
            drawingViewLayoutParams.width = prevParams.width
            drawingViewLayoutParams.height = prevParams.height
        }
        binding.drawingView.layoutParams = drawingViewLayoutParams

        /* Need to set FaceEngine specific bitmap size so DrawingView knows
         * where and how to draw face detection points. Otherwise it would
         * assume the bitmap size is 0.
         */
        binding.drawingView.setBitmapDimensions(P_WIDTH, P_HEIGHT)

        camera!!.parameters = parameters
        mIsCameraConfigured = true
    }

    private fun startPreview() {

        Log.d(App.TAG, "startPreview()")

        if (mIsCameraConfigured && null != camera) {
            Log.d(App.TAG, "Camera is configured & valid.")

            binding.statusTextView.text = ""
            binding.captureBtn.text = getString(R.string.capture_label)
            this.setCaptureButtonVisibility(true)

            binding.previewFrameLayout.visibility = VISIBLE
            binding.drawingView.visibility = VISIBLE
            binding.scanImageView.visibility = VISIBLE

            Log.d(App.TAG, "Executing: camera.startPreview()")
            camera!!.startPreview()
            inPreview = true

        } else {
            Log.w(TAG, "Camera not configured, aborting start preview.")
        }
    }

    private fun doPreview() {

        Log.d(App.TAG, "doPreview()")
        try {
            /* If camera was not already opened, open it. */
            if (null == camera) {
                Log.d(App.TAG, "Camera is null, opening camera.")
                camera = Camera.open()
                /* Tells camera to give us preview frames in these dimensions. */
                this.setPreviewSize(P_WIDTH, P_HEIGHT, P_WIDTH.toDouble() / P_HEIGHT)
            }

            if (null != camera) {
                Log.d(App.TAG, "Camera is not null, will set preview.")
                /* Tell camera where to draw frames to. */
                camera!!.setPreviewDisplay(surfaceHolder)
                /* Tell camera to invoke this callback on each frame. */
                camera!!.setPreviewCallback(mCameraPreviewCallback)
                /* Rotate preview frames to proper orientation based on DeviceType. */
                this.setCameraPreviewDisplayOrientation()
                /* Now we can tell camera to start preview frames. */
                this.startPreview()

            } else {
                Log.d(App.TAG, "Camera failed to open.")
            }
        } catch (e: Exception) {
            e.printStackTrace()

            if (null != camera)
                camera!!.release()


            camera = null
            inPreview = false
        }

    }

    /**
     * Tells camera to return preview frames in a certain width/height and aspect ratio.
     *
     * @param width Width of preview frames to send back.
     * @param height Height of preview frames to send back.
     * @param ratio Aspect ration of preview frames to send back.
     */
    private fun setPreviewSize(@Suppress("SameParameterValue") width: Int,
                               @Suppress("SameParameterValue") height: Int,
                               @Suppress("SameParameterValue") ratio: Double) {

        val parameters = camera!!.parameters
        parameters.setPreviewSize(width, height)
        binding.previewFrameLayout.setAspectRatio(ratio)
        camera!!.parameters = parameters
    }

    /**
     * Tells camera to rotate captured pictured by a certain angle. This is required since on some
     * devices the physical camera hardware is 90 degrees, etc.
     */
    private fun setCameraPictureOrientation() {

        val parameters = camera!!.parameters

        if (App.DevFamily == TridentOne)
            parameters.setRotation(270)
        else if (App.DevFamily == TridentTwo)
            parameters.setRotation(180)
        else if (App.DevFamily == CredenceOne || App.DevFamily == CredenceTAB)
            parameters.setRotation(0)

        camera!!.parameters = parameters
    }

    /**
     * Tells camera to rotate preview frames by a certain angle. This is required since on some
     * devices the physical camera hardware is 90 degrees, etc.
     */
    private fun setCameraPreviewDisplayOrientation() {

        var orientation = 90

        /* For C-TAB, the BACK camera requires 0, but FRONT camera is 180. In this example FRONT
         * camera is not used, so that case was not programed in.
         */
        if (App.DevFamily == TridentOne || App.DevFamily == TridentTwo
                || App.DevFamily == CredenceTAB) {

            orientation = 0
        }
        camera!!.setDisplayOrientation(orientation)
    }

    /**
     * Captures image, before capturing image it will set proper picture orientation.
     */
    private fun doCapture() {

        this.setCameraPictureOrientation()

        if (camera != null) {
            this.setCaptureButtonVisibility(false)
            binding.statusTextView.text = getString(R.string.start_capture_hold_still)

            /* We are no longer going to be in preview. Set variable BEFORE telling camera to take
             * picture. Camera takes time to take a picture so we do not want any preview event to
             * take place while a picture is being captured.
             */
            inPreview = false
            camera!!.takePicture(null, null, null, mOnPictureTakenCallback)
        }
    }

    /**
     * Sets camera flash.
     *
     * @param useFlash If true turns on flash, if false disables flash.
     */
    private fun setTorchEnable(useFlash: Boolean) {

        /* If camera object was destroyed, there is nothing to do. */
        if (null == camera)
            return

        /* Camera flash parameters do not work on TAB/TRIDENT devices. In order to use flash on
         * these devices you must use the Credence APIs.
         */
        if (App.DevFamily == CredenceTAB || App.DevFamily == TridentOne || App.DevFamily == TridentTwo) {
            App.BioManager!!.cameraTorchEnable(useFlash)
        } else {
            try {
                val p = camera!!.parameters
                p.flashMode = if (useFlash) FLASH_MODE_TORCH else FLASH_MODE_OFF
                camera!!.parameters = p
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    /**
     * Resets camera flash and UI back to camera preview state. */
    private fun reset() {

        Log.d(App.TAG, "reset()")

        /* Change capture button image to "Capture". */
        binding.captureBtn.text = getString(R.string.capture_label)

        /* Turn off flash since new preview. */
        this.setTorchEnable(false)

        /* Display all buttons in their proper states. */
        this.setCaptureButtonVisibility(true)
        this.setFlashButtonVisibility(true)
    }

    /**
     * Stops camera preview, turns off torch, releases camera object, and sets it to null.
     */
    private fun stopReleaseCamera() {

        Log.d(App.TAG, "stopReleaseCamera()")

        if (null != camera) {
            Log.d(App.TAG, "Camera is not null, will release.")

            /* Tell camera to no longer invoke callback on each preview frame. */
            camera!!.setPreviewCallback(null)
            /* Turn off flash. */
            this.setTorchEnable(false)

            /* Stop camera preview. */
            if (inPreview) {
                Log.d(App.TAG, "Camera was in preview, executing: camera.stopPreview().")
                camera!!.stopPreview()
            }

            Log.d(App.TAG, "Executing: camera.release().")
            /* Release camera and nullify object. */
            camera!!.release()
            mIsCameraConfigured = false;
            camera = null
            /* We are no longer in preview mode. */
            inPreview = false
        }

        /* Remove camera surfaces. */
        surfaceHolder!!.removeCallback(this)
        this.surfaceDestroyed(surfaceHolder!!)
    }

    /**
     * This method either hides or shows capture button allowing user to capture an image. This is
     * required because while camera is focusing user should not be allowed to press capture. Once
     * focusing finishes and a clear preview is available, only then should an image be allowed to
     * be taken.
     *
     * @param visibility If true button is shown, if false button is hidden.
     */
    private fun setCaptureButtonVisibility(visibility: Boolean) {

        binding.captureBtn.visibility = if (visibility) VISIBLE else INVISIBLE
    }

    /**
     * This method either hides or shows flash buttons allowing user to control flash. This is
     * required because after an image is captured a user should not be allowed to control flash
     * since camera is no longer in preview. Instead of disabling the buttons we hide them from
     * the user.
     *
     * @param visibility If true buttons are show, if false they are hidden.
     */
    private fun setFlashButtonVisibility(@Suppress("SameParameterValue") visibility: Boolean) {

        binding.flashOnBtn.visibility = if (visibility) VISIBLE else INVISIBLE
        binding.flashOffBtn.visibility = if (visibility) VISIBLE else INVISIBLE
    }

    /**
     * Attempts to perform tap-to-focus on camera with given focus region.
     *
     * @param touchRect Region to focus on.
     */
    @SuppressLint("SetTextI18n")
    fun performTapToFocus(touchRect: Rect) {

        if (!inPreview)
            return

        this.setCaptureButtonVisibility(false)
        binding.statusTextView.text = getString(R.string.autofocus_wait)

        val one = 2000
        val two = 1000

        /* Here we properly bound our Rect for a better tap to focus region */
        val targetFocusRect = Rect(
                touchRect.left * one / binding.drawingView.width - two,
                touchRect.top * one / binding.drawingView.height - two,
                touchRect.right * one / binding.drawingView.width - two,
                touchRect.bottom * one / binding.drawingView.height - two)

        /* Since Camera parameters only accept a List of  areas to focus, create a list. */
        val focusList = ArrayList<Camera.Area>()
        /* Convert Graphics.Rect to Camera.Rect for camera parameters to understand.
         * Add custom focus Rect. region to focus list.
         */
        focusList.add(Camera.Area(targetFocusRect, 1000))

        /* For certain device auto-focus parameters need to be explicitly setup. */
        if (App.DevFamily == CredenceOne) {
            val para = camera!!.parameters
            para.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
            para.focusAreas = focusList
            para.meteringAreas = focusList
            camera!!.parameters = para
        }

        /* Call camera AutoFocus and pass callback to be called when auto focus finishes */
        camera!!.autoFocus(mAutoFocusCallback)
        /* Tell our drawing view we have a touch in the given Rect */
        binding.drawingView.setHasTouch(true, touchRect)
        /* Tell our drawing view to Update */
        binding.drawingView.invalidate()
    }

    private inline fun <T : Any, R> whenNotNull(input: T?, callback: (T) -> R): R? {
        return input?.let(callback)
    }

    /**
     * Attempts to detect face Rect. region in given image. If face image is found it updates
     * DrawingView on where to draw Rectangle and then tells it to perform an "onDraw()".
     *
     * @param bitmapBytes Bitmap image in byte format to run detection on.
     */
    private fun detectFaceAsync(bitmapBytes: ByteArray?) {

        /* If camera was closed, immediately after a preview callback exit out, this is to prevent
         * NULL pointer exceptions when using the camera object later on.
         */
        if (null == camera || null == bitmapBytes)
            return

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
        camera!!.setPreviewCallback(null)

        /* Need to fix color format of raw camera preview frames. */
        val outStream = ByteArrayOutputStream()
        val rect = Rect(0, 0, 320, 240)
        val yuvimage = YuvImage(bitmapBytes, ImageFormat.NV21, 320, 240, null)
        yuvimage.compressToJpeg(rect, 100, outStream)

        /* Save fixed color image as final good Bitmap. */
        var bm = BitmapFactory.decodeByteArray(outStream.toByteArray(), 0, outStream.size())

        /* On CredenceTWO device's captured image is rotated by 270 degrees. To fix this rotate
         * image by another 90 degrees to have it right-side-up.
         */
        if (CredenceTwo == App.DevFamily)
            bm = Utils.rotateBitmap(bm, 90f)

        /* Detect face on finalized Bitmap image. */
        App.BioManager!!.detectFace(bm) { rc: Biometrics.ResultCode,
                                          rectF: RectF? ->

            /* If camera was closed or preview stopped, immediately exit out. This is done so that
             * we do not continue to process invalid frames, or draw to NULL surfaces.
             */
            if (null == camera || !inPreview)
                return@detectFace

            /* Tell camera to start preview callbacks again. */
            camera!!.setPreviewCallback(mCameraPreviewCallback)

            when (rc) {
                OK -> {
                    whenNotNull(rectF) {
                        /* Tell view that it will need to draw a detected face's Rect. region. */
                        binding.drawingView.setHasFace(true)

                        /* If CredenceTWO then bounding Rect needs to be scaled to properly fit. */
                        if (CredenceTwo == App.DevFamily) {
                            binding.drawingView.setFaceRect(rectF!!.left + 40, rectF.top - 25,
                                    rectF.right + 40, rectF.bottom - 50)
                        } else {
                            binding.drawingView.setFaceRect(rectF!!.left, rectF.top,
                                    rectF.right, rectF.bottom)
                        }
                    }
                }
                INTERMEDIATE -> {
                    /* This code is never returned for this API. */
                }
                FAIL -> {
                    /* Tell view to not draw face Rect. region on next "onDraw()" call. */
                    binding.drawingView.setHasFace(false)
                }
                else -> {
                    /* Tell view to not draw face Rect. region on next "onDraw()" call. */
                    binding.drawingView.setHasFace(false)
                }
            }

            /* Tell view to invoke an "onDraw()". */
            binding.drawingView.invalidate()
        }
    }

    /**
     * --------------------------------------------------------------------------------------------
     *
     * Methods demonstrating how to use Credence ID other fingerprint APIs. This methods are not
     * used in this application, as they are only here for usage reference.
     *
     * --------------------------------------------------------------------------------------------
     */

    @Suppress("unused")
    private fun detectFaceAsync(bitmapBytes: ByteArray,
                                bitmapWidth: Int,
                                bitmapHeight: Int) {

        App.BioManager!!.detectFace(bitmapBytes, bitmapWidth, bitmapHeight) { rc, rectF ->
            when (rc) {
                OK -> Log.d(TAG, "detectFaceAsync(byte[], int, int): RectF: $rectF")
                INTERMEDIATE -> {
                    /* This code is never returned for this API. */
                }
                FAIL -> Log.d(TAG, "detectFaceAsync(byte[], int, int): Failed to find face.")

                else -> {
                }
            }
        }
    }

    @Suppress("unused")
    private fun analyzeFaceAsync(bitmapBytes: ByteArray,
                                 bitmapWidth: Int,
                                 bitmapHeight: Int) {

        App.BioManager!!.analyzeFace(bitmapBytes, bitmapWidth, bitmapHeight) { rc,
                                                                               rectF,
                                                                               _,
                                                                               _,
                                                                               _,
                                                                               _,
                                                                               gender,
                                                                               age,
                                                                               emotion,
                                                                               hasGlasses,
                                                                               imageQuality ->

            when (rc) {
                OK -> {
                    Log.d(TAG, "analyzeFaceAsync(byte[], int, int): RectF: $rectF")
                    Log.d(TAG, "analyzeFaceAsync(byte[], int, int): Gender: " + gender.name)
                    Log.d(TAG, "analyzeFaceAsync(byte[], int, int): Age: $age")
                    Log.d(TAG, "analyzeFaceAsync(byte[], int, int): Emotion: " + emotion.name)
                    Log.d(TAG, "analyzeFaceAsync(byte[], int, int): Glasses: $hasGlasses")
                    Log.d(TAG, "analyzeFaceAsync(byte[], int, int): ImageQuality: $imageQuality")
                }
                INTERMEDIATE -> {
                    /* This code is never returned for this API. */
                }
                FAIL -> Log.d(TAG, "analyzeFaceAsync(byte[], int, int): Failed to find face.")
                else -> {
                }
            }
        }
    }

    @Suppress("unused")
    private fun createFaceTemplateAsync(bitmap: Bitmap) {

        App.BioManager!!.createFaceTemplate(bitmap) { rc, _ ->

            when (rc) {
                OK -> Log.d(TAG, "createFaceTemplateAsync(Bitmap): Template created.")
                INTERMEDIATE -> {
                    /* This code is never returned for this API. */
                }
                FAIL -> Log.d(TAG, "createFaceTemplateAsync(Bitmap): Failed to create template.")
                else -> {
                }
            }
        }
    }

    @Suppress("unused")
    private fun createFaceTemplateAsync(bitmapBytes: ByteArray,
                                        bitmapWidth: Int,
                                        bitmapHeight: Int) {

        App.BioManager!!.createFaceTemplate(bitmapBytes, bitmapWidth, bitmapHeight) { rc, _ ->

            when (rc) {
                OK -> Log.d(TAG, "createFaceTemplateAsync(byte[], int, int): PASS")
                INTERMEDIATE -> {
                    /* This code is never returned for this API. */
                }
                FAIL -> Log.d(TAG, "createFaceTemplateAsync(byte[], int, int): FAIL")
                else -> {
                }
            }
        }
    }

    @Suppress("unused")
    private fun matchFacesAsync(templateOne: ByteArray,
                                templateTwo: ByteArray) {

        App.BioManager!!.compareFace(templateOne, templateTwo) { rc, i ->
            when (rc) {
                OK -> Log.d(TAG, "matchFacesAsync(byte[], byte[]): Score = $i")
                INTERMEDIATE -> {
                    /* This code is never returned for this API. */
                }
                FAIL -> Log.d(TAG, "matchFacesAsync(byte[], byte[]): Failed to compare templates.")
                else -> {
                }
            }
        }
    }

    @Suppress("unused")
    private fun detectFaceSync(bitmap: Bitmap): RectF {

        val res = App.BioManager!!.detectFaceSync(bitmap, syncAPITimeoutMS)

        return if (null != res && OK == res.resultCode) res.faceRect else RectF(0f, 0f, 0f, 0f)
    }

    @Suppress("unused")
    private fun detectFaceSync(bitmapBytes: ByteArray,
                               bitmapWidth: Int,
                               bitmapHeight: Int): RectF {

        val res = App.BioManager!!.detectFaceSync(bitmapBytes,
                bitmapWidth,
                bitmapHeight,
                syncAPITimeoutMS)

        return if (null != res && OK == res.resultCode) res.faceRect else RectF(0f, 0f, 0f, 0f)
    }

    @Suppress("unused")
    private fun analyzeFaceSync(bitmap: Bitmap): FaceData {

        val res = App.BioManager!!.analyzeFaceSync(bitmap, syncAPITimeoutMS)

        return if (null != res && OK == res.resultCode) {
            FaceData(res.faceRect,
                    res.landmark5,
                    res.landmark68,
                    res.headPoseEstimations,
                    res.headPoseDirections,
                    res.gender,
                    res.dominantEmotion,
                    res.age,
                    res.hasGlasses,
                    res.imageQuality)

        } else FaceData()
    }

    @Suppress("unused")
    private fun analyzeFaceSync(bitmapBytes: ByteArray,
                                bitmapWidth: Int,
                                bitmapHeight: Int): FaceData {

        val res = App.BioManager!!.analyzeFaceSync(bitmapBytes,
                bitmapWidth,
                bitmapHeight,
                syncAPITimeoutMS)

        return if (null != res && OK == res.resultCode) {
            FaceData(res.faceRect,
                    res.landmark5,
                    res.landmark68,
                    res.headPoseEstimations,
                    res.headPoseDirections,
                    res.gender,
                    res.dominantEmotion,
                    res.age,
                    res.hasGlasses,
                    res.imageQuality)

        } else FaceData()
    }

    @Suppress("unused")
    private fun createFaceTemplateSync(bitmap: Bitmap): ByteArray {

        val res = App.BioManager!!.createFaceTemplateSync(bitmap, syncAPITimeoutMS)

        return if (null != res && OK == res.resultCode) res.template else byteArrayOf()
    }

    @Suppress("unused")
    private fun createFaceTemplateSync(bitmapBytes: ByteArray,
                                       bitmapWidth: Int,
                                       bitmapHeight: Int): ByteArray {

        val res = App.BioManager!!.createFaceTemplateSync(bitmapBytes,
                bitmapWidth,
                bitmapHeight,
                syncAPITimeoutMS)

        return if (null != res && OK == res.resultCode) res.template else byteArrayOf()
    }

    @Suppress("unused")
    private fun matchFacesSync(templateOne: ByteArray,
                               templateTwo: ByteArray): Int {

        val res = App.BioManager!!.compareFaceSync(templateOne, templateTwo, syncAPITimeoutMS)

        return if (null != res && OK == res.resultCode) res.score else 0
    }

    private inner class FaceData {

        var faceRect = RectF()
        var landmark5: ArrayList<PointF> = ArrayList()
        var landmark68: ArrayList<PointF> = ArrayList()
        var poseEstimations = FloatArray(3)
        var poseDirections: Array<HeadPoseDirection> = Array(3) { HeadPoseDirection.UNKNOWN }
        var gender = FaceEngine.Gender.UNKNOWN
        var age = -1
        var emotion = FaceEngine.Emotion.UNKNOWN
        var glasses = false
        var quality = -1

        constructor()

        constructor(rect: RectF,
                    landmark5: ArrayList<PointF>,
                    landmark68: ArrayList<PointF>,
                    pose: FloatArray,
                    poseDirections: Array<HeadPoseDirection>,
                    gender: Gender,
                    emotion: Emotion,
                    age: Int,
                    glasses: Boolean,
                    quality: Int) {

            this.faceRect = rect
            this.landmark5 = landmark5
            this.landmark68 = landmark68
            this.poseEstimations = pose
            this.poseDirections = poseDirections
            this.gender = gender
            this.age = age
            this.emotion = emotion
            this.glasses = glasses
            this.quality = quality
        }
    }
}
