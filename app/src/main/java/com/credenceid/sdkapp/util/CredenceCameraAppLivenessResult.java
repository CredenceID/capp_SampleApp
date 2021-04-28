package com.credenceid.sdkapp.util;


import android.graphics.Bitmap;
import android.net.Uri;

public class CredenceCameraAppLivenessResult {

    private int mSdkResult;
    private int mLivenessScore;
    private String mSdkResultMessage;
    private Uri mResultImageUri;

    public CredenceCameraAppLivenessResult(int cameraAppResult, int livenessScore, String sdkResultMessage, Uri resultImageUri ){

        mSdkResult = cameraAppResult;
        mLivenessScore = livenessScore;
        mSdkResultMessage = sdkResultMessage;
        mResultImageUri = resultImageUri;

    }

    public int getSdkResult() {
        return mSdkResult;
    }
    public int getLivenessScore() {
        return mLivenessScore;
    }
    public String getSdkResultMessage(){return mSdkResultMessage;}
    public Uri getResultImage(){return mResultImageUri;}
}
