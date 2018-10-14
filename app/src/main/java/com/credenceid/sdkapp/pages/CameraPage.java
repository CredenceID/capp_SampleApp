package com.credenceid.sdkapp.pages;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
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
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.camera.DrawingView;
import com.android.camera.PreviewFrameLayout;
import com.credenceid.biometrics.Biometrics;
import com.credenceid.biometrics.Biometrics.ResultCode;
import com.credenceid.biometrics.BiometricsManager;
import com.credenceid.sdkapp.CameraUtils;
import com.credenceid.sdkapp.DeviceFamily;
import com.credenceid.sdkapp.DeviceType;
import com.credenceid.sdkapp.R;
import com.credenceid.sdkapp.TheApp;
import com.credenceid.sdkapp.activity.SampleActivity;
import com.credenceid.sdkapp.models.PageView;
import com.credenceid.sdkapp.utils.Beeper;
import com.credenceid.sdkapp.utils.ImageTools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FLASH_MODE_TORCH;
import static android.media.ExifInterface.TAG_ORIENTATION;

public class CameraPage
		extends LinearLayout
		implements PageView, SurfaceHolder.Callback {
	private static final String TAG = CameraPage.class.getSimpleName();

	@SuppressWarnings({"SpellCheckingInspection"})
	private final static int FACEENGINE_PREVIEW_WIDTH = 320;
	@SuppressWarnings({"SpellCheckingInspection"})
	private final static int FACEENGINE_PREVIEW_HEIGHT = 240;

	// Captured face image will be stored to this absolute path on device.
	private final String mCapturedImageFilePath =
			Environment.getExternalStorageDirectory() + File.separator + "face.jpg";

	public Camera mCamera;
	// Surfaces where camera renders preview frames onto.
	private PreviewFrameLayout mPreviewFrameLayout;
	private SurfaceView mScannedImageView;
	private SurfaceHolder mSurfaceHolder;
	// Tap to focus circle and detected face Rect. are drawn to this surface.
	private DrawingView mDrawingView;
	// Either of these Biometric objects may be used for making API calls.
	private Biometrics mBiometrics;
	private BiometricsManager mBiometricsManger;
	// Reference to main activity, used for context, etc.
	private SampleActivity mSampleActivity;
	private ImageButton mCaptureButton;
	private ImageButton mCameraReverseButton;
	private ImageButton mFlashOnButton;
	private ImageButton mFlashOffButton;
	private TextView mStatusTextView;

	private Bitmap mFinalBitmap = null;

	private boolean mInPreview = false;
	private boolean mIsLive = false;
	private boolean mIsFrontCameraEnabled = false;
	private boolean mUseFlash = false;

	private Camera.PictureCallback mOnPictureTakenCallback = new Camera.PictureCallback() {
		public void onPictureTaken(byte[] data, Camera cam) {
			Log.d(TAG, "Camera.PictureCallback");

			Beeper.getInstance().click();

			mIsLive = false;
			mCaptureButton.setImageResource(R.drawable.button_recapture);

			runFaceOperation(data);
		}
	};

	private Camera.PreviewCallback mCameraPreviewCallback = new Camera.PreviewCallback() {
		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			Log.d(TAG, "onPreviewFrame()");

			detectFace(data);

			//camera.addCallbackBuffer(mCameraPreviewBuffer);
		}
	};

	private Camera.AutoFocusCallback myAutoFocusCallback = new Camera.AutoFocusCallback() {
		public void onAutoFocus(boolean autoFocusSuccess, Camera arg1) {
			Log.i("App", "Auto focus called success...........");

			mStatusTextView.setText("");
			setControlButtonVisibility(true);

			mDrawingView.setHaveTouch(false, new Rect(0, 0, 0, 0));
			mDrawingView.invalidate();
		}
	};

	private boolean mIsCameraConfigured = false;
	private boolean isRightSizeButtonVisible = false;

	public CameraPage(Context context) {
		super(context);

		this.initialize();
	}

	public CameraPage(Context context,
					  AttributeSet attrs) {
		super(context, attrs);

		this.initialize();
	}

	public CameraPage(Context context,
					  AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		this.initialize();
	}

	private void
	initialize() {
		LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		//noinspection ConstantConditions
		layoutInflater.inflate(R.layout.page_camera, this, true);

		this.initializeLayoutComponents();
		this.configureLayoutComponents();

		this.resetInternal();
		this.updateFlashButtons();
		this.setStatusText("");
	}

	private void
	initializeLayoutComponents() {
		mPreviewFrameLayout = (PreviewFrameLayout) findViewById(R.id.preview_frame_layout);
		mScannedImageView = (SurfaceView) findViewById(R.id.scanned_image);
		mDrawingView = (DrawingView) findViewById(R.id.drawing_view);

		mStatusTextView = (TextView) findViewById(R.id.status_view);
		mCaptureButton = (ImageButton) findViewById(R.id.scan_button);
		mFlashOnButton = (ImageButton) findViewById(R.id.flash_on_button);
		mFlashOffButton = (ImageButton) findViewById(R.id.flash_off_button);
		mCameraReverseButton = (ImageButton) findViewById(R.id.camera_flip_button);
	}

	private void
	configureLayoutComponents() {
		this.setFlashButtonVisibility(true);

		mPreviewFrameLayout.setVisibility(VISIBLE);
		mDrawingView.setVisibility(VISIBLE);
		mScannedImageView.setVisibility(VISIBLE);

		mSurfaceHolder = mScannedImageView.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		mCameraReverseButton.setVisibility(VISIBLE);
		mCameraReverseButton.setImageResource(R.drawable.icon_camera_flip);
//		mCameraReverseButton.setOnClickListener((View v) -> {
//			onClickReverseCamera();
//			mIsLive = true;
//		});

		mCaptureButton.setOnClickListener((View v) -> {
			if (!mIsLive) {
				this.resetInternal();
				this.doPreview();

				mIsLive = true;
				mCaptureButton.setImageResource(R.drawable.button_capture);
			} else if (mCamera != null)
				doCapture();
		});

		mFlashOnButton.setOnClickListener((View v) -> this.setFlashMode(true));
		mFlashOffButton.setOnClickListener((View v) -> this.setFlashMode(false));
	}

	public void
	setActivity(SampleActivity activity) {
		mSampleActivity = activity;
	}

	@Override
	public void
	activate(Biometrics biometrics) {
		Log.d(TAG, "activate(Biometrics)");

		mBiometrics = biometrics;
		mBiometricsManger = TheApp.getInstance().getBiometricsManager();

		mPreviewFrameLayout.setVisibility(VISIBLE);
		mDrawingView.setVisibility(VISIBLE);
		mScannedImageView.setVisibility(VISIBLE);
		mIsFrontCameraEnabled = false;

		this.setFlashButtonVisibility(true);
		this.updateFlashButtons();
		this.setStatusText("");
		this.resetInternal();
		this.doPreview();
	}

	@Override
	public void
	deactivate() {
		if (mCamera != null) {
			this.setFlashMode(false);

			if (mInPreview)
				mCamera.stopPreview();
			mCamera.release();
			mCamera = null;

			mInPreview = false;
			Log.d(TAG, "deactivate: ->>>Camera - null + inPreview = false");
		}

		Log.d(TAG, "deactivate: ->>>Camera - Deactivate is Called");
		this.resetMisc();
		mSurfaceHolder.removeCallback(this);
		this.surfaceDestroyed(mSurfaceHolder);
	}

	@Override
	public void
	doResume() {
		Log.i(TAG, "Camera onResume");
		new Thread(new Runnable() {
			public void run() {
				try {
					// Add a slight delay to avoid "app passed NULL surface" error
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mSampleActivity.runOnUiThread(new Runnable() {
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
	public void
	surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.d(TAG, "surfaceChanged(W: )" + width + ", H: " + height + ")");

		if (mCamera == null) {
			Log.w(TAG, "Camera object is NULL, will not set up preview.");
			return;
		}

		this.initPreview(width, height);
		this.startPreview();

		Log.i(TAG, "Zoom " + mCamera.getParameters().getZoom()
				+ " Focus " + mCamera.getParameters().getFocusMode()
				+ " Scene mode " + mCamera.getParameters().getSceneMode()
				+ " Exposure " + mCamera.getParameters().getExposureCompensation()
				+ " Preview format " + mCamera.getParameters().getPreviewFormat()
				+ " Preview width " + mCamera.getParameters().getPreviewSize().width
				+ " Preview height " + mCamera.getParameters().getPreviewSize().height
				+ " Zoom Ratio " + mCamera.getParameters().getZoomRatios().get(0)
				+ " Focus Mode " + mCamera.getParameters().getFocusMode());
	}

	@Override
	public void
	surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated.");
	}

	@Override
	public void
	surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "surfaceDestroyed(SurfaceHolder)");

		if (mCamera != null) {
			if (mInPreview)
				mCamera.stopPreview();

			mCamera.release();
			mCamera = null;
			 mInPreview = false;
		}
	}

	private void
	initPreview(int width,
				int height) {
		Log.d(TAG, "initPreview(int, int)");

		if (mCamera == null || mSurfaceHolder.getSurface() == null) {
			Log.d(TAG, "Either camera or SurfaceHolder was null, skip initPreview()");
			return;
		}

		if (mIsCameraConfigured) {
			Log.d(TAG, "camera was already configured, no need now");
			return;
		}

		try {
			// Tell camera object where to display preview frames.
			mCamera.setPreviewDisplay(mSurfaceHolder);
			// Initialize camera preview in proper orientation.
			this.setCameraDisplayOrientation();

			// Get camera parameters. We will edit these, then write them back to camera.
			Camera.Parameters parameters = mCamera.getParameters();

			// Enable auto-focus if available.
			List<String> focusModes = parameters.getSupportedFocusModes();
			if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
				parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

			Camera.Size size;
			if (TheApp.getDeviceFamily() == DeviceFamily.CTAB)
				size = CameraUtils.getOptimalPreviewSize(width, height, parameters);
			else size = CameraUtils.getBestPreviewSize(width, height, parameters);

			if (size == null) {
				Log.w(TAG, "Unable to determine best preview size for camera.");
				return;
			}

			size.width = FACEENGINE_PREVIEW_WIDTH;
			size.height = FACEENGINE_PREVIEW_HEIGHT;

			if (TheApp.getDeviceFamily() == DeviceFamily.TRIDENT) {
				mPreviewFrameLayout.setAspectRatio((size.height) / (double) (size.width));
				parameters.setZoom(0);
				// TODO: Drawing view width and height.
			} else if (TheApp.getDeviceFamily() == DeviceFamily.CTAB) {
				Log.d(TAG, "Configuring for C-TAB.");
				mPreviewFrameLayout.setAspectRatio((size.width) / (double) (size.height));

				// For FaceEngine we show a preview with 320x240, but the actual image is
				// captured with largest available picture size, this way we get a high
				// resolution in final image.
				Camera.Size picSize = CameraUtils.getLargestPictureSize(parameters);
				parameters.setPictureSize(picSize.width, picSize.height);

				ViewGroup.LayoutParams params = mDrawingView.getLayoutParams();
				params.width = (int) (size.width * 2.5);
				params.height = (int) (size.height * 2.5);
				mDrawingView.setLayoutParams(params);

				// We need to set FaceEngine specific bitmap size so DrawingView knows
				// where and how to draw face detection points. Otherwise it would
				// assume the bitmap size is 0.
				mDrawingView.faceEngineSetBitmapDimensions(size.width, size.height);
			} else if (TheApp.getDeviceFamily() == DeviceFamily.CTWO) {
				Log.d(TAG, "Configuring for C-TWO family");

				mPreviewFrameLayout.getLayoutParams().width = (int) (size.height * 2.5); //4.25);
				mPreviewFrameLayout.getLayoutParams().height = (int) (size.width * 2.5); //4.25);
				mPreviewFrameLayout.setAspectRatio(size.width / (double) size.height);

				ViewGroup.LayoutParams prevParams = mPreviewFrameLayout.getLayoutParams();
				ViewGroup.LayoutParams params = mDrawingView.getLayoutParams();

				params.width = prevParams.width;
				params.height = prevParams.height;
				mDrawingView.setLayoutParams(params);

				// We need to set FaceEngine specific bitmap size so DrawingView knows
				// where and how to draw face detection points. Otherwise it would
				// assume the bitmap size is 0.
				mDrawingView.faceEngineSetBitmapDimensions(FACEENGINE_PREVIEW_WIDTH,
						FACEENGINE_PREVIEW_HEIGHT);

			} else if (TheApp.getDeviceFamily() == DeviceFamily.CONE)
				mPreviewFrameLayout.setAspectRatio(size.width / (double) size.height);
			double aspect = (size.width / (double) size.height);

			Log.i(TAG, "aspect ratio " + aspect);

			mCamera.setParameters(parameters);
			mIsCameraConfigured = true;
		} catch (Throwable t) {
			Log.e("PreviewDemo-Callback", "Exception in setPreviewDisplay()", t);
		}
	}

	private void
	doPreview() {
		Log.d(TAG, "doPreview()");

		try {
			if (mCamera == null) {
				if (TheApp.getDeviceFamily() == DeviceFamily.CTAB && mIsFrontCameraEnabled) {
					Log.d(TAG, "Opening front camera.");
					mCamera = CameraUtils.openFrontFacingCamera();
				} else {
					Log.d(TAG, "Opening back camera.");
					mCamera = Camera.open();
				}

				setPreviewSize(FACEENGINE_PREVIEW_WIDTH,
						FACEENGINE_PREVIEW_HEIGHT,
						(double) FACEENGINE_PREVIEW_WIDTH / FACEENGINE_PREVIEW_HEIGHT);
			}

			if (mCamera != null) {
				Log.d(TAG, "Camera opened, setting preview buffers, surfaces, etc.");
				mCamera.setPreviewDisplay(mSurfaceHolder);
				this.setCameraDisplayOrientation();

//				mCamera.addCallbackBuffer(mCameraPreviewBuffer);
//				mCamera.setPreviewCallbackWithBuffer(mCameraPreviewCallback);

				mCamera.setPreviewCallback(mCameraPreviewCallback);

				this.startPreview();
				mInPreview = true;
			}
		} catch (Exception e) {
			Log.e(TAG, "Failed to start preview: " + e.getLocalizedMessage());
			if (mCamera != null)
				mCamera.release();

			mCamera = null;
			mInPreview = false;
		}
	}

	private void
	setPreviewSize(int width, int height, double ratio) {
		Camera.Parameters parameters = mCamera.getParameters();
		parameters.setPreviewSize(width, height);

		//set picture size for mCamera
		//parameters.setPictureSize(width, height);

		mPreviewFrameLayout.setAspectRatio(ratio);
		mCamera.setParameters(parameters);
	}

	private void
	startPreview() {
		Log.d(TAG, "startPreview()");

		if (mIsCameraConfigured && mCamera != null) {
			mStatusTextView.setText("");
			mPreviewFrameLayout.setVisibility(VISIBLE);
			mDrawingView.setVisibility(VISIBLE);
			mScannedImageView.setVisibility(VISIBLE);

			mCamera.startPreview();

			mInPreview = true;
			mCaptureButton.setImageResource(R.drawable.button_capture);
			mCaptureButton.setVisibility(VISIBLE);
		} else Log.w(TAG, "Camera not configured, aborting start preview.");
	}

	private void
	setCameraDisplayOrientation() {
		if (mBiometrics.getProductName().equals(getResources().getString(R.string.trident1_product_name)))
			mCamera.setDisplayOrientation(0);
		else if (mBiometrics.getProductName().equals(getResources().getString(R.string.trident2_product_name)))
			mCamera.setDisplayOrientation(0);
		else if (mBiometrics.getProductName().equals(getResources().getString(R.string.credencetab_v1_product_name))
				|| mBiometrics.getProductName().equals(getResources().getString(R.string.credencetab_v2_product_name))
				|| mBiometrics.getProductName().equals(getResources().getString(R.string.credencetab_v3_product_name))
				|| mBiometrics.getProductName().equals(getResources().getString(R.string.credencetab_v4_product_name))) {
			if (mIsFrontCameraEnabled) {
				mCamera.setDisplayOrientation(180);
			} else mCamera.setDisplayOrientation(0);
		} else if (mBiometrics.getProductName().equals(getResources().getString(R.string.credence_two_v1_product_name))
				|| mBiometrics.getProductName().equals(getResources().getString(R.string.credence_two_v2_product_name)))
			mCamera.setDisplayOrientation(90);
		else mCamera.setDisplayOrientation(90);
	}

	@Override
	public String getTitle() {
		return getContext().getResources().getString(R.string.title_face_camera);
	}

	public void resetInternal() {
		Log.d(TAG, "resetInternal()");

		mInPreview = true;

		if (mFinalBitmap != null)
			mFinalBitmap.recycle();
		mFinalBitmap = null;

		mPreviewFrameLayout.setVisibility(VISIBLE);
		mDrawingView.setVisibility(VISIBLE);
		mScannedImageView.setVisibility(VISIBLE);

		mCaptureButton.setImageResource(R.drawable.button_capture);

		this.setControlButtonVisibility(true);
	}

	/*
	 * Auto focus by touching
	 */
	public void
	touchFocus(final Rect tfocusRect) {
		if (!mInPreview) return;

		setControlButtonVisibility(false);
		setStatusText("Autofocusing, please wait...");

		final int one = 2000, two = 1000;
		// Here we properly bound our Rect for a better tap to focus region
		final Rect targetFocusRect = new Rect(
				tfocusRect.left * one / mDrawingView.getWidth() - two,
				tfocusRect.top * one / mDrawingView.getHeight() - two,
				tfocusRect.right * one / mDrawingView.getWidth() - two,
				tfocusRect.bottom * one / mDrawingView.getHeight() - two);

		// Since the mCamera parameters only accept a List of mCamera areas to focus, we create a list
		final List<Camera.Area> focusList = new ArrayList<Camera.Area>();
		// Here we take the Graphics.Rect and convert it to a Camera.Rect for the mCamera to understand
		Camera.Area focusArea = new Camera.Area(targetFocusRect, 1000);
		// Add our custom Rect to focus into our list
		focusList.add(focusArea);

		if (!mBiometrics.getProductName().equals(getResources().getString(R.string.trident2_product_name))
				&& !mBiometrics.getProductName().equals(getResources().getString(R.string.credencetab_v1_product_name))
				&& !mBiometrics.getProductName().equals(getResources().getString(R.string.credencetab_v2_product_name))
				&& !mBiometrics.getProductName().equals(getResources().getString(R.string.credencetab_v3_product_name))
				&& !mBiometrics.getProductName().equals(getResources().getString(R.string.credencetab_v4_product_name))
				&& !mBiometrics.getProductName().equals(getResources().getString(R.string.credence_two_v1_product_name))
				&& !mBiometrics.getProductName().equals(getResources().getString(R.string.credence_two_v2_product_name))) {
			Camera.Parameters para = mCamera.getParameters();
			para.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			para.setFocusAreas(focusList);
			para.setMeteringAreas(focusList);
			mCamera.setParameters(para);
		}

		// Call mCamera AutoFocus & pass callback to be called when auto focus finishes
		mCamera.autoFocus(myAutoFocusCallback);
		// Tell our drawing view we have a touch in the given Rect
		mDrawingView.setHaveTouch(true, tfocusRect);
		// Tell our drawing view to Update
		mDrawingView.invalidate();
	}

	private void
	setControlButtonVisibility(boolean visibility) {
		mCaptureButton.setVisibility(visibility ? VISIBLE : INVISIBLE);
	}

	/* captures image, before capturing image it will set proper picture orientation. */
	private void
	doCapture() {
		this.resetInternal();

		Camera.Parameters parameters = mCamera.getParameters();

		if (TheApp.getDeviceType() == DeviceType.TRIDENT_1)
			parameters.setRotation(270);
		else if (TheApp.getDeviceType() == DeviceType.TRIDENT_2)
			parameters.setRotation(180);
		else if (TheApp.getDeviceFamily() == DeviceFamily.CONE)
			parameters.setRotation(0);
		else if (TheApp.getDeviceFamily() == DeviceFamily.CTAB)
			parameters.setRotation(mIsFrontCameraEnabled ? 180 : 0);

		mCamera.setParameters(parameters);

		if (mCamera != null) {
			this.setControlButtonVisibility(false);
			this.setStatusText("Starting capture, hold still...");

			mInPreview = false;
			mCamera.takePicture(null, null, null, mOnPictureTakenCallback);
		}
	}

	private void
	setStatusText(String text) {
		mStatusTextView.setText(text);
	}

	private void
	flashControl(boolean useFlash) {
		Log.d(TAG, "flashControl(" + useFlash + ")");

		if (mCamera == null)
			return;

		DeviceFamily deviceFamily = TheApp.getDeviceFamily();
		if (deviceFamily == DeviceFamily.CTAB || deviceFamily == DeviceFamily.TRIDENT)
			mBiometrics.cameraFlashControl(useFlash);
		else {
			try {
				Camera.Parameters p = mCamera.getParameters();
				p.setFlashMode(useFlash ? FLASH_MODE_TORCH : FLASH_MODE_OFF);
				mCamera.setParameters(p);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void
	setFlashMode(boolean useFlash) {
		mUseFlash = useFlash;
		flashControl(useFlash);
		updateFlashButtons();
	}

	private void
	updateFlashButtons() {
		mFlashOnButton.setActivated(mUseFlash);
		mFlashOffButton.setActivated(!mUseFlash);
	}

	private void
	setFlashButtonVisibility(boolean visibility) {
		mFlashOnButton.setVisibility(visibility ? VISIBLE : INVISIBLE);
		mFlashOffButton.setVisibility(visibility ? VISIBLE : INVISIBLE);
	}

	private void
	resetMisc() {
		mInPreview = true;

		if (mFinalBitmap != null)
			mFinalBitmap.recycle();
		mFinalBitmap = null;

		mPreviewFrameLayout.setVisibility(VISIBLE);
		mDrawingView.setVisibility(VISIBLE);
		mScannedImageView.setVisibility(VISIBLE);

		this.setControlButtonVisibility(true);

		mCameraReverseButton.setVisibility(INVISIBLE);
		mCaptureButton.setImageResource(R.drawable.button_capture);
	}

	private void
	doFaceOperation(byte[] imageBytes) {

	}

	public void
	createFaceTemplate(Bitmap bitmap) {
		mBiometricsManger.createFaceTemplate(bitmap, (ResultCode resultCode, byte[] bytes) -> {
			if (resultCode == ResultCode.OK) {
				Log.i(TAG, "Face template created.");
				Log.i(TAG, "Length: " + bytes.length);

				try {
					Log.i(TAG, "Template data: " + new String(bytes, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					// Ignored.
				}

				mBiometricsManger.matchFaceTemplates(bytes, bytes,
						(ResultCode resultCodeTwo, int i) -> {
							if (resultCodeTwo == ResultCode.OK) {
								Log.i(TAG, "MATCH SCORE: " + i);
							}
						});
			}
		});
	}

	/* Saves given image to disk. Uses default path "mCapturedImageFilePath".
	 *
	 * @param imageData Bitmap image in bytes to save to disk.
	 */
	@SuppressLint({"StaticFieldLeak"})
	private void runFaceOperation(byte[] imageData) {
		this.setStatusText("Image Captured");

		new AsyncTask<byte[], String, String>() {
			File photo;

			@SuppressWarnings("ResultOfMethodCallIgnored")
			@Override
			protected String doInBackground(byte[]... jpeg) {
				mSampleActivity.runOnUiThread(() -> setStatusText("Saving image, please wait..."));

				photo = getOutputMediaFile();
				if (photo.exists())
					photo.delete();

				Bitmap realImage;

				if (Build.MODEL.equals("CID")) {
					realImage = BitmapFactory.decodeByteArray(jpeg[0], 0, jpeg[0].length);
				} else realImage = getProperOrientation(photo.getPath(), jpeg[0]);

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

				mSampleActivity.runOnUiThread(() -> {
					setStatusText("");
					setControlButtonVisibility(true);
				});
			}

			private File getOutputMediaFile() {
				File f;
				f = new File(mCapturedImageFilePath);
				Log.d(TAG, "Image file saved to " + f.getName());

				return f;
			}

		}.execute(imageData);
	}

	/* Corrects image orientation based on CredenceDeviceType. Certain device's have their physical
	 * camera hardware at a different angle, such as 90/180 degrees. Once an image is captured, the
	 * orientation needs to be fixed.
	 *
	 * @param filename Absolute path of image, used for reading image information.
	 * @param image Image to rotate.
	 * @return Corrected image.
	 */
	@SuppressWarnings("ConstantConditions")
	private Bitmap
	getProperOrientation(String filename,
						 byte[] image) {
		Bitmap realImage = BitmapFactory.decodeByteArray(image, 0, image.length);

		ExifInterface exif = null;
		try {
			exif = new ExifInterface(filename);
		} catch (IOException e) {
			e.printStackTrace();
		}

		int rotation = 0;
		final String productName = mBiometrics.getProductName();

		if ((productName.equals(getResources().getString(R.string.credencetab_v1_product_name))
				|| productName.equals(getResources().getString(R.string.credencetab_v2_product_name))
				|| productName.equals(getResources().getString(R.string.credencetab_v3_product_name))
				|| productName.equals(getResources().getString(R.string.credencetab_v4_product_name)))
				&& mIsFrontCameraEnabled) {
			rotation = 180;
		} else if (exif.getAttribute(TAG_ORIENTATION).equalsIgnoreCase("6"))
			rotation = 90;
		else if (exif.getAttribute(TAG_ORIENTATION).equalsIgnoreCase("8"))
			rotation = 270;
		else if (exif.getAttribute(TAG_ORIENTATION).equalsIgnoreCase("3"))
			rotation = 180;

		return ImageTools.Editor.Rotate(realImage, rotation);
	}

	/* Attempts to detect face Rect. region in given image.
	 *
	 * @param bitmapBytes Bitmap image in byte format to run detection on.
	 */
	private void
	detectFace(byte[] bitmapBytes) {
		// If camera was closed, immediately after a preview callback exit out, this is to prevent
		// NULL pointer exceptions when using the camera object later on.
		if (mCamera == null || bitmapBytes == null)
			return;

		// We need to stop camera preview callbacks from continuously being invoked while processing
		// is going on. Otherwise we would have a backlog of frames needing to be processed. To fix
		// this we remove preview callback, then re-enable it post-processing.
		//
		// - Preview callback invoked.
		// -- Tell camera to sto preview callbacks.
		// **** Meanwhile camera is still receiving frames, but continues to draw them. ****
		// -- Process camera preview frame.
		// -- Draw detected face Rect.
		// -- Tell camera to invoke preview callback with next frame.
		//
		// Using this technique does not drop camera frame-rate, so camera does not look "laggy".
		// Instead now we use every 5-th frame for face detection.
		//mCamera.setPreviewCallbackWithBuffer(null);

		mCamera.setPreviewCallback(null);

		// Need to fix color format of raw camera preview frames.
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		Rect rect = new Rect(0, 0, 320, 240);
		YuvImage yuvimage = new YuvImage(bitmapBytes, ImageFormat.NV21, 320, 240, null);
		yuvimage.compressToJpeg(rect, 100, outStream);
		// Save fixed color image as final good Bitmap.
		mFinalBitmap = BitmapFactory.decodeByteArray(outStream.toByteArray(), 0, outStream.size());

//		if (VIBEApplication.isDeviceOfType(CREDENCE_TWO_FAMILY)) {
//			mFinalBitmap = ImageTools.Editor.Rotate(mFinalBitmap, 90);
//		}

		// Detect face on finalized Bitmap image.
		mBiometricsManger.detectFace(mFinalBitmap, (Biometrics.ResultCode resultCode,
													RectF rectF) -> {
			// If camera was closed or preview stopped, immediately exit out. This is done so that
			// we do not continue to process invalid frames, or draw to NULL surfaces.
			if (mCamera == null || !mInPreview)
				return;

			// Tell camera to start preview callbacks again.
			mCamera.setPreviewCallback(mCameraPreviewCallback);

			if (resultCode == Biometrics.ResultCode.OK) {
				// Tell view that it will need to draw a detected face's Rect. region.
				mDrawingView.faceEngineSetHasFace(true);

				if (TheApp.getDeviceFamily() == DeviceFamily.CTWO) {
					mDrawingView.faceEngineSetFaceRect(rectF.left + 40,
							rectF.top - 50,
							rectF.right + 40,
							rectF.bottom - 50);
				} else {
					mDrawingView.faceEngineSetFaceRect(rectF.left,
							rectF.top,
							rectF.right,
							rectF.bottom);
				}
			} else {
				// Tell view to not draw face Rect. region on next "onDraw()" call.
				mDrawingView.faceEngineSetHasFace(false);
			}

			// Tell view to invoke an "onDraw()".
			mDrawingView.invalidate();
		});
	}
}
