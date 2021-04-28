package com.credenceid.sdkapp.android.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.credenceid.sdkapp.MainActivity;
import com.credenceid.sdkapp.util.CredenceCameraAppLivenessResult;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static com.credenceid.sdkapp.util.credenceCamera_Constant.CAMERA_DEVICE;
import static com.credenceid.sdkapp.util.credenceCamera_Constant.FEATURE;
import static com.credenceid.sdkapp.util.credenceCamera_Constant.FEATURE_BARCODE;
import static com.credenceid.sdkapp.util.credenceCamera_Constant.FEATURE_FACE_LIVENESS;
import static com.credenceid.sdkapp.util.credenceCamera_Constant.LIVENESS_MODE;
import static com.credenceid.sdkapp.util.credenceCamera_Constant.PROVIDER;
import static com.credenceid.sdkapp.util.credenceCamera_Constant.PROVIDER_CID;


public class CallCredenceCameraApp extends ActivityResultContract<Integer, CredenceCameraAppLivenessResult> {

    private static final String TAG = "CID-Sample";
    private int mFeature;
    private int mProvider;
    private int mCameraDevice;
    private Context mCtx;

    public CallCredenceCameraApp(int feature, int provider, int cameraDevice){
        mFeature = feature;
        mProvider = provider;
        mCameraDevice = cameraDevice;
    }

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, @NonNull Integer integer) {

        mCtx = context;
        Intent intent = new Intent(Intent.ACTION_RUN);
        intent.putExtra(FEATURE, mFeature );
        intent.putExtra(PROVIDER, mProvider );
        intent.putExtra(CAMERA_DEVICE, mCameraDevice);
        return intent;
    }

    @Override
    public CredenceCameraAppLivenessResult parseResult(int resultCode, @Nullable Intent result) {
        if (resultCode != Activity.RESULT_OK || result == null) {
            return null;
        }

        int sdkResult = result.getIntExtra("SDK_RESULT", 0);
        int livenessScore = result.getIntExtra("LIVENESS_SCORE", 0);
        String sdkResultMessage = result.getStringExtra("SDK_RESULT_MESSAGE");
        Uri resultImageUri = null;
        try {
            resultImageUri = result.getData();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "ERROR: " + e.toString());
        }

        Log.d("CID", "SDK_RESULT = " + sdkResult);
        Log.d("CID", "LIVENESS_SCORE = " + livenessScore);
        Log.d("CID", "SDK_RESULT_MESSAGE = " + sdkResultMessage);
        if(null != resultImageUri)
            Log.d("CID", "RESULT IMAGE SIZE = " + resultImageUri.getEncodedPath());

        return new CredenceCameraAppLivenessResult(sdkResult, livenessScore, sdkResultMessage, resultImageUri);

    }

}
