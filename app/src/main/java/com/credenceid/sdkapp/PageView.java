package com.credenceid.sdkapp;

import com.credenceid.biometrics.Biometrics;

public interface PageView {
	public void activate(Biometrics biometrics);

	public void deactivate();

	public String getTitle();

	public void doResume();
}
