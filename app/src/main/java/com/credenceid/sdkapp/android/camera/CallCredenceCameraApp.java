package com.credenceid.sdkapp.android.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.credenceid.sdkapp.util.CredenceCameraAppLivenessResult;

import static com.credenceid.sdkapp.util.credenceCamera_Constant.FEATURE;
import static com.credenceid.sdkapp.util.credenceCamera_Constant.FEATURE_BARCODE;
import static com.credenceid.sdkapp.util.credenceCamera_Constant.FEATURE_FACE_LIVENESS;
import static com.credenceid.sdkapp.util.credenceCamera_Constant.PROVIDER;
import static com.credenceid.sdkapp.util.credenceCamera_Constant.PROVIDER_CID;
import static com.credenceid.sdkapp.util.credenceCamera_Constant.PROVIDER_NEUROTECH;


public class CallCredenceCameraApp extends ActivityResultContract<Integer, CredenceCameraAppLivenessResult> {

    private int mFeature;
    private int mProvider;

    public CallCredenceCameraApp(int feature, int provider){
        mFeature = feature;
        mProvider = provider;
    }

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, @NonNull Integer integer) {

        Intent intent = new Intent(Intent.ACTION_RUN);
        intent.putExtra(FEATURE, mFeature );
        intent.putExtra(PROVIDER, mProvider );
        return intent;
    }

    @Override
    public CredenceCameraAppLivenessResult parseResult(int resultCode, @Nullable Intent result) {
        if (resultCode != Activity.RESULT_OK || result == null) {
            return null;
        }

        int sdkResult = result.getIntExtra("SDK_RESULT", 0);

        int livenessScore = result.getIntExtra("LIVENESS_SCORE", 0);

        Log.d("CID", "SDK_RESULT = " + sdkResult);

        Log.d("CID", "LIVENESS_SCORE = " + livenessScore);

        return new CredenceCameraAppLivenessResult(sdkResult, livenessScore);

    }



}
