package com.credenceid.sdkapp.models;

import com.credenceid.biometrics.Biometrics;

public interface PageView {
    void activate(Biometrics biometrics);

    void deactivate();

    String getTitle();

    void doResume();
}
