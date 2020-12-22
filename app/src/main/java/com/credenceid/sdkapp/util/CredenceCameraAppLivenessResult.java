package com.credenceid.sdkapp.util;


public class CredenceCameraAppLivenessResult {

    private int mSdkResult;
    private int mLivenessScore;

    public CredenceCameraAppLivenessResult(int cameraAppResult, int livenessScore){

        mSdkResult = cameraAppResult;
        mLivenessScore = livenessScore;

    }

    public int getSdkResult() {
        return mSdkResult;
    }
    public int getLivenessScore() {
        return mLivenessScore;
    }
}
