package com.cometchat.samplecallsringing

import android.app.Application
import com.cometchat.samplecallsringing.utils.Appearance
import com.cometchat.samplecallsringing.utils.SdkInit

class RingingApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Appearance.apply(this)
        // Every SDK is initialized here, not only on the splash screen: a push can wake a cold process,
        // and Android can bring the app back straight onto Home or a call after the process died.
        // Those screens must find the SDKs ready. Incoming calls are rung by the push SDK.
        SdkInit.credentials(this)?.let { (appId, region) -> SdkInit.init(this, appId, region) }
    }
}
