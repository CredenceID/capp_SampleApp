package com.credenceid.sdkapp.android.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.credenceid.sdkapp.util.CredenceCameraAppLivenessResult;

import java.io.File;
import java.io.FileOutputStream;

import static com.credenceid.sdkapp.util.credenceCamera_Constant.CAMERA_DEVICE;
import static com.credenceid.sdkapp.util.credenceCamera_Constant.FEATURE;
import static com.credenceid.sdkapp.util.credenceCamera_Constant.FEATURE_BARCODE;
import static com.credenceid.sdkapp.util.credenceCamera_Constant.FEATURE_FACE_LIVENESS;
import static com.credenceid.sdkapp.util.credenceCamera_Constant.LIVENESS_MODE;
import static com.credenceid.sdkapp.util.credenceCamera_Constant.PROVIDER;
import static com.credenceid.sdkapp.util.credenceCamera_Constant.PROVIDER_CID;
import static com.credenceid.sdkapp.util.credenceCamera_Constant.PROVIDER_NEUROTECH;


public class CallCredenceCameraApp extends ActivityResultContract<Integer, CredenceCameraAppLivenessResult> {

    private int mFeature;
    private int mProvider;
    private int mCameraDevice;

    public CallCredenceCameraApp(int feature, int provider, int cameraDevice){
        mFeature = feature;
        mProvider = provider;
        mCameraDevice = cameraDevice;
    }

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, @NonNull Integer integer) {

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
        Uri faceImageUri = null;
//        if (result.getStringExtra("IMAGE_URI") != null) {
            Log.d("IMAGE_URI", result.getStringExtra("IMAGE_URI"));
            faceImageUri = Uri.parse(result.getStringExtra("IMAGE_URI"));

//        }

        Log.d("CID", "SDK_RESULT = " + sdkResult);
        Log.d("CID", "LIVENESS_SCORE = " + livenessScore);
        Log.d("CID", "SDK_RESULT_MESSAGE = " + sdkResultMessage);
        if(null != faceImageUri)
            Log.d("CID", "FACE_IMAGE_URI = " + faceImageUri.getEncodedPath());

        return new CredenceCameraAppLivenessResult(sdkResult, livenessScore, sdkResultMessage, faceImageUri);

    }

}
