package com.credenceid.sdkapp.util;


import android.net.Uri;

public class CredenceCameraAppLivenessResult {

    private int mSdkResult;
    private int mLivenessScore;
    private String mSdkResultMessage;
    private Uri mFaceImageUri;

    public CredenceCameraAppLivenessResult(int cameraAppResult, int livenessScore, String sdkResultMessage, Uri faceImageUri ){

        mSdkResult = cameraAppResult;
        mLivenessScore = livenessScore;
        mSdkResultMessage = sdkResultMessage;
        mFaceImageUri = faceImageUri;

    }

    public int getSdkResult() {
        return mSdkResult;
    }
    public int getLivenessScore() {
        return mLivenessScore;
    }
    public String getSdkResultMessage(){return mSdkResultMessage;}
    public Uri getFaceImageUri(){return mFaceImageUri;}
}
