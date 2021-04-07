package com.credenceid.sdkapp.util;

public class credenceCamera_Constant {

    /*Constants to manage the C-Camera configuration requested*/

    /* Features of the application*/
    public static final String FEATURE = "FEATURE";
    public static final int FEATURE_BARCODE = 0;
    public static final int FEATURE_FACE_LIVENESS = 1;
    public static final int FEATURE_FACE_LIVENESS_AND_TEMPLATE = 2;
    public static final int FEATURE_ICAO = 3;
    public static final int FEATURE_DOCUMENT_MRZ = 4;
    public static final int FEATURE_DOCUMENT_CAPTURE = 5;
    //..

    /*Providers that will be used for the feature*/
    public static final String PROVIDER = "PROVIDER";
    public static final int PROVIDER_CID = 10;
    public static final int PROVIDER_NEUROTECH = 11;
    public static final int PROVIDER_INNOVATRICS = 12;
    public static final int PROVIDER_NFV = 13;
    public static final int PROVIDER_LUNA = 14;
    //...

    public static final String CAMERA_DEVICE = "CAMERA_DEVICE";
    public static final int CAMERA_FRONT = 1;
    public static final int CAMERA_BACK = 0;


    public static final String LIVENESS_THREASHOLD = "LIVENESS_THREASHOLD";
    public static final String LIVENESS_MODE = "LIVENESS_MODE";

    public static final int LIVENESS_MODE_PASSIVE = 0;
    public static final int LIVENESS_MODE_ACTIVE = 1;

    public static final int RESULT_OK = 1;
    public static final int RESULT_FAILED = 0;


}
