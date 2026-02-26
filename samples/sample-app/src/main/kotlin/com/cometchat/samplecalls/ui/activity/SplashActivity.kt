package com.cometchat.samplecalls.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.calls.core.CallAppSettings
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException
import com.cometchat.samplecalls.R
import com.cometchat.samplecalls.SampleCallsApplication
import com.cometchat.samplecalls.utils.AppConstants
import com.cometchat.samplecalls.utils.AppUtils

/**
 * Splash screen that initializes SDK and routes to appropriate activity.
 */
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Check if we have valid credentials to initialize SDK
        when {
            hasValidAppConstants() -> initSdkAndNavigate(AppConstants.APP_ID, AppConstants.REGION)
            AppUtils.hasCredentials(this) -> {
                val appId = AppUtils.getAppId(this)
                val region = AppUtils.getRegion(this) ?: AppUtils.REGION_US
                if (!appId.isNullOrEmpty()) {
                    initSdkAndNavigate(appId, region)
                } else {
                    navigateToAppCredentials()
                }
            }
            else -> navigateToAppCredentials()
        }
    }

    private fun initSdkAndNavigate(appId: String, region: String) {
        Log.d(TAG, "Initializing SDK with appId: $appId, region: $region")

        val callAppSettings = CallAppSettings.CallAppSettingBuilder()
            .setAppId(appId)
            .setRegion(region)
            .build()

        CometChatCalls.init(
            applicationContext,
            callAppSettings,
            object : CometChatCalls.CallbackListener<String>() {
                override fun onSuccess(result: String) {
                    Log.d(TAG, "CometChat Calls SDK initialized successfully")
                    SampleCallsApplication.isInitialized = true
                    navigateAfterInit()
                }

                override fun onError(e: CometChatException) {
                    Log.e(TAG, "CometChat Calls SDK initialization failed: ${e.message}")
                    runOnUiThread {
                        Toast.makeText(
                            this@SplashActivity,
                            "SDK initialization failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    // Navigate to credentials screen on failure
                    navigateToAppCredentials()
                }
            }
        )
    }

    private fun navigateAfterInit() {
        val destination = if (CometChatCalls.getLoggedInUser() != null) {
            HomeActivity::class.java
        } else {
            LoginActivity::class.java
        }
        startActivity(Intent(this, destination))
        finish()
    }

    private fun navigateToAppCredentials() {
        startActivity(Intent(this, AppCredentialsActivity::class.java))
        finish()
    }

    private fun hasValidAppConstants(): Boolean {
        return AppConstants.APP_ID.isNotEmpty() &&
                AppConstants.APP_ID != "YOUR_APP_ID" &&
                AppConstants.AUTH_KEY.isNotEmpty() &&
                AppConstants.AUTH_KEY != "YOUR_AUTH_KEY"
    }
}
