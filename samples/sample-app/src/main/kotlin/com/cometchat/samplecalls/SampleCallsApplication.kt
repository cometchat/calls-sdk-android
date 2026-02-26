package com.cometchat.samplecalls

import android.app.Application

/**
 * Application class for Sample Calls app.
 * SDK initialization is handled in SplashActivity.
 */
class SampleCallsApplication : Application() {

    companion object {
        @Volatile
        var isInitialized = false
    }

    override fun onCreate() {
        super.onCreate()
    }
}
