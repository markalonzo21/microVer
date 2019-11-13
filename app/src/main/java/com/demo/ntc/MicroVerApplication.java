package com.demo.ntc;

import android.app.Application;

import com.microblink.MicroblinkSDK;
import com.microblink.intent.IntentDataTransferMode;

public class MicroVerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MicroblinkSDK.setLicenseFile("MB_com.demo.ntc_BlinkID_Android_2019-12-06.mblic", this);
        MicroblinkSDK.setIntentDataTransferMode(IntentDataTransferMode.PERSISTED_OPTIMISED);

        //        check if blinkid is supported
//        RecognizerCompatibilityStatus status = RecognizerCompatibility.getRecognizerCompatibilityStatus(this);
//        if(status == RecognizerCompatibilityStatus.RECOGNIZER_SUPPORTED)
//        {
//            Toast.makeText(this, "BlinkId is supported", Toast.LENGTH_LONG).show();
//        }
//        else if(status == RecognizerCompatibilityStatus.NO_CAMERA)
//        {
//            Toast.makeText(this, "BlinkID is supported only via Direct API!", Toast.LENGTH_LONG).show();
//        }
//        else
//        {
//            Toast.makeText(this, "BlinkID is not supported! Reason: " + status.name(), Toast.LENGTH_LONG).show();
//        }
    }
}
