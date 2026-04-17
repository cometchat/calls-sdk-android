package com.cometchat.samplecallsringing.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.calls.core.CallAppSettings
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException as CallsException
import com.cometchat.chat.core.AppSettings
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException as ChatException
import com.cometchat.samplecallsringing.R
import com.cometchat.samplecallsringing.utils.AppConstants
import com.cometchat.samplecallsringing.utils.AppUtils

/**
 * Splash screen that initializes both Chat SDK and Calls SDK, then routes
 * to the appropriate screen based on credential and login state.
 */
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

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
        Log.d(TAG, "Initializing Chat SDK with appId: $appId, region: $region")

        val appSettings = AppSettings.AppSettingsBuilder()
            .subscribePresenceForAllUsers()
            .setRegion(region)
            .build()

        CometChat.init(applicationContext, appId, appSettings, object : CometChat.CallbackListener<String>() {
            override fun onSuccess(s: String) {
                Log.d(TAG, "Chat SDK initialized successfully")
                initCallsSdk(appId, region)
            }

            override fun onError(e: ChatException) {
                Log.e(TAG, "Chat SDK initialization failed: ${e.message}")
                runOnUiThread {
                    Toast.makeText(
                        this@SplashActivity,
                        "Chat SDK init failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                navigateToAppCredentials()
            }
        })
    }

    private fun initCallsSdk(appId: String, region: String) {
        Log.d(TAG, "Initializing Calls SDK")

        val callAppSettings = CallAppSettings.CallAppSettingBuilder()
            .setAppId(appId)
            .setRegion(region)
            .build()

        CometChatCalls.init(applicationContext, callAppSettings, object : CometChatCalls.CallbackListener<String>() {
            override fun onSuccess(result: String) {
                Log.d(TAG, "Calls SDK initialized successfully")
                navigateAfterInit()
            }

            override fun onError(e: CallsException) {
                Log.e(TAG, "Calls SDK initialization failed: ${e.message}")
                runOnUiThread {
                    Toast.makeText(
                        this@SplashActivity,
                        "Calls SDK init failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                navigateToAppCredentials()
            }
        })
    }

    private fun navigateAfterInit() {
        val destination = if (CometChat.getLoggedInUser() != null) {
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
                AppConstants.REGION.isNotEmpty() &&
                AppConstants.AUTH_KEY.isNotEmpty() &&
                AppConstants.AUTH_KEY != "YOUR_AUTH_KEY"
    }
}
