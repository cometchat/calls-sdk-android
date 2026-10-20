package com.cometchat.samplecallsringing

import android.app.Application
import com.cometchat.samplecallsringing.push.PushNotifications
import com.cometchat.samplecallsringing.utils.AppConstants
import com.cometchat.samplecallsringing.utils.AppUtils

class RingingApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // A push can wake a cold process before the splash screen runs, so the ring is configured here
        // too. Incoming calls are rung by the push SDK, not by a Chat SDK listener.
        val appId = AppConstants.APP_ID.ifEmpty { AppUtils.getAppId(this) }
        val region = AppConstants.REGION.ifEmpty { AppUtils.getRegion(this) }
        if (!appId.isNullOrEmpty() && !region.isNullOrEmpty()) {
            PushNotifications.init(this, appId, region)
        }
    }
}
